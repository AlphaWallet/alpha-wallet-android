package com.alphawallet.app.viewmodel;

import static com.alphawallet.app.C.EXTRA_ADDRESS;
import static com.alphawallet.app.repository.TokensRealmSource.databaseKey;
import static com.alphawallet.app.widget.CopyTextView.KEY_ADDRESS;

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

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.interact.ChangeTokenEnableInteract;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.OnRampRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.router.ManageWalletsRouter;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.router.TokenDetailRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.RealmManager;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.NameThisWalletActivity;
import com.alphawallet.app.ui.QRScanning.QRScanner;
import com.alphawallet.app.ui.TokenManagementActivity;
import com.alphawallet.app.widget.WalletFragmentActionsView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

@HiltViewModel
public class WalletViewModel extends BaseViewModel
{
    public static long BALANCE_BACKUP_CHECK_INTERVAL = 5 * DateUtils.MINUTE_IN_MILLIS;
    public static double VALUE_THRESHOLD = 200.0; //$200 USD value is difference between red and grey backup warnings

    private final MutableLiveData<TokenCardMeta[]> tokens = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<GenericWalletInteract.BackupLevel> backupEvent = new MutableLiveData<>();
    private final MutableLiveData<Pair<Double, Double>> fiatValues = new MutableLiveData<>();

    private final FetchTokensInteract fetchTokensInteract;
    private final TokenDetailRouter tokenDetailRouter;
    private final GenericWalletInteract genericWalletInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;
    private final ChangeTokenEnableInteract changeTokenEnableInteract;
    private final PreferenceRepositoryType preferenceRepository;
    private final MyAddressRouter myAddressRouter;
    private final ManageWalletsRouter manageWalletsRouter;
    private final RealmManager realmManager;
    private long lastBackupCheck = 0;
    private BottomSheetDialog dialog;
    private final OnRampRepositoryType onRampRepository;

    @Inject
    WalletViewModel(
            FetchTokensInteract fetchTokensInteract,
            TokenDetailRouter tokenDetailRouter,
            GenericWalletInteract genericWalletInteract,
            AssetDefinitionService assetDefinitionService,
            TokensService tokensService,
            ChangeTokenEnableInteract changeTokenEnableInteract,
            MyAddressRouter myAddressRouter,
            ManageWalletsRouter manageWalletsRouter,
            PreferenceRepositoryType preferenceRepository,
            RealmManager realmManager,
            OnRampRepositoryType onRampRepository)
    {
        this.fetchTokensInteract = fetchTokensInteract;
        this.tokenDetailRouter = tokenDetailRouter;
        this.genericWalletInteract = genericWalletInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
        this.changeTokenEnableInteract = changeTokenEnableInteract;
        this.myAddressRouter = myAddressRouter;
        this.manageWalletsRouter = manageWalletsRouter;
        this.preferenceRepository = preferenceRepository;
        this.realmManager = realmManager;
        this.onRampRepository = onRampRepository;
    }

    public LiveData<TokenCardMeta[]> tokens() {
        return tokens;
    }
    public LiveData<Wallet> defaultWallet() { return defaultWallet; }
    public LiveData<GenericWalletInteract.BackupLevel> backupEvent() { return backupEvent; }
    public LiveData<Pair<Double, Double>> onFiatValues() { return fiatValues; }

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

    public void reloadTokens()
    {
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
        // show bottomsheet dialog
        WalletFragmentActionsView actionsView = new WalletFragmentActionsView(context);
        actionsView.setOnCopyWalletAddressClickListener(v -> {
            dialog.dismiss();
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(KEY_ADDRESS, getWalletAddr());
            if (clipboard != null) {
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
            context.startActivity(intent);
        });
        actionsView.setOnRenameThisWalletClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(context, NameThisWalletActivity.class);
            context.startActivity(intent);
        });

        dialog = new BottomSheetDialog(context, R.style.FullscreenBottomSheetDialogStyle);
        dialog.setContentView(actionsView);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        BottomSheetBehavior<?> behavior = BottomSheetBehavior.from((View) actionsView.getParent());
        dialog.setOnShowListener(dialog -> behavior.setPeekHeight(actionsView.getHeight()));
        dialog.show();
    }

    public void showQRCodeScanning(Activity activity) {
        Intent intent = new Intent(activity, QRScanner.class);
        intent.putExtra(C.EXTRA_UNIVERSAL_SCAN, true);
        activity.startActivityForResult(intent, C.REQUEST_UNIVERSAL_SCAN);
    }

    public Realm getRealmInstance()
    {
        return realmManager.getRealmInstance(getWallet());
    }

    public TokenGroup getTokenGroup(long chainId, String address)
    {
        return tokensService.getTokenGroup(tokensService.getToken(chainId, address));
    }

    public void showTokenDetail(Activity activity, Token token)
    {
        boolean hasDefinition = assetDefinitionService.hasDefinition(token.tokenInfo.chainId, token.getAddress());
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

            case ERC721:
            case ERC875_LEGACY:
            case ERC875:
            case ERC721_LEGACY:
            case ERC721_TICKET:
            case ERC721_UNDETERMINED:
                tokenDetailRouter.open(activity, token, defaultWallet.getValue(), false); //TODO: Fold this into tokenDetailRouter
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
        if (TextUtils.isEmpty(getWalletAddr()) || System.currentTimeMillis() < (lastBackupCheck + BALANCE_BACKUP_CHECK_INTERVAL)) return;
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

    public boolean isMarshMallowWarningShown() {
        return preferenceRepository.isMarshMallowWarningShown();
    }

    public void setMarshMallowWarning(boolean shown) {
        preferenceRepository.setMarshMallowWarning(shown);
    }

    public void saveAvatar(Wallet wallet)
    {
        genericWalletInteract.updateWalletInfo(wallet, wallet.name, () -> { });
    }

    public Intent getBuyIntent(String address) {
        Intent intent = new Intent();
        intent.putExtra(C.DAPP_URL_LOAD, onRampRepository.getUri(address, null));
        return intent;
    }
}
