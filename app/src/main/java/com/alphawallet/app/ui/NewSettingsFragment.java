package com.alphawallet.app.ui;


import static android.app.Activity.RESULT_OK;
import static com.alphawallet.app.C.CHANGED_LOCALE;
import static com.alphawallet.app.C.CHANGE_CURRENCY;
import static com.alphawallet.app.C.Key.WALLET;
import static com.alphawallet.app.C.RESET_TOOLBAR;
import static com.alphawallet.app.C.RESET_WALLET;
import static com.alphawallet.app.C.SETTINGS_INSTANTIATED;
import static com.alphawallet.app.entity.BackupOperationType.BACKUP_HD_KEY;
import static com.alphawallet.app.entity.BackupOperationType.BACKUP_KEYSTORE_KEY;
import static com.alphawallet.app.ui.HomeActivity.RESET_TOKEN_SERVICE;
import static com.alphawallet.token.tools.TokenDefinition.TOKENSCRIPT_CURRENT_SCHEMA;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.BackupOperationType;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletPage;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.util.LocaleUtils;
import com.alphawallet.app.util.UpdateUtils;
import com.alphawallet.app.viewmodel.NewSettingsViewModel;
import com.alphawallet.app.widget.NotificationView;
import com.alphawallet.app.widget.SettingsItemView;

