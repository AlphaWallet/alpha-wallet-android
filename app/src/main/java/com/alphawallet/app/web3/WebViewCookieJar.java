package com.alphawallet.app.web3;

import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.webkit.CookieManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class WebViewCookieJar implements CookieJar {
    private CookieManager webViewCookieManager;

    public WebViewCookieJar() {
        try {
            webViewCookieManager = CookieManager.getInstance();
        } catch (Exception ex) {
            /* Caused by android.content.pm.PackageManager$NameNotFoundException com.google.android.webview */
        }
    }

    @Override
    public void saveFromResponse(@NonNull HttpUrl url, @NonNull List<Cookie> cookies) {
        if (webViewCookieManager != null) {
            String urlString = url.toString();
            cookies.forEach(cookie -> webViewCookieManager.setCookie(urlString, cookie.toString()));
        }
    }

    @Override
    public List<Cookie> loadForRequest(@NonNull HttpUrl url) {
        if (webViewCookieManager != null) {
            String urlString = url.toString();
            String cookiesString = webViewCookieManager.getCookie(urlString);
            if (cookiesString != null && !TextUtils.isEmpty(cookiesString)) {
                String[] cookieHeaders = cookiesString.split(";");
                return Arrays.stream(cookieHeaders).map(cookieHeader -> Cookie.parse(url, cookieHeader)).collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }
}
