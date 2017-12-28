package com.wallet.crypto.trustapp.views;

import android.os.Bundle;

import com.github.omadahealth.lollipin.lib.PinCompatActivity;
import com.wallet.crypto.trustapp.R;

public class SettingsActivity extends PinCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
