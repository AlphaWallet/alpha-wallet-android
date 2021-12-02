package com.alphawallet.app.service;

import static com.alphawallet.app.C.WALLET_CONNECT_CLIENT_TERMINATE;
import static com.alphawallet.app.C.WALLET_CONNECT_COUNT_CHANGE;
import static com.alphawallet.app.C.WALLET_CONNECT_FAIL;
import static com.alphawallet.app.C.WALLET_CONNECT_NEW_SESSION;
import static com.alphawallet.app.C.WALLET_CONNECT_REQUEST;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.entity.WalletConnectActions;
import com.alphawallet.app.entity.walletconnect.WCRequest;
import com.alphawallet.app.walletconnect.WCClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import kotlin.Unit;

/**
 * The purpose of this service is to manage the currently active WalletConnect sessions. Keep the connections alive and terminate where required.
 * Doing this in an activity means the connection objects are subject to activity lifecycle events (Destroy etc).
 */
public class WalletConnectService extends Service
{
    private final long CONNECTION_TIMEOUT = 10*DateUtils.MINUTE_IN_MILLIS;
    private final ConcurrentHashMap<String, WCClient> clientMap = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<WCRequest> signRequests = new ConcurrentLinkedQueue<>();

    private final ConcurrentHashMap<String, Long> clientTimes = new ConcurrentHashMap<>();
    private WCRequest currentRequest = null;

    private static final String TAG = "WCClientSvs";

