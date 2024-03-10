package com.alphawallet.app.viewmodel;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.GasEstimate;
import com.alphawallet.app.entity.GenericCallback;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.TransactionReturn;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletConnectActions;
import com.alphawallet.app.entity.walletconnect.WalletConnectSessionItem;
import com.alphawallet.app.entity.walletconnect.WalletConnectV2SessionItem;
import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.FetchWalletsInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.interact.WalletConnectInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.SignRecord;
import com.alphawallet.app.repository.entity.RealmWCSession;
import com.alphawallet.app.repository.entity.RealmWCSignElement;
import com.alphawallet.app.service.AnalyticsServiceType;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.RealmManager;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.service.TransactionSendHandlerInterface;
import com.alphawallet.app.walletconnect.AWWalletConnectClient;
import com.alphawallet.app.walletconnect.WCClient;
import com.alphawallet.app.walletconnect.WCSession;
import com.alphawallet.app.walletconnect.entity.GetClientCallback;
import com.alphawallet.app.walletconnect.entity.WCPeerMeta;
import com.alphawallet.app.web3.entity.WalletAddEthereumChainObject;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.hardware.SignatureFromKey;
import com.alphawallet.token.entity.EthereumTypedMessage;
import com.alphawallet.token.entity.Signable;

import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.realm.Realm;
import io.realm.RealmResults;
import timber.log.Timber;

@HiltViewModel
public class WalletConnectViewModel extends BaseViewModel implements TransactionSendHandlerInterface
{
    public static final String WC_SESSION_DB = "wc_data-db.realm";
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Boolean> serviceReady = new MutableLiveData<>();
    private final MutableLiveData<TransactionReturn> transactionFinalised = new MutableLiveData<>();
    private final MutableLiveData<TransactionReturn> transactionSigned = new MutableLiveData<>();
    private final MutableLiveData<TransactionReturn> transactionError = new MutableLiveData<>();
    private final MutableLiveData<List<WalletConnectSessionItem>> sessions = new MutableLiveData<>();
    protected Disposable disposable;
    private final KeyService keyService;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final GenericWalletInteract genericWalletInteract;
    private final FetchWalletsInteract fetchWalletsInteract;
    private final CreateTransactionInteract createTransactionInteract;
    private final WalletConnectInteract walletConnectInteract;
    private final RealmManager realmManager;
    private final GasService gasService;
    private final TokensService tokensService;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;

    private final HashMap<String, WCClient> clientBuffer = new HashMap<>();

    private Wallet wallet;

    @Nullable
    private Disposable prepareDisposable;

    private final AWWalletConnectClient awWalletConnectClient;

    private static final String TAG = "WCClientVM";

    @Inject
    WalletConnectViewModel(KeyService keyService,
                           FindDefaultNetworkInteract findDefaultNetworkInteract,
                           FetchWalletsInteract fetchWalletsInteract, CreateTransactionInteract createTransactionInteract,
                           GenericWalletInteract genericWalletInteract,
                           WalletConnectInteract walletConnectInteract, RealmManager realmManager,
                           GasService gasService,
                           TokensService tokensService,
                           AnalyticsServiceType analyticsService,
                           EthereumNetworkRepositoryType ethereumNetworkRepository,
                           AWWalletConnectClient awWalletConnectClient
    )
    {
        this.keyService = keyService;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.fetchWalletsInteract = fetchWalletsInteract;
        this.createTransactionInteract = createTransactionInteract;
        this.genericWalletInteract = genericWalletInteract;
        this.walletConnectInteract = walletConnectInteract;
        this.realmManager = realmManager;
        this.gasService = gasService;
        this.tokensService = tokensService;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        setAnalyticsService(analyticsService);
        this.awWalletConnectClient = awWalletConnectClient;
        prepareDisposable = null;
        disposable = genericWalletInteract
                .find()
                .subscribe(w -> this.wallet = w, this::onError);
    }

