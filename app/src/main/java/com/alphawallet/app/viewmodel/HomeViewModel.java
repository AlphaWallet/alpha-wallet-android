package com.alphawallet.app.viewmodel;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.analytics.Analytics;
import com.alphawallet.app.entity.AnalyticsProperties;
import com.alphawallet.app.entity.CryptoFunctions;
import com.alphawallet.app.entity.EIP681Type;
import com.alphawallet.app.entity.FragmentMessenger;
import com.alphawallet.app.entity.GitHubRelease;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.QRResult;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.Version;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.analytics.QrScanResultType;
import com.alphawallet.app.entity.attestation.ImportAttestation;
import com.alphawallet.app.interact.FetchWalletsInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.CurrencyRepositoryType;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.LocaleRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.router.ExternalBrowserRouter;
import com.alphawallet.app.router.ImportTokenRouter;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.service.AlphaWalletNotificationService;
import com.alphawallet.app.service.AnalyticsServiceType;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.RealmManager;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.service.TransactionsService;
import com.alphawallet.app.ui.AddTokenActivity;
import com.alphawallet.app.ui.HomeActivity;
import com.alphawallet.app.ui.ImportWalletActivity;
import com.alphawallet.app.ui.SendActivity;
import com.alphawallet.app.ui.WalletConnectV2Activity;
import com.alphawallet.app.util.QRParser;
import com.alphawallet.app.util.RateApp;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.util.ens.AWEnsResolver;
import com.alphawallet.app.widget.EmailPromptView;
import com.alphawallet.app.widget.QRCodeActionsView;
import com.alphawallet.app.widget.WhatsNewView;
import com.alphawallet.token.entity.ContractInfo;
import com.alphawallet.token.entity.MagicLinkData;
import com.alphawallet.token.tools.ParseMagicLink;
import com.alphawallet.token.tools.TokenDefinition;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.web3j.utils.Numeric;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import timber.log.Timber;
import wallet.core.jni.Hash;

@HiltViewModel
public class HomeViewModel extends BaseViewModel
{
    public static final String ALPHAWALLET_DIR = "AlphaWallet";
    private static final long ECHO_MAX_MILLIS = 250; //if second QRCODE read comes before 250 millis, reject
    private final MutableLiveData<Transaction[]> transactions = new MutableLiveData<>();
    private final MutableLiveData<String> backUpMessage = new MutableLiveData<>();

    private final PreferenceRepositoryType preferenceRepository;
    private final ImportTokenRouter importTokenRouter;
    private final LocaleRepositoryType localeRepository;
    private final AssetDefinitionService assetDefinitionService;
    private final GenericWalletInteract genericWalletInteract;
    private final FetchWalletsInteract fetchWalletsInteract;
    private final CurrencyRepositoryType currencyRepository;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final TransactionsService transactionsService;
    private final MyAddressRouter myAddressRouter;
    private final ExternalBrowserRouter externalBrowserRouter;
    private final OkHttpClient httpClient;
    private final RealmManager realmManager;
    private final TokensService tokensService;
    private final GasService gasService;
    private final AlphaWalletNotificationService alphaWalletNotificationService;
    private final MutableLiveData<String> walletName = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Boolean> splashActivity = new MutableLiveData<>();
    private final MutableLiveData<String> updateAvailable = new MutableLiveData<>();
    private CryptoFunctions cryptoFunctions;
    private ParseMagicLink parser;
    private BottomSheetDialog dialog;
    private long qrReadTime = Long.MAX_VALUE;
    private Disposable githubRead;

    @Inject
    HomeViewModel(
            PreferenceRepositoryType preferenceRepository,
            LocaleRepositoryType localeRepository,
            ImportTokenRouter importTokenRouter,
            AssetDefinitionService assetDefinitionService,
            GenericWalletInteract genericWalletInteract,
            FetchWalletsInteract fetchWalletsInteract,
            CurrencyRepositoryType currencyRepository,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            MyAddressRouter myAddressRouter,
            TransactionsService transactionsService,
            AnalyticsServiceType analyticsService,
            ExternalBrowserRouter externalBrowserRouter,
            OkHttpClient httpClient,
            RealmManager realmManager,
            TokensService tokensService,
            AlphaWalletNotificationService alphaWalletNotificationService,
            GasService gasService)
    {
        this.preferenceRepository = preferenceRepository;
        this.importTokenRouter = importTokenRouter;
        this.localeRepository = localeRepository;
        this.assetDefinitionService = assetDefinitionService;
        this.genericWalletInteract = genericWalletInteract;
        this.fetchWalletsInteract = fetchWalletsInteract;
        this.currencyRepository = currencyRepository;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.myAddressRouter = myAddressRouter;
        this.transactionsService = transactionsService;
        this.externalBrowserRouter = externalBrowserRouter;
        this.httpClient = httpClient;
        this.realmManager = realmManager;
        this.alphaWalletNotificationService = alphaWalletNotificationService;
        setAnalyticsService(analyticsService);
        this.preferenceRepository.incrementLaunchCount();
        this.tokensService = tokensService;
        this.gasService = gasService;
    }

