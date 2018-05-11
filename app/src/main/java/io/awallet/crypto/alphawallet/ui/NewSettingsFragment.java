package io.awallet.crypto.alphawallet.ui;


import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import io.awallet.crypto.alphawallet.C;
import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.entity.NetworkInfo;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.viewmodel.NewSettingsViewModel;
import io.awallet.crypto.alphawallet.viewmodel.NewSettingsViewModelFactory;
import io.awallet.crypto.alphawallet.widget.SelectNetworkDialog;

public class NewSettingsFragment extends Fragment {
    @Inject
    NewSettingsViewModelFactory newSettingsViewModelFactory;

    private NewSettingsViewModel viewModel;
    private Wallet wallet;
    private TextView networksSubtext;
    private TextView walletsSubtext;
    private Switch notificationState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        AndroidSupportInjection.inject(this);
        viewModel = ViewModelProviders.of(this, newSettingsViewModelFactory).get(NewSettingsViewModel.class);
        viewModel.defaultWallet().observe(this, this::onDefaultWallet);
        viewModel.defaultNetwork().observe(this, this::onDefaultNetwork);

        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        networksSubtext = view.findViewById(R.id.networks_subtext);
        walletsSubtext = view.findViewById(R.id.wallets_subtext);
        notificationState = view.findViewById(R.id.switch_notifications);

        updateNotificationState();

        final LinearLayout layoutWalletAddress = view.findViewById(R.id.layout_wallet_address);
        layoutWalletAddress.setOnClickListener(v -> {
            viewModel.showMyAddress(getContext());
        });

        final LinearLayout layoutManageWallets = view.findViewById(R.id.layout_manage_wallets);
        layoutManageWallets.setOnClickListener(v -> {
            viewModel.showManageWallets(getContext(), false);
        });

        final LinearLayout layoutSwitchnetworks = view.findViewById(R.id.layout_switch_network);
        layoutSwitchnetworks.setOnClickListener(v -> {
            String currentNetwork = viewModel.getDefaultNetworkInfo().name;
            SelectNetworkDialog dialog = new SelectNetworkDialog(getActivity(), viewModel.getNetworkList(), currentNetwork);
            dialog.setOnClickListener(v1 -> {
                viewModel.setNetwork(dialog.getSelectedItem());
                networksSubtext.setText(dialog.getSelectedItem());
                if (!currentNetwork.equals(dialog.getSelectedItem())) {
                    viewModel.showHome(getContext(), true, true);
                }
                dialog.dismiss();
            });
            dialog.show();
        });

        final LinearLayout layoutHelp = view.findViewById(R.id.layout_help_faq);
        layoutHelp.setOnClickListener(v -> {
            viewModel.showHelp(getContext());
        });

        final LinearLayout layoutTwitter = view.findViewById(R.id.layout_twitter);
        layoutTwitter.setOnClickListener(v -> {
            Intent intent;
            try {
                getActivity().getPackageManager().getPackageInfo(C.TWITTER_PACKAGE_NAME, 0);
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(C.AWALLET_TWITTER_ID));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            } catch (Exception e) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(C.AWALLET_TWITTER_URL));
            }
            startActivity(intent);
        });

        final LinearLayout layoutNotifications = view.findViewById(R.id.layout_notification_settings);
        layoutNotifications.setOnClickListener(v -> {
            boolean currentState = viewModel.getNotificationState();
            currentState = !currentState;
            viewModel.setNotificationState(currentState);
            updateNotificationState();
        });

        LinearLayout layoutFacebook = view.findViewById(R.id.layout_facebook);
        layoutFacebook.setOnClickListener(v -> {
            Intent intent;
            try {
                getActivity().getPackageManager().getPackageInfo(C.FACEBOOK_PACKAGE_NAME, 0);
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(C.AWALLET_FACEBOOK_ID));
            } catch (Exception e) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(C.AWALLET_FACEBOOK_URL));
            }
            startActivity(intent);
        });

        return view;
    }

    private void updateNotificationState()
    {
        boolean state = viewModel.getNotificationState();
        notificationState.setChecked(state);
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        networksSubtext.setText(networkInfo.name);
    }

    private void onDefaultWallet(Wallet wallet) {
        this.wallet = wallet;
        walletsSubtext.setText(wallet.address);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.prepare();
    }
}
