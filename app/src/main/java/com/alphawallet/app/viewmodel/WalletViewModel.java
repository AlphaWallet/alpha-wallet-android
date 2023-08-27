package com.alphawallet.app.viewmodel;

import static com.alphawallet.app.C.EXTRA_ADDRESS;
import static com.alphawallet.app.repository.TokensRealmSource.ADDRESS_FORMAT;
import static com.alphawallet.app.widget.CopyTextView.KEY_ADDRESS;
import static com.alphawallet.token.tools.TokenDefinition.NO_SCRIPT;
import static com.alphawallet.token.tools.TokenDefinition.UNCHANGED_SCRIPT;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.EasAttestation;
import com.alphawallet.app.entity.QRResult;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.analytics.QrScanSource;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.app.entity.tokens.Attestation;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.entity.walletconnect.WalletConnectSessionItem;
import com.alphawallet.app.interact.ChangeTokenEnableInteract;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.CoinbasePayRepository;
import com.alphawallet.app.repository.OnRampRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.repository.TokensMappingRepositoryType;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.WalletItem;
import com.alphawallet.app.repository.entity.RealmAttestation;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.router.CoinbasePayRouter;
import com.alphawallet.app.router.ManageWalletsRouter;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.router.TokenDetailRouter;
import com.alphawallet.app.service.AnalyticsServiceType;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.RealmManager;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.NameThisWalletActivity;
import com.alphawallet.app.ui.QRScanning.QRScannerActivity;
import com.alphawallet.app.ui.TokenManagementActivity;
import com.alphawallet.app.walletconnect.AWWalletConnectClient;
import com.alphawallet.app.widget.WalletFragmentActionsView;
import com.alphawallet.token.entity.AttestationValidationStatus;
import com.alphawallet.token.tools.TokenDefinition;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.web3j.crypto.Keys;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmResults;
import timber.log.Timber;

@HiltViewModel
public class WalletViewModel extends BaseViewModel
{
    public static long BALANCE_BACKUP_CHECK_INTERVAL = 5 * DateUtils.MINUTE_IN_MILLIS;
    public static double VALUE_THRESHOLD = 200.0; //$200 USD value is difference between red and grey backup warnings

    private final MutableLiveData<TokenCardMeta[]> tokens = new MutableLiveData<>();
    private final MutableLiveData<TokenCardMeta[]> updatedTokens = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<GenericWalletInteract.BackupLevel> backupEvent = new MutableLiveData<>();
    private final MutableLiveData<Pair<Double, Double>> fiatValues = new MutableLiveData<>();
    private final FetchTokensInteract fetchTokensInteract;
    private final TokensMappingRepositoryType tokensMappingRepository;
    private final TokenDetailRouter tokenDetailRouter;
    private final GenericWalletInteract genericWalletInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;
    private final ChangeTokenEnableInteract changeTokenEnableInteract;
    private final PreferenceRepositoryType preferenceRepository;
    private final MyAddressRouter myAddressRouter;
    private final CoinbasePayRouter coinbasePayRouter;
    private final ManageWalletsRouter manageWalletsRouter;
    private final RealmManager realmManager;
    private final OnRampRepositoryType onRampRepository;
    private long lastBackupCheck = 0;
    private long lastTokenFetchTime = 0;
    private BottomSheetDialog dialog;
    private final AWWalletConnectClient awWalletConnectClient;
    @Nullable
    private Disposable balanceUpdateCheck;

