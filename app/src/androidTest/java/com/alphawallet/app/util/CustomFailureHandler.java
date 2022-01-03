package com.alphawallet.app.util;

import android.content.Context;
import android.util.Log;
import android.view.View;

import org.hamcrest.Matcher;

import androidx.annotation.NonNull;
import androidx.test.espresso.FailureHandler;
import androidx.test.espresso.base.DefaultFailureHandler;

public class CustomFailureHandler implements FailureHandler {
    private final FailureHandler delegate;
    private final Context context;

    public CustomFailureHandler(Context targetContext) {
        delegate = new DefaultFailureHandler(targetContext);
        context = targetContext;
    }

    @Override
    public void handle(Throwable error, Matcher<View> viewMatcher) {
        String method = getTestMethodName(error);
        Log.e("seaborn", method);
        SnapshotUtil.take(method);
        delegate.handle(error, viewMatcher);
    }

    @NonNull
    private String getTestMethodName(Throwable error) {
        String file = "";
        String line = "";
        String method = "";
        for (final StackTraceElement st : error.getStackTrace())
        {
            final String currStackElement = st.toString();
            if (currStackElement.contains("io.stormbird.wallet"))
            {
                final String file1 = currStackElement.split("\\(")[1].split("\\)")[0];
                file = file1.split(":")[0];
                line = file1.split(":")[1];
                final String temp = currStackElement.split("\\(")[0];
                method = temp.substring(temp.lastIndexOf(".") + 1);
            }
        }
        return "should_transfer_from_an_account_to_another";
    }
}