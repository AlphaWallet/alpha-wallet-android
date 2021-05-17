package com.alphawallet.app.ui;


import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.viewmodel.GasSettingsViewModel;
import com.alphawallet.app.viewmodel.GasSettingsViewModelFactory;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.widget.GasSliderViewLegacy;
import com.alphawallet.token.tools.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

public class GasSettingsActivityLegacy extends BaseActivity implements StandardFunctionInterface
{
    @Inject
    GasSettingsViewModelFactory viewModelFactory;
    GasSettingsViewModel viewModel;

    private GasSliderViewLegacy gasSliderView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gas_settings_legacy);
        toolbar();
        setTitle(R.string.advanced_settings);

        gasSliderView = findViewById(R.id.gasSliderView);

        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(GasSettingsViewModel.class);

        FunctionButtonBar saveButton = findViewById(R.id.layoutButtons);
        saveButton.setVisibility(View.VISIBLE);
        List<Integer> functions = new ArrayList<>(Collections.singletonList(R.string.action_save));
        saveButton.setupFunctions(this, functions);

        BigDecimal gasPrice = new BigDecimal(getIntent().getStringExtra(C.EXTRA_GAS_PRICE));
        BigInteger gasLimit = new BigInteger(getIntent().getStringExtra(C.EXTRA_GAS_LIMIT));
        int chainId = getIntent().getIntExtra(C.EXTRA_NETWORKID, MAINNET_ID);
        boolean openWithLimitSlider = getIntent().getBooleanExtra(C.EXTRA_STATE, false);

        gasSliderView.initGasLimit(gasLimit);
        gasSliderView.initGasPrice(Convert.fromWei(gasPrice, Convert.Unit.GWEI));
        gasSliderView.setChainId(chainId);

        if (openWithLimitSlider)
        {
            gasSliderView.openGasSlider();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void handleClick(String action, int id)
    {
        Intent intent = new Intent();
        BigInteger weiGasPrice = Convert.toWei(gasSliderView.getGasPrice(), Convert.Unit.GWEI).toBigInteger();
        intent.putExtra(C.EXTRA_GAS_PRICE, weiGasPrice.toString());
        intent.putExtra(C.EXTRA_GAS_LIMIT, gasSliderView.getGasLimit().toString());
        setResult(RESULT_OK, intent);
        finish();
    }
}

