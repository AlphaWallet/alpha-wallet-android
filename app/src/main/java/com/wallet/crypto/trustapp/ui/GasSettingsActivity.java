package com.wallet.crypto.trustapp.ui;


import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.TextView;

import com.wallet.crypto.trustapp.C;
import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.entity.NetworkInfo;
import com.wallet.crypto.trustapp.util.BalanceUtils;
import com.wallet.crypto.trustapp.viewmodel.GasSettingsViewModel;
import com.wallet.crypto.trustapp.viewmodel.GasSettingsViewModelFactory;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class GasSettingsActivity extends BaseActivity {

    @Inject
    GasSettingsViewModelFactory viewModelFactory;
    GasSettingsViewModel viewModel;

    private TextView gasPriceText;
    private TextView gasLimitText;
    private TextView networkFeeText;
    private TextView gasPriceInfoText;
    private TextView gasLimitInfoText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gas_settings);
        toolbar();

        SeekBar gasPriceSlider = findViewById(R.id.gas_price_slider);
        SeekBar gasLimitSlider = findViewById(R.id.gas_limit_slider);
        gasPriceText = findViewById(R.id.gas_price_text);
        gasLimitText = findViewById(R.id.gas_limit_text);
        networkFeeText = findViewById(R.id.text_network_fee);
        gasPriceInfoText = findViewById(R.id.gas_price_info_text);
        gasLimitInfoText = findViewById(R.id.gas_limit_info_text);

        gasPriceSlider.setPadding(0, 0, 0, 0);
        gasLimitSlider.setPadding(0, 0, 0, 0);

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(GasSettingsViewModel.class);

        BigInteger gasPrice = new BigInteger(getIntent().getStringExtra(C.EXTRA_GAS_PRICE));
        BigInteger gasLimit = new BigInteger(getIntent().getStringExtra(C.EXTRA_GAS_LIMIT));
        BigInteger gasLimitMin = BigInteger.valueOf(C.GAS_LIMIT_MIN);
        BigInteger gasLimitMax = BigInteger.valueOf(C.GAS_LIMIT_MAX);
        BigInteger gasPriceMin = BigInteger.valueOf(C.GAS_PRICE_MIN);
        BigInteger networkFeeMax = BigInteger.valueOf(C.NETWORK_FEE_MAX);

        final int gasPriceMinGwei = BalanceUtils.weiToGweiBI(gasPriceMin).intValue();
        gasPriceSlider.setMax(BalanceUtils
                .weiToGweiBI(networkFeeMax.divide(gasLimitMax))
                .subtract(BigDecimal.valueOf(gasPriceMinGwei))
                .intValue());
        int gasPriceProgress = BalanceUtils
                .weiToGweiBI(gasPrice)
                .subtract(BigDecimal.valueOf(gasPriceMinGwei))
                .intValue();
        gasPriceSlider.setProgress(gasPriceProgress);
        gasPriceSlider.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        viewModel.gasPrice().setValue(BalanceUtils.gweiToWei(BigDecimal.valueOf(progress + gasPriceMinGwei)));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });

        gasLimitSlider.setMax(gasLimitMax.subtract(gasLimitMin).intValue());
        gasLimitSlider.setProgress(gasLimit.subtract(gasLimitMin).intValue());
        gasLimitSlider.refreshDrawableState();
        gasLimitSlider.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        progress = progress / 100;
                        progress = progress * 100;
                        viewModel.gasLimit().setValue(BigInteger.valueOf(progress).add(gasLimitMin));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });


        viewModel.gasPrice().observe(this, this::onGasPrice);
        viewModel.gasLimit().observe(this, this::onGasLimit);
        viewModel.defaultNetwork().observe(this, this::onDefaultNetwork);

        viewModel.gasPrice().setValue(gasPrice);
        viewModel.gasLimit().setValue(gasLimit);
    }

    @Override
    public void onResume() {

        super.onResume();

        viewModel.prepare();
    }

    private void onDefaultNetwork(NetworkInfo network) {
        gasPriceInfoText.setText(getString(R.string.info_gas_price).replace(C.ETHEREUM_NETWORK_NAME, network.name));
        gasLimitInfoText.setText(getString(R.string.info_gas_limit).replace(C.ETHEREUM_NETWORK_NAME, network.symbol));
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
                Intent intent = new Intent();
                intent.putExtra(C.EXTRA_GAS_PRICE, viewModel.gasPrice().getValue().toString());
                intent.putExtra(C.EXTRA_GAS_LIMIT, viewModel.gasLimit().getValue().toString());
                setResult(RESULT_OK, intent);
                finish();
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }
}
