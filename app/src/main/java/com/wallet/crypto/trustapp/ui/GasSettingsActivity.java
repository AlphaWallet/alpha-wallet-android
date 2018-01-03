package com.wallet.crypto.trustapp.ui;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.viewmodel.GasSettingsViewModel;
import com.wallet.crypto.trustapp.viewmodel.GasSettingsViewModelFactory;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class GasSettingsActivity extends BaseActivity {

    @Inject
    GasSettingsViewModelFactory viewModelFactory;
    GasSettingsViewModel viewModel;

    private SeekBar gasPriceSlider;
    private SeekBar gasLimitSlider;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gas_settings);
        toolbar();

        gasPriceSlider = findViewById(R.id.gas_price_slider);
        gasLimitSlider = findViewById(R.id.gas_limit_slider);

        gasPriceSlider.setPadding(0, 0, 0, 0);
        gasLimitSlider.setPadding(0, 0, 0, 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.send_settings_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save: {
                viewModel.saveSettings();
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }
}