    public void prepare()
    {
        prepareDisposable = genericWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    public GasService getGasService()
    {
        return gasService;
    }

    public void startGasCycle(long chainId)
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

    private void onDefaultWallet(Wallet w)
    {
        this.wallet = w;
        defaultWallet.postValue(w);
    }

    public LiveData<Wallet> defaultWallet()
    {
        return defaultWallet;
    }

    public MutableLiveData<TransactionReturn> transactionFinalised()
    {
        return transactionFinalised;
    }

    public MutableLiveData<TransactionReturn> transactionSigned()
    {
        return transactionSigned;
    }

    public MutableLiveData<TransactionReturn> transactionError()
    {
        return transactionError;
    }

    public Wallet getWallet()
    {
        return wallet;
    }

    public LiveData<Boolean> serviceReady()
    {
        return serviceReady;
    }

    public void getAuthenticationForSignature(Wallet wallet, Activity activity, SignAuthenticationCallback callback)
    {
        keyService.getAuthenticationForSignature(wallet, activity, callback);
    }

    public void requestSignature(Web3Transaction finalTx, Wallet wallet, long chainId)
    {
        createTransactionInteract.requestSignature(finalTx, wallet, chainId, this);
    }

    public void requestSignatureOnly(Web3Transaction tx, Wallet fromWallet, long chainId)
    {
        createTransactionInteract.requestSignTransaction(tx, wallet, chainId, this);
    }

    public void sendTransaction(Wallet wallet, long chainId, Web3Transaction tx, SignatureFromKey signatureFromKey)
    {
        createTransactionInteract.sendTransaction(wallet, chainId, tx, signatureFromKey);
    }

    public Single<GasEstimate> calculateGasEstimate(Wallet wallet, byte[] transactionBytes, long chainId, String sendAddress, BigDecimal sendAmount, BigInteger defaultLimit)
    {
        return gasService.calculateGasEstimate(transactionBytes, chainId, sendAddress, sendAmount.toBigInteger(), wallet, defaultLimit);
    }

    public Single<GasEstimate> calculateGasEstimate(Wallet wallet, Web3Transaction transaction, long chainId)
    {
        if (transaction.isBaseTransfer())
        {
            return Single.fromCallable(() -> new GasEstimate(BigInteger.valueOf(C.GAS_LIMIT_MIN)));
        }
        else
        {
            return gasService.calculateGasEstimate(org.web3j.utils.Numeric.hexStringToByteArray(transaction.payload), chainId,
                    transaction.recipient.toString(), transaction.value, wallet, transaction.gasLimit);
        }
    }

    public void resetSignDialog()
    {
        keyService.resetSigningDialog();
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

    public long getChainId(String sessionId)
    {
        long chainId = MAINNET_ID;
        try (Realm realm = realmManager.getRealmInstance(WC_SESSION_DB))
        {
            RealmWCSession sessionData = realm.where(RealmWCSession.class)
                    .equalTo("sessionId", sessionId)
                    .findFirst();

            if (sessionData != null)
            {
                chainId = sessionData.getChainId();
            }
        }

        return chainId;
    }

    public void createNewSession(String sessionId, String peerId, String remotePeerId, String sessionData,
                                 String remotePeerData, long sessionChainId)
    {
        try (Realm realm = realmManager.getRealmInstance(WC_SESSION_DB))
        {
            realm.executeTransactionAsync(r -> {
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
        }

        gasService.startGasPriceCycle(sessionChainId);
    }

    public void updateSession(String sessionId, long sessionChainId)
    {
        Timber.tag(TAG).d("updateSession: sessionId: %s, updated chainId: %s", sessionId, sessionChainId);
        try (Realm realm = realmManager.getRealmInstance(WC_SESSION_DB))
        {
            RealmWCSession sessionData = realm.where(RealmWCSession.class)
                    .equalTo("sessionId", sessionId)
                    .findFirst();
            realm.beginTransaction();
            if (sessionData != null)
            {
                sessionData.setChainId(sessionChainId);
            }
            else
            {
                Timber.tag("TAG").d("updateSession: could not fin session!");
            }
            realm.commitTransaction();
        }
        catch (Exception e)
        {
            Timber.e(e);
        }
    }

    public void deleteSession(WalletConnectSessionItem session, AWWalletConnectClient.WalletConnectV2Callback callback)
    {
        if (session instanceof WalletConnectV2SessionItem)
        {
            deleteSessionV2(session, callback);
        }
        else
        {
            deleteSessionV1(session, callback);
        }
        updateSessions();
    }

    private void deleteSessionV2(WalletConnectSessionItem session, AWWalletConnectClient.WalletConnectV2Callback callback)
    {
        awWalletConnectClient.disconnect(session.sessionId, callback);
    }

    private void deleteSessionV1(WalletConnectSessionItem session, AWWalletConnectClient.WalletConnectV2Callback callback)
    {
        try (Realm realm = realmManager.getRealmInstance(WC_SESSION_DB))
        {
            realm.executeTransactionAsync(r -> {
                RealmWCSession sessionAux = r.where(RealmWCSession.class)
                        .equalTo("sessionId", session.sessionId)
                        .findFirst();

                if (sessionAux != null)
                {
                    sessionAux.deleteFromRealm();
                }
                callback.onSessionDisconnected();
            });
        }
    }

    public void recordSign(Signable signable, String sessionId, Realm.Transaction.OnSuccess onSuccess)
    {
        try (Realm realm = realmManager.getRealmInstance(WC_SESSION_DB))
        {
            realm.executeTransactionAsync(r -> {
                RealmWCSignElement signMessage = r.createObject(RealmWCSignElement.class);

                String signType = "Message";
                if (signable instanceof EthereumTypedMessage) signType = "TypedMessage";

                signMessage.setSessionId(sessionId);
                signMessage.setSignType(signType);
                signMessage.setSignTime(System.currentTimeMillis());
                signMessage.setSignMessage(signable.getUserMessage());
            }, onSuccess);
        }
    }

    public void recordSignTransaction(Context ctx, Web3Transaction tx, String chainIdStr, String sessionId)
    {
        try (Realm realm = realmManager.getRealmInstance(WC_SESSION_DB))
        {
            realm.executeTransactionAsync(r -> {
                long chainId = chainIdStr != null ? Long.parseLong(chainIdStr) : MAINNET_ID;
                RealmWCSignElement signMessage = r.createObject(RealmWCSignElement.class);
                String signType = "Transaction";
                signMessage.setSessionId(sessionId);
                signMessage.setSignType(signType);
                signMessage.setSignTime(System.currentTimeMillis());
                signMessage.setSignMessage(tx.getFormattedTransaction(ctx, chainId, getNetworkSymbol(chainId)));
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

    public MutableLiveData<List<WalletConnectSessionItem>> sessions()
    {
        return sessions;
    }

    public String getNetworkSymbol(long chainId)
    {
        NetworkInfo info = findDefaultNetworkInteract.getNetworkInfo(chainId);
        if (info == null)
        {
            info = findDefaultNetworkInteract.getNetworkInfo(MAINNET_ID);
        }
        return info.symbol;
    }

    private String extractRpc(WalletAddEthereumChainObject chainObject)
    {
        for (String thisRpc : chainObject.rpcUrls)
        {
            if (thisRpc.toLowerCase().startsWith("http"))
            {
                return thisRpc;
            }
        }

        return "";
    }

    public boolean isChainAdded(long chainId)
    {
        return ethereumNetworkRepository.getNetworkByChain(chainId) != null;
    }

    public TokensService getTokenService()
    {
        return tokensService;
    }

    public Wallet findWallet(String address)
    {
        return fetchWalletsInteract.getWallet(address).blockingGet();
    }

    public void endSession(String sessionId)
    {
        try (Realm realm = realmManager.getRealmInstance(WC_SESSION_DB))
        {
            realm.executeTransactionAsync(r -> {
                RealmWCSession sessionAux = r.where(RealmWCSession.class)
                        .equalTo("sessionId", sessionId)
                        .findFirst();

                if (sessionAux != null)
                {
                    sessionAux.setPeerId("");
                    r.insertOrUpdate(sessionAux);
                }
            });
        }
    }

    @NonNull
    private ArrayList<String> filterSessionsWithoutSignRecords(List<String> sessions)
    {
        ArrayList<String> result = new ArrayList<>();
        for (String sessionId : sessions)
        {
            if (getSignRecords(sessionId).isEmpty())
            {
                result.add(sessionId);
            }
        }
        return result;
    }

    public void updateSessions()
    {
        sessions.postValue(walletConnectInteract.getSessions());
    }

    // deletes the RealmWCSession objects with the given sessionIds present in the list
    private void deleteSessionsFromRealm(List<String> sessionIds, Runnable onSuccess)
    {
        if (sessionIds.isEmpty())
            return;
        try (Realm realm = realmManager.getRealmInstance(WC_SESSION_DB))
        {
            realm.executeTransactionAsync(r -> {
                boolean isDeleted = r.where(RealmWCSession.class)
                        .in("sessionId", sessionIds.toArray(new String[]{}))
                        .findAll()
                        .deleteAllFromRealm();
                Timber.tag(TAG).i("deleteSessions: Success: %s\nList: %s", isDeleted, sessionIds);
            }, onSuccess::run);
        }
        catch (Exception e)
        {
            Timber.tag(TAG).e(e);
        }
    }

    @Override
    public void transactionFinalised(TransactionReturn txData)
    {
        transactionFinalised.postValue(txData);
    }

    @Override
    public void transactionError(TransactionReturn txError)
    {
        transactionError.postValue(txError);
    }

    @Override
    public void transactionSigned(SignatureFromKey sigData, Web3Transaction w3Tx)
    {
        transactionSigned.postValue(new TransactionReturn(Numeric.toHexString(sigData.signature), w3Tx));
    }

    public void signTransaction(long chainId, Web3Transaction tx, SignatureFromKey signatureFromKey)
    {
        createTransactionInteract.signTransaction(chainId, tx, signatureFromKey);
    }

    public boolean isActiveNetwork(long chainId)
    {
        return ethereumNetworkRepository.getSelectedFilters().contains(chainId);
    }

    public void blankLiveData()
    {
        transactionFinalised.postValue(null);
        transactionSigned.postValue(null);
    }
}
