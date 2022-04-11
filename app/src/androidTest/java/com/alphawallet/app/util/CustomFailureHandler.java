package com.alphawallet.app.util;

import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.test.espresso.FailureHandler;
import androidx.test.espresso.base.DefaultFailureHandler;

import org.hamcrest.Matcher;

public class CustomFailureHandler implements FailureHandler {
    private final String methodName;
    private final FailureHandler delegate;

    public CustomFailureHandler(String methodName, Context targetContext) {
        this.methodName = methodName;
        delegate = new DefaultFailureHandler(targetContext);
    }

    @Override
    public void handle(Throwable error, Matcher<View> viewMatcher) {
        SnapshotUtil.take(methodName);
        delegate.handle(error, viewMatcher);
    }
}