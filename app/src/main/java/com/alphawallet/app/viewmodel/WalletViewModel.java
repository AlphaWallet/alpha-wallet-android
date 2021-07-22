package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.text.format.DateUtils;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.interact.ChangeTokenEnableInteract;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.router.AssetDisplayRouter;
import com.alphawallet.app.router.Erc20DetailRouter;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.zxing.QRScanningActivity;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

public class WalletViewModel extends BaseViewModel
{
    public static long BALANCE_BACKUP_CHECK_INTERVAL = 5 * DateUtils.MINUTE_IN_MILLIS;
    public static double VALUE_THRESHOLD = 200.0; //$200 USD value is difference between red and grey backup warnings

    private final MutableLiveData<TokenCardMeta[]> tokens = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<GenericWalletInteract.BackupLevel> backupEvent = new MutableLiveData<>();

    private final FetchTokensInteract fetchTokensInteract;
    private final Erc20DetailRouter erc20DetailRouter;
    private final AssetDisplayRouter assetDisplayRouter;
    private final GenericWalletInteract genericWalletInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;
    private final ChangeTokenEnableInteract changeTokenEnableInteract;
    private final MyAddressRouter myAddressRouter;
    private long lastBackupCheck = 0;

    WalletViewModel(
            FetchTokensInteract fetchTokensInteract,
            Erc20DetailRouter erc20DetailRouter,
            AssetDisplayRouter assetDisplayRouter,
            GenericWalletInteract genericWalletInteract,
            AssetDefinitionService assetDefinitionService,
            TokensService tokensService,
            ChangeTokenEnableInteract changeTokenEnableInteract,
            MyAddressRouter myAddressRouter)
    {
        this.fetchTokensInteract = fetchTokensInteract;
        this.erc20DetailRouter = erc20DetailRouter;
        this.assetDisplayRouter = assetDisplayRouter;
        this.genericWalletInteract = genericWalletInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
        this.changeTokenEnableInteract = changeTokenEnableInteract;
        this.myAddressRouter = myAddressRouter;
    }

    public LiveData<TokenCardMeta[]> tokens() {
        return tokens;
    }
    public LiveData<Wallet> defaultWallet() { return defaultWallet; }
    public LiveData<GenericWalletInteract.BackupLevel> backupEvent() { return backupEvent; }

    public String getWalletAddr() { return defaultWallet.getValue() != null ? defaultWallet.getValue().address : ""; }
    public WalletType getWalletType() { return defaultWallet.getValue() != null ? defaultWallet.getValue().type : WalletType.KEYSTORE; }

    public void prepare()
    {
        lastBackupCheck = System.currentTimeMillis() - BALANCE_BACKUP_CHECK_INTERVAL + 5*DateUtils.SECOND_IN_MILLIS;
        //load the activity meta list
        disposable = genericWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(Wallet wallet)
    {
        tokensService.setCurrentAddress(wallet.address);
        assetDefinitionService.startEventListener();
        defaultWallet.postValue(wallet);
        fetchTokens(wallet);
    }

    private void fetchTokens(Wallet wallet)
    {
        disposable =
                fetchTokensInteract.fetchTokenMetas(wallet, tokensService.getNetworkFilters(), assetDefinitionService)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onTokenMetas, this::onError);
    }

    private void onTokenMetas(TokenCardMeta[] metaTokens)
    {
        tokens.postValue(metaTokens);
        tokensService.updateTickers();
        tokensService.startUpdateCycle();
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }

    public TokensService getTokensService()
    {
        return tokensService;
    }

    public Token getTokenFromService(@NotNull Token token)
    {
        Token serviceToken = tokensService.getToken(token.tokenInfo.chainId, token.getAddress());
        if (serviceToken != null && serviceToken.isEthereum())
        {
            return tokensService.getServiceToken(token.tokenInfo.chainId);
        }
        else
        {
            return (serviceToken != null) ? serviceToken : token;
        }
    }

    public Wallet getWallet()
    {
        return defaultWallet.getValue();
    }

    public void setKeyBackupTime(String walletAddr)
    {
        genericWalletInteract.updateBackupTime(walletAddr);
    }

    public void setKeyWarningDismissTime(String walletAddr)
    {
        genericWalletInteract.updateWarningTime(walletAddr);
    }

    public void setTokenEnabled(Token token, boolean enabled) {
        changeTokenEnableInteract.setEnable(defaultWallet.getValue(), token, enabled);
        token.tokenInfo.isEnabled = enabled;
    }

    public void showMyAddress(Context context)
    {
        myAddressRouter.open(context, defaultWallet.getValue());
    }

    public void showQRCodeScanning(Activity activity) {
        Intent intent = new Intent(activity, QRScanningActivity.class);
        intent.putExtra(C.EXTRA_UNIVERSAL_SCAN, true);
        activity.startActivityForResult(intent, C.REQUEST_UNIVERSAL_SCAN);
    }

    public Realm getRealmInstance(Wallet wallet)
    {
        return tokensService.getRealmInstance(wallet);
    }

    @Override
    public void showErc20TokenDetail(Activity context, @NotNull String address, String symbol, int decimals, @NotNull Token token) {
        boolean isToken = !address.equalsIgnoreCase(defaultWallet.getValue().address);
        boolean hasDefinition = assetDefinitionService.hasDefinition(token.tokenInfo.chainId, address);
        erc20DetailRouter.open(context, address, symbol, decimals, isToken, defaultWallet.getValue(), token, hasDefinition);
    }

    @Override
    public void showTokenList(Activity activity, Token token) {
        assetDisplayRouter.open(activity, token, defaultWallet.getValue());
    }

    public void checkBackup()
    {
        if (TextUtils.isEmpty(getWalletAddr()) || System.currentTimeMillis() < (lastBackupCheck + BALANCE_BACKUP_CHECK_INTERVAL)) return;
        lastBackupCheck = System.currentTimeMillis();
        double walletUSDValue = tokensService.getUSDValue();

        if (walletUSDValue > 0.0)
        {
            final BigDecimal calcValue = BigDecimal.valueOf(walletUSDValue);
            genericWalletInteract.getBackupWarning(getWalletAddr())
                    .map(needsBackup -> calculateBackupWarning(needsBackup, calcValue))
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(backupEvent::postValue, this::onTokenBalanceError).isDisposed();
        }
    }

    private void onTokenBalanceError(Throwable throwable)
    {
        //unable to resolve - phone may be offline
    }

    private GenericWalletInteract.BackupLevel calculateBackupWarning(Boolean needsBackup, @NotNull BigDecimal value)
    {
        if (!needsBackup)
        {
            return GenericWalletInteract.BackupLevel.BACKUP_NOT_REQUIRED;
        }
        else if (value.compareTo(BigDecimal.valueOf(VALUE_THRESHOLD)) >= 0)
        {
            return GenericWalletInteract.BackupLevel.WALLET_HAS_HIGH_VALUE;
        }
        else
        {
            return GenericWalletInteract.BackupLevel.WALLET_HAS_LOW_VALUE;
        }
    }

    public void notifyRefresh()
    {
        tokensService.clearFocusToken(); //ensure if we do a refresh there's no focus token preventing correct update
    }

    public boolean isChainToken(int chainId, String tokenAddress)
    {
        return tokensService.isChainToken(chainId, tokenAddress);
    }
}
