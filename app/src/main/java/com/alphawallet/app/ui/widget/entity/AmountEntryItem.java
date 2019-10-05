package com.alphawallet.app.ui.widget.entity;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import com.alphawallet.app.util.BalanceUtils;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.AmountUpdateCallback;
import com.alphawallet.app.entity.Ticker;
import com.alphawallet.app.repository.TokenRepositoryType;

import java.util.concurrent.TimeUnit;

import static com.alphawallet.token.tools.Convert.getEthString;
import static com.alphawallet.app.ui.ImportTokenActivity.getUsdString;

/**
 * Created by James on 25/02/2019.
 * Stormbird in Singapore
 */
public class AmountEntryItem
{
    private static final long CHECK_ETHPRICE_INTERVAL = 10;

    private TextView amountError;
    private AutoCompleteTextView amountEditText;
    private RelativeLayout amountLayout;
    private ImageButton switchBtn;
    private ImageButton quantityUpBtn;
    private ImageButton quantityDownBtn;
    private TextView usdLabel;
    private TextView tokenSymbolLabel;
    private TextView usdValue;
    private boolean usdInput = false;
    private final boolean hasRealValue;
    private final int chainId;

    private LinearLayout tokenEquivalentLayout;
    private TextView tokenEquivalent;
    private TextView tokenEquivalentSymbol;

    private TokenRepositoryType tokenRepository;

    private Double currentEthPrice;

    private Disposable disposable;

    private AmountUpdateCallback callback;

    public void onClear()
    {
        if (disposable != null && !disposable.isDisposed()) disposable.dispose();
    }

