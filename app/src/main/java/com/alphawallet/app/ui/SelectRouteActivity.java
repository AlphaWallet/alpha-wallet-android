package com.alphawallet.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.lifi.Route;
import com.alphawallet.app.ui.widget.adapter.RouteAdapter;
import com.alphawallet.app.ui.widget.entity.ProgressInfo;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.SwapUtils;
import com.alphawallet.app.viewmodel.SelectRouteViewModel;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.AddressIcon;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SelectRouteActivity extends BaseActivity
{
    private static final long GET_ROUTES_INTERVAL_MS = 30000;
    private static final long COUNTDOWN_INTERVAL_MS = 1000;
    private SelectRouteViewModel viewModel;
    private RecyclerView recyclerView;
    private TextView fromAmount;
    private TextView fromSymbol;
    private TextView currentPrice;
    private TextView countdownText;
    private LinearLayout noRoutesLayout;
    private MaterialButton selectExchangesBtn;
    private AddressIcon fromTokenIcon;
    private AWalletAlertDialog progressDialog;
    private CountDownTimer getRoutesTimer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_select_route);

        toolbar();

        setTitle(getString(R.string.title_select_route));

        initViews();

        initViewModel();

        initTimer();
    }

    @Override
    protected void onResume()
    {
        getRoutes();
        super.onResume();
    }

    @Override
    protected void onPause()
    {
        if (getRoutesTimer != null)
        {
            getRoutesTimer.cancel();
        }
        super.onPause();
    }

    private void initViews()
    {
        recyclerView = findViewById(R.id.list_routes);
        fromAmount = findViewById(R.id.from_amount);
        fromSymbol = findViewById(R.id.from_symbol);
        fromTokenIcon = findViewById(R.id.from_token_icon);
        currentPrice = findViewById(R.id.current_price);
        countdownText = findViewById(R.id.text_countdown);
        noRoutesLayout = findViewById(R.id.layout_no_routes_found);
        selectExchangesBtn = findViewById(R.id.btn_select_exchanges);
        selectExchangesBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, SelectSwapProvidersActivity.class);
            startActivity(intent);
        });

        progressDialog = new AWalletAlertDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setProgressMode();
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this)
                .get(SelectRouteViewModel.class);
        viewModel.routes().observe(this, this::onRoutes);
        viewModel.progressInfo().observe(this, this::onProgressInfo);
    }

    private void initTimer()
    {
        getRoutesTimer = new CountDownTimer(GET_ROUTES_INTERVAL_MS, COUNTDOWN_INTERVAL_MS)
        {
            @Override
            public void onTick(long millisUntilFinished)
            {
                String format = millisUntilFinished < 10000 ? "0:0%d" : "0:%d";
                String time = String.format(Locale.ENGLISH, format, millisUntilFinished / 1000);
                countdownText.setText(getString(R.string.label_available_routes, time));
            }

            @Override
            public void onFinish()
            {
                getRoutes();
            }
        };
    }

    private void getRoutes()
    {
        String fromChainId = getIntent().getStringExtra("fromChainId");
        String toChainId = getIntent().getStringExtra("toChainId");
        String fromTokenAddress = getIntent().getStringExtra("fromTokenAddress");
        String toTokenAddress = getIntent().getStringExtra("toTokenAddress");
        String fromAddress = getIntent().getStringExtra("fromAddress");
        String fromAmount = getIntent().getStringExtra("fromAmount");
        long fromTokenDecimals = getIntent().getLongExtra("fromTokenDecimals", -1);
        String slippage = getIntent().getStringExtra("slippage");
        String fromSymbol = getIntent().getStringExtra("fromTokenSymbol");
        String fromTokenLogoUri = getIntent().getStringExtra("fromTokenLogoUri");

        this.fromAmount.setText(BalanceUtils.getShortFormat(fromAmount, fromTokenDecimals));
        this.fromSymbol.setText(fromSymbol);
        this.fromTokenIcon.bindData(fromTokenLogoUri, Long.parseLong(fromChainId), fromTokenAddress, fromSymbol);

        viewModel.getRoutes(fromChainId, toChainId, fromTokenAddress, toTokenAddress, fromAddress, fromAmount, slippage, viewModel.getPreferredExchanges());
    }

    private void onRoutes(List<Route> routes)
    {
        processRoutes(routes);

        getRoutesTimer.start();
    }

    private void processRoutes(List<Route> routeList)
    {
        RouteAdapter adapter = new RouteAdapter(this, routeList, provider -> {
            Intent intent = new Intent();
            intent.putExtra("provider", provider);
            setResult(RESULT_OK, intent);
            finish();
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        if (!routeList.isEmpty())
        {
            Route route = routeList.get(0);
            currentPrice.setText(SwapUtils.getFormattedCurrentPrice(route.steps.get(0).action));
            noRoutesLayout.setVisibility(View.GONE);
        }
        else
        {
            currentPrice.setText(R.string.NA);
            noRoutesLayout.setVisibility(View.VISIBLE);
        }
    }

    private void onProgressInfo(ProgressInfo progressInfo)
    {
        if (progressInfo.shouldShow())
        {
            progressDialog.setMessage(progressInfo.getMessage());
            progressDialog.show();
        }
        else
        {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onBackPressed()
    {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
}
