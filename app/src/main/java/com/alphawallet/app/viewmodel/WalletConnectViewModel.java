package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.AnalyticsProperties;
import com.alphawallet.app.entity.DAppFunction;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.SendTransactionInterface;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletConnectActions;
import com.alphawallet.app.entity.walletconnect.WCRequest;
import com.alphawallet.app.entity.walletconnect.WalletConnectSessionItem;
import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.SignRecord;
import com.alphawallet.app.repository.entity.RealmWCSession;
import com.alphawallet.app.repository.entity.RealmWCSignElement;
import com.alphawallet.app.service.AnalyticsServiceType;
import com.alphawallet.app.service.GasService2;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.RealmManager;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.service.WalletConnectService;
import com.alphawallet.app.walletconnect.WCClient;
import com.alphawallet.app.walletconnect.WCSession;
import com.alphawallet.app.walletconnect.entity.WCPeerMeta;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.token.entity.EthereumMessage;
import com.alphawallet.token.entity.EthereumTypedMessage;
import com.alphawallet.token.entity.SignMessageType;
import com.alphawallet.token.entity.Signable;
import com.alphawallet.token.tools.Numeric;

import org.web3j.protocol.core.methods.response.EthEstimateGas;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.RealmResults;
import io.realm.Sort;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

public class WalletConnectViewModel extends BaseViewModel {
    private static final String WC_SESSION_DB = "wc_data-db.realm";
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Boolean> serviceReady = new MutableLiveData<>();
    protected Disposable disposable;
    private final KeyService keyService;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final GenericWalletInteract genericWalletInteract;
    private final CreateTransactionInteract createTransactionInteract;
    private final RealmManager realmManager;
    private final GasService2 gasService;
    private final TokensService tokensService;
    private final AnalyticsServiceType analyticsService;
    private final Context context;
    private WalletConnectService walletConnectService;
    private final ServiceConnection serviceConnection;

    private final HashMap<String, WCClient> clientBuffer = new HashMap<>();

    private Wallet wallet;

    private static final String TAG = "WCClientVM";

    WalletConnectViewModel(KeyService keyService,
                           FindDefaultNetworkInteract findDefaultNetworkInteract,
                           CreateTransactionInteract createTransactionInteract,
                           GenericWalletInteract genericWalletInteract,
                           RealmManager realmManager,
                           GasService2 gasService,
                           TokensService tokensService,
                           AnalyticsServiceType analyticsService,
                           Context ctx) {
        this.keyService = keyService;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.createTransactionInteract = createTransactionInteract;
        this.genericWalletInteract = genericWalletInteract;
        this.realmManager = realmManager;
        this.context = ctx;
        this.gasService = gasService;
        this.tokensService = tokensService;
        this.analyticsService = analyticsService;
        serviceConnection = startService();
        disposable = genericWalletInteract
                .find()
                .subscribe(w -> this.wallet = w, this::onError);
    }

    public ServiceConnection startService()
    {
        ServiceConnection serviceConnection = new ServiceConnection()
        {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service)
            {
                walletConnectService = ((WalletConnectService.LocalBinder)service).getService();
                Log.d(TAG, "Service connected");
                for (String sessionId : clientBuffer.keySet())
                {
                    Log.d(TAG, "put from buffer: " + sessionId);
                    WCClient c = clientBuffer.get(sessionId);
                    walletConnectService.putClient(sessionId, c);
                }
                clientBuffer.clear();
                serviceReady.postValue(true);
            }

            @Override
            public void onServiceDisconnected(ComponentName name)
            {
                walletConnectService = null;
                Log.d(TAG, "Service disconnected");
            }
        };

        Intent i = new Intent(context, WalletConnectService.class);
        i.setAction(String.valueOf(WalletConnectActions.CONNECT.ordinal()));
        context.startService(i);
        context.bindService(i, serviceConnection, Context.BIND_ABOVE_CLIENT);

