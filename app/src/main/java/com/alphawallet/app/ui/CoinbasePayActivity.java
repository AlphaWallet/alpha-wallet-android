package com.alphawallet.app.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.R;
import com.alphawallet.app.analytics.Analytics;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.coinbasepay.DestinationWallet;
import com.alphawallet.app.viewmodel.CoinbasePayViewModel;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CoinbasePayActivity extends BaseActivity
{
    private CoinbasePayViewModel viewModel;
    private WebView webView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_coinbase_pay);

        toolbar();

        setTitle(getString(R.string.title_buy_with_coinbase_pay));

        initViewModel();

        initWebView();

        viewModel.track(Analytics.Navigation.COINBASE_PAY);

        viewModel.prepare();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView()
    {
        webView = findViewById(R.id.web_view);
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setAppCacheEnabled(false);
        webView.clearCache(true);
        webView.clearHistory();
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this).get(CoinbasePayViewModel.class);
        viewModel.defaultWallet().observe(this, this::onDefaultWallet);
    }

    private void onDefaultWallet(Wallet wallet)
    {
        DestinationWallet.Type type;
        List<String> list = new ArrayList<>();
        String asset = getIntent().getStringExtra("asset");
        if (!TextUtils.isEmpty(asset))
        {
            type = DestinationWallet.Type.ASSETS;
            list.add(asset);
        }
        else
        {
            type = DestinationWallet.Type.BLOCKCHAINS;
            String blockchain = getIntent().getStringExtra("blockchain");
            list.add(blockchain);
        }

        String uri = viewModel.getUri(type, wallet.address, list);
        if (TextUtils.isEmpty(uri))
        {
            Toast.makeText(this, "Missing Coinbase Pay App ID.", Toast.LENGTH_LONG).show();
            finish();
        }
        else
        {
            webView.loadUrl(uri);
        }
    }

    @Override
    public void onBackPressed()
    {
        webView.clearCache(true);
        super.onBackPressed();
        overridePendingTransition(R.anim.hold, R.anim.slide_out_right);
    }
}
