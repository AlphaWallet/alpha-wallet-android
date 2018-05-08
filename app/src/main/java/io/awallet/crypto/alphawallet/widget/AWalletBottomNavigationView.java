package io.awallet.crypto.alphawallet.widget;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;

import io.awallet.crypto.alphawallet.R;

public class AWalletBottomNavigationView extends LinearLayout {
    public static final int MARKETPLACE = 0;
    public static final int WALLET = 1;
    public static final int TRANSACTIONS = 2;
    public static final int SETTINGS = 3;

    private ImageView transactions;
    private ImageView marketplace;
    private ImageView wallet;
    private ImageView settings;

    private OnBottomNavigationItemSelectedListener listener;

    public AWalletBottomNavigationView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.layout_bottom_navigation, this);
        transactions = findViewById(R.id.nav_transactions);
        marketplace = findViewById(R.id.nav_marketplace);
        wallet = findViewById(R.id.nav_wallet);
        settings = findViewById(R.id.nav_settings);

        transactions.setOnClickListener(v -> selectItem(TRANSACTIONS));
        marketplace.setOnClickListener(v -> selectItem(MARKETPLACE));
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
        switch (index) {
            case TRANSACTIONS:
                transactions.setImageResource(R.drawable.ic_transactions_active);
                break;
            case MARKETPLACE:
                marketplace.setImageResource(R.drawable.ic_market_active);
                break;
            case WALLET:
                wallet.setImageResource(R.drawable.ic_wallet_active);
                break;
            case SETTINGS:
                settings.setImageResource(R.drawable.ic_settings_active);
                break;
        }
    }

    private void deselectAll() {
        transactions.setImageResource(R.drawable.ic_transactions);
        marketplace.setImageResource(R.drawable.ic_market);
        wallet.setImageResource(R.drawable.ic_wallet);
        settings.setImageResource(R.drawable.ic_settings);
    }

    public interface OnBottomNavigationItemSelectedListener {
        boolean onBottomNavigationItemSelected(int index);
    }
}
