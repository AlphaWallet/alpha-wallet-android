package com.alphawallet.app.viewmodel;

import static com.alphawallet.app.C.Key.WALLET;
import static com.alphawallet.app.util.Utils.isValidUrl;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.analytics.Analytics;
import com.alphawallet.app.entity.DApp;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Operation;
import com.alphawallet.app.entity.QRResult;
import com.alphawallet.app.entity.SendTransactionInterface;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.analytics.QrScanSource;
import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.service.AnalyticsServiceType;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.AddEditDappActivity;
import com.alphawallet.app.ui.HomeActivity;
import com.alphawallet.app.ui.ImportTokenActivity;
import com.alphawallet.app.ui.MyAddressActivity;
import com.alphawallet.app.ui.QRScanning.QRScannerActivity;
import com.alphawallet.app.ui.SendActivity;
import com.alphawallet.app.ui.WalletConnectActivity;
import com.alphawallet.app.ui.WalletConnectV2Activity;
import com.alphawallet.app.util.DappBrowserUtils;
import com.alphawallet.app.walletconnect.util.WalletConnectHelper;
import com.alphawallet.app.web3.entity.WalletAddEthereumChainObject;
import com.alphawallet.app.web3.entity.Web3Transaction;

import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

@HiltViewModel
public class DappBrowserViewModel extends BaseViewModel
{
    private static final int BALANCE_CHECK_INTERVAL_SECONDS = 20;

    private final MutableLiveData<NetworkInfo> activeNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final GenericWalletInteract genericWalletInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final CreateTransactionInteract createTransactionInteract;
    private final TokensService tokensService;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final KeyService keyService;
    private final GasService gasService;

    @Nullable
    private Disposable balanceTimerDisposable;

    @Inject
    DappBrowserViewModel(
            GenericWalletInteract genericWalletInteract,
            AssetDefinitionService assetDefinitionService,
            CreateTransactionInteract createTransactionInteract,
            TokensService tokensService,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            KeyService keyService,
            GasService gasService,
            AnalyticsServiceType analyticsService)
    {
        this.genericWalletInteract = genericWalletInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.createTransactionInteract = createTransactionInteract;
        this.tokensService = tokensService;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.keyService = keyService;
        this.gasService = gasService;
        setAnalyticsService(analyticsService);
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }

    public LiveData<NetworkInfo> activeNetwork()
    {
        return activeNetwork;
    }

    public LiveData<Wallet> defaultWallet()
    {
        return defaultWallet;
    }

