package com.alphawallet.app.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.entity.RealmGasSpread;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.repository.entity.RealmTokenTicker;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.entity.AmountReadyCallback;
import com.alphawallet.app.ui.widget.entity.NumericInput;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.token.entity.ContractAddress;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.web3j.protocol.Web3j;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.Sort;
import timber.log.Timber;

import static com.alphawallet.app.C.GAS_LIMIT_MIN;
import static com.alphawallet.app.repository.TokensRealmSource.databaseKey;

/**
 * Created by JB on 10/11/2020.
 */
public class InputAmount extends LinearLayout
{
    private final Context context;
    private final NumericInput editText;
    private final TextView symbolText;
    private final TokenIcon icon;
    private final ChainName chainName;
    private final TextView availableSymbol;
    private final TextView availableAmount;
    private final TextView allFunds;
    private final ProgressBar gasFetch;
    private Token token;
    private Realm realm;
    private Realm tickerRealm;
    private TokensService tokensService;
    private AssetDefinitionService assetService;
    private BigInteger gasPriceEstimate = BigInteger.ZERO;
    private BigDecimal exactAmount = BigDecimal.ZERO;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AmountReadyCallback amountReadyCallback;
    private boolean amountReady;

    //These need to be members because the listener is shut down if the object doesn't exist
    private RealmTokenTicker realmTickerUpdate;
    private RealmToken realmTokenUpdate;

    private boolean showingCrypto;

    public InputAmount(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.context = context;
        inflate(context, R.layout.item_input_amount, this);

        editText = findViewById(R.id.amount_entry);
        symbolText = findViewById(R.id.text_token_symbol);
        icon = findViewById(R.id.token_icon);
        chainName = findViewById(R.id.chain_name);
        availableSymbol = findViewById(R.id.text_symbol);
        availableAmount = findViewById(R.id.text_available);
        allFunds = findViewById(R.id.text_all_funds);
        gasFetch = findViewById(R.id.gas_fetch_progress);
        showingCrypto = !CustomViewSettings.inputAmountFiatDefault();
        amountReady = false;

        setupAttrs(context, attrs);

        setupViewListeners();
    }

    /**
     * Initialise the component. Note that it will still work if assetDefinitionService is null, however some tokens (notably ERC721) may not show correctly if it is null.
     * Perhaps the token icon info should go into the TokensService not the AssetDefinitionService?
     *
     * @param token
     * @param assetDefinitionService
     * @param svs
     */
    public void setupToken(@NotNull Token token, @Nullable AssetDefinitionService assetDefinitionService,
                           @NotNull TokensService svs, @NotNull AmountReadyCallback amountCallback)
    {
        this.token = token;
        this.tokensService = svs;
        this.assetService = assetDefinitionService;
        this.amountReadyCallback = amountCallback;
        icon.bindData(token, assetService);
        chainName.setChainID(token.tokenInfo.chainId);
        updateAvailableBalance();

        this.realm = tokensService.getWalletRealmInstance();
        this.tickerRealm = tokensService.getTickerRealmInstance();
        bindDataSource();
        setupAllFunds();
    }

    public void getInputAmount()
    {
        if (gasFetch.getVisibility() == View.VISIBLE)
        {
            amountReady = true;
        }
        else
        {
            //immediate return
            if (exactAmount.compareTo(BigDecimal.ZERO) > 0)
            {
                amountReadyCallback.amountReady(exactAmount, new BigDecimal(gasPriceEstimate)); //'All Funds', must include gas Price
            }
            else
            {
                amountReadyCallback.amountReady(getWeiInputAmount(), BigDecimal.ZERO);
            }
        }
    }

    public void onDestroy()
    {
        if (realmTokenUpdate != null) realmTokenUpdate.removeAllChangeListeners();
        if (realmTickerUpdate != null) realmTickerUpdate.removeAllChangeListeners();
        if (realm != null)
        {
            realm.removeAllChangeListeners();
            if (!realm.isClosed()) realm.close();
        }
        if (tickerRealm != null)
        {
            tickerRealm.removeAllChangeListeners();
            if (!tickerRealm.isClosed()) tickerRealm.close();
        }

        realmTickerUpdate = null;
        realmTokenUpdate = null;
    }

    public void setAmount(String ethAmount)
    {
        exactAmount = BigDecimal.ZERO;
        editText.setText(ethAmount);
        handler.post(setCursor);
    }

    public void showError(boolean showError, int customError)
    {
        TextView errorText = findViewById(R.id.text_error);
        if (customError != 0)
        {
            errorText.setText(customError);
        }
        else
        {
            errorText.setText(String.format(getResources().getString(R.string.error_insufficient_funds), token.getShortSymbol()));
        }

        if (showError)
        {
            errorText.setVisibility(View.VISIBLE);
            editText.setTextColor(context.getColor(R.color.danger));
        }
        else
        {
            errorText.setVisibility(View.GONE);
            editText.setTextColor(context.getColor(R.color.text_dark_gray));
        }

    }

    private void updateAvailableBalance()
    {
        if (showingCrypto)
        {
            showCrypto();
        }
        else
        {
            showFiat();
        }
    }

