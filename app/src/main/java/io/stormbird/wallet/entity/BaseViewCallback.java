package io.stormbird.wallet.entity;

/**
 * Created by James on 21/03/2018.
 */

public interface BaseViewCallback {
    void queueUpdate(int complete);
    void pushToast(String message);
    void showMarketQueueSuccessDialog(Integer resId);
    void showMarketQueueErrorDialog(Integer resId);
}
