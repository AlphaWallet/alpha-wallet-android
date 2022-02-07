package com.alphawallet.app.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.alphawallet.app.R;
import com.walletconnect.walletconnectv2.client.WalletConnect;
import com.walletconnect.walletconnectv2.client.WalletConnectClient;

import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class WalletConnectV2Service extends Service
{
    private static final String TAG = "seaborn";

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        Log.d(TAG, "onBind: ");
        return null;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d(TAG, "onCreate: ");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG, "onStartCommand: ");
        initWalletConnectV2Client();

        WalletConnectClient.INSTANCE.setWalletDelegate(new AWWalletConnectClient(getApplication()));
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
    }

    private void initWalletConnectV2Client()
    {
        WalletConnect.Model.AppMetaData appMetaData = getAppMetaData();
        WalletConnect.Params.Init init = new WalletConnect.Params.Init(getApplication(),
                "wss://relay.walletconnect.com/?projectId=40c6071febfd93f4fe485c232a8a4cd9",
                true,
                appMetaData);

        WalletConnectClient.INSTANCE.initialize(init);
    }

    @NonNull
    private WalletConnect.Model.AppMetaData getAppMetaData()
    {

        String name = getString(R.string.app_name);
        String url = "https://alphawallet.com";
        String[] icons = {"https://gblobscdn.gitbook.com/spaces%2F-LJJeCjcLrr53DcT1Ml7%2Favatar.png?alt=media"};

        String description = "The ultimate Web3 Wallet to power your tokens.";
        return new WalletConnect.Model.AppMetaData(name, description, url, Arrays.asList(icons));
    }

}