    @Override
    protected void onCleared()
    {
        super.onCleared();
        if (githubRead != null && !githubRead.isDisposed())
        {
            githubRead.dispose();
        }
    }

    public LiveData<Transaction[]> transactions()
    {
        return transactions;
    }

    public LiveData<String> backUpMessage()
    {
        return backUpMessage;
    }

    public LiveData<Boolean> splashReset()
    {
        return splashActivity;
    }

    public LiveData<String> updateAvailable()
    {
        return updateAvailable;
    }

    public LiveData<Wallet> defaultWallet()
    {
        return defaultWallet;
    }

    public GasService getGasService() { return gasService; }

    public void prepare()
    {
        progress.postValue(false);
        disposable = genericWalletInteract
            .find()
            .subscribe(this::onDefaultWallet, this::onError);
    }

    public void onClean()
    {

    }

    private void onDefaultWallet(final Wallet wallet)
    {
        preferenceRepository.setWatchOnly(wallet.watchOnly());
        defaultWallet.setValue(wallet);
    }

    public void showImportLink(Activity activity, String importData)
    {
        disposable = genericWalletInteract
            .find().toObservable()
            .filter(wallet -> checkWalletNotEqual(wallet, importData))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(wallet -> importLink(activity, importData), this::onError);
    }

    private boolean checkWalletNotEqual(Wallet wallet, String importData)
    {
        boolean filterPass = false;

        try
        {
            if (cryptoFunctions == null)
            {
                cryptoFunctions = new CryptoFunctions();
            }
            if (parser == null)
            {
                parser = new ParseMagicLink(cryptoFunctions, EthereumNetworkRepository.extraChains());
            }

            MagicLinkData data = parser.parseUniversalLink(importData);
            String linkAddress = parser.getOwnerKey(data);

            if (Utils.isAddressValid(data.contractAddress))
            {
                filterPass = !wallet.address.equals(linkAddress);
            }
        }
        catch (Exception e)
        {
            Timber.e(e);
        }

        return filterPass;
    }

    private void importLink(Activity activity, String importData)
    {
        importTokenRouter.open(activity, importData);
    }