        return serviceConnection;
    }

    public void prepare()
    {
        disposable = genericWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    public void pruneSession(String sessionId)
    {
        realmManager.getRealmInstance(WC_SESSION_DB).executeTransactionAsync(r -> {
            RealmWCSession item = r.where(RealmWCSession.class)
                    .equalTo("sessionId", sessionId)
                    .findFirst();

            RealmResults<RealmWCSignElement> signItems = r.where(RealmWCSignElement.class)
                    .equalTo("sessionId", sessionId)
                    .findAll();

            if (item != null && signItems.size() == 0)
            {
                Log.d(TAG, "Delete from realm: " + sessionId);
                item.deleteFromRealm();
            }
        });
    }

    public void startGasCycle(int chainId)
    {
        gasService.startGasPriceCycle(chainId);
    }

    public TokensService getTokensService()
    {
        return tokensService;
    }

    public void onDestroy()
    {
        gasService.stopGasPriceCycle();
    }

    private void onDefaultWallet(Wallet w) {
        this.wallet = w;
        defaultWallet.postValue(w);
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }

    public Wallet getWallet()
    {
        return wallet;
    }

    public LiveData<Boolean> serviceReady() {
        return serviceReady;
    }

    public void getAuthenticationForSignature(Wallet wallet, Activity activity, SignAuthenticationCallback callback) {
        keyService.getAuthenticationForSignature(wallet, activity, callback);
    }

    public void signMessage(Signable message, DAppFunction dAppFunction) {
        resetSignDialog();
        disposable = createTransactionInteract.sign(defaultWallet.getValue(), message, MAINNET_ID)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sig -> dAppFunction.DAppReturn(sig.signature, message),
                        error -> dAppFunction.DAppError(error, message));
    }

    public void signTransaction(Context ctx, Web3Transaction w3tx, DAppFunction dAppFunction, String requesterURL, int chainId)
    {
        resetSignDialog();
        EthereumMessage etm = new EthereumMessage(w3tx.getFormattedTransaction(ctx, chainId, getNetworkSymbol(chainId)).toString(),
                requesterURL, w3tx.leafPosition, SignMessageType.SIGN_MESSAGE);
        disposable = createTransactionInteract.signTransaction(defaultWallet.getValue(), w3tx, chainId)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sig -> dAppFunction.DAppReturn(Numeric.hexStringToByteArray(sig.signature), etm),
                        error -> dAppFunction.DAppError(error, etm));
    }

    public void sendTransaction(final Web3Transaction finalTx, int chainId, SendTransactionInterface callback)
    {
        if (finalTx.isConstructor())
        {
            disposable = createTransactionInteract
                    .createWithSig(defaultWallet.getValue(), finalTx.gasPrice, finalTx.gasLimit, finalTx.payload, chainId)
                    .subscribe(txData -> callback.transactionSuccess(finalTx, txData.txHash),
                            error -> callback.transactionError(finalTx.leafPosition, error));
        }
        else
        {
            disposable = createTransactionInteract
                    .createWithSig(defaultWallet.getValue(), finalTx, chainId)
                    .subscribe(txData -> callback.transactionSuccess(finalTx, txData.txHash),
                            error -> callback.transactionError(finalTx.leafPosition, error));
        }
    }

    public Single<EthEstimateGas> calculateGasEstimate(Wallet wallet, byte[] transactionBytes, int chainId, String sendAddress, BigDecimal sendAmount)
    {
        return gasService.calculateGasEstimate(transactionBytes, chainId, sendAddress, sendAmount.toBigInteger(), wallet);
    }

    public void resetSignDialog()
    {
        keyService.resetSigningDialog();
    }

    public WCClient getClient(String sessionId)
    {
        //may fail.
        if (walletConnectService == null)
        {
            return null;
        }
        else
        {
            Log.d(TAG, "fetch: " + sessionId);
            return walletConnectService.getClient(sessionId);
        }
    }

    public void putClient(String sessionId, WCClient client)
    {
        if (walletConnectService == null)
        {
            clientBuffer.put(sessionId, client);
            Log.d(TAG, "buffering: " + sessionId);
        }
        else
        {
            Log.d(TAG, "put: " + sessionId);
            walletConnectService.putClient(sessionId, client);
        }
    }

    public WCSession getSession(String sessionId)
    {
        WCSession session = null;
        RealmWCSession sessionData = realmManager.getRealmInstance(WC_SESSION_DB).where(RealmWCSession.class)
                .equalTo("sessionId", sessionId)
                .findFirst();

        if (sessionData != null)
        {
            session = sessionData.getSession();
        }

        return session;
    }

    public WCPeerMeta getRemotePeer(String sessionId)
    {
        WCPeerMeta peerMeta = null;
        RealmWCSession sessionData = realmManager.getRealmInstance(WC_SESSION_DB).where(RealmWCSession.class)
                .equalTo("sessionId", sessionId)
                .findFirst();

        if (sessionData != null)
        {
            peerMeta = sessionData.getRemotePeerData();
        }

        return peerMeta;
    }

    public String getRemotePeerId(String sessionId)
    {
        String remotePeerId = null;
        RealmWCSession sessionData = realmManager.getRealmInstance(WC_SESSION_DB).where(RealmWCSession.class)
                .equalTo("sessionId", sessionId)
                .findFirst();

        if (sessionData != null)
        {
            remotePeerId = sessionData.getRemotePeerId();
        }

        return remotePeerId;
    }

    public String getPeerId(String sessionId)
    {
        String peerId = null;
        RealmWCSession sessionData = realmManager.getRealmInstance(WC_SESSION_DB).where(RealmWCSession.class)
                .equalTo("sessionId", sessionId)
                .findFirst();

        if (sessionData != null)
        {
            peerId = sessionData.getPeerId();
        }

        return peerId;
    }

    public int getChainId(String sessionId)
    {
        int chainId = MAINNET_ID;
        RealmWCSession sessionData = realmManager.getRealmInstance(WC_SESSION_DB).where(RealmWCSession.class)
                .equalTo("sessionId", sessionId)
                .findFirst();

        if (sessionData != null)
        {
            chainId = sessionData.getChainId();
        }

        return chainId;
    }

    public void createNewSession(String sessionId, String peerId, String remotePeerId, String sessionData,
                                 String remotePeerData, int sessionChainId)
    {
        realmManager.getRealmInstance(WC_SESSION_DB).executeTransactionAsync(r -> {
            RealmWCSession sessionAux = r.where(RealmWCSession.class)
                    .equalTo("sessionId", sessionId)
                    .findFirst();

            if (sessionAux == null)
                sessionAux = r.createObject(RealmWCSession.class, sessionId);

            sessionAux.setLastUsageTime(System.currentTimeMillis());
            sessionAux.setRemotePeerId(remotePeerId);
            sessionAux.setPeerId(peerId);
            sessionAux.setRemotePeerData(remotePeerData);
            sessionAux.setSessionData(sessionData);
            sessionAux.setUsageCount(0);
            sessionAux.setWalletAccount(defaultWallet.getValue().address);
            sessionAux.setChainId(sessionChainId);
        });

        gasService.startGasPriceCycle(sessionChainId);
    }

    public void deleteSession(String sessionId)
    {
        realmManager.getRealmInstance(WC_SESSION_DB).executeTransactionAsync(r -> {
            RealmWCSession sessionAux = r.where(RealmWCSession.class)
                    .equalTo("sessionId", sessionId)
                    .findFirst();

            if (sessionAux != null)
            {
                sessionAux.deleteFromRealm();
            }
        });
    }

    public void recordSign(Signable signable, String sessionId)
    {
        realmManager.getRealmInstance(WC_SESSION_DB).executeTransactionAsync(r -> {
            RealmWCSignElement signMessage = r.createObject(RealmWCSignElement.class);

            String signType = "Message";
            if (signable instanceof EthereumTypedMessage) signType = "TypedMessage";

            signMessage.setSessionId(sessionId);
            signMessage.setSignType(signType);
            signMessage.setSignTime(System.currentTimeMillis());
            signMessage.setSignMessage(signable.getUserMessage());
        });
    }

    public void recordSignTransaction(Context ctx, Web3Transaction tx, int chainId, String sessionId)
    {
        realmManager.getRealmInstance(WC_SESSION_DB).executeTransactionAsync(r -> {
                RealmWCSignElement signMessage = r.createObject(RealmWCSignElement.class);
                String signType = "Transaction";
                signMessage.setSessionId(sessionId);
                signMessage.setSignType(signType);
                signMessage.setSignTime(System.currentTimeMillis());
                signMessage.setSignMessage(tx.getFormattedTransaction(ctx, chainId, getNetworkSymbol(chainId)));
            });
    }

    public ArrayList<SignRecord> getSignRecords(String sessionId)
    {
        ArrayList<SignRecord> records = new ArrayList<>();
        RealmResults<RealmWCSignElement> sessionList = realmManager.getRealmInstance(WC_SESSION_DB).where(RealmWCSignElement.class)
                .equalTo("sessionId", sessionId)
                .findAll();

        for (RealmWCSignElement s : sessionList)
        {
            records.add(new SignRecord(s));
        }

        return records;
    }

    public List<WalletConnectSessionItem> getSessions()
    {
        List<WalletConnectSessionItem> sessions = new ArrayList<>();
        RealmResults<RealmWCSession> items = realmManager.getRealmInstance(WC_SESSION_DB).where(RealmWCSession.class)
                .sort("lastUsageTime", Sort.DESCENDING)
                .findAll();

        for (RealmWCSession r : items)
        {
            sessions.add(new WalletConnectSessionItem(r));
        }

        return sessions;
    }

    public WCRequest getPendingRequest(String sessionId)
    {
        if (walletConnectService != null) return walletConnectService.getPendingRequest(sessionId);
        else return null;
    }

    public WCRequest getCurrentRequest()
    {
        if (walletConnectService != null) return walletConnectService.getCurrentRequest();
        else return null;
    }

    public void rejectRequest(String sessionId, long id, String message)
    {
        if (walletConnectService != null) walletConnectService.rejectRequest(sessionId, id, message);
    }

    public void approveRequest(String sessionId, long id, String message)
    {
        if (walletConnectService != null) walletConnectService.approveRequest(sessionId, id, message);
    }

    public int getConnectionCount()
    {
        if (walletConnectService != null) return walletConnectService.getConnectionCount();
        else return 0;
    }

    public String getNetworkSymbol(int chainId)
    {
        NetworkInfo info = findDefaultNetworkInteract.getNetworkInfo(chainId);
        if (info == null) { info = findDefaultNetworkInteract.getNetworkInfo(MAINNET_ID); }
        return info.symbol;
    }

    public void actionSheetConfirm(String mode)
    {
        AnalyticsProperties analyticsProperties = new AnalyticsProperties();
        analyticsProperties.setData("(WC)" + mode); //disambiguate signs/sends etc through WC

        analyticsService.track(C.AN_CALL_ACTIONSHEET, analyticsProperties);
    }

    public boolean connectedToService()
    {
        return walletConnectService != null;
    }
}
