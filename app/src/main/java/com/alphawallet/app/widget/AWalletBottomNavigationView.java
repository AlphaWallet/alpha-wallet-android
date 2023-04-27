package com.alphawallet.app.widget;

import static com.alphawallet.app.entity.WalletPage.ACTIVITY;
import static com.alphawallet.app.entity.WalletPage.DAPP_BROWSER;
import static com.alphawallet.app.entity.WalletPage.SETTINGS;
import static com.alphawallet.app.entity.WalletPage.WALLET;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.WalletPage;

import java.util.ArrayList;

public class AWalletBottomNavigationView extends LinearLayout
{
    private final TextView dappBrowserLabel;
    private final TextView walletLabel;
    private final TextView settingsBadge;
    private final TextView settingsLabel;

    private final TextView sendButton;
    private final RelativeLayout settingsTab;
    private final TextView activityLabel;
    private final ArrayList<String> settingsBadgeKeys = new ArrayList<>();
    private OnBottomNavigationItemSelectedListener listener;
    private WalletPage selectedItem;

    public AWalletBottomNavigationView(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.layout_bottom_navigation, this);
        walletLabel = findViewById(R.id.nav_wallet_text);
        activityLabel = findViewById(R.id.nav_activity_text);
        sendButton = findViewById(R.id.nav_send);
        dappBrowserLabel = findViewById(R.id.nav_browser_text);
        settingsTab = findViewById(R.id.settings_tab);
        settingsLabel = findViewById(R.id.nav_settings_text);
        settingsBadge = findViewById(R.id.settings_badge);

        walletLabel.setOnClickListener(v -> selectItem(WALLET));
        activityLabel.setOnClickListener(v -> selectItem(ACTIVITY));
        dappBrowserLabel.setOnClickListener(v -> selectItem(DAPP_BROWSER));
        settingsTab.setOnClickListener(v -> selectItem(SETTINGS));

        // set wallet fragment selected on start
        setSelectedItem(WALLET);
    }

    public void setSendButtonListener(View.OnClickListener listener)
    {
        sendButton.setOnClickListener(listener);
    }

    public void setListener(OnBottomNavigationItemSelectedListener listener)
    {
        this.listener = listener;
    }

    private void selectItem(WalletPage index)
    {
        listener.onBottomNavigationItemSelected(index);
    }

    public WalletPage getSelectedItem()
    {
        return selectedItem;
    }

    public void setSelectedItem(WalletPage index)
    {
        deselectAll();
        selectedItem = index;
        switch (index)
        {
            case DAPP_BROWSER:
                dappBrowserLabel.setSelected(true);
                break;
            case WALLET:
                walletLabel.setSelected(true);
                break;
            case SETTINGS:
                settingsLabel.setSelected(true);
                break;
            case ACTIVITY:
                activityLabel.setSelected(true);
                break;
        }
    }

    private void deselectAll()
    {
        dappBrowserLabel.setSelected(false);
        walletLabel.setSelected(false);
        settingsLabel.setSelected(false);
        activityLabel.setSelected(false);
    }

    public void setSettingsBadgeCount(int count)
    {
        if (count > 0)
        {
            settingsBadge.setVisibility(View.VISIBLE);
        }
        else
        {
            settingsBadge.setVisibility(View.GONE);
        }
        settingsBadge.setText(String.valueOf(count));
    }

    public void addSettingsBadgeKey(String key)
    {
        if (!settingsBadgeKeys.contains(key))
        {
            settingsBadgeKeys.add(key);
        }
        showOrHideSettingsBadge();
    }

    public void removeSettingsBadgeKey(String key)
    {
        settingsBadgeKeys.remove(key);
        showOrHideSettingsBadge();
    }

    private void showOrHideSettingsBadge()
    {
        if (settingsBadgeKeys.size() > 0)
        {
            settingsBadge.setVisibility(View.VISIBLE);
        }
        else
        {
            settingsBadge.setVisibility(View.GONE);
        }
        settingsBadge.setText(String.valueOf(settingsBadgeKeys.size()));
    }

    public void hideBrowserTab()
    {
        if (dappBrowserLabel != null) dappBrowserLabel.setVisibility(View.GONE);
    }

    public interface OnBottomNavigationItemSelectedListener
    {
        boolean onBottomNavigationItemSelected(WalletPage index);
    }
}
