package com.alphawallet.app.widget;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.GasPriceSpread;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.TXSpeed;
import com.alphawallet.app.entity.analytics.ActionSheetMode;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.entity.RealmGasSpread;
import com.alphawallet.app.repository.entity.RealmTokenTicker;
import com.alphawallet.app.walletconnect.AWWalletConnectClient;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.GasSettingsActivity;
import com.alphawallet.app.ui.widget.entity.GasSpeed2;
import com.alphawallet.app.ui.widget.entity.GasWidgetInterface;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.web3.entity.Web3Transaction;

import java.math.BigDecimal;
import java.math.BigInteger;

import io.realm.Realm;
import io.realm.RealmQuery;
import timber.log.Timber;

/**
 * Created by JB on 19/11/2020.
 */
public class GasWidget extends LinearLayout implements Runnable, GasWidgetInterface
{
    private GasPriceSpread gasSpread;
    private RealmGasSpread realmGasSpread;
    private TokensService tokensService;
    private BigInteger customGasLimit;    //from slider
    private BigInteger initialTxGasLimit; //this is the gas limit specified from the dapp transaction.
    private BigInteger baseLineGasLimit;  //this is our candidate gas limit - either from the dapp or a default, can be replaced by accurate estimate if initialTx was zero
    private BigInteger presetGasLimit;    //this is the gas limit used for the presets. It will use, in order of priority: gas estimate from node, gas from dapp tx, calculated gas
    private BigInteger transactionValue;  //'value' base token amount from dapp transaction
    private BigInteger adjustedValue;     //adjusted value, in case we are use 'all funds' to wipe an account.
    private BigInteger initialGasPrice;   //gasprice from dapp transaction
    private Token token;
    private StandardFunctionInterface functionInterface;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final TextView speedText;
    private final TextView timeEstimate;
    private final LinearLayout gasWarning;
    private final LinearLayout speedWarning;

    private TXSpeed currentGasSpeedIndex = TXSpeed.STANDARD;
    private long customNonce = -1;
    private boolean isSendingAll;
    private BigInteger resendGasPrice = BigInteger.ZERO;

    public GasWidget(Context ctx, AttributeSet attrs)
    {
        super(ctx, attrs);
        inflate(ctx, R.layout.item_gas_settings, this);

        speedText = findViewById(R.id.text_speed);
        timeEstimate = findViewById(R.id.text_time_estimate);
        gasWarning = findViewById(R.id.layout_gas_warning);
        speedWarning = findViewById(R.id.layout_speed_warning);
    }

    //For legacy transaction, either we are sending all or the chain doesn't support EIP1559
    //Since these chains are not so well used, we will compromise and send at the standard gas rate
    //That is - not allow selection of gas price
    public void setupWidget(TokensService svs, Token t, Web3Transaction tx, StandardFunctionInterface sfi, ActivityResultLauncher<Intent> gasSelectLauncher)
    {
        tokensService = svs;
        token = t;
        initialTxGasLimit = tx.gasLimit;
        functionInterface = sfi;
        transactionValue = tx.value;
        adjustedValue = tx.value;
        isSendingAll = isSendingAll(tx);
        initialGasPrice = tx.gasPrice;
        customNonce = tx.nonce;

        if (tx.gasLimit.equals(BigInteger.ZERO)) //dapp didn't specify a limit, use default limits until node returns an estimate (see setGasEstimate())
        {
            baseLineGasLimit = GasService.getDefaultGasLimit(token, tx);
        }
        else
        {
            baseLineGasLimit = tx.gasLimit;
        }

        presetGasLimit = baseLineGasLimit;
        customGasLimit = baseLineGasLimit;

        setupGasSpeeds(tx);
        startGasListener();

        if (!tokensService.hasLockedGas(token.tokenInfo.chainId))
        {
            findViewById(R.id.edit_text).setVisibility(View.VISIBLE);
            setOnClickListener(v -> {
                Token baseEth = tokensService.getToken(token.tokenInfo.chainId, token.getWallet());
                Intent intent = new Intent(getContext(), GasSettingsActivity.class);
                intent.putExtra(C.EXTRA_SINGLE_ITEM, currentGasSpeedIndex.ordinal());
                intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
                intent.putExtra(C.EXTRA_CUSTOM_GAS_LIMIT, customGasLimit.toString());
                intent.putExtra(C.EXTRA_GAS_LIMIT_PRESET, presetGasLimit.toString());
                intent.putExtra(C.EXTRA_TOKEN_BALANCE, baseEth.balance.toString());
                intent.putExtra(C.EXTRA_AMOUNT, transactionValue.toString());
                intent.putExtra(C.EXTRA_GAS_PRICE, gasSpread);  //Parcelised
                intent.putExtra(C.EXTRA_NONCE, customNonce);
                intent.putExtra(C.EXTRA_1559_TX, false);
                intent.putExtra(C.EXTRA_MIN_GAS_PRICE, resendGasPrice.longValue());
                gasSelectLauncher.launch(intent);
            });
        }
    }

