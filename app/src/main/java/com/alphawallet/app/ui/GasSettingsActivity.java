package com.alphawallet.app.ui;


import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatSeekBar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.util.BalanceUtils;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.viewmodel.GasSettingsViewModel;
import com.alphawallet.app.viewmodel.GasSettingsViewModelFactory;

public class GasSettingsActivity extends BaseActivity {

    @Inject
    GasSettingsViewModelFactory viewModelFactory;
    GasSettingsViewModel viewModel;

    private TextView gasPriceText;
    private TextView gasLimitText;
    private TextView networkFeeText;
    private TextView gasPriceInfoText;
    private TextView gasLimitInfoText;
    private Button saveButton;

    private BigInteger granularity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gas_settings);
        toolbar();
        setTitle("");

        AppCompatSeekBar gasPriceSlider = findViewById(R.id.gas_price_slider);
        AppCompatSeekBar gasLimitSlider = findViewById(R.id.gas_limit_slider);
        gasPriceText = findViewById(R.id.gas_price_text);
        gasLimitText = findViewById(R.id.gas_limit_text);
        networkFeeText = findViewById(R.id.text_network_fee);
        gasPriceInfoText = findViewById(R.id.gas_price_info_text);
        gasLimitInfoText = findViewById(R.id.gas_limit_info_text);
        saveButton = findViewById(R.id.button_save);

        saveButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putExtra(C.EXTRA_GAS_PRICE, viewModel.gasPrice().getValue().toString());
            intent.putExtra(C.EXTRA_GAS_LIMIT, viewModel.gasLimit().getValue().toString());
            setResult(RESULT_OK, intent);
            finish();
        });

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(GasSettingsViewModel.class);

        BigInteger gasPrice = new BigInteger(getIntent().getStringExtra(C.EXTRA_GAS_PRICE));
        BigInteger gasLimit = new BigInteger(getIntent().getStringExtra(C.EXTRA_GAS_LIMIT));
        int chainId = getIntent().getIntExtra(C.EXTRA_NETWORKID, EthereumNetworkRepository.MAINNET_ID);
        BigInteger gasLimitMin = BigInteger.valueOf(C.GAS_LIMIT_MIN);
        BigInteger gasLimitMax = BigInteger.valueOf(C.GAS_LIMIT_MAX);
        BigInteger gasPriceMin = BigInteger.valueOf(C.GAS_PRICE_MIN);
        BigInteger networkFeeMax = BigInteger.valueOf(C.NETWORK_FEE_MAX);
        BigDecimal gasPriceMaxGwei;

        // The math here is to keep the gas intervals clean
        switch (chainId)
        {
            case EthereumNetworkRepository.XDAI_ID:
                granularity = BalanceUtils.gweiToWei(BigDecimal.ONE.divide(BigDecimal.valueOf(100)));
                gasPriceSlider.setMax(100);
                break;
            default:
                gasPriceMaxGwei = BalanceUtils
                        .weiToGweiBI(networkFeeMax.divide(gasLimitMax));
                granularity = BalanceUtils.gweiToWei(gasPriceMaxGwei.divide(BigDecimal.valueOf(100)));
                gasPriceSlider.setMax(99);
                break;
        }

        gasPriceSlider.setProgress(((gasPrice.subtract(gasPriceMin)).divide(granularity)).intValue());
        gasPriceSlider.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
                    {
                        BigInteger scaledProgress = BigInteger.valueOf(progress).multiply(granularity);
                        viewModel.gasPrice().setValue(scaledProgress.add(gasPriceMin));
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
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