import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class NewSettingsFragment extends BaseFragment {

    private NewSettingsViewModel viewModel;

    private LinearLayout walletSettingsLayout;
    private LinearLayout systemSettingsLayout;
    private LinearLayout supportSettingsLayout;

    private SettingsItemView myAddressSetting;
    private SettingsItemView changeWalletSetting;
    private SettingsItemView backUpWalletSetting;
    private SettingsItemView notificationsSetting;
    private SettingsItemView biometricsSetting;
    private SettingsItemView selectNetworksSetting;
    private SettingsItemView advancedSetting;
    private SettingsItemView supportSetting;
    private SettingsItemView walletConnectSetting;
    private SettingsItemView showSeedPhrase;
    private SettingsItemView nameThisWallet;

    private LinearLayout layoutBackup;
    private View warningSeparator;
    private Button backupButton;
    private TextView backupTitle;
    private TextView backupDetail;
    private ImageView backupMenuButton;
    private View backupPopupAnchor;
    private NotificationView notificationView;
    private int pendingUpdate = 0;
    private LinearLayout updateLayout;

    private Wallet wallet;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this)
                .get(NewSettingsViewModel.class);
        viewModel.defaultWallet().observe(getViewLifecycleOwner(), this::onDefaultWallet);
        viewModel.backUpMessage().observe(getViewLifecycleOwner(), this::backupWarning);
        LocaleUtils.setActiveLocale(getContext());

        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        toolbar(view);

        setToolbarTitle(R.string.toolbar_header_settings);

        initializeSettings(view);

        addSettingsToLayout();

        setInitialSettingsData(view);

        initBackupWarningViews(view);

        initNotificationView(view);

        checkPendingUpdate(view);

        getParentFragmentManager().setFragmentResult(SETTINGS_INSTANTIATED, new Bundle());

        return view;
    }

    private void initNotificationView(View view) {
        notificationView = view.findViewById(R.id.notification);
        if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            notificationView.setNotificationBackgroundColor(R.color.indigo);
            notificationView.setTitle(getContext().getString(R.string.title_version_support_warning));
            notificationView.setMessage(getContext().getString(R.string.message_version_support_warning));
        } else {
            notificationView.setVisibility(View.GONE);
        }
    }

    public void signalUpdate(int updateVersion)
    {
        //add wallet update signal to adapter
        pendingUpdate = updateVersion;
        checkPendingUpdate(getView());
    }

    private void initBackupWarningViews(View view) {
        layoutBackup = view.findViewById(R.id.layout_item_warning);
        backupTitle = view.findViewById(R.id.text_title);
        backupDetail = view.findViewById(R.id.text_detail);
        backupButton = view.findViewById(R.id.button_backup);
        backupMenuButton = view.findViewById(R.id.btn_menu);
        backupPopupAnchor = view.findViewById(R.id.popup_anchor);
        layoutBackup.setVisibility(View.GONE);
        warningSeparator = view.findViewById(R.id.warning_separator);
    }

    private void initializeSettings(View view) {
        walletSettingsLayout = view.findViewById(R.id.layout_settings_wallet);
        systemSettingsLayout = view.findViewById(R.id.layout_settings_system);
        supportSettingsLayout = view.findViewById(R.id.layout_settings_support);
        updateLayout = view.findViewById(R.id.layout_update);

        myAddressSetting =
                new SettingsItemView.Builder(getContext())
                        .withIcon(R.drawable.ic_settings_wallet_address)
                        .withTitle(R.string.title_show_wallet_address)
                        .withListener(this::onShowWalletAddressSettingClicked)
                        .build();

        changeWalletSetting =
                new SettingsItemView.Builder(getContext())
                        .withIcon(R.drawable.ic_settings_change_wallet)
                        .withTitle(R.string.title_change_add_wallet)
                        .withListener(this::onChangeWalletSettingClicked)
                        .build();

        backUpWalletSetting =
                new SettingsItemView.Builder(getContext())
                        .withIcon(R.drawable.ic_settings_backup)
                        .withTitle(R.string.title_back_up_wallet)
                        .withListener(this::onBackUpWalletSettingClicked)
                        .build();

        showSeedPhrase = new SettingsItemView.Builder(getContext())
                .withIcon(R.drawable.ic_settings_show_seed)
                .withTitle(R.string.show_seed_phrase)
                .withListener(this::onShowSeedPhrase) //onShow
                .build();

        nameThisWallet = new SettingsItemView.Builder(getContext())
                .withIcon(R.drawable.ic_settings_name_this_wallet)
                .withTitle(R.string.name_this_wallet)
                .withListener(this::onNameThisWallet)
                .build();

        walletConnectSetting =
                new SettingsItemView.Builder(getContext())
                        .withIcon(R.drawable.ic_wallet_connect)
                        .withTitle(R.string.title_wallet_connect)
                        .withListener(this::onWalletConnectSettingClicked)
                        .build();

        notificationsSetting =
                new SettingsItemView.Builder(getContext())
                        .withType(SettingsItemView.Type.TOGGLE)
                        .withIcon(R.drawable.ic_settings_notifications)
                        .withTitle(R.string.title_notifications)
                        .withListener(this::onNotificationsSettingClicked)
                        .build();

//        biometricsSetting =
//                new SettingsItemView.Builder(getContext())
//                        .withType(SettingsItemView.Type.TOGGLE)
//                        .withIcon(R.drawable.ic_settings_biometrics)
//                        .withTitle(R.string.title_biometrics)
//                        .withListener(this::onBiometricsSettingClicked)
//                        .build();

        selectNetworksSetting =
                new SettingsItemView.Builder(getContext())
                        .withIcon(R.drawable.ic_settings_networks)
                        .withTitle(R.string.select_active_networks)
                        .withListener(this::onSelectNetworksSettingClicked)
                        .build();

        advancedSetting =
                new SettingsItemView.Builder(getContext())
                        .withIcon(R.drawable.ic_settings_advanced)
                        .withTitle(R.string.title_advanced)
                        .withListener(this::onAdvancedSettingClicked)
                        .build();

        supportSetting =
                new SettingsItemView.Builder(getContext())
                        .withIcon(R.drawable.ic_settings_support)
                        .withTitle(R.string.title_support)
                        .withListener(this::onSupportSettingClicked)
                        .build();
    }

    private void addSettingsToLayout() {
        int walletIndex = 0;
        int systemIndex = 0;
        int supportIndex = 0;

        walletSettingsLayout.addView(myAddressSetting, walletIndex++);

        if (CustomViewSettings.canChangeWallets())
            walletSettingsLayout.addView(changeWalletSetting, walletIndex++);

        walletSettingsLayout.addView(backUpWalletSetting, walletIndex++);

        walletSettingsLayout.addView(showSeedPhrase, walletIndex++);
        showSeedPhrase.setVisibility(View.GONE);

        walletSettingsLayout.addView(nameThisWallet, walletIndex++);

        walletSettingsLayout.addView(walletConnectSetting, walletIndex++);

        systemSettingsLayout.addView(notificationsSetting, systemIndex++);

        if (biometricsSetting != null) systemSettingsLayout.addView(biometricsSetting, systemIndex++);

        if (CustomViewSettings.getLockedChains().size() == 0)
            systemSettingsLayout.addView(selectNetworksSetting, systemIndex++);

        systemSettingsLayout.addView(advancedSetting, systemIndex++);

        supportSettingsLayout.addView(supportSetting, supportIndex++);
    }

    private void setInitialSettingsData(View view) {
        TextView appVersionText = view.findViewById(R.id.text_version);
        appVersionText.setText(String.format(Locale.getDefault(), "%s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
        TextView tokenScriptVersionText = view.findViewById(R.id.text_tokenscript_compatibility);
        tokenScriptVersionText.setText(TOKENSCRIPT_CURRENT_SCHEMA);

        notificationsSetting.setToggleState(viewModel.getNotificationState());
    }

    private void openShowSeedPhrase(Wallet wallet)
    {
        if (wallet.type != WalletType.HDKEY) return;

        Intent intent = new Intent(getContext(), ScammerWarningActivity.class);
        intent.putExtra(WALLET, wallet);
        startActivity(intent);
    }

    ActivityResultLauncher<Intent> handleBackupClick = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                String keyBackup = "";
                boolean noLockScreen = false;
                Intent data = result.getData();
                if (data != null) keyBackup = data.getStringExtra("Key");
                if (data != null) noLockScreen = data.getBooleanExtra("nolock", false);

                Bundle b = new Bundle();
                b.putBoolean(C.HANDLE_BACKUP, result.getResultCode() == RESULT_OK);
                b.putString("Key", keyBackup);
                b.putBoolean("nolock", noLockScreen);
                getParentFragmentManager().setFragmentResult(C.HANDLE_BACKUP, b);
            });

    private void openBackupActivity(Wallet wallet) {
        Intent intent = new Intent(getContext(), BackupFlowActivity.class);
        intent.putExtra(WALLET, wallet);

        switch (wallet.type)
        {
            case HDKEY:
                intent.putExtra("TYPE", BACKUP_HD_KEY);
                break;
            case KEYSTORE_LEGACY:
            case KEYSTORE:
                intent.putExtra("TYPE", BACKUP_KEYSTORE_KEY);
                break;
        }

        //override if this is an upgrade
        switch (wallet.authLevel)
        {
            case NOT_SET:
            case STRONGBOX_NO_AUTHENTICATION:
            case TEE_NO_AUTHENTICATION:
                if (wallet.lastBackupTime > 0)
                {
                    intent.putExtra("TYPE", BackupOperationType.UPGRADE_KEY);
                }
                break;
            default:
                break;
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        handleBackupClick.launch(intent);
    }

    private void onDefaultWallet(Wallet wallet) {
        this.wallet = wallet;
        if (wallet.address != null)
        {
            if (!wallet.ENSname.isEmpty())
            {
                changeWalletSetting.setSubtitle(wallet.ENSname + " | " + wallet.address);
            }
            else
            {
                changeWalletSetting.setSubtitle(wallet.address);
            }
        }

        switch (wallet.authLevel) {
            case NOT_SET:
            case STRONGBOX_NO_AUTHENTICATION:
            case TEE_NO_AUTHENTICATION:
                if (wallet.lastBackupTime > 0) {
                    backUpWalletSetting.setTitle(getString(R.string.action_upgrade_key));
                    backUpWalletSetting.setSubtitle(getString(R.string.not_locked));
                } else {
                    backUpWalletSetting.setTitle(getString(R.string.back_up_this_wallet));
                    backUpWalletSetting.setSubtitle(getString(R.string.back_up_now));
                }
                break;
            case TEE_AUTHENTICATION:
            case STRONGBOX_AUTHENTICATION:
                backUpWalletSetting.setTitle(getString(R.string.back_up_this_wallet));
                backUpWalletSetting.setSubtitle(getString(R.string.key_secure));
                break;
        }

        switch (wallet.type)
        {
            case NOT_DEFINED:
                break;
            case KEYSTORE:
                break;
            case HDKEY:
                showSeedPhrase.setVisibility(View.VISIBLE);
                break;
            case WATCH:
                backUpWalletSetting.setVisibility(View.GONE);
                break;
            case TEXT_MARKER:
                break;
            case KEYSTORE_LEGACY:
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel == null)
        {
            ((HomeActivity)getActivity()).resetFragment(WalletPage.SETTINGS);
        }
        else
        {
            viewModel.prepare();
        }
    }

    public void backupSeedSuccess(boolean hasNoLock) {
        if (viewModel != null) viewModel.TestWalletBackup();
        if (layoutBackup != null) layoutBackup.setVisibility(View.GONE);
        if (hasNoLock)
        {
            backUpWalletSetting.setSubtitle(getString(R.string.not_locked));
        }
    }

    private void backupWarning(String s) {
        if (s.equals(viewModel.defaultWallet().getValue().address)) {
            addBackupNotice(GenericWalletInteract.BackupLevel.WALLET_HAS_HIGH_VALUE);
        } else {
            if (layoutBackup != null) {
                layoutBackup.setVisibility(View.GONE);
            }
            //remove the number prompt
            if (getActivity() != null)
                ((HomeActivity) getActivity()).removeSettingsBadgeKey(C.KEY_NEEDS_BACKUP);
            onDefaultWallet(viewModel.defaultWallet().getValue());
        }
    }

    void addBackupNotice(GenericWalletInteract.BackupLevel walletValue) {
        layoutBackup.setVisibility(View.VISIBLE);
        warningSeparator.setVisibility(View.VISIBLE);
        if (wallet != null) {
            backupButton.setText(getString(R.string.back_up_wallet_action, wallet.address.substring(0, 5)));
            backupButton.setOnClickListener(v -> openBackupActivity(wallet));
            backupTitle.setText(getString(R.string.wallet_not_backed_up));
            layoutBackup.setBackgroundResource(R.drawable.background_warning_red_8dp);
            backupDetail.setText(getString(R.string.backup_wallet_detail));
            backupMenuButton.setOnClickListener(v -> {
                showPopup(backupPopupAnchor, wallet.address);
            });

            if (getActivity() != null) {
                ((HomeActivity) getActivity()).addSettingsBadgeKey(C.KEY_NEEDS_BACKUP);
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
            viewModel.setIsDismissed(walletAddress, true);
            backedUp(walletAddress);
            popupWindow.dismiss();
        });
        popupWindow.showAsDropDown(view, 0, 0);
    }

    private void backedUp(String walletAddress) {
        layoutBackup.setVisibility(View.GONE);
        warningSeparator.setVisibility(View.GONE);
        if (getActivity() != null)
            ((HomeActivity) getActivity()).postponeWalletBackupWarning(walletAddress);
    }

    private void onShowWalletAddressSettingClicked() {
        viewModel.showMyAddress(getContext());
    }

    private void onChangeWalletSettingClicked() {
        viewModel.showManageWallets(getContext(), false);
    }

    private void onBackUpWalletSettingClicked() {
        Wallet wallet = viewModel.defaultWallet().getValue();
        if (wallet != null) {
            openBackupActivity(wallet);
        }
    }

    private void onShowSeedPhrase()
    {
        Wallet wallet = viewModel.defaultWallet().getValue();
        if (wallet != null) {
            openShowSeedPhrase(wallet);
        }
    }

    private void onNameThisWallet()
    {
        Intent intent = new Intent(getActivity(), NameThisWalletActivity.class);
        requireActivity().startActivity(intent);
    }

    private void onNotificationsSettingClicked() {
        viewModel.setNotificationState(notificationsSetting.getToggleState());
    }

    private void onBiometricsSettingClicked() {
        // TODO: Implementation
    }

    ActivityResultLauncher<Intent> networkSettingsHandler = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                //send instruction to restart tokenService
                getParentFragmentManager().setFragmentResult(RESET_TOKEN_SERVICE, new Bundle());
            });

    private void onSelectNetworksSettingClicked() {
        Intent intent = new Intent(getActivity(), SelectNetworkFilterActivity.class);
        intent.putExtra(C.EXTRA_SINGLE_ITEM, false);
        networkSettingsHandler.launch(intent);
    }

    ActivityResultLauncher<Intent> advancedSettingsHandler = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (data == null) return;
                if (data.getBooleanExtra(RESET_WALLET, false))
                {
                    getParentFragmentManager().setFragmentResult(RESET_WALLET, new Bundle());
                }
                else if (data.getBooleanExtra(CHANGE_CURRENCY, false))
                {
                    getParentFragmentManager().setFragmentResult(CHANGE_CURRENCY, new Bundle());
                }
                else if (data.getBooleanExtra(CHANGED_LOCALE, false))
                {
                    getParentFragmentManager().setFragmentResult(CHANGED_LOCALE, new Bundle());
                }
            });

    private void onAdvancedSettingClicked() {
        Intent intent = new Intent(getActivity(), AdvancedSettingsActivity.class);
        advancedSettingsHandler.launch(intent);
    }

    private void onSupportSettingClicked() {
        Intent intent = new Intent(getActivity(), SupportSettingsActivity.class);
        startActivity(intent);
    }

    private void onWalletConnectSettingClicked() {
        Intent intent = new Intent(getActivity(), WalletConnectSessionActivity.class);
        intent.putExtra("wallet", wallet);
        startActivity(intent);
    }

    private void checkPendingUpdate(View view)
    {
        if (updateLayout == null || view == null) return;

        if (pendingUpdate > 0)
        {
            updateLayout.setVisibility(View.VISIBLE);
            TextView current = view.findViewById(R.id.text_detail_current);
            TextView available = view.findViewById(R.id.text_detail_available);
            current.setText(getString(R.string.installed_version, String.valueOf(BuildConfig.VERSION_CODE)));
            available.setText(getString(R.string.available_version, String.valueOf(pendingUpdate)));
            if (getActivity() != null) {
                ((HomeActivity) getActivity()).addSettingsBadgeKey(C.KEY_UPDATE_AVAILABLE);
            }

            updateLayout.setOnClickListener(v -> {
                UpdateUtils.pushUpdateDialog(getActivity());
                updateLayout.setVisibility(View.GONE);
                pendingUpdate = 0;
                if (getActivity() != null) {
                    ((HomeActivity) getActivity()).removeSettingsBadgeKey(C.KEY_UPDATE_AVAILABLE);
                }
            });
        }
        else
        {
            updateLayout.setVisibility(View.GONE);
        }
    }
}
