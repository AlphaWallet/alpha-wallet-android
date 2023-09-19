package com.alphawallet.app.service;

import androidx.annotation.NonNull;

import com.alphawallet.app.entity.notification.DataMessage;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AlphaWalletFirebaseMessagingService extends FirebaseMessagingService
{
    @Inject
    TokensService tokensService;
    @Inject
    TransactionsService transactionsService;
    @Inject
    PreferenceRepositoryType preferenceRepository;

    /**
     * There are two scenarios when onNewToken is called:
     * 1) When a new token is generated on initial app startup
     * 2) Whenever an existing token is changed
     * Under #2, there are three scenarios when the existing token is changed:
     * A) App is restored to a new device
     * B) User uninstalls/reinstalls the app
     * C) User clears app data
     */
    @Override
    public void onNewToken(@NonNull String token)
    {
//         sendRegistrationToServer(token);
        preferenceRepository.setFirebaseMessagingToken(token);
    }

    private void sendRegistrationToServer(String token)
    {
        // TODO:
        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage)
    {
        super.onMessageReceived(remoteMessage);
        DataMessage.Body body = new Gson().fromJson(remoteMessage.getData().get("body"), DataMessage.Body.class);

        // If recipient is active wallet and app is on background, fetch transactions
        if (body != null &&
            body.to.equalsIgnoreCase(preferenceRepository.getCurrentWalletAddress()) &&
            !tokensService.isOnFocus())
        {
            transactionsService.fetchTransactionsFromBackground();
        }
    }
}