package com.alphawallet.app.ui;


import android.Manifest;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.alphawallet.app.util.LocaleUtils;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.viewmodel.NewSettingsViewModel;
import com.alphawallet.app.viewmodel.NewSettingsViewModelFactory;
import com.alphawallet.app.widget.AWalletConfirmationDialog;
import com.alphawallet.app.widget.SelectLocaleDialog;
import com.crashlytics.android.Crashlytics;

import static com.alphawallet.app.C.*;
import static com.alphawallet.app.C.Key.WALLET;

public class NewSettingsFragment extends Fragment
{
    @Inject
    NewSettingsViewModelFactory newSettingsViewModelFactory;

    private NewSettingsViewModel viewModel;

    private TextView walletsSubtext;
    private TextView backupStatusText;
    private TextView localeSubtext;
    private Switch notificationState;
    private LinearLayout layoutEnableXML;
    private LinearLayout layoutBackup;
    private Button backupButton;
    private TextView backupTitle;
    private TextView backupDetail;
    private RelativeLayout backupLayoutBackground;
    private ImageView backupMenuButton;
    private ImageView backupStatusImage;
    private View backupPopupAnchor;
    private LinearLayout layoutBackupKey;
    private LinearLayout layoutBackupDivider;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        AndroidSupportInjection.inject(this);
        viewModel = ViewModelProviders.of(this, newSettingsViewModelFactory).get(NewSettingsViewModel.class);
        viewModel.defaultWallet().observe(this, this::onDefaultWallet);
        viewModel.backUpMessage().observe(this, this::backupWarning);
        viewModel.setLocale(getContext());

        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        walletsSubtext = view.findViewById(R.id.wallets_subtext);
        localeSubtext = view.findViewById(R.id.locale_lang_subtext);
        notificationState = view.findViewById(R.id.switch_notifications);
        backupStatusText = view.findViewById(R.id.text_backup_status);
        backupStatusImage = view.findViewById(R.id.image_backup_status);

        TextView helpText = view.findViewById(R.id.text_version);
        try
        {
            String version = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
            helpText.setText(version);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }

        localeSubtext.setText(LocaleUtils.getDisplayLanguage(viewModel.getDefaultLocale(), viewModel.getDefaultLocale()));

        updateNotificationState();

        final LinearLayout layoutWalletAddress = view.findViewById(R.id.layout_wallet_address);
        layoutWalletAddress.setOnClickListener(v -> {
            viewModel.showMyAddress(getContext());
        });

        final LinearLayout layoutManageWallets = view.findViewById(R.id.layout_manage_wallets);
        layoutManageWallets.setOnClickListener(v -> {
            viewModel.showManageWallets(getContext(), false);
        });

        layoutBackupDivider = view.findViewById(R.id.backup_divider);
        layoutBackupKey = view.findViewById(R.id.layout_backup_wallet);
        layoutBackupKey.setOnClickListener(v -> {
            Wallet wallet = viewModel.defaultWallet().getValue();
            if (wallet != null)
            {
                openBackupActivity(wallet);
            }
        });