    private void setupGasSpeeds(Web3Transaction w3tx)
    {
        RealmGasSpread rgs = getGasQuery().findFirst();
        if (rgs != null)
        {
            initGasSpeeds(rgs);
        }
        else
        {
            // Couldn't get current gas. Add a blank custom gas speed node
            gasSpread = new GasPriceSpread(getContext(), w3tx.gasPrice);
        }

        if (w3tx.gasPrice.compareTo(BigInteger.ZERO) > 0)
        {
            gasSpread.setCustom(w3tx.gasPrice, GasPriceSpread.FAST_SECONDS);
        }
    }

    public void setupResendSettings(ActionSheetMode mode, BigInteger minGas)
    {
        resendGasPrice = minGas;
        TextView speedupNote = findViewById(R.id.text_speedup_note);
        //If user wishes to cancel transaction, otherwise default is speed it up.
        if (mode == ActionSheetMode.CANCEL_TRANSACTION)
        {
            speedupNote.setText(R.string.text_cancel_note);
        }
        else
        {
            speedupNote.setText(R.string.text_speedup_note);
        }
        speedupNote.setVisibility(View.VISIBLE);

    }

    @Override
    public void onDestroy()
    {
        if (realmGasSpread != null && realmGasSpread.isValid()) realmGasSpread.removeAllChangeListeners();
    }

    /**
     * This function is the leaf for when the user clicks on a gas setting; fast, slow, custom, etc
     *
     * @param gasSelectionIndex
     * @param gasPrice
     * @param expectedTxTime
     * @param nonce
     */
    @Override
    public void setCurrentGasIndex(int gasSelectionIndex, BigInteger gasPrice, BigInteger dummyVar, BigDecimal custGasLimit, long expectedTxTime, long nonce)
    {
        if (gasSelectionIndex < TXSpeed.values().length)
        {
            currentGasSpeedIndex = TXSpeed.values()[gasSelectionIndex];
        }

        customNonce = nonce;
        customGasLimit = custGasLimit.toBigInteger();

        if (gasPrice.compareTo(BigInteger.ZERO) > 0)
        {
            gasSpread.setCustom(gasPrice, expectedTxTime);
        }

        tokensService.track(currentGasSpeedIndex.name());
        handler.post(this);
    }

    public boolean checkSufficientGas()
    {
        BigInteger useGasLimit = getUseGasLimit();
        boolean sufficientGas = true;

        GasSpeed2 gs = gasSpread.getSelectedGasFee(currentGasSpeedIndex);
        BigDecimal networkFee = new BigDecimal(gs.gasPrice.maxFeePerGas.multiply(useGasLimit));
        Token base = tokensService.getTokenOrBase(token.tokenInfo.chainId, token.getWallet());

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
        }
        else
        {
            gasWarning.setVisibility(View.GONE);
        }

