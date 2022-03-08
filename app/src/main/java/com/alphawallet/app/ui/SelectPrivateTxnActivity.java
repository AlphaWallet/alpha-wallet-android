package com.alphawallet.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.PrivateEthTxnNetwork;
import com.alphawallet.app.widget.SelectableItem;

import timber.log.Timber;

public class SelectPrivateTxnActivity extends BaseActivity {

    private SelectableItem eden;
    private SelectableItem ethermine;
    private SelectableItem currentlySelected = null;

    private SelectableItem[] networks;

    private PrivateEthTxnNetwork privateEthTxnNetwork = PrivateEthTxnNetwork.NONE;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_private_eth_txn);

        toolbar();
        setTitle(getString(R.string.private_eth_txn));
        enableDisplayHomeAsUp();
        setupViews();
    }

    private void setupViews() {
        eden = findViewById(R.id.eden);
        ethermine = findViewById(R.id.ethermine);
        networks = new SelectableItem[] { eden, ethermine };

        for (SelectableItem network : networks) {
            network.setSelected(false);
        }

        eden.setup(R.drawable.ic_settings_eden, R.string.eden, false);
        ethermine.setup(R.drawable.ic_settings_ethermine, R.string.ethermine, false);

        View.OnClickListener listener = v -> {
            if (v instanceof SelectableItem) {
                if (!v.isSelected()) {
                    v.setSelected(true);
                    if (currentlySelected != null) {
                        currentlySelected.setSelected(false);
                    }
                    currentlySelected = (SelectableItem) v;
                } else {
                    v.setSelected(false);
                    currentlySelected = null;
                }
            }
        };

        for (SelectableItem r : networks) {
            r.setOnClickListener(listener);
        }

        PrivateEthTxnNetwork selectedNetwork = (PrivateEthTxnNetwork) getIntent().getSerializableExtra(C.EXTRA_PRIVATE_ETH_NETWORK);
        Timber.d("Selected Network: %s", selectedNetwork != null ? selectedNetwork.name() : "null");
        if (selectedNetwork != null && selectedNetwork.ordinal() > 0) {     // Not 'NONE'
            networks[selectedNetwork.ordinal()-1].performClick();        // in enum, networks start from 1, while in array of views it starts from 0
        }
    }

    private void setCurrentSelectedNetwork() {
        String text = currentlySelected != null ? currentlySelected.getText() : "";

        if (text.equals(getString(R.string.eden))) {
            privateEthTxnNetwork = PrivateEthTxnNetwork.EDEN;
        } else if (text.equals(getString(R.string.ethermine))) {
            privateEthTxnNetwork = PrivateEthTxnNetwork.ETHERMINE;
        } else {
            privateEthTxnNetwork = PrivateEthTxnNetwork.NONE;
        }

        Intent i = new Intent();
        i.putExtra(C.EXTRA_PRIVATE_ETH_NETWORK, privateEthTxnNetwork);
        setResult(RESULT_OK, i);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Timber.d("onOptionsItemSelected: %s", item.getItemId());
        if (item.getItemId() == android.R.id.home) {
            setCurrentSelectedNetwork();
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        setCurrentSelectedNetwork();
    }
}