    @Inject
    WalletViewModel(
            FetchTokensInteract fetchTokensInteract,
            TokenDetailRouter tokenDetailRouter,
            GenericWalletInteract genericWalletInteract,
            AssetDefinitionService assetDefinitionService,
            TokensService tokensService,
            ChangeTokenEnableInteract changeTokenEnableInteract,
            MyAddressRouter myAddressRouter,
            CoinbasePayRouter coinbasePayRouter,
            ManageWalletsRouter manageWalletsRouter,
            PreferenceRepositoryType preferenceRepository,
            RealmManager realmManager,
            OnRampRepositoryType onRampRepository,
            AnalyticsServiceType analyticsService,
            AWWalletConnectClient awWalletConnectClient,
            TokensMappingRepositoryType tokensMappingRepository)
    {
        this.fetchTokensInteract = fetchTokensInteract;
        this.tokenDetailRouter = tokenDetailRouter;
        this.genericWalletInteract = genericWalletInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
        this.changeTokenEnableInteract = changeTokenEnableInteract;
        this.myAddressRouter = myAddressRouter;
        this.coinbasePayRouter = coinbasePayRouter;
        this.manageWalletsRouter = manageWalletsRouter;
        this.preferenceRepository = preferenceRepository;
        this.realmManager = realmManager;
        this.onRampRepository = onRampRepository;
        this.awWalletConnectClient = awWalletConnectClient;
        this.tokensMappingRepository = tokensMappingRepository;
        setAnalyticsService(analyticsService);
    }

    public LiveData<TokenCardMeta[]> tokens()
    {
        return tokens;
    }

    public LiveData<TokenCardMeta[]> onUpdatedTokens()
    {
        return updatedTokens;
    }

    public LiveData<Wallet> defaultWallet()
    {
        return defaultWallet;
    }

    public LiveData<GenericWalletInteract.BackupLevel> backupEvent()
    {
        return backupEvent;
    }

    public LiveData<Pair<Double, Double>> onFiatValues()
    {
        return fiatValues;
    }

    public String getWalletAddr()
    {
        return defaultWallet.getValue() != null ? defaultWallet.getValue().address : "";
    }

    public WalletType getWalletType()
    {
        return defaultWallet.getValue() != null ? defaultWallet.getValue().type : WalletType.KEYSTORE;
    }

    public void prepare()
    {
        lastBackupCheck = System.currentTimeMillis() - BALANCE_BACKUP_CHECK_INTERVAL + 5 * DateUtils.SECOND_IN_MILLIS;
        //load the activity meta list
        disposable = genericWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    public void reloadTokens()
    {
        tokensService.startUpdateCycle();
        if (defaultWallet.getValue() != null)
        {
            fetchTokens(defaultWallet().getValue());
        }
        else
        {
            prepare();
        }
    }

    private void onDefaultWallet(Wallet wallet)
    {
        tokensService.setCurrentAddress(wallet.address);
        assetDefinitionService.startEventListener();
        defaultWallet.postValue(wallet);
        tokensService.startUpdateCycle();
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
        lastTokenFetchTime = System.currentTimeMillis();
        tokens.postValue(metaTokens);
        tokensService.updateTickers();
    }

    public void searchTokens(String search)
    {
        disposable =
                fetchTokensInteract.searchTokenMetas(defaultWallet.getValue(), tokensService.getNetworkFilters(), search)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onTokenMetas, this::onError);
    }

    public void startUpdateListener()
    {
        if (balanceUpdateCheck == null || balanceUpdateCheck.isDisposed())
        {
            balanceUpdateCheck = Observable.interval(2, 2, TimeUnit.SECONDS) //check every 2 seconds for new tokens
                    .doOnNext(l -> checkTokenUpdates()).subscribe();
        }
    }

    public void stopUpdateListener()
    {
        if (balanceUpdateCheck != null && !balanceUpdateCheck.isDisposed())
        {
            balanceUpdateCheck.dispose();
            balanceUpdateCheck = null;
        }
    }

