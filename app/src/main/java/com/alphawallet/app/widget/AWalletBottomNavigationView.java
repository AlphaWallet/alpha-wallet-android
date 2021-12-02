package com.alphawallet.app.widget;

import android.content.Context;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.WalletPage;

import java.util.ArrayList;

import static com.alphawallet.app.entity.WalletPage.ACTIVITY;
import static com.alphawallet.app.entity.WalletPage.DAPP_BROWSER;
import static com.alphawallet.app.entity.WalletPage.SETTINGS;
import static com.alphawallet.app.entity.WalletPage.WALLET;

public class AWalletBottomNavigationView extends LinearLayout {

    private final ImageView dappBrowser;
    private final ImageView wallet;
    private final ImageView settings;
    private final ImageView activity;

    private final TextView dappBrowserLabel;
    private final TextView walletLabel;
    private final TextView settingsLabel;
    private final TextView settingsBadge;
    private final TextView activityLabel;

    private OnBottomNavigationItemSelectedListener listener;

    private WalletPage selectedItem;

    private final ArrayList<String> settingsBadgeKeys = new ArrayList<>();

    public AWalletBottomNavigationView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.layout_bottom_navigation, this);
        dappBrowser = findViewById(R.id.nav_browser);
        wallet = findViewById(R.id.nav_wallet);
        settings = findViewById(R.id.nav_settings);
        activity = findViewById(R.id.nav_activity);

        dappBrowserLabel = findViewById(R.id.nav_browser_text);
        walletLabel = findViewById(R.id.nav_wallet_text);
        settingsLabel = findViewById(R.id.nav_settings_text);
        settingsBadge = findViewById(R.id.settings_badge);
        activityLabel = findViewById(R.id.nav_activity_text);

        //TODO: Refactor with click overlay
        findViewById(R.id.wallet_tab).setOnClickListener(v -> selectItem(WALLET));
        findViewById(R.id.browser_tab).setOnClickListener(v -> selectItem(DAPP_BROWSER));
        findViewById(R.id.settings_tab).setOnClickListener(v -> selectItem(SETTINGS));
        findViewById(R.id.activity_tab).setOnClickListener(v -> selectItem(ACTIVITY));

        dappBrowser.setOnClickListener(v -> selectItem(DAPP_BROWSER));
        wallet.setOnClickListener(v -> selectItem(WALLET));
        settings.setOnClickListener(v -> selectItem(SETTINGS));
        activity.setOnClickListener(v -> selectItem(ACTIVITY));

        dappBrowserLabel.setOnClickListener(v -> selectItem(DAPP_BROWSER));
        walletLabel.setOnClickListener(v -> selectItem(WALLET));
        settingsLabel.setOnClickListener(v -> selectItem(SETTINGS));
        activityLabel.setOnClickListener(v -> selectItem(ACTIVITY));

        // set wallet fragment selected on start
        setSelectedItem(WALLET);
    }

    public void setListener(OnBottomNavigationItemSelectedListener listener) {
        this.listener = listener;
    }

    private void selectItem(WalletPage index) {
        listener.onBottomNavigationItemSelected(index);
    }

    public void setSelectedItem(WalletPage index) {
        deselectAll();
        selectedItem = index;
        switch (index) {
            case DAPP_BROWSER:
                dappBrowser.setImageResource(R.drawable.ic_tab_browser_active);
                dappBrowserLabel.setTextColor(getResources().getColor(R.color.colorHighlight, getContext().getTheme()));
                break;
            case WALLET:
                wallet.setImageResource(R.drawable.ic_tab_wallet_active);
                walletLabel.setTextColor(getResources().getColor(R.color.colorHighlight, getContext().getTheme()));
                break;
            case SETTINGS:
                settings.setImageResource(R.drawable.ic_tab_settings_active);
                settingsLabel.setTextColor(getResources().getColor(R.color.colorHighlight, getContext().getTheme()));
                break;
            case ACTIVITY:
                activity.setImageResource(R.drawable.ic_tab_activity_active);
                activityLabel.setTextColor(getResources().getColor(R.color.colorHighlight, getContext().getTheme()));
                break;
        }
    }

    public WalletPage getSelectedItem() {
        return selectedItem;
    }

    private void deselectAll() {
        dappBrowser.setImageResource(R.drawable.ic_tab_browser);
        wallet.setImageResource(R.drawable.ic_tab_wallet);
        settings.setImageResource(R.drawable.ic_tab_settings);
        activity.setImageResource(R.drawable.ic_tab_activity);
        //reset text colour
        dappBrowserLabel.setTextColor(getContext().getColor(R.color.dove));
        walletLabel.setTextColor(getContext().getColor(R.color.dove));
        settingsLabel.setTextColor(getContext().getColor(R.color.dove));
        activityLabel.setTextColor(getContext().getColor(R.color.dove));
    }

    public interface OnBottomNavigationItemSelectedListener {
        boolean onBottomNavigationItemSelected(WalletPage index);
    }

    public void setSettingsBadgeCount(int count) {
        if (count > 0) {
            settingsBadge.setVisibility(View.VISIBLE);
        } else {
            settingsBadge.setVisibility(View.GONE);
        }
        settingsBadge.setText(String.valueOf(count));
    }

    public void addSettingsBadgeKey(String key) {
        if (!settingsBadgeKeys.contains(key)) {
            settingsBadgeKeys.add(key);
        }
        showOrHideSettingsBadge();
    }

    public void removeSettingsBadgeKey(String key) {
        settingsBadgeKeys.remove(key);
        showOrHideSettingsBadge();
    }

    private void showOrHideSettingsBadge() {
        if (settingsBadgeKeys.size() > 0) {
            settingsBadge.setVisibility(View.VISIBLE);
        } else {
            settingsBadge.setVisibility(View.GONE);
        }
        settingsBadge.setText(String.valueOf(settingsBadgeKeys.size()));
    }

    public void hideBrowserTab() {
        LinearLayout browserTab = findViewById(R.id.browser_tab);
        if (browserTab != null) browserTab.setVisibility(View.GONE);
    }
}
