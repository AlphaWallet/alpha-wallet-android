package com.alphawallet.app.service;

import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.alphawallet.app.entity.walletconnect.WalletConnectV2SessionItem;
import com.alphawallet.app.ui.WalletConnectV2Activity;
import com.walletconnect.walletconnectv2.client.WalletConnect;
import com.walletconnect.walletconnectv2.client.WalletConnectClient;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class AWWalletConnectClient implements WalletConnectClient.WalletDelegate
{
    public static WalletConnect.Model.SessionProposal sessionProposal;

    private static final String TAG = "seaborn";
    private Application application;

    public AWWalletConnectClient(Application application)
    {
        this.application = application;
    }

    @Override
    public void onSessionDelete(@NonNull WalletConnect.Model.DeletedSession deletedSession)
    {

    }

    @Override
    public void onSessionNotification(@NonNull WalletConnect.Model.SessionNotification sessionNotification)
    {

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onSessionProposal(@NonNull WalletConnect.Model.SessionProposal sessionProposal)
    {
        AWWalletConnectClient.sessionProposal = sessionProposal;
        Log.d(TAG, "onSessionProposal: ");
        Intent intent = new Intent(application, WalletConnectV2Activity.class);
        intent.putExtra("session", WalletConnectV2SessionItem.from(sessionProposal));
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        application.startActivity(intent);
    }

    @Override
    public void onSessionRequest(@NonNull WalletConnect.Model.SessionRequest sessionRequest)
    {
        Log.d(TAG, "onSessionRequest: ");
    }
}