        final LinearLayout layoutSwitchnetworks = view.findViewById(R.id.layout_switch_network);
        layoutSwitchnetworks.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SelectNetworkActivity.class);
            intent.putExtra(C.EXTRA_SINGLE_ITEM, false);
            getActivity().startActivity(intent);
        });

        final LinearLayout layoutSwitchLocale = view.findViewById(R.id.layout_locale_lang);
        layoutSwitchLocale.setOnClickListener(v -> {
            String currentLocale = viewModel.getDefaultLocale();
            SelectLocaleDialog dialog = new SelectLocaleDialog(getActivity(), viewModel.getLocaleList(getContext()), currentLocale);
            dialog.setOnClickListener(v1 -> {
                if (!currentLocale.equals(dialog.getSelectedItemId())) {
                    viewModel.setDefaultLocale(getContext(), dialog.getSelectedItemId());
                    localeSubtext.setText(LocaleUtils.getDisplayLanguage(dialog.getSelectedItemId(), currentLocale));
                    getActivity().sendBroadcast(new Intent(CHANGED_LOCALE));
                }
                dialog.dismiss();
            });
            dialog.show();
        });

        final LinearLayout layoutHelp = view.findViewById(R.id.layout_help_faq);
        layoutHelp.setOnClickListener(v -> {
            viewModel.showHelp(getContext());
        });

        final LinearLayout layoutTelegram = view.findViewById(R.id.layout_telegram);
        layoutTelegram.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(C.AWALLET_TELEGRAM_URL));
            if (isAppAvailable(C.TELEGRAM_PACKAGE_NAME)) {
                intent.setPackage(C.TELEGRAM_PACKAGE_NAME);
            }
            try
            {
                getActivity().startActivity(intent);
            }
            catch (Exception e)
            {
                Crashlytics.logException(e);
                e.printStackTrace();
            }
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
            try
            {
                getActivity().startActivity(intent);
            }
            catch (Exception e)
            {
                Crashlytics.logException(e);
                e.printStackTrace();
            }
        });

        final LinearLayout layoutNotifications = view.findViewById(R.id.layout_notification_settings);
        layoutNotifications.setOnClickListener(v -> {
            boolean currentState = viewModel.getNotificationState();
            currentState = !currentState;
            viewModel.setNotificationState(currentState);
            updateNotificationState();
        });

        layoutBackup = view.findViewById(R.id.layout_item_warning);
        backupTitle = view.findViewById(R.id.text_title);
        backupDetail = view.findViewById(R.id.text_detail);
        backupLayoutBackground = view.findViewById(R.id.layout_backup_text);
        backupButton = view.findViewById(R.id.button_backup);
        backupMenuButton = view.findViewById(R.id.btn_menu);
        backupPopupAnchor = view.findViewById(R.id.popup_anchor);
        layoutBackup.setVisibility(View.GONE);

        LinearLayout layoutFacebook = view.findViewById(R.id.layout_facebook);
        layoutFacebook.setOnClickListener(v -> {
            Intent intent;
            try {
                getActivity().getPackageManager().getPackageInfo(C.FACEBOOK_PACKAGE_NAME, 0);
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(C.AWALLET_FACEBOOK_ID));
            } catch (Exception e) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(C.AWALLET_FACEBOOK_URL));
            }
            try
            {
                getActivity().startActivity(intent);
            }
            catch (Exception e)
            {
                Crashlytics.logException(e);
                e.printStackTrace();
            }
        });

        layoutEnableXML = view.findViewById(R.id.layout_xml_override);
        if (checkWritePermission() == false) {
            layoutEnableXML.setVisibility(View.VISIBLE);
            layoutEnableXML.setOnClickListener(v -> {
                //ask OS to ask user if we can use the 'AlphaWallet' directory
                showXMLOverrideDialog();
            });
        }

        return view;
    }

    private void openBackupActivity(Wallet wallet)
    {
        Intent intent = new Intent(getContext(), BackupKeyActivity.class);
        intent.putExtra(WALLET, wallet);

        switch (wallet.type)
        {
            case HDKEY:
                intent.putExtra("TYPE", BackupKeyActivity.BackupOperationType.BACKUP_HD_KEY);
                break;
            case KEYSTORE_LEGACY:
            case KEYSTORE:
                intent.putExtra("TYPE", BackupKeyActivity.BackupOperationType.BACKUP_KEYSTORE_KEY);
                break;
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        getActivity().startActivityForResult(intent, C.REQUEST_BACKUP_WALLET);
    }

    private boolean isAppAvailable(String packageName) {
        PackageManager pm = getActivity().getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void updateNotificationState() {
        boolean state = viewModel.getNotificationState();
        notificationState.setChecked(state);
    }

    private void onDefaultWallet(Wallet wallet) {
        walletsSubtext.setText(wallet.address);
        switch (wallet.authLevel)
        {
            case NOT_SET:
            case STRONGBOX_NO_AUTHENTICATION:
            case TEE_NO_AUTHENTICATION:
                if (wallet.lastBackupTime > 0)
                {
                    backupStatusText.setText(R.string.not_locked);
                    backupStatusImage.setImageResource(R.drawable.ic_orange_bar);
                }
                else
                {
                    backupStatusText.setText(R.string.back_up_now);
                    backupStatusImage.setImageResource(R.drawable.ic_red_bar);
                }
                break;
            case TEE_AUTHENTICATION:
            case STRONGBOX_AUTHENTICATION:
                backupStatusText.setText(R.string.key_secure);
                backupStatusImage.setImageResource(R.drawable.ic_green_bar);
                break;
        }

        if (wallet.type == WalletType.WATCH)
        {
            layoutBackupKey.setVisibility(View.GONE);
            layoutBackupDivider.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.prepare();
    }

    private void showXMLOverrideDialog() {
        AWalletConfirmationDialog cDialog = new AWalletConfirmationDialog(getActivity());
        cDialog.setTitle(R.string.enable_xml_override_dir);
        cDialog.setSmallText(R.string.explain_xml_override);
        cDialog.setMediumText(R.string.ask_user_about_xml_override);
        cDialog.setPrimaryButtonText(R.string.dialog_ok);
        cDialog.setPrimaryButtonListener(v -> {
            //ask for OS permission and write directory
            askWritePermission();
            cDialog.dismiss();
        });
        cDialog.setSecondaryButtonText(R.string.dialog_cancel_back);
        cDialog.setSecondaryButtonListener(v -> {
            cDialog.dismiss();
        });
        cDialog.show();
    }

    private boolean checkWritePermission() {
        return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    public void refresh() {
        if (layoutEnableXML != null) {
            if (checkWritePermission()) {
                layoutEnableXML.setVisibility(View.GONE);
            } else {
                layoutEnableXML.setVisibility(View.VISIBLE);
            }
        }
    }

    public void backupSeedSuccess()
    {
        if (viewModel != null) viewModel.TestWalletBackup();
        if (layoutBackup != null) layoutBackup.setVisibility(View.GONE);
    }

    private void askWritePermission() {
        if (getActivity() != null)
        {
            final String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
            Log.w("SettingsFragment", "Folder write permission is not granted. Requesting permission");
            ActivityCompat.requestPermissions(getActivity(), permissions, HomeActivity.RC_ASSET_EXTERNAL_WRITE_PERM);
        }
    }

    private void backupWarning(String s)
    {
        if (s.equals(viewModel.defaultWallet().getValue().address))
        {
            addBackupNotice(GenericWalletInteract.BackupLevel.WALLET_HAS_HIGH_VALUE);
        }
        else
        {
            if (layoutBackup != null)
            {
                layoutBackup.setVisibility(View.GONE);
            }
            //remove the number prompt
            if (getActivity() != null) ((HomeActivity) getActivity()).removeSettingsBadgeKey(C.KEY_NEEDS_BACKUP);
            onDefaultWallet(viewModel.defaultWallet().getValue());
        }
    }

    void addBackupNotice(GenericWalletInteract.BackupLevel walletValue)
    {
        layoutBackup.setVisibility(View.VISIBLE);
        //current Wallet only
        Wallet wallet = viewModel.defaultWallet().getValue();
        if (wallet != null)
        {
            backupButton.setText(getString(R.string.back_up_wallet_action, wallet.address.substring(0, 5)));
            backupButton.setOnClickListener(v -> openBackupActivity(wallet));
            backupTitle.setText(getString(R.string.wallet_not_backed_up));
            backupLayoutBackground.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.warning_red));
            backupButton.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.warning_dark_red));
            backupDetail.setText(getString(R.string.backup_wallet_detail));
            if (getActivity() !=null) ((HomeActivity) getActivity()).addSettingsBadgeKey(C.KEY_NEEDS_BACKUP);

            backupMenuButton.setOnClickListener(v -> {
                showPopup(backupPopupAnchor, wallet.address);
            });
        }
    }

    private void showPopup(View view, String walletAddress) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View popupView = inflater.inflate(R.layout.popup_remind_later, null);
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);
        popupView.setOnClickListener(v -> {
            //if (getActivity() != null) ((HomeActivity)getActivity()).postponeWalletBackupWarning(walletAddress);
            viewModel.setIsDismissed(walletAddress, true).subscribe(this::backedUp);
            popupWindow.dismiss();
        });
        popupWindow.showAsDropDown(view, 0, 20);
    }

    private void backedUp(String walletAddress)
    {
        layoutBackup.setVisibility(View.GONE);
        if (getActivity() != null) ((HomeActivity)getActivity()).postponeWalletBackupWarning(walletAddress);
    }
}