    private void checkTokenUpdates()
    {
        if (defaultWallet.getValue() == null) return;

        disposable = getUpdatedTokenMetas()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(updatedTokens::postValue, this::onError);
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

    public void setTokenEnabled(Token token, boolean enabled)
    {
        changeTokenEnableInteract.setEnable(defaultWallet.getValue(), token.getContractAddress(), enabled);
        token.tokenInfo.isEnabled = enabled;
    }

    public void showBuyEthOptions(Activity activity)
    {
        coinbasePayRouter.buyFromSelectedChain(activity, CoinbasePayRepository.Blockchains.ETHEREUM);
    }

    public void showMyAddress(Activity context)
    {
        // show bottomsheet dialog
        WalletFragmentActionsView actionsView = new WalletFragmentActionsView(context);
        actionsView.setOnCopyWalletAddressClickListener(v -> {
            dialog.dismiss();
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(KEY_ADDRESS, Keys.toChecksumAddress(getWalletAddr()));
            if (clipboard != null)
            {
                clipboard.setPrimaryClip(clip);
            }

            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
        });
        actionsView.setOnShowMyWalletAddressClickListener(v -> {
            dialog.dismiss();
            myAddressRouter.open(context, defaultWallet.getValue());
        });
        actionsView.setOnAddHideTokensClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(context, TokenManagementActivity.class);
            intent.putExtra(EXTRA_ADDRESS, getWalletAddr());
            context.startActivityForResult(intent, C.ADDED_TOKEN_RETURN);
        });
        actionsView.setOnRenameThisWalletClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(context, NameThisWalletActivity.class);
            context.startActivity(intent);
        });

        dialog = new BottomSheetDialog(context);
        dialog.setContentView(actionsView);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        BottomSheetBehavior<?> behavior = BottomSheetBehavior.from((View) actionsView.getParent());
        dialog.setOnShowListener(dialog -> behavior.setPeekHeight(actionsView.getHeight()));
        dialog.show();
    }

    public void showQRCodeScanning(Activity activity)
    {
        Intent intent = new Intent(activity, QRScannerActivity.class);
        intent.putExtra(C.EXTRA_UNIVERSAL_SCAN, true);
        intent.putExtra(QrScanSource.KEY, QrScanSource.WALLET_SCREEN.getValue());
        activity.startActivityForResult(intent, C.REQUEST_UNIVERSAL_SCAN);
    }

    public TokenGroup getTokenGroup(long chainId, String address)
    {
        return tokensService.getTokenGroup(tokensService.getToken(chainId, address));
    }

    public void showTokenDetail(Activity activity, Token token)
    {
        boolean hasDefinition = assetDefinitionService.hasDefinition(token);
        switch (token.getInterfaceSpec())
        {
            case ETHEREUM:
            case ERC20:
            case CURRENCY:
            case DYNAMIC_CONTRACT:
            case LEGACY_DYNAMIC_CONTRACT:
            case ETHEREUM_INVISIBLE:
            case MAYBE_ERC20:
                tokenDetailRouter.open(activity, token.getAddress(), token.tokenInfo.symbol, token.tokenInfo.decimals,
                        !token.isEthereum(), defaultWallet.getValue(), token, hasDefinition);
                break;

            case ERC1155:
                tokenDetailRouter.open(activity, token, defaultWallet.getValue(), hasDefinition);
                break;

            case ATTESTATION:
                tokenDetailRouter.openAttestation(activity, token, defaultWallet.getValue(), new NFTAsset((Attestation)token));
                break;

            case ERC721:
            case ERC721_LEGACY:
            case ERC721_TICKET:
            case ERC721_UNDETERMINED:
            case ERC721_ENUMERABLE:
                tokenDetailRouter.open(activity, token, defaultWallet.getValue(), false);
                break;

            case ERC875_LEGACY:
            case ERC875:
                tokenDetailRouter.openLegacyToken(activity, token, defaultWallet.getValue());
                break;

            case NOT_SET:
            case OTHER:
            case DELETED_ACCOUNT:
            case CREATION:
                break;
        }
    }

    public void checkBackup(double fiatValue)
    {
        if (TextUtils.isEmpty(getWalletAddr()) || System.currentTimeMillis() < (lastBackupCheck + BALANCE_BACKUP_CHECK_INTERVAL))
            return;
        lastBackupCheck = System.currentTimeMillis();
        double walletUSDValue = tokensService.convertToUSD(fiatValue);

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
        tokensService.onWalletRefreshSwipe();
    }

    public boolean isChainToken(long chainId, String tokenAddress)
    {
        return tokensService.isChainToken(chainId, tokenAddress);
    }

    public void calculateFiatValues()
    {
        disposable = tokensService.getFiatValuePair()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fiatValues::postValue);
    }

    public void showManageWallets(Context context, boolean clearStack)
    {
        manageWalletsRouter.open(context, clearStack);
    }

    public boolean isMarshMallowWarningShown()
    {
        return preferenceRepository.isMarshMallowWarningShown();
    }

    public void setMarshMallowWarning(boolean shown)
    {
        preferenceRepository.setMarshMallowWarning(shown);
    }

    public void saveAvatar(Wallet wallet)
    {
        genericWalletInteract.updateWalletItem(wallet, WalletItem.ENS_AVATAR, () -> {});
    }

    public Intent getBuyIntent(String address)
    {
        Intent intent = new Intent();
        intent.putExtra(C.DAPP_URL_LOAD, onRampRepository.getUri(address, null));
        return intent;
    }

    public MutableLiveData<List<WalletConnectSessionItem>> activeWalletConnectSessions()
    {
        return awWalletConnectClient.sessionItemMutableLiveData();
    }

    public void checkDeleteMetas(TokenCardMeta[] metas)
    {
        List<TokenCardMeta> metasToDelete = new ArrayList<>();
        for (TokenCardMeta meta : metas)
        {
            if (meta.balance.equals("-2"))
            {
                metasToDelete.add(meta);
            }
        }

        if (metasToDelete.size() > 0)
        {
            disposable = tokensService.deleteTokens(metasToDelete)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe();
        }
    }

    private Single<TokenCardMeta[]> getUpdatedTokenMetas()
    {
        return Single.fromCallable(() -> {
            List<TokenCardMeta> tokenMetas = new ArrayList<>();
            try (Realm r = realmManager.getRealmInstance(defaultWallet.getValue()))
            {
                RealmResults<RealmToken> updatedTokens = r.where(RealmToken.class).equalTo("isEnabled", true)
                        .like("address", ADDRESS_FORMAT)
                        .greaterThan("updatedTime", lastTokenFetchTime)
                        .findAll();

                for (RealmToken t : updatedTokens)
                {
                    if (!tokensService.getNetworkFilters().contains(t.getChainId()))
                    {
                        continue;
                    }

                    String balance = TokensRealmSource.convertStringBalance(t.getBalance(), t.getContractType());

                    TokenCardMeta meta = new TokenCardMeta(t.getChainId(), t.getTokenAddress(), balance,
                            t.getUpdateTime(), assetDefinitionService, t.getName(), t.getSymbol(), t.getContractType(),
                            getTokenGroup(t.getChainId(), t.getTokenAddress()));
                    meta.lastTxUpdate = t.getLastTxTime();
                    meta.isEnabled = t.isEnabled();
                    tokenMetas.add(meta);
                    if (t.getBalanceUpdateTime() > lastTokenFetchTime)
                    {
                        lastTokenFetchTime = t.getBalanceUpdateTime() + 1;
                    }
                }
            }

            return tokenMetas.toArray(new TokenCardMeta[0]);
        });
    }

    public void removeTokenMetaItem(String tokenKeyId)
    {
        final String tokenKey = tokenKeyId.endsWith(Attestation.ATTESTATION_SUFFIX) ? tokenKeyId.substring(0, tokenKeyId.length() - Attestation.ATTESTATION_SUFFIX.length())
                : tokenKeyId;

        try (Realm realm = realmManager.getRealmInstance(defaultWallet.getValue()))
        {
            realm.executeTransactionAsync(r -> {
                RealmAttestation realmAttn = r.where(RealmAttestation.class)
                        .equalTo("address", tokenKey)
                        .findFirst();

                if (realmAttn != null)
                {
                    realmAttn.deleteFromRealm();
                }
            });
        }
    }

    public void removeAttestation(Token token)
    {
        try (Realm realm = realmManager.getRealmInstance(defaultWallet.getValue()))
        {
            realm.executeTransactionAsync(r -> {
                String key = ((Attestation)token).getDatabaseKey();
                RealmAttestation realmAttn = r.where(RealmAttestation.class)
                        .equalTo("address", key)
                        .findFirst();

                if (realmAttn != null)
                {
                    realmAttn.deleteFromRealm();
                }
            });
        }
    }
}
