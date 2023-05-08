package com.alphawallet.app.ui;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.viewmodel.NotificationSettingsViewModel;
import com.alphawallet.app.widget.PermissionRationaleDialog;
import com.alphawallet.app.widget.SettingsItemView;
import com.alphawallet.ethereum.EthereumNetworkBase;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class NotificationSettingsActivity extends BaseActivity
{
    private NotificationSettingsViewModel viewModel;
    private final ActivityResultLauncher<String> requestPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted)
            {
                // FCM SDK (and your app) can post notifications.
                viewModel.subscribe(MAINNET_ID);
            }
            else
            {
                Toast.makeText(this, getString(R.string.message_deny_request_post_notifications_permission), Toast.LENGTH_LONG).show();
            }
        });
    private SettingsItemView notifications;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_notification_settings);

        toolbar();

        setTitle(getString(R.string.title_notifications));

        initViewModel();
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this).get(NotificationSettingsViewModel.class);
        viewModel.wallet().observe(this, this::onWallet);
    }

    private void onWallet(Wallet wallet)
    {
        initializeSettings(wallet);
    }

    private void initializeSettings(Wallet wallet)
    {
        notifications = findViewById(R.id.setting_transaction_notification);
        notifications.setListener(() -> onNotificationsClicked(wallet));
        notifications.setToggleState(viewModel.getToggleState(wallet.address));
    }

    private void onNotificationsClicked(Wallet wallet)
    {
        boolean isEnabled = viewModel.getToggleState(wallet.address);
        notifications.setToggleState(!isEnabled);
        viewModel.setToggleState(wallet.address, !isEnabled);

        // TODO: [Notifications] Uncomment when backend service is implemented
//        if (viewModel.getToggleState(wallet.address))
//        {
//            requestPostNotificationsPermission();
//        }
//        else
//        {
//            viewModel.unsubscribeToTopic(EthereumNetworkBase.MAINNET_ID);
//        }
    }

    private void requestPostNotificationsPermission()
    {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED)
            {
                // FCM SDK (and your app) can post notifications.
                viewModel.subscribe(MAINNET_ID);
            }
            else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))
            {
                PermissionRationaleDialog.show(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS,
                    ok -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS),
                    cancel -> {}
                );
            }
            else
            {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        else
        {
            viewModel.subscribe(MAINNET_ID);
        }
    }
}
