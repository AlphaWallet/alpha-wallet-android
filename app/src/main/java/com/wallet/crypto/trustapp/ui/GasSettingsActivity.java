package com.wallet.crypto.trustapp.ui;


import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.TextView;

import com.wallet.crypto.trustapp.C;
import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.util.BalanceUtils;
import com.wallet.crypto.trustapp.viewmodel.GasSettingsViewModel;
import com.wallet.crypto.trustapp.viewmodel.GasSettingsViewModelFactory;

import java.math.BigInteger;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class GasSettingsActivity extends BaseActivity {

    @Inject
    GasSettingsViewModelFactory viewModelFactory;
    GasSettingsViewModel viewModel;

    private SeekBar gasPriceSlider;
    private SeekBar gasLimitSlider;
    private TextView gasPriceText;
    private TextView gasLimitText;
    private TextView networkFeeText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gas_settings);
        toolbar();

        gasPriceSlider = findViewById(R.id.gas_price_slider);
        gasLimitSlider = findViewById(R.id.gas_limit_slider);
        gasPriceText = findViewById(R.id.gas_price_text);
        gasLimitText = findViewById(R.id.gas_limit_text);
        networkFeeText = findViewById(R.id.text_network_fee);

        gasPriceSlider.setPadding(0, 0, 0, 0);
        gasLimitSlider.setPadding(0, 0, 0, 0);

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(GasSettingsViewModel.class);

        BigInteger gasPrice = new BigInteger(getIntent().getStringExtra(C.EXTRA_GAS_PRICE));
        BigInteger gasLimit = new BigInteger(getIntent().getStringExtra(C.EXTRA_GAS_LIMIT));

        Log.d("GAS", gasPrice.toString());
        Log.d("GAS", gasLimit.toString());

        viewModel.gasPrice().observe(this, this::onGasPrice);
        viewModel.gasLimit().observe(this, this::onGasLimit);

        viewModel.gasPrice().setValue(gasPrice);
        viewModel.gasLimit().setValue(gasLimit);
    }

    private void onGasPrice(BigInteger price) {
        String priceStr = BalanceUtils.weiToGwei(price) + " " + C.GWEI_UNIT;
        gasPriceText.setText(priceStr);

        updateNetworkFee();
    }

    private void onGasLimit(BigInteger limit) {
        gasLimitText.setText(limit.toString());

        updateNetworkFee();
    }

    private void updateNetworkFee() {
        String fee = BalanceUtils.weiToEth(viewModel.networkFee()).toPlainString() + " " + C.ETH_SYMBOL;
        networkFeeText.setText(fee);
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