    public void restartHomeActivity(Context context)
    {
        //restart activity
        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    public void getWalletName(Context context)
    {
        disposable = fetchWalletsInteract
            .getWallet(preferenceRepository.getCurrentWalletAddress())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(wallet -> updateWalletTitle(context, wallet), this::walletError);
    }

    private void walletError(Throwable throwable)
    {
        //no wallets
        splashActivity.postValue(true);
    }

    private void updateWalletTitle(Context context, Wallet wallet)
    {
        transactionsService.changeWallet(wallet);
        boolean usingDefaultName = Utils.isDefaultName(wallet.name, context);
        if (!TextUtils.isEmpty(wallet.name) && !usingDefaultName)
        {
            walletName.postValue(wallet.name);
        }
        else if (!TextUtils.isEmpty(wallet.ENSname))
        {
            walletName.postValue(wallet.ENSname);
        }
        else
        {
            walletName.postValue("");
            //check for ENS name
            new AWEnsResolver(TokenRepository.getWeb3jService(MAINNET_ID), context)
                .reverseResolveEns(wallet.address)
                .map(ensName -> {
                    wallet.ENSname = ensName;
                    return wallet;
                })
                .flatMap(fetchWalletsInteract::updateENS) //store the ENS name
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(updatedWallet -> {
                    String name = Utils.formatAddress(wallet.address);
                    if (!TextUtils.isEmpty(updatedWallet.ENSname))
                    {
                        name = updatedWallet.ENSname;
                    }
                    walletName.postValue(name);
                }, this::onENSError).isDisposed();
        }
    }

    public LiveData<String> walletName()
    {
        return walletName;
    }

    public void checkIsBackedUp(String walletAddress)
    {
        genericWalletInteract.getWalletNeedsBackup(walletAddress)
            .subscribe(backUpMessage::postValue).isDisposed();
    }

    public boolean isFindWalletAddressDialogShown()
    {
        return preferenceRepository.isFindWalletAddressDialogShown();
    }

    public void setFindWalletAddressDialogShown(boolean isShown)
    {
        preferenceRepository.setFindWalletAddressDialogShown(isShown);
    }

    private void onENSError(Throwable throwable)
    {
        Timber.e(throwable);
    }

    public void setErrorCallback(FragmentMessenger callback)
    {
        assetDefinitionService.setErrorCallback(callback);
    }

    public void handleQRCode(HomeActivity activity, String qrCode)
    {
        try
        {
            AnalyticsProperties props = new AnalyticsProperties();
            QRParser parser = QRParser.getInstance(EthereumNetworkBase.extraChains());
            QRResult qrResult = parser.parse(qrCode);
            switch (qrResult.type)
            {
                case ATTESTATION:
                case EAS_ATTESTATION:
                    handleImportAttestation(activity, qrResult);
                    break;
                case ADDRESS:
                    props.put(QrScanResultType.KEY, QrScanResultType.ADDRESS.getValue());
                    track(Analytics.Action.SCAN_QR_CODE_SUCCESS, props);

                    //showSend(activity, qrResult); //For now, direct an ETH address to send screen
                    //TODO: Issue #1504: bottom-screen popup to choose between: Add to Address book, Sent to Address, or Watch Wallet
                    showActionSheet(activity, qrResult);
                    break;
                case PAYMENT:
                case TRANSFER:
                    props.put(QrScanResultType.KEY, QrScanResultType.ADDRESS_OR_EIP_681.getValue());
                    track(Analytics.Action.SCAN_QR_CODE_SUCCESS, props);

                    showSend(activity, qrResult);
                    break;
                case FUNCTION_CALL:
                    props.put(QrScanResultType.KEY, QrScanResultType.ADDRESS_OR_EIP_681.getValue());
                    track(Analytics.Action.SCAN_QR_CODE_SUCCESS, props);

                    //TODO: Handle via ConfirmationActivity, need to generate function signature + data then call ConfirmationActivity
                    //TODO: Code to generate the function signature will look like the code in generateTransactionFunction
                    break;
                case URL:
                    props.put(QrScanResultType.KEY, QrScanResultType.URL.getValue());
                    track(Analytics.Action.SCAN_QR_CODE_SUCCESS, props);
                    activity.onBrowserWithURL(qrCode);
                    break;
                case MAGIC_LINK:
                    showImportLink(activity, qrCode);
                    break;
                case OTHER:
                    qrCode = null;
                    break;
                case OTHER_PROTOCOL:
                    break;
                case WALLET_CONNECT:
                    startWalletConnect(activity, qrCode);
                    break;
            }

        }
        catch (Exception e)
        {
            Timber.e(e);
            qrCode = null;
        }

        if (qrCode == null)
        {
            Toast.makeText(activity, R.string.toast_invalid_code, Toast.LENGTH_SHORT).show();
        }
    }

    public boolean requiresProcessing(String qrCode)
    {
        //prevent echoes
        long currentTime = System.currentTimeMillis();
        if (qrCode == null || ((qrReadTime + ECHO_MAX_MILLIS) > currentTime))
        {
            Timber.d("QR Read: %s", (qrReadTime - currentTime));
            return false;
        }

        qrReadTime = currentTime;
        return true;
    }

    private void startWalletConnect(Activity activity, String qrCode)
    {
        Intent intent = new Intent(activity, WalletConnectV2Activity.class);
        intent.putExtra("url", qrCode);

        activity.startActivity(intent);
    }

    private void showActionSheet(Activity activity, QRResult qrResult)
    {
        View.OnClickListener listener = v -> {
            if (v.getId() == R.id.send_to_this_address_action)
            {
                showSend(activity, qrResult);
            }
            else if (v.getId() == R.id.add_custom_token_action)
            {
                Intent intent = new Intent(activity, AddTokenActivity.class);
                intent.putExtra(C.EXTRA_QR_CODE, qrResult.getAddress());
                activity.startActivityForResult(intent, C.ADDED_TOKEN_RETURN);
            }
            else if (v.getId() == R.id.watch_account_action)
            {
                Intent intent = new Intent(activity, ImportWalletActivity.class);
                intent.putExtra(C.EXTRA_QR_CODE, qrResult.getAddress());
                intent.putExtra(C.EXTRA_STATE, "watch");
                activity.startActivity(intent);
            }
            else if (v.getId() == R.id.open_in_etherscan_action)
            {
                NetworkInfo info = ethereumNetworkRepository.getNetworkByChain(qrResult.chainId);
                if (info == null) return;

                Uri blockChainInfoUrl = info.getEtherscanAddressUri(qrResult.getAddress());

                if (blockChainInfoUrl != Uri.EMPTY)
                {
                    externalBrowserRouter.open(activity, blockChainInfoUrl);
                }
            }
            else if (v.getId() == R.id.close_action)
            {
                //NOP
            }

            dialog.dismiss();
        };

        QRCodeActionsView contentView = new QRCodeActionsView(activity);

        contentView.setOnSendToAddressClickListener(listener);
        contentView.setOnAddCustonTokenClickListener(listener);


        contentView.setOnWatchWalletClickListener(listener);

        contentView.setOnOpenInEtherscanClickListener(listener);

        contentView.setOnCloseActionListener(listener);

        dialog = new BottomSheetDialog(activity);
        dialog.setContentView(contentView);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from((View) contentView.getParent());
        dialog.setOnShowListener(dialog -> behavior.setPeekHeight(contentView.getHeight()));
        dialog.show();
    }

    public void showSend(Activity ctx, QRResult result)
    {
        Intent intent = new Intent(ctx, SendActivity.class);
        boolean sendingTokens = (result.getFunction() != null && result.getFunction().length() > 0);
        String address = defaultWallet.getValue().address;
        int decimals = 18;

        intent.putExtra(C.EXTRA_SENDING_TOKENS, sendingTokens);
        intent.putExtra(C.EXTRA_CONTRACT_ADDRESS, address);
        intent.putExtra(C.EXTRA_NETWORKID, result.chainId);
        intent.putExtra(C.EXTRA_SYMBOL, ethereumNetworkRepository.getNetworkByChain(result.chainId).symbol);
        intent.putExtra(C.EXTRA_DECIMALS, decimals);
        intent.putExtra(C.Key.WALLET, defaultWallet.getValue());
        intent.putExtra(C.EXTRA_AMOUNT, result);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        ctx.startActivity(intent);
    }

    public void showMyAddress(Activity activity)
    {
        myAddressRouter.open(activity, defaultWallet.getValue());
    }

    /**
     * This method will uniquely identify the device by creating an ID and store in preference.
     * This will be changed if user reinstall application or clear the storage explicitly.
     **/
    public void identify()
    {
        String uuid = preferenceRepository.getUniqueId();

        if (uuid.isEmpty())
        {
            uuid = UUID.randomUUID().toString();
        }

        preferenceRepository.setUniqueId(uuid);

        identify(uuid);
    }

    public void checkTransactionEngine()
    {
        transactionsService.resumeFocus();
    }

    public void stopTransactionUpdate()
    {
        transactionsService.stopService();
    }

    public void outOfFocus()
    {
        transactionsService.lostFocus();
    }

    public boolean fullScreenSelected()
    {
        return preferenceRepository.getFullScreenState();
    }

    public void tryToShowRateAppDialog(Activity context)
    {
        //only if installed from PlayStore (checked within the showRateTheApp method)
        RateApp.showRateTheApp(context, preferenceRepository, false);
    }

    public int getUpdateWarnings()
    {
        return preferenceRepository.getUpdateWarningCount();
    }

    public void setUpdateWarningCount(int warns)
    {
        preferenceRepository.setUpdateWarningCount(warns);
    }

    public void setInstallTime(int time)
    {
        preferenceRepository.setInstallTime(time);
    }

    public void restartTokensService()
    {
        transactionsService.restartService();
    }

    public void storeCurrentFragmentId(int ordinal)
    {
        preferenceRepository.storeLastFragmentPage(ordinal);
    }

    public int getLastFragmentId()
    {
        return preferenceRepository.getLastFragmentPage();
    }

    public void tryToShowEmailPrompt(Context context, View successOverlay, Handler handler, Runnable onSuccessRunnable)
    {
        if (preferenceRepository.getLaunchCount() == 4)
        {
            EmailPromptView emailPromptView = new EmailPromptView(context, successOverlay, handler, onSuccessRunnable);
            BottomSheetDialog emailPromptDialog = new BottomSheetDialog(context);
            emailPromptDialog.setContentView(emailPromptView);
            emailPromptDialog.setCancelable(true);
            emailPromptDialog.setCanceledOnTouchOutside(true);
            emailPromptView.setParentDialog(emailPromptDialog);
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from((View) emailPromptView.getParent());
            emailPromptDialog.setOnShowListener(dialog -> behavior.setPeekHeight(emailPromptView.getHeight()));
            emailPromptDialog.show();
        }
    }

    public void tryToShowWhatsNewDialog(Context context)
    {
        PackageInfo packageInfo;
        try
        {
            packageInfo = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0);

            int versionCode = packageInfo.versionCode;
            if (preferenceRepository.getLastVersionCode(versionCode) < versionCode)
            {
                Request request = getRequest();
                githubRead = Single.fromCallable(getGitHubReleases(request))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe((releases) -> {
                        if (!releases.isEmpty())
                        {
                            doShowWhatsNewDialog(context, releases);
                            preferenceRepository.setLastVersionCode(versionCode);
                        }
                    }, Timber::w);
            }
        }
        catch (PackageManager.NameNotFoundException e)
        {
            Timber.e(e);
        }
    }

