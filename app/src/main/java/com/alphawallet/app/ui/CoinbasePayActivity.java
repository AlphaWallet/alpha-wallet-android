package com.alphawallet.app.ui;

import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;

import com.alphawallet.app.R;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CoinbasePayActivity extends BaseActivity
{
    private WebView webView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_coinbase_pay);

        toolbar();

        setTitle("Coinbase Pay");

        webView = findViewById(R.id.webview);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        webView.clearCache(true);
        webView.clearHistory();
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        webView.loadUrl("file:///android_asset/cbpay.html");
    }
}
