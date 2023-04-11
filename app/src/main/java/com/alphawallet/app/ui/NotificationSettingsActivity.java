package com.alphawallet.app.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.viewmodel.NotificationSettingsViewModel;
import com.alphawallet.app.widget.SettingsItemView;
import com.google.firebase.messaging.FirebaseMessaging;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class NotificationSettingsActivity extends BaseActivity
{
    private NotificationSettingsViewModel viewModel;
    private SettingsItemView notifications;
    private Wallet wallet;
    private final ActivityResultLauncher<String> requestPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted)
            {
                // FCM SDK (and your app) can post notifications.
                Toast.makeText(this, "Permission granted.", Toast.LENGTH_SHORT).show();
                viewModel.subscribe(wallet.address, "1");
            }
            else
            {
                // TODO: Inform user that that your app will not show notifications.
                Toast.makeText(this, "Permission not granted.", Toast.LENGTH_SHORT).show();
            }
        });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_generic_settings);

        toolbar();

        setTitle(getString(R.string.title_notifications));

        initViewModel();

        initializeSettings();
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this)
            .get(NotificationSettingsViewModel.class);
        viewModel.defaultWallet().observe(this, this::onDefaultWallet);
        viewModel.subscribe().observe(this, this::onSubscribe);
        viewModel.unsubscribe().observe(this, this::onUnsubscribe);
        viewModel.prepare();
    }

    private void onSubscribe(String result)
    {
        subscribe(wallet.address + "-1");
    }

    private void onUnsubscribe(String result)
    {
        unsubscribe(wallet.address + "-1");
    }

    private void onDefaultWallet(Wallet wallet)
    {
        this.wallet = wallet;
    }

    private void initializeSettings()
    {
        notifications = new SettingsItemView.Builder(this)
            .withType(SettingsItemView.Type.TOGGLE)
            .withIcon(R.drawable.ic_settings_notifications)
            .withTitle(R.string.title_notifications)
            .withListener(this::onNotificationsClicked)
            .build();

        notifications.setToggleState(viewModel.getToggleState());

        LinearLayout advancedSettingsLayout = findViewById(R.id.layout);
        advancedSettingsLayout.addView(notifications);
    }

    private void onNotificationsClicked()
    {
        boolean isEnabled = viewModel.getToggleState();
        notifications.setToggleState(!isEnabled);
        viewModel.setToggleState(!isEnabled);
        if (viewModel.getToggleState())
        {
            askNotificationPermission();
        }
        else
        {
            viewModel.unsubscribe(wallet.address, "1");
        }
    }

    private void askNotificationPermission()
    {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED)
            {
                // FCM SDK (and your app) can post notifications.
                Timber.d("Permission granted.");
                viewModel.subscribe(wallet.address, "1");
            }
            else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))
            {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
                Timber.d("Permission must be granted.");
            }
            else
            {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        else
        {
            viewModel.subscribe(wallet.address, "1");
        }
    }

    private void subscribe(String topic)
    {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener(task -> {
                String msg = "Subscribed to " + topic;
                if (!task.isSuccessful())
                {
                    msg = "Subscribe failed";
                }
                Timber.d(msg);
            });
    }

    private void unsubscribe(String topic)
    {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnCompleteListener(task -> {
                String msg = "Unsubscribed to" + topic;
                if (!task.isSuccessful())
                {
                    msg = "Unsubscribe failed";
                }
                Timber.d(msg);
            });
    }
}