    private void doShowWhatsNewDialog(Context context, List<GitHubRelease> releases)
    {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        WhatsNewView view = new WhatsNewView(context, releases, v -> dialog.dismiss(), true);
        dialog.setContentView(view);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from((View) view.getParent());
        dialog.setOnShowListener(d -> behavior.setPeekHeight(view.getHeight()));
        dialog.show();
    }

    @NonNull
    private Request getRequest()
    {
        return new Request.Builder()
            .header("Accept", "application/vnd.github.v3+json")
            .url("https://api.github.com/repos/alphawallet/alpha-wallet-android/releases")
            .get()
            .build();
    }

    private TokenDefinition parseFile(Context ctx, InputStream xmlInputStream) throws Exception
    {
        Locale locale = ctx.getResources().getConfiguration().getLocales().get(0);
        return new TokenDefinition(
            xmlInputStream, locale, null);
    }

    public void importScriptFile(HomeActivity ctx, Intent startIntent)
    {
        Uri uri = startIntent.getData();
        final ContentResolver contentResolver = ctx.getContentResolver();
        try
        {
            InputStream iStream = contentResolver.openInputStream(uri);
            TokenDefinition td = parseFile(ctx, iStream);
            if (td.holdingToken == null || td.holdingToken.length() == 0)
                return; //tokenscript with no holding token is currently meaningless. Is this always the case?

            //determine type of holding token
            String newFileName;
            ContractInfo info = td.contracts.get(td.holdingToken);
            byte[] preHash = td.getAttestationCollectionPreHash();
            if (preHash != null)
            {
                newFileName = Numeric.toHexString(Hash.keccak256(preHash))
                        + "-" + info.addresses.keySet().iterator().next();
            }
            else
            {
                //calculate using formula: "{address.lowercased}-{chainId}"
                newFileName = info.addresses.values().iterator().next().iterator().next() + "-"
                        + info.addresses.keySet().iterator().next();
            }

            newFileName = assetDefinitionService.getDebugPath(newFileName + ".tsml");

            //Store the new Definition
            try (FileOutputStream fos = new FileOutputStream(newFileName))
            {
                byte[] writeBuffer = new byte[32768];
                iStream = contentResolver.openInputStream(uri);
                while (iStream.available() > 0)
                {
                    fos.write(writeBuffer, 0, iStream.read(writeBuffer));
                }
                fos.flush();
            }

            iStream.close();

            //register new file
            assetDefinitionService.notifyNewScriptLoaded(newFileName);

            disposable = assetDefinitionService.resetAttributes(td)
                    .map(pf -> assetDefinitionService.updateAttestations(td))// do required update for attestations
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe();
        }
        catch (Exception e)
        {
            //import error
            ctx.tokenScriptError(e.getMessage());
        }
    }