        return sufficientGas;
    }

    private BigInteger getUseGasLimit()
    {
        if (currentGasSpeedIndex == TXSpeed.CUSTOM)
        {
            return customGasLimit;
        }
        else
        {
            return presetGasLimit;
        }
    }

    private BigInteger calculateSendAllValue()
    {
        BigInteger sendAllValue;
        GasSpeed2 gs = gasSpread.getSelectedGasFee(currentGasSpeedIndex);
        BigDecimal networkFee = new BigDecimal(gs.gasPrice.maxFeePerGas.multiply(getUseGasLimit()));

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
                .equalTo("chainId", token.tokenInfo.chainId);
    }

    private void startGasListener()
    {
        if (realmGasSpread != null) realmGasSpread.removeAllChangeListeners();
        realmGasSpread = getGasQuery().findFirstAsync();
        realmGasSpread.addChangeListener(realmToken -> {
            if (realmGasSpread.isValid())
            {
                initGasSpeeds(((RealmGasSpread) realmToken));
            }
        });
    }

    private void initGasSpeeds(RealmGasSpread rgs)
    {
        try
        {
            gasSpread = new GasPriceSpread(getContext(), gasSpread, rgs.getTimeStamp(), rgs.getGasFees(), rgs.isLocked());

            TextView editTxt = findViewById(R.id.edit_text);

            if (gasSpread.hasLockedGas() && editTxt.getVisibility() == View.VISIBLE)
            {
                findViewById(R.id.edit_text).setVisibility(View.GONE);
                setOnClickListener(null);
            }
            //if we have mainnet then show timings, otherwise no timing, if the token has fiat value, show fiat value of gas, so we need the ticker
            handler.post(this);
        }
        catch (Exception e)
        {
            currentGasSpeedIndex = TXSpeed.STANDARD;
            if (BuildConfig.DEBUG) e.printStackTrace();
        }
    }

    /**
     * Update the UI with the gas value and expected transaction time (if main net).
     * Note - there is no ticker listener - it's unlikely any ticker change would produce a noticeable change in the gas price
     */
    @Override
    public void run()
    {
        GasSpeed2 gs = gasSpread.getSelectedGasFee(currentGasSpeedIndex);

        if (gs == null || gs.gasPrice == null || gs.gasPrice.maxFeePerGas == null)
        {
            return;
        }

        Token baseCurrency = tokensService.getTokenOrBase(token.tokenInfo.chainId, token.getWallet());
        BigInteger networkFee = gs.gasPrice.maxFeePerGas.multiply(getUseGasLimit());
        String gasAmountInBase = BalanceUtils.getSlidingBaseValue(new BigDecimal(networkFee), baseCurrency.tokenInfo.decimals, GasSettingsActivity.GAS_PRECISION);
        if (gasAmountInBase.equals("0")) gasAmountInBase = "0.0001";
        String displayStr = getContext().getString(R.string.gas_amount, gasAmountInBase, baseCurrency.getSymbol());

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
                double cryptoAmount = BalanceUtils.weiToEth(new BigDecimal(networkFee)).doubleValue();//Double.parseDouble(gasAmountInBase);
                displayStr += getContext().getString(R.string.gas_fiat_suffix,
                        TickerService.getCurrencyString(cryptoAmount * cryptoRate),
                        rtt.getCurrencySymbol());

                if (token.tokenInfo.chainId == MAINNET_ID && gs.seconds > 0)
                {
                    displayStr += getContext().getString(R.string.gas_time_suffix,
                            Utils.shortConvertTimePeriodInSeconds(gs.seconds, getContext()));
                }
            }
        }
        catch (Exception e)
        {
            Timber.w(e);
        }
        timeEstimate.setText(displayStr);
        speedText.setText(gs.speed);
        adjustedValue = calculateSendAllValue();

        if (isSendingAll)
        {
            functionInterface.updateAmount();
        }

        if (currentGasSpeedIndex == TXSpeed.CUSTOM)
        {
            BigInteger customPrice = gasSpread.getSelectedGasFee(TXSpeed.CUSTOM).gasPrice.maxFeePerGas;
            checkCustomGasPrice(customPrice);
        }
        else
        {
            speedWarning.setVisibility(View.GONE);
        }
        checkSufficientGas();
        manageWarnings();
    }

    @Override
    public BigInteger getGasPrice(BigInteger defaultPrice)
    {
        GasSpeed2 gs = gasSpread.getSelectedGasFee(currentGasSpeedIndex);
        return gs.gasPrice.maxFeePerGas;
    }

    @Override
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

    private void checkCustomGasPrice(BigInteger customGasPrice)
    {
        double dGasPrice = customGasPrice.doubleValue();

        GasSpeed2 ug = gasSpread.getQuickestGasSpeed();
        GasSpeed2 lg = gasSpread.getSlowestGasSpeed();

        double lowerBound = lg.gasPrice.maxFeePerGas.doubleValue();
        double upperBound = ug.gasPrice.maxFeePerGas.doubleValue();

        if (resendGasPrice.compareTo(BigInteger.ZERO) > 0)
        {
            if (dGasPrice > (3.0 * resendGasPrice.doubleValue()))
            {
                showCustomSpeedWarning(true);
            }
            else
            {
                speedWarning.setVisibility(View.GONE);
            }
        }
        else if (dGasPrice < lowerBound)
        {
            showCustomSpeedWarning(false);
        }
        else if (dGasPrice > 2.0 * upperBound)
        {
            showCustomSpeedWarning(true);
        }
        else
        {
            speedWarning.setVisibility(View.GONE);
        }
    }

    private void showCustomSpeedWarning(boolean high)
    {
        if (currentGasSpeedIndex != TXSpeed.CUSTOM)
        {
            return;
        }

        TextView warningText = findViewById(R.id.text_speed_warning);

        if (high)
        {
            warningText.setText(getResources().getString(R.string.speed_high_gas));
        }
        else
        {
            warningText.setText(getResources().getString(R.string.speed_too_low));
        }
        speedWarning.setVisibility(View.VISIBLE);
    }

    private void manageWarnings()
    {
        if (gasWarning.getVisibility() == VISIBLE || speedWarning.getVisibility() == VISIBLE)
        {
            speedText.setVisibility(View.GONE);
            if (gasWarning.getVisibility() == VISIBLE && speedWarning.getVisibility() == VISIBLE)
            {
                speedWarning.setVisibility(View.GONE);
            }
        }
        else
        {
            speedText.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public BigInteger getGasLimit()
    {
        return getUseGasLimit();
    }

    @Override
    public long getNonce()
    {
        if (currentGasSpeedIndex == TXSpeed.CUSTOM)
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
        GasSpeed2 gs = gasSpread.getSelectedGasFee(currentGasSpeedIndex);
        return gs.seconds;
    }

    @Override
    public boolean isSendingAll(Web3Transaction tx)
    {
        if (token.isEthereum())
        {
            return (tx.leafPosition == -2);
        }

        return false;
    }

    @Override
    public BigInteger getGasMax()
    {
        return BigInteger.ZERO;
    }

    @Override
    public BigInteger getPriorityFee()
    {
        return BigInteger.ZERO;
    }

    @Override
    public BigInteger getGasPrice()
    {
        return null;
    }

    /**
     * Node eth_gasEstimate returned a transaction estimate
     *
     * @param estimate
     */
    public void setGasEstimate(BigInteger estimate)
    {
        if (!isSendingAll && estimate.longValue() > C.GAS_LIMIT_MIN) //some kind of contract interaction
        {
            estimate = estimate.multiply(BigInteger.valueOf(6)).divide(BigInteger.valueOf(5)); // increase estimate by 20% to be safe
        }

        //Override custom gas limit
        if (customGasLimit.equals(baseLineGasLimit))
        {
            customGasLimit = estimate;
        }
        if (initialTxGasLimit.equals(BigInteger.ZERO))
        {
            baseLineGasLimit = estimate;
        }

        //presets always use estimate if available
        presetGasLimit = estimate;

        //now update speeds
        handler.post(this);
    }
}
