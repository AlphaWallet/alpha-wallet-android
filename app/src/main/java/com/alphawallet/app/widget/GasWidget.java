package com.alphawallet.app.widget;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.GasPriceSpread;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.entity.RealmGasSpread;
import com.alphawallet.app.repository.entity.RealmTokenTicker;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.GasSettingsActivity;
import com.alphawallet.app.ui.widget.entity.GasSpeed;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.GasSettingsViewModel;
import com.alphawallet.app.web3.entity.Web3Transaction;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.Sort;

import static com.alphawallet.app.repository.EthereumNetworkBase.MAINNET_ID;

/**
 * Created by JB on 19/11/2020.
 */
public class GasWidget extends LinearLayout implements Runnable
{
    private RealmGasSpread realmGasSpread;
    private TokensService tokensService;
    private BigInteger gasLimit;
    private BigInteger baseGasLimit;
    private Token token;
    private Activity baseActivity;

    private final Handler handler = new Handler();

    private final TextView speedText;
    private final TextView timeEstimate;
    private final LinearLayout editClick;
    private final Context context;

    private final List<GasSpeed> gasSpeeds;
    private int currentGasSpeedIndex = -1;
    private long customNonce = -1;

    public GasWidget(Context ctx, AttributeSet attrs)
    {
        super(ctx, attrs);
        inflate(ctx, R.layout.item_gas_settings, this);

        context = ctx;
        speedText = findViewById(R.id.text_speed);
        timeEstimate = findViewById(R.id.text_time_estimate);
        editClick = findViewById(R.id.edit_click_layer);

        gasSpeeds = new ArrayList<>();

        editClick.setOnClickListener(v -> {
            Intent intent = new Intent(context, GasSettingsActivity.class);
            intent.putExtra(C.EXTRA_SINGLE_ITEM, currentGasSpeedIndex);
            intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
            intent.putExtra(C.EXTRA_GAS_LIMIT, gasLimit.toString());
            String customGasPrice = "0";
            if (gasSpeeds.get(currentGasSpeedIndex).speed.equals(context.getString(R.string.speed_custom)))
            {
                customGasPrice = gasSpeeds.get(currentGasSpeedIndex).gasPrice.toString();
            }
            intent.putExtra(C.EXTRA_GAS_PRICE, customGasPrice);
            intent.putExtra(C.EXTRA_NONCE, customNonce);
            baseActivity.startActivityForResult(intent, C.SET_GAS_SETTINGS);
        });
    }

    public void setupWidget(TokensService svs, Token t, Web3Transaction tx, Activity act)
    {
        tokensService = svs;
        token = t;
        gasLimit = tx.gasLimit;
        baseActivity = act;
        baseGasLimit = tx.gasLimit;

        startGasListener();
    }

    public void onDestroy()
    {
        if (realmGasSpread != null) realmGasSpread.removeAllChangeListeners();
    }

    public void setCurrentGasIndex(int gasSelectionIndex, BigDecimal customGasPrice, BigDecimal customGasLimit, long expectedTxTime, long nonce)
    {
        currentGasSpeedIndex = gasSelectionIndex;
        customNonce = nonce;
        handleCustomGas(customGasPrice, customGasLimit, expectedTxTime);
        handler.post(this);
    }

    private void handleCustomGas(BigDecimal customGasPrice, BigDecimal customGasLimit, long expectedTxTime)
    {
        GasSpeed gs = gasSpeeds.get(currentGasSpeedIndex);
        if (gs.speed.equals(context.getString(R.string.speed_custom)))
        {
            gs = new GasSpeed(gs.speed, expectedTxTime, customGasPrice.toBigInteger());
            gasSpeeds.remove(currentGasSpeedIndex);
            gasSpeeds.add(gs);
            gasLimit = customGasLimit.toBigInteger();
        }
        else
        {
            gasLimit = baseGasLimit;
        }

        tokensService.track(gs.speed);
    }

    private void startGasListener()
    {
        realmGasSpread = tokensService.getTickerRealmInstance().where(RealmGasSpread.class)
                .equalTo("chainId", token.tokenInfo.chainId)
                .sort("timeStamp", Sort.DESCENDING)
                .findFirstAsync();

        realmGasSpread.addChangeListener(realmToken -> {
            RealmGasSpread rgs = (RealmGasSpread) realmToken;
            GasPriceSpread gs = rgs.getGasPrice();
            currentGasSpeedIndex = gs.setupGasSpeeds(context, gasSpeeds, currentGasSpeedIndex);
            //if we have mainnet then show timings, otherwise no timing, if the token has fiat value, show fiat value of gas, so we need the ticker
            handler.post(this);
        });
    }

    /**
     * Update the UI with the gas value and expected transaction time (if main net).
     * Note - there is no ticker listener - it's unlikely any ticker change would produce a noticeable change in the gas price
     */
    @Override
    public void run()
    {
        if (currentGasSpeedIndex == -1) currentGasSpeedIndex = 0;
        GasSpeed gs = gasSpeeds.get(currentGasSpeedIndex);

        Token baseCurrency = tokensService.getToken(token.tokenInfo.chainId, token.getWallet());
        BigInteger networkFee = gs.gasPrice.multiply(gasLimit);
        String gasAmountInBase = BalanceUtils.getScaledValueScientific(new BigDecimal(networkFee), baseCurrency.tokenInfo.decimals);
        if (gasAmountInBase.equals("0")) gasAmountInBase = "0.0001";
        String displayStr = context.getString(R.string.gas_amount, gasAmountInBase, baseCurrency.getSymbol());

        //Can we display value for gas?
        try (Realm realm = tokensService.getTickerRealmInstance())
        {
            RealmTokenTicker rtt = realm.where(RealmTokenTicker.class)
                    .equalTo("contract", TokensRealmSource.databaseKey(token.tokenInfo.chainId, "eth"))
                    .findFirst();

            if (rtt != null)
            {
                //calculate equivalent fiat
                double cryptoRate = Double.parseDouble(rtt.getPrice());
                double cryptoAmount = Double.parseDouble(gasAmountInBase);
                displayStr += context.getString(R.string.gas_fiat_suffix,
                        TickerService.getCurrencyString(cryptoAmount * cryptoRate),
                        rtt.getCurrencySymbol());

                if (token.tokenInfo.chainId == MAINNET_ID && gs.seconds > 0)
                {
                    displayStr += context.getString(R.string.gas_time_suffix,
                            Utils.shortConvertTimePeriodInSeconds(gs.seconds, context));
                }
            }
        }
        catch (Exception e)
        {
            //
        }

        timeEstimate.setText(displayStr);
        speedText.setText(gs.speed);
    }

    public BigInteger getGasPrice()
    {
        GasSpeed gs = gasSpeeds.get(currentGasSpeedIndex);
        return gs.gasPrice;
    }

    public BigInteger getGasLimit()
    {
        return gasLimit;
    }

    public long getNonce()
    {
        return customNonce;
    }

    public long getExpectedTransactionTime()
    {
        GasSpeed gs = gasSpeeds.get(currentGasSpeedIndex);
        return gs.seconds;
    }
}