    public void findWallet()
    {
        disposable = genericWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    public NetworkInfo getActiveNetwork()
    {
        return ethereumNetworkRepository.getActiveBrowserNetwork();
    }

    public void checkForNetworkChanges()
    {
        activeNetwork.postValue(ethereumNetworkRepository.getActiveBrowserNetwork());
    }

    private void onDefaultWallet(final Wallet wallet)
    {
        defaultWallet.setValue(wallet);
    }

    private void checkBalance(final Wallet wallet)
    {
        final NetworkInfo info = getActiveNetwork();
        if (info != null && wallet != null)
        {
            disposable = tokensService.getChainBalance(wallet.address.toLowerCase(), info.chainId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(w -> {}, e -> {});
        }
    }

    public void setLastUrl(Context context, String url)
    {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putString(C.DAPP_LASTURL_KEY, url).apply();
    }

    public void setHomePage(Context context, String url)
    {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putString(C.DAPP_HOMEPAGE_KEY, url).apply();
    }

    public String getHomePage(Context context)
    {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(C.DAPP_HOMEPAGE_KEY, null);
    }

    public void addToMyDapps(Context context, String title, String url)
    {
        Intent intent = new Intent(context, AddEditDappActivity.class);
        DApp dapp = new DApp(title, url);
        intent.putExtra(AddEditDappActivity.KEY_DAPP, dapp);
        intent.putExtra(AddEditDappActivity.KEY_MODE, AddEditDappActivity.MODE_ADD);
        context.startActivity(intent);
    }

    public void share(Context context, String url)
    {
        track(Analytics.Action.SHARE_URL);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, url);
        intent.setType("text/plain");
        context.startActivity(intent);
    }

    public void onClearBrowserCacheClicked(Context context)
    {
        WebView webView = new WebView(context);
        webView.clearCache(true);
        Toast.makeText(context, context.getString(R.string.toast_browser_cache_cleared),
                Toast.LENGTH_SHORT).show();
        track(Analytics.Action.CLEAR_BROWSER_CACHE);
    }

    public void startScan(Activity activity)
    {
        Intent intent = new Intent(activity, QRScannerActivity.class);
        intent.putExtra(QrScanSource.KEY, QrScanSource.BROWSER_SCREEN.getValue());
        activity.startActivityForResult(intent, HomeActivity.DAPP_BARCODE_READER_REQUEST_CODE);
    }

    public List<DApp> getDappsMasterList(Context context)
    {
        return DappBrowserUtils.getDappsList(context);
    }

    public void setNetwork(long chainId)
    {
        NetworkInfo info = ethereumNetworkRepository.getNetworkByChain(chainId);
        if (info != null)
        {
            ethereumNetworkRepository.setActiveBrowserNetwork(info);
            gasService.startGasPriceCycle(chainId);
        }
    }

    public void showImportLink(Context ctx, String qrCode)
    {
        Intent intent = new Intent(ctx, ImportTokenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(C.IMPORT_STRING, qrCode);
        ctx.startActivity(intent);
    }

    public void getAuthorisation(Wallet wallet, Activity activity, SignAuthenticationCallback callback)
    {
        keyService.getAuthenticationForSignature(wallet, activity, callback);
    }

    public void resetSignDialog()
    {
        keyService.resetSigningDialog();
    }

    public void completeAuthentication(Operation signData)
    {
        keyService.completeAuthentication(signData);
    }

    public void failedAuthentication(Operation signData)
    {
        keyService.failedAuthentication(signData);
    }

    public void showSend(Context ctx, QRResult result)
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

    public void sendTransaction(final Web3Transaction finalTx, long chainId, SendTransactionInterface callback)
    {
        if (finalTx.isConstructor())
        {
            disposable = createTransactionInteract
                    .createWithSig(defaultWallet.getValue(), finalTx.gasPrice, finalTx.gasLimit, finalTx.payload, chainId)
                    .subscribe(txData -> callback.transactionSuccess(finalTx, txData.signature),
                            error -> callback.transactionError(finalTx.leafPosition, error));
        }
        else
        {
            disposable = createTransactionInteract
                    .createWithSig(defaultWallet.getValue(), finalTx, chainId)
                    .subscribe(txData -> callback.transactionSuccess(finalTx, txData.signature),
                            error -> callback.transactionError(finalTx.leafPosition, error));
        }
    }

    public void showMyAddress(Context ctx)
    {
        Intent intent = new Intent(ctx, MyAddressActivity.class);
        intent.putExtra(WALLET, defaultWallet.getValue());
        ctx.startActivity(intent);
    }

    public void onDestroy()
    {
        if (balanceTimerDisposable != null && !balanceTimerDisposable.isDisposed())
            balanceTimerDisposable.dispose();
        gasService.stopGasPriceCycle();
    }

    public void updateGasPrice(long chainId)
    {
        gasService.startGasPriceCycle(chainId);
    }

    public Realm getRealmInstance(Wallet wallet)
    {
        return tokensService.getRealmInstance(wallet);
    }

    public void startBalanceUpdate()
    {
        if (balanceTimerDisposable == null || balanceTimerDisposable.isDisposed())
        {
            balanceTimerDisposable = Observable.interval(0, BALANCE_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS)
                    .doOnNext(l -> checkBalance(defaultWallet.getValue())).subscribe();
        }
    }

    public void stopBalanceUpdate()
    {
        if (balanceTimerDisposable != null && !balanceTimerDisposable.isDisposed())
            balanceTimerDisposable.dispose();
        balanceTimerDisposable = null;
        gasService.stopGasPriceCycle();
    }

    public void handleWalletConnect(Context context, String url, NetworkInfo activeNetwork)
    {
        Intent intent;
        if (WalletConnectHelper.isWalletConnectV1(url))
        {
            intent = getIntentOfWalletConnectV1(context, url, activeNetwork);
        }
        else
        {
            intent = getIntentOfWalletConnectV2(context, url);
        }

        context.startActivity(intent);
    }

    @NonNull
    private Intent getIntentOfWalletConnectV2(Context context, String url)
    {
        Intent intent = new Intent(context, WalletConnectV2Activity.class);
        intent.putExtra("url", url);
        return intent;
    }

    @NonNull
    private Intent getIntentOfWalletConnectV1(Context context, String url, NetworkInfo activeNetwork)
    {
        String importPassData = WalletConnectActivity.WC_LOCAL_PREFIX + url;
        Intent intent = new Intent(context, WalletConnectActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra(C.EXTRA_CHAIN_ID, activeNetwork.chainId);
        intent.putExtra("qrCode", importPassData);
        return intent;
    }

    public TokensService getTokenService()
    {
        return tokensService;
    }

    public Single<BigInteger> calculateGasEstimate(Wallet wallet, Web3Transaction transaction, long chainId)
    {
        if (transaction.isBaseTransfer())
        {
            return Single.fromCallable(() -> BigInteger.valueOf(C.GAS_LIMIT_MIN));
        }
        else
        {
            return gasService.calculateGasEstimate(Numeric.hexStringToByteArray(transaction.payload), chainId,
                    transaction.recipient.toString(), transaction.value, wallet, transaction.gasLimit);
        }
    }

    public String getNetworkNodeRPC(long chainId)
    {
        return ethereumNetworkRepository.getNetworkByChain(chainId).rpcServerUrl;
    }

    public NetworkInfo getNetworkInfo(long chainId)
    {
        return ethereumNetworkRepository.getNetworkByChain(chainId);
    }

    public String getSessionId(String url)
    {
        String uriString = url.replace("wc:", "wc://");
        return Uri.parse(uriString).getUserInfo();
    }

    public boolean addCustomChain(WalletAddEthereumChainObject chainObject)
    {
        String rpc = extractRpc(chainObject);
        if (rpc == null) return false;

        this.ethereumNetworkRepository.saveCustomRPCNetwork(chainObject.chainName, rpc, chainObject.getChainId(),
                chainObject.nativeCurrency.symbol, extractBlockExplorer(chainObject), "", false, -1L);

        tokensService.createBaseToken(chainObject.getChainId())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(w -> {}, e -> {})
                .isDisposed();

        return true;
    }

    private String extractBlockExplorer(WalletAddEthereumChainObject chainObject)
    {
        for (String thisRpc : chainObject.blockExplorerUrls)
        {
            if (isValidUrl(thisRpc)) //ensure RPC doesn't contain malicious code
            {
                String retRpc = thisRpc;
                if (thisRpc.endsWith("/tx"))
                {
                    retRpc = thisRpc + "/";
                }
                else if (!thisRpc.endsWith("/tx/"))
                {
                    if (!thisRpc.endsWith("/"))
                    {
                        retRpc = thisRpc + "/";
                    }

                    retRpc = retRpc + "tx/";
                }

                return retRpc;
            }
        }

        return "";
    }

    //NB Chain descriptions can contain WSS socket defs, which might come first.
    private String extractRpc(WalletAddEthereumChainObject chainObject)
    {
        for (String thisRpc : chainObject.rpcUrls)
        {
            if (isValidUrl(thisRpc)) //ensure RPC doesn't contain malicious code
            {
                return thisRpc;
            }
        }

        return null;
    }

    public boolean isMainNetsSelected()
    {
        return ethereumNetworkRepository.isMainNetSelected();
    }

    public void setMainNetsSelected(boolean isMainNet)
    {
        ethereumNetworkRepository.setActiveMainnet(isMainNet);
    }

    public void addNetworkToFilters(NetworkInfo info)
    {
        List<Long> filters = ethereumNetworkRepository.getFilterNetworkList();
        if (!filters.contains(info.chainId))
        {
            filters.add(info.chainId);
            ethereumNetworkRepository.setFilterNetworkList(filters.toArray(new Long[0]));
        }

        tokensService.setupFilter(true);
    }
}
