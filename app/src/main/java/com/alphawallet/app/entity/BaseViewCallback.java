package com.alphawallet.app.entity;

/**
 * Created by James on 21/03/2018.
 */

public interface BaseViewCallback {
    void queueUpdate(int complete);
    void pushToast(String message);
}
