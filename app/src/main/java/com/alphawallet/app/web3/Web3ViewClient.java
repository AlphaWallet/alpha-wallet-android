package com.alphawallet.app.web3;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.N;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.R;
import com.alphawallet.app.widget.AWalletAlertDialog;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import okhttp3.HttpUrl;

public class Web3ViewClient extends WebViewClient {

    private final Object lock = new Object();

    private final JsInjectorClient jsInjectorClient;
    private final UrlHandlerManager urlHandlerManager;

    private final Context context;

    private boolean isInjected;

    public Web3ViewClient(Context context) {
        this.jsInjectorClient = new JsInjectorClient(context);
        this.urlHandlerManager = new UrlHandlerManager();
        this.context = context;
    }

    void addUrlHandler(UrlHandler urlHandler) {
        urlHandlerManager.add(urlHandler);
    }

    void removeUrlHandler(UrlHandler urlHandler) {
        urlHandlerManager.remove(urlHandler);
    }

    public JsInjectorClient getJsInjectorClient()
    {
        return jsInjectorClient;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return shouldOverrideUrlLoading(view, url, false, false);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        if (request == null || view == null) {
            return false;
        }
        String url = request.getUrl().toString();
        boolean isMainFrame = request.isForMainFrame();
        boolean isRedirect = SDK_INT >= N && request.isRedirect();
        return shouldOverrideUrlLoading(view, url, isMainFrame, isRedirect);
    }

    public boolean didInjection()
    {
        return isInjected;
    }

    public void didInjection(boolean injection)
    {
        isInjected = injection;
    }

    private boolean shouldOverrideUrlLoading(WebView webView, String url, boolean isMainFrame, boolean isRedirect) {
        boolean result = false;
        synchronized (lock) {
            isInjected = false;
        }
        String urlToOpen = urlHandlerManager.handle(url);
        //manually handle trusted intents
        if (handleTrustedApps(url))
        {
            return true;
        }

        if (!url.startsWith("http"))
        {
            result = true;
        }
        if (isMainFrame && isRedirect) {
            urlToOpen = url;
            result = true;
        }

        if (result && !TextUtils.isEmpty(urlToOpen)) {
            webView.loadUrl(urlToOpen);
        }
        return result;
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

    public void onReload() {
        synchronized (lock) {
            isInjected = false;
        }
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

    private boolean handleTrustedExtension(String url)
    {
        String[] strArray = context.getResources().getStringArray(R.array.TrustedExtensions);
        for (String item : strArray)
        {
            String[] split = item.split(",");
            if (url.endsWith(split[1]))
            {
                useKnownOpenIntent(split[0], url);
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
            context.startActivity(myIntent);
        }
        else
        {
            Toast.makeText(context, "Required App not Installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void useKnownOpenIntent(String type, String url)
    {
        Intent openIntent = new Intent(Intent.ACTION_VIEW);
        openIntent.setDataAndType(Uri.parse(url), type);
        Intent intent = Intent.createChooser(openIntent, "Open using");
        if (isIntentAvailable(intent))
        {
            context.startActivity(intent);
        }
        else
        {
            Toast.makeText(context, "Required App not Installed", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isIntentAvailable(Intent intent)
    {
        final PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
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
        isInjected = false;
    }
}