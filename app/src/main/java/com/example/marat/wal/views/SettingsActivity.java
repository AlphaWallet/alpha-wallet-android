package com.example.marat.wal.views;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.example.marat.wal.R;
import com.example.marat.wal.controller.Controller;
import com.example.marat.wal.model.VMNetwork;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    Spinner mNetworkSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

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
    }

}
