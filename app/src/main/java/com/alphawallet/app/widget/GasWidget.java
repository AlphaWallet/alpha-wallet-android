package com.alphawallet.app.widget;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.GasPriceSpread;
import com.alphawallet.app.entity.StandardFunctionInterface;
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
import com.alphawallet.app.web3.entity.Web3Transaction;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.Sort;

import static com.alphawallet.app.C.DEFAULT_GAS_PRICE;
import static com.alphawallet.app.C.GAS_LIMIT_MIN;
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
    private BigInteger transactionValue;
    private BigInteger adjustedValue;
    private Token token;
    private Activity baseActivity;
    private StandardFunctionInterface functionInterface;

    private final Handler handler = new Handler();

    private final TextView speedText;
    private final TextView timeEstimate;
    private final LinearLayout gasWarning;
    private final Context context;

    private final List<GasSpeed> gasSpeeds;
    private int currentGasSpeedIndex = -1;
    private int customGasSpeedIndex = 0;
    private long customNonce = -1;
    private boolean isSendingAll;
    private boolean forceCustomGas;

    public GasWidget(Context ctx, AttributeSet attrs)
    {
        super(ctx, attrs);
        inflate(ctx, R.layout.item_gas_settings, this);

        context = ctx;
        speedText = findViewById(R.id.text_speed);
        timeEstimate = findViewById(R.id.text_time_estimate);
        gasWarning = findViewById(R.id.layout_gas_warning);

        gasSpeeds = new ArrayList<>();

        setOnClickListener(v -> {
            if (gasSpeeds.size() == 0) return;
            Token baseEth = tokensService.getToken(token.tokenInfo.chainId, token.getWallet());
            Intent intent = new Intent(context, GasSettingsActivity.class);
            intent.putExtra(C.EXTRA_SINGLE_ITEM, currentGasSpeedIndex);
            intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
            intent.putExtra(C.EXTRA_GAS_LIMIT, baseGasLimit.toString());
            intent.putExtra(C.EXTRA_CUSTOM_GAS_LIMIT, gasLimit.toString());
            intent.putExtra(C.EXTRA_TOKEN_BALANCE, baseEth.balance.toString());
            intent.putExtra(C.EXTRA_AMOUNT, transactionValue.toString());
            intent.putExtra(C.EXTRA_GAS_PRICE, gasSpeeds.get(customGasSpeedIndex).gasPrice.toString());
            intent.putExtra(C.EXTRA_NONCE, customNonce);
            baseActivity.startActivityForResult(intent, C.SET_GAS_SETTINGS);
        });
    }

    public void setupWidget(TokensService svs, Token t, Web3Transaction tx, StandardFunctionInterface sfi, Activity act)
    {
        tokensService = svs;
        token = t;
        gasLimit = tx.gasLimit;
        baseActivity = act;
        functionInterface = sfi;
        baseGasLimit = tx.gasLimit;
        transactionValue = tx.value;
        adjustedValue = tx.value;
        isSendingAll = isSendingAll(tx);

        setupGasSpeeds(tx.gasPrice);
        startGasListener();
    }

    private void setupGasSpeeds(BigInteger priceFromTx)
    {
        if (priceFromTx.compareTo(BigInteger.ZERO) > 0)
        {
            gasSpeeds.add(new GasSpeed(getContext().getString(R.string.speed_custom), GasPriceSpread.FAST_SECONDS, priceFromTx));
            forceCustomGas = true;
        }
        else
        {
            priceFromTx = new BigInteger(DEFAULT_GAS_PRICE);
        }

        RealmGasSpread getGas = getGasQuery().findFirst();
        if (getGas != null)
        {
            initGasSpeeds(getGas);
        }
        else
        {
            // Couldn't get current gas. Add a blank custom gas speed node
            gasSpeeds.add(new GasSpeed(getContext().getString(R.string.speed_custom), GasPriceSpread.FAST_SECONDS, priceFromTx));
            forceCustomGas = true;
        }
    }

    public void onDestroy()
    {
        if (realmGasSpread != null) realmGasSpread.removeAllChangeListeners();
    }

    /**
     * This function is the leaf for when the user clicks on a gas setting; fast, slow, custom, etc
     *
     * @param gasSelectionIndex
     * @param customGasPrice
     * @param customGasLimit
     * @param expectedTxTime
     * @param nonce
     */
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

        adjustedValue = calculateSendAllValue();
        tokensService.track(gs.speed);
    }

    public boolean checkSufficientGas()
    {
        boolean sufficientGas = true;
        GasSpeed gs = gasSpeeds.get(currentGasSpeedIndex);
        BigDecimal networkFee = new BigDecimal(gs.gasPrice.multiply(gasLimit));
        Token base = tokensService.getToken(token.tokenInfo.chainId, token.getWallet());

        if (isSendingAll)
        {
            sufficientGas = token.balance.subtract(new BigDecimal(adjustedValue).add(networkFee)).compareTo(BigDecimal.ZERO) >= 0;
        }
        else if (token.isEthereum() && token.balance.subtract(new BigDecimal(transactionValue).add(networkFee)).compareTo(BigDecimal.ZERO) < 0)
        {
            sufficientGas = false;
        }
        else if (!token.isEthereum() && base.balance.subtract(networkFee).compareTo(BigDecimal.ZERO) < 0)
        {
            sufficientGas = false;
        }

        if (!sufficientGas)
        {
            gasWarning.setVisibility(View.VISIBLE);
            speedText.setVisibility(View.GONE);
        }
        else
        {
            gasWarning.setVisibility(View.GONE);
            speedText.setVisibility(View.VISIBLE);
        }

        return sufficientGas;
    }

    private BigInteger calculateSendAllValue()
    {
        BigInteger sendAllValue;
        GasSpeed gs = gasSpeeds.get(currentGasSpeedIndex);
        BigDecimal networkFee = new BigDecimal(gs.gasPrice.multiply(gasLimit));

        if (isSendingAll)
        {
            //need to recalculate the 'send all' value
            //calculate max amount possible
            sendAllValue = token.balance.subtract(networkFee).toBigInteger();
            if (sendAllValue.compareTo(BigInteger.ZERO) < 0) sendAllValue = BigInteger.ZERO;
        }
        else
        {
            sendAllValue = transactionValue;
        }

        return sendAllValue;
    }

    private RealmQuery<RealmGasSpread> getGasQuery()
    {
        return tokensService.getTickerRealmInstance().where(RealmGasSpread.class)
                .equalTo("chainId", token.tokenInfo.chainId)
                .sort("timeStamp", Sort.DESCENDING);
    }

    private void startGasListener()
    {
        realmGasSpread = getGasQuery().findFirstAsync();

        realmGasSpread.addChangeListener(realmToken -> {
            initGasSpeeds((RealmGasSpread) realmToken);
        });
    }

    private void initGasSpeeds(RealmGasSpread rgs)
    {
        try
        {
            GasPriceSpread gs = rgs.getGasPrice();
            currentGasSpeedIndex = gs.setupGasSpeeds(context, gasSpeeds, currentGasSpeedIndex);
            customGasSpeedIndex = gs.getCustomIndex();
            if (forceCustomGas)
            {
                currentGasSpeedIndex = customGasSpeedIndex;
                forceCustomGas = false;
            }
            //if we have mainnet then show timings, otherwise no timing, if the token has fiat value, show fiat value of gas, so we need the ticker
            handler.post(this);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
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
        adjustedValue = calculateSendAllValue();

        if (isSendingAll)
        {
            functionInterface.updateAmount();
        }

        checkSufficientGas();
    }

    public BigInteger getGasPrice(BigInteger defaultPrice)
    {
        if (currentGasSpeedIndex == -1)
        {
            return defaultPrice;
        }
        else
        {
            GasSpeed gs = gasSpeeds.get(currentGasSpeedIndex);
            return gs.gasPrice;
        }
    }

    public BigInteger getValue()
    {
        if (isSendingAll)
        {
            return adjustedValue;
        }
        else
        {
            return transactionValue;
        }
    }

    public BigInteger getGasLimit()
    {
        return gasLimit;
    }

    public long getNonce()
    {
        if (currentGasSpeedIndex == customGasSpeedIndex)
        {
            return customNonce;
        }
        else
        {
            return -1;
        }
    }

    public long getExpectedTransactionTime()
    {
        GasSpeed gs = gasSpeeds.get(currentGasSpeedIndex);
        return gs.seconds;
    }

    private boolean isSendingAll(Web3Transaction tx)
    {
        if (token.isEthereum())
        {
            //gas fee:
            BigDecimal networkFee = new BigDecimal(tx.gasPrice.multiply(BigInteger.valueOf(GAS_LIMIT_MIN)));
            BigDecimal remainder = token.balance.subtract(new BigDecimal(tx.value).add(networkFee));
            return remainder.equals(BigDecimal.ZERO);
        }

        return false;
    }

    public void setGasEstimate(BigInteger estimate)
    {
        if (baseGasLimit.equals(BigInteger.ZERO))
        {
            baseGasLimit = estimate;
        }

        if (gasLimit.equals(BigInteger.ZERO))
        {
            gasLimit = estimate;
        }
    }
}
