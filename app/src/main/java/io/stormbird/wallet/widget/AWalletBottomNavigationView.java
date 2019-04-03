package io.stormbird.wallet.widget;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;

import io.stormbird.wallet.R;

public class AWalletBottomNavigationView extends LinearLayout {
    public static final int WALLET = 0;
    public static final int TRANSACTIONS = 1;
    public static final int DAPP_BROWSER = 2;
    public static final int SETTINGS = 3;
    public static final int MARKETPLACE = 4;

    private ImageView transactions;
    private ImageView dappBrowser;
    private ImageView wallet;
    private ImageView settings;

    private OnBottomNavigationItemSelectedListener listener;

    private int selectedItem;

    public AWalletBottomNavigationView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.layout_bottom_navigation, this);
        transactions = findViewById(R.id.nav_transactions);
        dappBrowser = findViewById(R.id.nav_browser);
        wallet = findViewById(R.id.nav_wallet);
        settings = findViewById(R.id.nav_settings);

        transactions.setOnClickListener(v -> selectItem(TRANSACTIONS));
        dappBrowser.setOnClickListener(v -> selectItem(DAPP_BROWSER));
        wallet.setOnClickListener(v -> selectItem(WALLET));
        settings.setOnClickListener(v -> selectItem(SETTINGS));
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
                break;
            case DAPP_BROWSER:
                dappBrowser.setImageResource(R.drawable.ic_browser_active);
                break;
            case WALLET:
                wallet.setImageResource(R.drawable.ic_wallet_active);
                break;
            case SETTINGS:
                settings.setImageResource(R.drawable.ic_settings_active);
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
    }

    public interface OnBottomNavigationItemSelectedListener {
        boolean onBottomNavigationItemSelected(int index);
    }
}
