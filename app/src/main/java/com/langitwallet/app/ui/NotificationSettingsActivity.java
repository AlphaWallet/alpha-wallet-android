package com.langitwallet.app.ui;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.langitwallet.app.R;
import com.langitwallet.app.entity.Wallet;
import com.langitwallet.app.viewmodel.NotificationSettingsViewModel;
import com.langitwallet.app.widget.SettingsItemView;

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
        notifications.setToggleState(viewModel.isTransactionNotificationsEnabled(wallet.address));
    }

    private void onNotificationsClicked(Wallet wallet)
    {
        boolean isEnabled = viewModel.isTransactionNotificationsEnabled(wallet.address);
        notifications.setToggleState(!isEnabled);
        viewModel.setTransactionNotificationsEnabled(wallet.address, !isEnabled);

        // TODO: [Notifications] Uncomment when backend service is implemented
//        if (viewModel.isTransactionNotificationsEnabled(wallet.address))
//        {
//            if (PermissionUtils.requestPostNotificationsPermission(this, requestPermissionLauncher))
//            {
//                viewModel.subscribe(MAINNET_ID);
//            }
//        }
//        else
//        {
//            viewModel.unsubscribeToTopic(EthereumNetworkBase.MAINNET_ID);
//        }
    }
}
