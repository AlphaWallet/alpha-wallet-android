package com.wallet.crypto.trust.views;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.wallet.crypto.trust.R;
import com.wallet.crypto.trust.controller.Controller;
import com.wallet.crypto.trust.model.VMNetwork;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    final static String TAG = "SETTINGS";
    Spinner mNetworkSpinner;
    Button mExportButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        final Controller mController = Controller.get();

        List<VMNetwork> networks = mController.getNetworks();

        final List<String> network_names = new ArrayList<String>();

        int currentNetworkIdx = 0;
        for (int ii = 0; ii < networks.size(); ii++) {
            VMNetwork n = networks.get(ii);
            if (n.getName().equals(mController.getCurrentNetwork().getName())) {
                currentNetworkIdx = ii;
            }
            network_names.add(n.getName());
        }

        mNetworkSpinner = (Spinner) findViewById(R.id.network_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, network_names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mNetworkSpinner.setAdapter(adapter);
        mNetworkSpinner.setSelection(currentNetworkIdx);
        mNetworkSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                mController.setCurrentNetwork(network_names.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });

        mExportButton = findViewById(R.id.export_button);
        mExportButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "Export " + mController.getCurrentAccount().getAddress());
                mController.navigateToExportAccount(SettingsActivity.this, mController.getCurrentAccount().getAddress());
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