    public void setWalletStartup()
    {
        TokensService.setWalletStartup();
    }

    public void setCurrencyAndLocale(Context context)
    {
        if (TextUtils.isEmpty(localeRepository.getUserPreferenceLocale()))
        {
            localeRepository.setLocale(context, localeRepository.getActiveLocale());
        }
        currencyRepository.setDefaultCurrency(preferenceRepository.getDefaultCurrency());
    }

    public boolean checkNewWallet(String address)
    {
        return preferenceRepository.isNewWallet(address);
    }

    public void setNewWallet(String address, boolean isNewWallet)
    {
        preferenceRepository.setNewWallet(address, isNewWallet);
    }

    public void checkLatestGithubRelease()
    {
        Request request = getRequest();
        githubRead = Single.fromCallable(getGitHubReleases(request))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((releases) -> {
                    if (!releases.isEmpty())
                    {
                        GitHubRelease latestRelease = releases.get(0);
                        if (latestRelease != null)
                        {
                            String latestTag = latestRelease.getTagName();
                            if (latestRelease.getTagName().charAt(0) == 'v')
                            {
                                latestTag = latestTag.substring(1);
                            }
                            Version latest = new Version(latestTag);
                            Version installed = new Version(BuildConfig.VERSION_NAME);

                            if (latest.compareTo(installed) > 0)
                            {
                                updateAvailable.postValue(latest.get());
                            }
                        }
                    }}, Timber::w);
    }

