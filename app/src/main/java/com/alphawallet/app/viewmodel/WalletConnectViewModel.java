package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.ConfirmationType;
import com.alphawallet.app.entity.DAppFunction;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.walletconnect.WCRequest;
import com.alphawallet.app.entity.walletconnect.WalletConnectSessionItem;
import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.SignRecord;
import com.alphawallet.app.repository.entity.RealmWCSession;
import com.alphawallet.app.repository.entity.RealmWCSignElement;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.RealmManager;
import com.alphawallet.app.service.WalletConnectService;
import com.alphawallet.app.ui.ConfirmationActivity;
import com.alphawallet.app.walletconnect.WCClient;
import com.alphawallet.app.walletconnect.WCSession;
import com.alphawallet.app.walletconnect.entity.WCEthereumTransaction;
import com.alphawallet.app.walletconnect.entity.WCPeerMeta;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.token.entity.EthereumMessage;
import com.alphawallet.token.entity.EthereumTypedMessage;
import com.alphawallet.token.entity.Signable;
import com.alphawallet.token.tools.Convert;
import com.alphawallet.token.tools.Numeric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class WalletConnectViewModel extends BaseViewModel {
    private static final String WC_SESSION_DB = "wc_data-db.realm";
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Boolean> serviceReady = new MutableLiveData<>();
    protected Disposable disposable;
    private KeyService keyService;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final GenericWalletInteract genericWalletInteract;
    private final CreateTransactionInteract createTransactionInteract;
    private final RealmManager realmManager;
    private final Context context;
    private WalletConnectService walletConnectService;
    private ServiceConnection serviceConnection;

    private final HashMap<String, WCClient> clientBuffer = new HashMap<>();

    private static final String TAG = "WCClientVM";

    WalletConnectViewModel(KeyService keyService,
                           FindDefaultNetworkInteract findDefaultNetworkInteract,
                           CreateTransactionInteract createTransactionInteract,
                           GenericWalletInteract genericWalletInteract,
                           RealmManager realmManager,
                           Context ctx) {
        this.keyService = keyService;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.createTransactionInteract = createTransactionInteract;
        this.genericWalletInteract = genericWalletInteract;
        this.realmManager = realmManager;
        this.context = ctx;
        startService();
    }

    public void startService()
    {
        serviceConnection = new ServiceConnection()
        {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service)
            {
                walletConnectService = ((WalletConnectService.LocalBinder)service).getService();
                Log.d(TAG, "Service connected");
                for (String sessionId : clientBuffer.keySet())
                {
                    Log.d(TAG, "put from buffer: " + sessionId);
                    WCClient c  = clientBuffer.get(sessionId);
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
        context.startService(i);
        context.bindService(i, serviceConnection, Context.BIND_ABOVE_CLIENT);
    }

    public void prepare()
    {
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
    }

    public void pruneSession(String sessionId)
    {
        try (Realm realm = realmManager.getRealmInstance(WC_SESSION_DB))
        {
            RealmWCSession item = realm.where(RealmWCSession.class)
                    .equalTo("sessionId", sessionId)
                    .findFirst();

            RealmResults<RealmWCSignElement> signItems = realm.where(RealmWCSignElement.class)
                    .equalTo("sessionId", sessionId)
                    .findAll();

            if (item != null && signItems.size() == 0)
            {
                realm.executeTransaction(r -> {
                    Log.d(TAG, "Delete from realm: " + sessionId);
                    item.deleteFromRealm();
                });
            }
        }
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        defaultNetwork.postValue(networkInfo);
        disposable = genericWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(Wallet wallet) {
        defaultWallet.postValue(wallet);
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }

    public LiveData<Boolean> serviceReady() {
        return serviceReady;
    }

    public void getAuthenticationForSignature(Wallet wallet, Activity activity, SignAuthenticationCallback callback) {
        keyService.getAuthenticationForSignature(wallet, activity, callback);
    }

    public void signMessage(Signable message, DAppFunction dAppFunction) {
        resetSignDialog();
        disposable = createTransactionInteract.sign(defaultWallet.getValue(), message, 1)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sig -> dAppFunction.DAppReturn(sig.signature, message),
                        error -> dAppFunction.DAppError(error, message));
    }

    public void confirmTransaction(Activity context, WCEthereumTransaction transaction, String requesterURL, int chainId, Long callbackId)
    {
        Web3Transaction w3tx = new Web3Transaction(transaction, callbackId);

        Intent intent = new Intent(context, ConfirmationActivity.class);
        intent.putExtra(C.EXTRA_WEB3TRANSACTION, w3tx);
        intent.putExtra(C.EXTRA_AMOUNT, Convert.fromWei(w3tx.value.toString(), Convert.Unit.WEI).toString());
        intent.putExtra(C.TOKEN_TYPE, ConfirmationType.WEB3TRANSACTION.ordinal());
        intent.putExtra(C.EXTRA_ACTION_NAME, requesterURL);
        intent.putExtra(C.EXTRA_NETWORKID, chainId);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivityForResult(intent, C.REQUEST_TRANSACTION_CALLBACK);
    }

    public void signTransaction(Context ctx, Web3Transaction w3tx, DAppFunction dAppFunction, String requesterURL, int chainId)
    {
        resetSignDialog();
        EthereumMessage etm = new EthereumMessage(w3tx.getFormattedTransaction(ctx, chainId).toString(), requesterURL, w3tx.leafPosition);
        disposable = createTransactionInteract.signTransaction(defaultWallet.getValue(), w3tx, chainId)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sig -> dAppFunction.DAppReturn(Numeric.hexStringToByteArray(sig.signature), etm),
                        error -> dAppFunction.DAppError(error, etm));
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
        try (Realm realm = realmManager.getRealmInstance(WC_SESSION_DB))
        {
            RealmWCSession sessionData = realm.where(RealmWCSession.class)
                    .equalTo("sessionId", sessionId)
                    .findFirst();

            if (sessionData != null)
            {
                session = sessionData.getSession();
            }
        }

        return session;
    }

    public WCPeerMeta getRemotePeer(String sessionId)
    {
        WCPeerMeta peerMeta = null;
        try (Realm realm = realmManager.getRealmInstance(WC_SESSION_DB))
        {
            RealmWCSession sessionData = realm.where(RealmWCSession.class)
                    .equalTo("sessionId", sessionId)
                    .findFirst();

            if (sessionData != null)
            {
                peerMeta = sessionData.getRemotePeerData();
            }
        }

        return peerMeta;
    }

    public String getRemotePeerId(String sessionId)
    {
        String remotePeerId = null;
        try (Realm realm = realmManager.getRealmInstance(WC_SESSION_DB))
        {
            RealmWCSession sessionData = realm.where(RealmWCSession.class)
                    .equalTo("sessionId", sessionId)
                    .findFirst();

            if (sessionData != null)
            {
                remotePeerId = sessionData.getRemotePeerId();
            }
        }

        return remotePeerId;
    }

    public String getPeerId(String sessionId)
    {
        String peerId = null;
        try (Realm realm = realmManager.getRealmInstance(WC_SESSION_DB))
        {
            RealmWCSession sessionData = realm.where(RealmWCSession.class)
                    .equalTo("sessionId", sessionId)
                    .findFirst();

            if (sessionData != null)
            {
                peerId = sessionData.getPeerId();
            }
        }

        return peerId;
    }

    public void createNewSession(String sessionId, String peerId, String remotePeerId, String sessionData, String remotePeerData)
    {
        try (Realm realm = realmManager.getRealmInstance(WC_SESSION_DB))
        {
            realm.executeTransaction(r -> {
                RealmWCSession sessionAux = realm.where(RealmWCSession.class)
                        .equalTo("sessionId", sessionId)
                        .findFirst();

                if (sessionAux == null) sessionAux = realm.createObject(RealmWCSession.class, sessionId);

                sessionAux.setLastUsageTime(System.currentTimeMillis());
                sessionAux.setRemotePeerId(remotePeerId);
                sessionAux.setPeerId(peerId);
                sessionAux.setRemotePeerData(remotePeerData);
                sessionAux.setSessionData(sessionData);
                sessionAux.setUsageCount(0);
                sessionAux.setWalletAccount(defaultWallet.getValue().address);
            });
        }
    }

    public void deleteSession(String sessionId)
    {
        try (Realm realm = realmManager.getRealmInstance(WC_SESSION_DB))
        {
            realm.executeTransaction(r -> {
                RealmWCSession sessionAux = realm.where(RealmWCSession.class)
                        .equalTo("sessionId", sessionId)
                        .findFirst();

                if (sessionAux != null)
                {
                    sessionAux.deleteFromRealm();
                }
            });
        }
    }

    public void recordSign(Signable signable, String sessionId)
    {
        try (Realm realm = realmManager.getRealmInstance(WC_SESSION_DB))
        {
            realm.executeTransaction(r -> {
                RealmWCSignElement signMessage = realm.createObject(RealmWCSignElement.class);

                String signType = "Message";
                if (signable instanceof EthereumTypedMessage) signType = "TypedMessage";

                signMessage.setSessionId(sessionId);
                signMessage.setSignType(signType);
                signMessage.setSignTime(System.currentTimeMillis());
                signMessage.setSignMessage(signable.getUserMessage());
            });
        }
    }

    public void recordSignTransaction(Context ctx, Web3Transaction tx, int chainId, String sessionId)
    {
        try (Realm realm = realmManager.getRealmInstance(WC_SESSION_DB))
        {
            realm.executeTransaction(r -> {
                RealmWCSignElement signMessage = realm.createObject(RealmWCSignElement.class);

                String signType = "Transaction";

                signMessage.setSessionId(sessionId);
                signMessage.setSignType(signType);
                signMessage.setSignTime(System.currentTimeMillis());
                signMessage.setSignMessage(tx.getFormattedTransaction(ctx, chainId));
            });
        }
    }

    public ArrayList<SignRecord> getSignRecords(String sessionId)
    {
        ArrayList<SignRecord> records = new ArrayList<>();
        try (Realm realm = realmManager.getRealmInstance(WC_SESSION_DB))
        {
            RealmResults<RealmWCSignElement> sessionList = realm.where(RealmWCSignElement.class)
                    .equalTo("sessionId", sessionId)
                    .findAll();

            for (RealmWCSignElement s : sessionList)
            {
                records.add(new SignRecord(s));
            }
        }

        return records;
    }

    public List<WalletConnectSessionItem> getSessions()
    {
        List<WalletConnectSessionItem> sessions = new ArrayList<>();
        try (Realm realm = realmManager.getRealmInstance(WC_SESSION_DB))
        {
            RealmResults<RealmWCSession> items = realm.where(RealmWCSession.class)
                    .sort("lastUsageTime", Sort.DESCENDING)
                    .findAll();

            for (RealmWCSession r : items)
            {
                sessions.add(new WalletConnectSessionItem(r));
            }
        }

        return sessions;
    }

    public WCRequest getPendingRequest(String sessionId)
    {
        if (walletConnectService != null) return walletConnectService.getPendingRequest(sessionId);
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
}
