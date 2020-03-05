package com.alphawallet.app.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;

public class StaticViewer extends BaseActivity
{
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_static_viewer);
        super.onCreate(savedInstanceState);
        toolbar();

        String base64WebData = getIntent().getStringExtra(C.EXTRA_STATE);
        String title = getIntent().getStringExtra(C.EXTRA_PAGE_TITLE);
        WebView webView = findViewById(R.id.webview);
        setTitle(title);

        //load string
        webView.loadData(base64WebData, "text/html; charset=utf-8", "base64");
    }

    //allow back
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