    public AmountEntryItem(Activity activity, TokenRepositoryType tokenRepository, String symbol, boolean isEth, int chainId, boolean hasRealValue)
    {
        currentEthPrice = 0.0;
        this.tokenRepository = tokenRepository;
        this.callback = (AmountUpdateCallback)activity;
        amountError = activity.findViewById(R.id.amount_error);
        this.chainId = chainId;
        this.hasRealValue = hasRealValue;

        amountEditText = activity.findViewById(R.id.edit_amount);
        amountEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                amountError.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
                try
                {
                    updateEquivalentValue();
                }
                catch (NumberFormatException e)
                {
                    //
                }
            }
        });

        amountLayout = activity.findViewById(R.id.layout_amount);
        amountLayout.setOnClickListener(v -> {
            amountEditText.requestFocus();
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(amountEditText, InputMethodManager.SHOW_IMPLICIT);
        });

        usdLabel = activity.findViewById(R.id.amount_edit_usd_symbol);

        tokenSymbolLabel = activity.findViewById(R.id.amount_edit_token_symbol);
        tokenSymbolLabel.setText(symbol);

        tokenEquivalentLayout = activity.findViewById(R.id.layout_token_equivalent_value);
        tokenEquivalent = activity.findViewById(R.id.text_token_value);
        tokenEquivalent.setText("0 ");
        tokenEquivalentSymbol = activity.findViewById(R.id.text_token_symbol);
        tokenEquivalentSymbol.setText(symbol);
        usdValue = activity.findViewById(R.id.text_usd_value);

        switchBtn = activity.findViewById(R.id.img_switch_usd_eth);
        switchBtn.setOnClickListener(v -> {
            if (usdInput) {
                usdInput = false;
                usdLabel.setVisibility(View.GONE);
                usdValue.setVisibility(View.VISIBLE);
                tokenSymbolLabel.setVisibility(View.VISIBLE);
                tokenEquivalentLayout.setVisibility(View.GONE);
            } else {
                usdInput = true;
                usdLabel.setVisibility(View.VISIBLE);
                usdValue.setVisibility(View.GONE);
                tokenSymbolLabel.setVisibility(View.GONE);
                tokenEquivalentLayout.setVisibility(View.VISIBLE);
            }

            try
            {
                updateEquivalentValue();
            }
            catch (NumberFormatException e)
            {
                //
            }
        });

        quantityUpBtn = activity.findViewById(R.id.img_quantity_up);
        quantityUpBtn.setOnClickListener(v -> {
            double amount;
            if (!amountEditText.getText().toString().isEmpty()) {
                amount = Double.parseDouble(amountEditText.getText().toString());
                amount += 1.0;
            } else {
                amount = 1.0;
            }
            amountEditText.setText(String.valueOf(amount));
            callback.amountChanged(String.valueOf(amount));
        });

        quantityDownBtn = activity.findViewById(R.id.img_quantity_down);
        quantityDownBtn.setOnClickListener(v -> {
            double amount;
            if (!amountEditText.getText().toString().isEmpty()
                    && Double.parseDouble(amountEditText.getText().toString()) >= 1.0) {
                amount = Double.parseDouble(amountEditText.getText().toString());
                amount -= 1.0;
            } else {
                amount = 0;
            }
            amountEditText.setText(String.valueOf(amount));
            callback.amountChanged(String.valueOf(amount));
        });

        if (isEth)
        {
            startEthereumTicker();
            switchBtn.setVisibility(View.VISIBLE);
        }
        else
        {
            usdValue.setVisibility(View.GONE);
            quantityUpBtn.setVisibility(View.VISIBLE);
            quantityDownBtn.setVisibility(View.VISIBLE);
        }
    }

    private void updateEquivalentValue() throws NumberFormatException
    {
        String amountStr = amountEditText.getText().toString();
        if (usdInput) {
            String tokenAmountEquivalent = ethEquivalent(amountStr);
            tokenEquivalent.setText(tokenAmountEquivalent);

            double equivalent = 0.0;

            if (amountStr.length() > 0) {
                double amount = Double.parseDouble(amountStr);
                equivalent = amount / currentEthPrice;
            }

            callback.amountChanged(String.valueOf(equivalent));
        } else
        {
            if (amountStr.length() == 0)
                amountStr = "0";
            if (isValidAmount(amountStr))
            {
                String usdEquivStr = "US$ " + getUsdString(Double.parseDouble(amountStr) * currentEthPrice);
                if (!hasRealValue) usdEquivStr = "(TEST) " + usdEquivStr;
                usdValue.setText(usdEquivStr);
            }
            callback.amountChanged(amountStr);
        }
    }

    private String getEthValue()
    {
        if (usdInput)
        {
            return tokenEquivalent.getText().toString();
        }
        else if (!amountEditText.getText().toString().isEmpty())
        {
            return amountEditText.getText().toString();
        }
        else
        {
            return "0";
        }
    }

    public void getValue()
    {
        callback.amountChanged(getEthValue());
    }

    private String ethEquivalent(String amountStr) throws NumberFormatException
    {
        String result = "0";

        if (amountStr.length() > 0) {
            double equivalent = 0.0;
            double amount = Double.parseDouble(amountStr);
            equivalent = amount / currentEthPrice;
            result = getEthString(equivalent);
        }

        return result;
    }

    public void setAmount(String value)
    {
        try
        {
            if (usdInput)
            {
                tokenEquivalent.setText(ethEquivalent(value));
            }
            else
            {
                if (isValidAmount(value))
                {
                    String usdEquivStr = "US$ " + getUsdString(Double.valueOf(value) * currentEthPrice);
                    if (!hasRealValue)
                        usdEquivStr = "(TEST) " + usdEquivStr;
                    usdValue.setText(usdEquivStr);
                }
            }
        }
        catch (NumberFormatException e)
        {
            //
        }
    }

    public void startEthereumTicker()
    {
        disposable = Observable.interval(0, CHECK_ETHPRICE_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> tokenRepository
                        .getEthTicker(chainId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onTicker, this::onError)).subscribe();
    }

    private void onTicker(Ticker ticker)
    {
        if (ticker != null && ticker.price_usd != null)
        {
            currentEthPrice = Double.valueOf(ticker.price_usd);
            //now update UI
            setAmount(amountEditText.getText().toString());
        }
    }

    boolean isValidAmount(String eth) {
        try {
            String wei = BalanceUtils.EthToWei(eth);
            return wei != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void onError(Throwable throwable)
    {

    }

    public boolean checkValidAmount()
    {
        amountError.setVisibility(View.GONE);

        if (!isValidAmount(getEthValue())) {
            amountError.setVisibility(View.VISIBLE);
            amountError.setText(R.string.error_invalid_amount);
            return false;
        }
        else
        {
            return true;
        }
    }

    public void setError(int errorMessage)
    {
        amountError.setVisibility(View.VISIBLE);
        amountError.setText(errorMessage);
    }

    public void setAmountText(String ethAmount)
    {
        amountEditText.setText(ethAmount);
    }
}