    /**
     * Setup realm binding for token balance updates
     */
    private void bindDataSource()
    {
        if (realmTokenUpdate != null) realmTokenUpdate.removeAllChangeListeners();

        realmTokenUpdate = realm.where(RealmToken.class)
                .equalTo("address", databaseKey(token.tokenInfo.chainId, token.tokenInfo.address.toLowerCase()), Case.INSENSITIVE)
                .findFirstAsync();

        //if the token doesn't exist yet, first ask the TokensService to pick it up
        tokensService.storeToken(token);

        realmTokenUpdate.addChangeListener(realmToken -> {
            RealmToken rt = (RealmToken)realmToken;
            if (rt.isValid())
            {
                token = tokensService.getToken(rt.getChainId(), rt.getTokenAddress());
                updateAvailableBalance();
            }
        });
    }

    private void setupViewListeners()
    {
        LinearLayout clickMore = findViewById(R.id.layout_more_click);

        clickMore.setOnClickListener(v -> {
            //on down caret clicked - switch to fiat currency equivalent if there's a ticker
            if (getTickerQuery() == null) return;

            RealmTokenTicker rtt = getTickerQuery().findFirst();
            if (showingCrypto && rtt != null)
            {
                showingCrypto = false;
                startTickerListener();
            }
            else
            {
                showingCrypto = true;
                if (tickerRealm != null) tickerRealm.removeAllChangeListeners(); //stop ticker listener
            }

            updateAvailableBalance();
        });

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (editText.hasFocus())
                {
                    exactAmount = BigDecimal.ZERO; //invalidate the 'all funds' amount
                    showError(false, 0);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (editText.hasFocus())
                {
                    amountReadyCallback.updateCryptoAmount(getWeiInputAmount());
                }
            }
        });

        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus)
            {
                showError(false, 0);
            }
        });

        editText.setOnClickListener(v -> {
            showError(false, 0);
        });
    }

    private RealmQuery<RealmTokenTicker> getTickerQuery()
    {
        if (tickerRealm != null)
        {
            return tickerRealm.where(RealmTokenTicker.class)
                    .equalTo("contract", TokensRealmSource.databaseKey(token.tokenInfo.chainId, token.isEthereum() ? "eth" : token.getAddress().toLowerCase()));
        }
        else
        {
            return null;
        }
    }

    private void startTickerListener()
    {
        if (getTickerQuery() == null) return;
        if (realmTickerUpdate != null) realmTickerUpdate.removeAllChangeListeners();
        realmTickerUpdate = getTickerQuery().findFirstAsync();
        realmTickerUpdate.addChangeListener(realmTicker -> {
            updateAvailableBalance();
        });
    }

    private void showCrypto()
    {
        icon.bindData(token, assetService);
        symbolText.setText(token.getSymbol());
        availableSymbol.setText(token.getSymbol());
        availableAmount.setText(token.getStringBalance());
        updateAllFundsAmount();
    }

    private void showFiat()
    {
        icon.showLocalCurrency();

        RealmQuery<RealmTokenTicker> rawQuery = getTickerQuery();
        if (rawQuery == null) return;

        try
        {
            RealmTokenTicker rtt = rawQuery.findFirst();

            if (rtt != null)
            {
                String currencyLabel = rtt.getCurrencySymbol() + TickerService.getCurrencySymbol();
                symbolText.setText(currencyLabel);
                //calculate available fiat
                double cryptoRate = Double.parseDouble(rtt.getPrice());
                double availableCryptoBalance = token.getCorrectedBalance(18).doubleValue();
                availableAmount.setText(TickerService.getCurrencyString(availableCryptoBalance * cryptoRate));
                availableSymbol.setText(rtt.getCurrencySymbol());
                updateAllFundsAmount(); //update amount if showing 'All Funds'

                amountReadyCallback.updateCryptoAmount(
                        getWeiInputAmount()
                ); //now update
            }
            updateAllFundsAmount();
        }
        catch (Exception e)
        {
            Timber.e(e);
            // continue with old value
        }
    }

    private BigDecimal getWeiInputAmount()
    {
        BigDecimal inputVal = editText.getBigDecimalValue();
        //get wei value
        if (inputVal.equals(BigDecimal.ZERO))
        {
            return inputVal;
        }
        else if (showingCrypto)
        {
            return inputVal.multiply(BigDecimal.valueOf(Math.pow(10, token.tokenInfo.decimals)));
        }
        else
        {
            return convertFiatAmountToWei(inputVal.doubleValue());
        }
    }

    /**
     * Setting up the 'All Funds' button
     */
    private void setupAllFunds()
    {
        allFunds.setOnClickListener(v -> {
            if (token.isEthereum() && token.hasPositiveBalance())
            {
                RealmGasSpread gasSpread = tokensService.getTickerRealmInstance().where(RealmGasSpread.class)
                            .equalTo("chainId", token.tokenInfo.chainId)
                            .findFirst();

                if (gasSpread != null && gasSpread.getGasPrice().standard.compareTo(BigInteger.ZERO) > 0)
                {
                    //assume 'average' gas cost here
                    onLatestGasPrice(gasSpread.getGasPrice().standard);
                }
                else //fallback to node price
                {
                    gasFetch.setVisibility(View.VISIBLE);
                    Web3j web3j = TokenRepository.getWeb3jService(token.tokenInfo.chainId);
                    web3j.ethGasPrice().sendAsync()
                            .thenAccept(ethGasPrice -> onLatestGasPrice(ethGasPrice.getGasPrice()))
                            .exceptionally(this::onGasFetchError);
                }
            }
            else
            {
                exactAmount = token.balance;
                updateAllFundsAmount();
            }
            handler.post(setCursor);
        });
    }

    private void onLatestGasPrice(BigInteger price)
    {
        gasPriceEstimate = price;
        //calculate max amount possible
        BigDecimal networkFee = new BigDecimal(gasPriceEstimate.multiply(BigInteger.valueOf(GAS_LIMIT_MIN)));
        exactAmount = token.balance.subtract(networkFee);
        if (exactAmount.compareTo(BigDecimal.ZERO) < 0) exactAmount = BigDecimal.ZERO;
        //display in the view
        handler.post(updateValue);
    }

    private final Runnable updateValue = new Runnable()
    {
        @Override
        public void run()
        {
            gasFetch.setVisibility(View.GONE);
            updateAllFundsAmount();

            if (amountReady)
            {
                amountReadyCallback.amountReady(exactAmount, new BigDecimal(gasPriceEstimate));
                amountReady = false;
            }
        }
    };

    private final Runnable setCursor = new Runnable()
    {
        @Override
        public void run()
        {
            editText.setSelection(editText.getText().length());
        }
    };

    private void setupAttrs(Context context, AttributeSet attrs)
    {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.InputView,
                0, 0
        );

        try
        {
            boolean showHeader = a.getBoolean(R.styleable.InputView_show_header, true);
            boolean showAllFunds = a.getBoolean(R.styleable.InputView_show_allFunds, true);
            boolean showChainName = a.getBoolean(R.styleable.InputView_showChainName, true);
            boolean currencyMode = a.getBoolean(R.styleable.InputView_currencyMode, false);
            int headerTextId = a.getResourceId(R.styleable.InputView_label, R.string.amount);
            findViewById(R.id.layout_header_amount).setVisibility(showHeader ? View.VISIBLE : View.GONE);
            allFunds.setVisibility(showAllFunds ? View.VISIBLE : View.GONE);
            TextView headerText = findViewById(R.id.text_header);
            headerText.setText(headerTextId);
            chainName.setVisibility(showChainName ? View.VISIBLE : View.GONE);
            if (currencyMode)
            {
                symbolText.setText(TickerService.getCurrencySymbolTxt());
                icon.showLocalCurrency();
            }
        }
        finally
        {
            a.recycle();
        }
    }

    private Void onGasFetchError(Throwable throwable)
    {
        gasFetch.setVisibility(View.GONE);
        return null;
    }

    private String convertWeiAmountToFiat(BigDecimal value)
    {
        String fiatValue = "0";
        try
        {
            RealmTokenTicker rtt = getTickerQuery().findFirst();

            if (rtt != null)
            {
                double cryptoRate = Double.parseDouble(rtt.getPrice());
                double availableCryptoBalance = value.divide(BigDecimal.valueOf(Math.pow(10, token.tokenInfo.decimals)), 18, RoundingMode.DOWN).doubleValue();
                DecimalFormat df = new DecimalFormat("#,##0.00");
                df.setRoundingMode(RoundingMode.DOWN);
                fiatValue = df.format(availableCryptoBalance * cryptoRate);
            }
        }
        catch (Exception e)
        {
            // continue with old value
        }

        return fiatValue;
    }

    private BigDecimal convertFiatAmountToWei(double fiatAmount)
    {
        BigDecimal tokenGranularValue = BigDecimal.ZERO;
        try
        {
            RealmTokenTicker rtt = getTickerQuery().findFirst();

            if (rtt != null)
            {
                double tokenDisplayValue = (fiatAmount + 0.0001) / Double.parseDouble(rtt.getPrice()); //add a tiny amount to ensure fiat equals the amount specified when rounded down
                tokenGranularValue = BigDecimal.valueOf(tokenDisplayValue).multiply(BigDecimal.valueOf(Math.pow(10, token.tokenInfo.decimals)));
            }
        }
        catch (Exception e)
        {
            // continue with old value
        }

        return tokenGranularValue;
    }

    /**
     * After user clicked on 'All Funds' and we calculated the exactAmount which is the largest value (minus gas fee) the account can support
     */
    private void updateAllFundsAmount()
    {
        if (exactAmount.compareTo(BigDecimal.ZERO) > 0)
        {
            String showValue = "";
            if (showingCrypto)
            {
                showValue = BalanceUtils.getScaledValueScientific(exactAmount, token.tokenInfo.decimals);
            }
            else
            {
                showValue = convertWeiAmountToFiat(exactAmount);
            }

            editText.setText(showValue);
        }
    }
}
