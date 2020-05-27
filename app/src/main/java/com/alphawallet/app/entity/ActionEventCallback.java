package com.alphawallet.app.entity;

import io.reactivex.Single;

/**
 * Created by JB on 26/03/2020.
 */
public interface ActionEventCallback
{
    void receivedEvent(String selectedParamName, String selectedParamValue, long timeStamp, int chainId);
    void eventsLoaded(Event[] events);
}
