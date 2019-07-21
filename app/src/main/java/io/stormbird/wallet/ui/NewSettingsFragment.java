package io.stormbird.wallet.ui;


import android.Manifest;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.ArraySet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.GenericWalletInteract;
import io.stormbird.wallet.service.HDKeyService;
import io.stormbird.wallet.util.LocaleUtils;
import io.stormbird.wallet.viewmodel.NewSettingsViewModel;
import io.stormbird.wallet.viewmodel.NewSettingsViewModelFactory;
import io.stormbird.wallet.widget.AWalletConfirmationDialog;
import io.stormbird.wallet.widget.SelectLocaleDialog;

import java.util.Set;

import static io.stormbird.wallet.C.*;
import static io.stormbird.wallet.ui.HomeActivity.RC_ASSET_EXTERNAL_WRITE_PERM;

public class NewSettingsFragment extends Fragment {
    @Inject
    NewSettingsViewModelFactory newSettingsViewModelFactory;

    private NewSettingsViewModel viewModel;

    private TextView walletsSubtext;
    private TextView localeSubtext;
    private Switch notificationState;
    private LinearLayout layoutEnableXML;
    private LinearLayout layoutBackup;
    private Button backupButton;
    private TextView backupTitle;
    private TextView backupDetail;
    private RelativeLayout backupLayoutBackground;
    private ImageView backupMenuButton;
    private View backupPopupAnchor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        AndroidSupportInjection.inject(this);
        viewModel = ViewModelProviders.of(this, newSettingsViewModelFactory).get(NewSettingsViewModel.class);
        viewModel.defaultWallet().observe(this, this::onDefaultWallet);
        viewModel.setLocale(getContext());

        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        walletsSubtext = view.findViewById(R.id.wallets_subtext);
        localeSubtext = view.findViewById(R.id.locale_lang_subtext);
        notificationState = view.findViewById(R.id.switch_notifications);

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
            startActivity(intent);
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
            startActivity(intent);
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
        intent.putExtra("ADDRESS", wallet.address);

        switch (wallet.type)
        {
            case HDKEY:
                intent.putExtra("TYPE", "HDKEY");
                break;
            case KEYSTORE:
                intent.putExtra("TYPE", "JSON");
                break;
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        getActivity().startActivityForResult(intent, C.REQUEST_BACKUP_SEED);
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
        layoutBackup.setVisibility(View.GONE);
    }

    private void askWritePermission() {
        if (getActivity() != null)
        {
            final String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
            Log.w("SettingsFragment", "Folder write permission is not granted. Requesting permission");
            ActivityCompat.requestPermissions(getActivity(), permissions, RC_ASSET_EXTERNAL_WRITE_PERM);
        }
    }

    public void addBackupNotice(GenericWalletInteract.BackupLevel walletValue)
    {
        layoutBackup.setVisibility(View.VISIBLE);
        //current Wallet only
        Wallet wallet = viewModel.defaultWallet().getValue();
        if (wallet != null)
        {
            backupButton.setText(getString(R.string.back_up_wallet_action, wallet.address.substring(0, 5)));
            backupButton.setOnClickListener(v -> openBackupActivity(wallet));
            backupMenuButton.setOnClickListener(v -> {
                showPopup(backupPopupAnchor, wallet.address);
            });

            switch (walletValue)
            {
                case WALLET_HAS_LOW_VALUE:
                    backupTitle.setText(getString(R.string.time_to_backup_wallet));
                    backupDetail.setText(getString(R.string.recommend_monthly_backup));
                    backupLayoutBackground.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.slate_grey));
                    backupButton.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.backup_grey));
                    break;
                case WALLET_HAS_HIGH_VALUE:
                    backupTitle.setText(getString(R.string.wallet_not_backed_up));
                    backupDetail.setText(getString(R.string.not_backed_up_detail));
                    backupLayoutBackground.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.warning_red));
                    backupButton.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.warning_dark_red));
                    break;
            }
        }
    }

    private void showPopup(View view, String walletAddress) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View popupView = inflater.inflate(R.layout.popup_remind_later, null);
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);
        popupView.setOnClickListener(v -> {
            if (getActivity() != null) ((HomeActivity)getActivity()).postponeWalletBackupWarning(walletAddress);
            popupWindow.dismiss();
        });
        popupWindow.showAsDropDown(view, 0, 20);
    }
}