    @Nullable
    private Disposable messagePump;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG, "SERVICE STARTING");
        try
        {
            int actionVal = Integer.parseInt(intent.getAction());
            WalletConnectActions action = WalletConnectActions.values()[actionVal];

            switch (action)
            {
                case CONNECT:
                    Log.d(TAG, "SERVICE CONNECT");
                    break;
                case APPROVE:
                    approveRequest(intent);
                    break;
                case REJECT:
                    rejectRequest(intent);
                    break;
                case DISCONNECT:
                    Log.d(TAG, "SERVICE DISCONNECT");
                    //kill any active connection
                    disconnectCurrentSessions();
                    break;
                case CLOSE:
                    Log.d(TAG, "SERVICE CLOSE");
                    //result.getData().getIntExtra(C.EXTRA_CHAIN_ID, -1);
                    String sessionId = intent.getStringExtra("session");
                    disconnectSession(sessionId);
                    break;
            }
        }
        catch (Exception e)
        {
            if (BuildConfig.DEBUG) e.printStackTrace();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    private final IBinder mBinder = new LocalBinder();

    public WCRequest getPendingRequest(String sessionId)
    {
        WCRequest request = signRequests.poll();
        if (request != null && !request.sessionId.equals(sessionId))
        {
            signRequests.add(request); // not for this client, put it back on the stack, at the back
            request = null;
        }
        else if (request != null)
        {
            currentRequest = request;
        }

        return request;
    }

    public int getConnectionCount()
    {
        return clientMap.size();
    }

    public WCRequest getCurrentRequest()
    {
        return currentRequest;
    }

    private void disconnectCurrentSessions()
    {
        for (WCClient client : clientMap.values())
        {
            if (client.isConnected())
            {
                client.killSession();
            }
        }
    }

    private void disconnectSession(String sessionId)
    {
        if (TextUtils.isEmpty(sessionId))
        {
            disconnectCurrentSessions();
        }
        else
        {
            WCClient client = clientMap.get(sessionId);
            if (client != null) client.killSession();
        }
    }

    public class LocalBinder extends Binder
    {
        public WalletConnectService getService()
        {
            return WalletConnectService.this;
        }
    }

    @Nullable
    private Disposable pingTimer;

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    public WCClient getClient(String sessionId)
    {
        if (sessionId == null) { return null; }
        else { return clientMap.get(sessionId); }
    }

    public void putClient(String sessionId, WCClient client)
    {
        Log.d(TAG, "Add session: " + sessionId);
        clientMap.put(sessionId, client);
        broadcastConnectionCount(clientMap.size());
        clientTimes.put(sessionId, System.currentTimeMillis());
        setupClient(client);
        startSessionPinger();
    }

    private void rejectRequest(Intent intent)
    {
        String sessionId = intent.getStringExtra("sessionId");
        long id = intent.getLongExtra("id", 0L);
        String message = intent.getStringExtra("message");

        WCClient c = clientMap.get(sessionId);
        if (c != null && c.isConnected())
        {
            c.rejectRequest(id, message);
        }
    }

    private void approveRequest(Intent intent)
    {
        String sessionId = intent.getStringExtra("sessionId");
        long id = intent.getLongExtra("id", 0L);
        String message = intent.getStringExtra("message");

        WCClient c = clientMap.get(sessionId);

        if (c != null && c.isConnected())
        {
            c.approveRequest(id, message);
        }
    }

    private void setupClient(WCClient client)
    {
        client.setOnSessionRequest((id, peer) -> {
            if (client.sessionId() == null) return Unit.INSTANCE;
            setLastUsed(client);
            signRequests.add(new WCRequest(client.sessionId(), id, peer, client.chainIdVal()));
            broadcastSessionEvent(WALLET_CONNECT_NEW_SESSION, client.sessionId());
            Log.d(TAG, "On Request: " + peer.getName());
            return Unit.INSTANCE;
        });

        client.setOnFailure(throwable -> {
            //alert UI
            if (client.sessionId() == null) return Unit.INSTANCE;
            Log.d(TAG, "On Fail: " + throwable.getMessage());
            signRequests.add(new WCRequest(client.sessionId(), throwable, client.chainIdVal()));
            broadcastSessionEvent(WALLET_CONNECT_FAIL, client.sessionId());
            return Unit.INSTANCE;
        });

        client.setOnDisconnect((code, reason) -> {
            Log.d(TAG, "Terminate session?");
            terminateClient(client.sessionId());
            client.resetState();
            return Unit.INSTANCE;
        });

        client.setOnEthSign((id, message) -> {
            if (client.sessionId() == null) return Unit.INSTANCE;
            setLastUsed(client);
            signRequests.add(new WCRequest(client.sessionId(), id, message));
            //see if this connection is live, if so then bring WC request to foreground
            switchToWalletConnectApprove(client.sessionId());
            Log.d(TAG, "Sign Request: " + message.toString());
            startMessagePump();
            return Unit.INSTANCE;
        });

        client.setOnEthSignTransaction((id, transaction) -> {
            if (client.sessionId() == null) return Unit.INSTANCE;
            setLastUsed(client);
            signRequests.add(new WCRequest(client.sessionId(), id, transaction, true, client.chainIdVal()));
            switchToWalletConnectApprove(client.sessionId());
            startMessagePump();
            return Unit.INSTANCE;
        });

        client.setOnEthSendTransaction((id, transaction) -> {
            if (client.sessionId() == null) return Unit.INSTANCE;
            setLastUsed(client);
            signRequests.add(new WCRequest(client.sessionId(), id, transaction, false, client.chainIdVal()));
            switchToWalletConnectApprove(client.sessionId());
            startMessagePump();
            return Unit.INSTANCE;
        });
    }

    private void broadcastSessionEvent(String command, String sessionId)
    {
        Intent intent = new Intent(command);
        intent.putExtra("sessionid", sessionId);
        sendBroadcast(intent);
    }

    private void broadcastConnectionCount(int count)
    {
        Intent intent = new Intent(WALLET_CONNECT_COUNT_CHANGE);
        intent.putExtra("count", count);
        sendBroadcast(intent);
    }

    private void switchToWalletConnectApprove(String sessionId)
    {
        WCClient cc = clientMap.get(sessionId);

        if (cc != null)
        {
            Intent intent = new Intent(WALLET_CONNECT_REQUEST);
            intent.putExtra("sessionid", sessionId);
            sendBroadcast(intent);

            Log.d(TAG, "Connected clients: " + clientMap.size());
        }
    }

    private void startSessionPinger()
    {
        if (pingTimer == null || pingTimer.isDisposed())
        {
            pingTimer = Observable.interval(0, 30, TimeUnit.SECONDS)
                    .doOnNext(l -> ping()).subscribe();
        }
    }

    private void ping()
    {
        List<String> removeKeyList = new ArrayList<>();
        for (String sessionKey : clientMap.keySet())
        {
            WCClient c = clientMap.get(sessionKey);
            if (c == null) return;
            if (c.isConnected() && c.chainIdVal() != 0 && c.getAccounts() != null)
            {
                Log.d(TAG, "Ping Key: " + sessionKey);
                c.approveSession(c.getAccounts(), c.chainIdVal());
            }

            long lastUsed = getLastUsed(c);
            long timeUntilTerminate = CONNECTION_TIMEOUT - (System.currentTimeMillis() - lastUsed);
            Log.d(TAG, "Time until terminate: " + timeUntilTerminate/DateUtils.SECOND_IN_MILLIS + " (" + sessionKey + ")");
            if ((System.currentTimeMillis() - lastUsed) > CONNECTION_TIMEOUT)
            {
                if (c.getSession() != null)
                {
                    Log.d(TAG, "Terminate session: " + sessionKey);
                    c.killSession();
                }
                else
                {
                    Log.d(TAG, "Disconnect session: " + sessionKey);
                    c.disconnect();
                }
                removeKeyList.add(sessionKey);
            }
        }

        for (String removeKey : removeKeyList)
        {
            Log.d(TAG, "Removing Key: " + removeKey);
            terminateClient(removeKey);
        }
    }

    private void terminateClient(String sessionKey)
    {
        broadcastSessionEvent(WALLET_CONNECT_CLIENT_TERMINATE, sessionKey);
        clientMap.remove(sessionKey);
        broadcastConnectionCount(clientMap.size());
        if (clientMap.size() == 0 && pingTimer != null && !pingTimer.isDisposed())
        {
            Log.d(TAG, "Stop timer & service");
            pingTimer.dispose();
            pingTimer = null;
            stopSelf();
        }
    }

    private long getLastUsed(WCClient c)
    {
        String sessionId = c.sessionId();
        if (sessionId != null && clientTimes.containsKey(sessionId)) return clientTimes.get(sessionId);
        else return 0;
    }

    private void setLastUsed(WCClient c)
    {
        String sessionId = c.sessionId();
        if (sessionId != null) clientTimes.put(sessionId, System.currentTimeMillis());
    }

    private void startMessagePump()
    {
        if (messagePump != null && !messagePump.isDisposed()) messagePump.dispose();

        messagePump = Observable.interval(2000, 2000, TimeUnit.MILLISECONDS)
                .doOnNext(l -> checkMessages())
                .observeOn(Schedulers.newThread()).subscribe();
    }

    private void checkMessages()
    {
        WCRequest rq = signRequests.peek();
        if (rq != null)
        {
            switchToWalletConnectApprove(rq.sessionId);
        }
        else if (messagePump != null && !messagePump.isDisposed())
        {
            messagePump.dispose();
        }
    }
}
