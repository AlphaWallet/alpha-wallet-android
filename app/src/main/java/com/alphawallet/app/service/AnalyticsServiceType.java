package com.alphawallet.app.service;

import androidx.annotation.Keep;

@Keep
public interface AnalyticsServiceType<T> {

    void track(String eventName);

    void track(String eventName, T event);

    void flush();

    void identify(String uuid);
}