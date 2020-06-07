package com.alphawallet.app.web3;

import android.net.Uri;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

public class UrlHandlerManager {

    private final Map<String, UrlHandler> handlers = new HashMap<>();

    public UrlHandlerManager(UrlHandler... handlers) {
        for (UrlHandler urlHandler : handlers) {
            this.handlers.put(urlHandler.getScheme(), urlHandler);
        }
    }

    public void add(@NonNull UrlHandler urlHandler) {
        this.handlers.put(urlHandler.getScheme(), urlHandler);
    }

    public void remove(@NonNull UrlHandler urlHandler) {
        this.handlers.remove(urlHandler.getScheme());
    }

    String handle(String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        Uri uri = Uri.parse(url);
        return handle(uri);
    }

    String handle(Uri uri) {
        if (uri == null) {
            return null;
        }
        if (!handlers.containsKey(uri.getScheme())) {
            return uri.toString();
        }
        return handlers.get(uri.getScheme()).handle(uri);
    }
}
