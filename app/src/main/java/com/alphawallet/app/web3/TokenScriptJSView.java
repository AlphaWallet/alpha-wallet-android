package com.alphawallet.app.web3;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.JavascriptInterface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import timber.log.Timber;

public class TokenScriptJSView extends Web3View {

    public TokenScriptJSView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void init() {
        super.init();

        addJavascriptInterface(new TokenScriptListener(), "tokenscript.engine");
    }

    static class TokenScriptListener {

        @JavascriptInterface
        public void onEvent(){
            Timber.tag("TOKENSCRIPT_JS").w("Event received from tokenscript view");
        }
    }
}
