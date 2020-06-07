package com.alphawallet.app.web3;

import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.webkit.CookieManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
            for (Cookie cookie : cookies) {
                webViewCookieManager.setCookie(urlString, cookie.toString());
            }
        }
    }

    @Override
    public List<Cookie> loadForRequest(@NonNull HttpUrl url) {
        if (webViewCookieManager != null) {
            String urlString = url.toString();
            String cookiesString = webViewCookieManager.getCookie(urlString);
            if (cookiesString != null && !TextUtils.isEmpty(cookiesString)) {
                String[] cookieHeaders = cookiesString.split(";");
                List<Cookie> cookies = new ArrayList<>();
                for (String cookieHeader : cookieHeaders) {
                    cookies.add(Cookie.parse(url, cookieHeader));
                }
                return cookies;
            }
        }
        return Collections.emptyList();
    }
}
