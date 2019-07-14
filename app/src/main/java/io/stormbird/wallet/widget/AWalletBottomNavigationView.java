package io.stormbird.wallet.widget;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.stormbird.wallet.R;

public class AWalletBottomNavigationView extends LinearLayout {
    public static final int WALLET = 0;
    public static final int TRANSACTIONS = 1;
    public static final int DAPP_BROWSER = 2;
    public static final int SETTINGS = 3;
    public static final int MARKETPLACE = 4;

    private final ImageView transactions;
    private final ImageView dappBrowser;
    private final ImageView wallet;
    private final ImageView settings;

    private final TextView transactionsLabel;
    private final TextView dappBrowserLabel;
    private final TextView walletLabel;
    private final TextView settingsLabel;

    private OnBottomNavigationItemSelectedListener listener;

    private int selectedItem;

    public AWalletBottomNavigationView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.layout_bottom_navigation, this);
        transactions = findViewById(R.id.nav_transactions);
        dappBrowser = findViewById(R.id.nav_browser);
        wallet = findViewById(R.id.nav_wallet);
        settings = findViewById(R.id.nav_settings);

        transactionsLabel = findViewById(R.id.nav_transactions_text);
        dappBrowserLabel = findViewById(R.id.nav_browser_text);
        walletLabel = findViewById(R.id.nav_wallet_text);
        settingsLabel = findViewById(R.id.nav_settings_text);

        transactions.setOnClickListener(v -> selectItem(TRANSACTIONS));
        dappBrowser.setOnClickListener(v -> selectItem(DAPP_BROWSER));
        wallet.setOnClickListener(v -> selectItem(WALLET));
        settings.setOnClickListener(v -> selectItem(SETTINGS));

        transactionsLabel.setOnClickListener(v -> selectItem(TRANSACTIONS));
        dappBrowserLabel.setOnClickListener(v -> selectItem(DAPP_BROWSER));
        walletLabel.setOnClickListener(v -> selectItem(WALLET));
        settingsLabel.setOnClickListener(v -> selectItem(SETTINGS));
    }

    public void setListener(OnBottomNavigationItemSelectedListener listener) {
        this.listener = listener;
    }

    private void selectItem(int index) {
        listener.onBottomNavigationItemSelected(index);
    }

    public void setSelectedItem(int index) {
        deselectAll();
        selectedItem = index;
        switch (index) {
            case TRANSACTIONS:
                transactions.setImageResource(R.drawable.ic_transactions_active);
                transactionsLabel.setTextColor(Color.parseColor("#1899c2"));
                break;
            case DAPP_BROWSER:
                dappBrowser.setImageResource(R.drawable.ic_browser_active);
                dappBrowserLabel.setTextColor(Color.parseColor("#1899c2"));
                break;
            case WALLET:
                wallet.setImageResource(R.drawable.ic_wallet_active);
                walletLabel.setTextColor(Color.parseColor("#1899c2"));
                break;
            case SETTINGS:
                settings.setImageResource(R.drawable.ic_settings_active);
                settingsLabel.setTextColor(Color.parseColor("#1899c2"));
                break;
        }
    }

    public int getSelectedItem() {
        return selectedItem;
    }

    private void deselectAll() {
        transactions.setImageResource(R.drawable.ic_transactions);
        dappBrowser.setImageResource(R.drawable.ic_browser);
        wallet.setImageResource(R.drawable.ic_wallet);
        settings.setImageResource(R.drawable.ic_settings);
        //reset text colour
        transactionsLabel.setTextColor(Color.parseColor("#8a000000"));
        dappBrowserLabel.setTextColor(Color.parseColor("#8a000000"));
        walletLabel.setTextColor(Color.parseColor("#8a000000"));
        settingsLabel.setTextColor(Color.parseColor("#8a000000"));
    }

    public interface OnBottomNavigationItemSelectedListener {
        boolean onBottomNavigationItemSelected(int index);
    }
}
