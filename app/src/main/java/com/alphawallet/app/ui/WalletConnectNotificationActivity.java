package com.alphawallet.app.ui;

import android.content.Intent;
import android.os.Bundle;

import com.alphawallet.app.entity.walletconnect.WalletConnectSessionItem;
import com.alphawallet.app.interact.WalletConnectInteract;

import java.util.List;

import javax.inject.Inject;

import androidx.annotation.Nullable;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * This activity is created to simplify notification click event, according to sessions count, when:
 * 1: to session details
 * more than 1: to sessions list
 */
@AndroidEntryPoint
public class WalletConnectNotificationActivity extends BaseActivity
{
    @Inject
    WalletConnectInteract walletConnectInteract;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        route();
        finish();
    }

    private void route()
    {
        Intent intent;
        List<WalletConnectSessionItem> sessions = walletConnectInteract.getSessions();
        if (sessions.size() == 1)
        {
            intent = WalletConnectSessionActivity.newIntent(getApplicationContext(), sessions.get(0));
        }
        else
        {
            intent = new Intent(getApplicationContext(), WalletConnectSessionActivity.class);
        }

        startActivity(intent);
    }
}
