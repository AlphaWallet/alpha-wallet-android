package com.alphawallet.app.web3;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.alphawallet.app.R;
import com.alphawallet.app.widget.AWalletAlertDialog;

public class Web3ViewClient extends WebViewClient {

    private final JsInjectorClient jsInjectorClient;

    private final Context context;

    public Web3ViewClient(Context context) {
        this.jsInjectorClient = new JsInjectorClient(context);
        this.context = context;
    }

    public JsInjectorClient getJsInjectorClient()
    {
        return jsInjectorClient;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return handleTrustedApps(url);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        if (request == null || view == null) {
            return false;
        }
        String url = request.getUrl().toString();
        return handleTrustedApps(url);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        if (request == null) {
            return null;
        }

        return super.shouldInterceptRequest(view, request);
    }

    public String getInitString(WebView view)
    {
        return jsInjectorClient.initJs(view.getContext());
    }

    public String getProviderString(WebView view)
    {
        return jsInjectorClient.providerJs(view.getContext());
    }

    @Override
    public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error)
    {
        AWalletAlertDialog aDialog = new AWalletAlertDialog(context);
        aDialog.setTitle(R.string.title_dialog_error);
        aDialog.setIcon(AWalletAlertDialog.ERROR);
        aDialog.setMessage(R.string.ssl_cert_invalid);
        aDialog.setButtonText(R.string.dialog_approve);
        aDialog.setButtonListener(v -> {
            handler.proceed();
            aDialog.dismiss();
        });
        aDialog.setSecondaryButtonText(R.string.action_cancel);
        aDialog.setButtonListener(v -> {
            handler.cancel();
            aDialog.dismiss();
        });
        aDialog.show();
    }

    //Handling of trusted apps
    private boolean handleTrustedApps(String url)
    {
        //get list
        String[] strArray = context.getResources().getStringArray(R.array.TrustedApps);
        for (String item : strArray)
        {
            String[] split = item.split(",");
            if (url.startsWith(split[1]))
            {
                intentTryApp(split[0], url);
                return true;
            }
        }

        return false;
    }

    private void intentTryApp(String appId, String msg)
    {
        final boolean isAppInstalled = isAppAvailable(appId);
        if (isAppInstalled)
        {
            Intent myIntent = new Intent(Intent.ACTION_VIEW);
            myIntent.setPackage(appId);
            myIntent.setData(Uri.parse(msg));
            myIntent.putExtra(Intent.EXTRA_TEXT, msg);
            myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(myIntent);
        }
        else
        {
            Toast.makeText(context, "Required App not Installed", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isAppAvailable(String appName)
    {
        PackageManager pm = context.getPackageManager();
        try
        {
            pm.getPackageInfo(appName, PackageManager.GET_ACTIVITIES);
            return true;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            return false;
        }
    }

    public void resetInject()
    {
    }
}