    @NonNull
    private Callable<List<GitHubRelease>> getGitHubReleases(Request request)
    {
        return () ->
        {
            try (okhttp3.Response response = httpClient.newCall(request)
                    .execute())
            {
                if (response.code() / 100 == 2)
                {
                    return new Gson().fromJson(response.body().string(), new TypeToken<List<GitHubRelease>>()
                    {
                    }.getType());
                }
                else
                {
                    return new ArrayList<>();
                }
            }
            catch (Exception e)
            {
                Timber.e(e);
            }
            return new ArrayList<>();
        };
    }

    public void subscribeToNotifications()
    {
        alphaWalletNotificationService.subscribe(com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID);
    }

    public void unsubscribeToNotifications()
    {
        alphaWalletNotificationService.unsubscribeToTopic(com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID);
    }

    public void setPostNotificationsPermissionRequested(String address)
    {
        preferenceRepository.setPostNotificationsPermissionRequested(address, true);
    }

    public boolean isPostNotificationsPermissionRequested(String address)
    {
        return preferenceRepository.isPostNotificationsPermissionRequested(address);
    }

    public boolean isWatchOnlyWallet()
    {
        return preferenceRepository.isWatchOnly();
    }

    private Single<Wallet> getCurrentWallet()
    {
        if (defaultWallet.getValue() != null)
        {
            return Single.fromCallable(defaultWallet::getValue);
        }
        else
        {
            return genericWalletInteract.find();
        }
    }

    private void handleImportAttestation(HomeActivity activity, QRResult qrResult)
    {
        disposable = getCurrentWallet()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(wallet -> completeImport(activity, wallet, qrResult));
    }

    private void completeImport(HomeActivity activity, Wallet wallet, QRResult qrResult)
    {
        if (wallet == null || wallet.watchOnly())
        {
            activity.importError(activity.getString(R.string.watch_wallet));
        }
        else
        {
            ImportAttestation attnImport = new ImportAttestation(assetDefinitionService, tokensService,
                    activity, wallet, realmManager, httpClient);

            //attempt to import the wallet
            attnImport.importAttestation(qrResult);
        }
    }

    public boolean handleSmartPass(HomeActivity homeActivity, String smartPassCandidate)
    {
        if (smartPassCandidate.startsWith(ImportAttestation.SMART_PASS_URL))
        {
            smartPassCandidate = smartPassCandidate.substring(ImportAttestation.SMART_PASS_URL.length()); //chop off leading URL
            QRResult result = new QRResult(smartPassCandidate);
            result.type = EIP681Type.EAS_ATTESTATION;
            String taglessAttestation = Utils.parseEASAttestation(smartPassCandidate);
            result.functionDetail = Utils.toAttestationJson(taglessAttestation);

            if (!TextUtils.isEmpty(result.functionDetail))
            {
                handleImportAttestation(homeActivity, result);
                return true;
            }
        }

        return false;
    }
}
