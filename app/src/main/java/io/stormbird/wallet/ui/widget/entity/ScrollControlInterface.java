package io.stormbird.wallet.ui.widget.entity;

/**
 * Created by James on 8/07/2019.
 * Stormbird in Sydney
 */
public interface ScrollControlInterface
{
    int getCurrentPage();
    boolean isViewingDappBrowser();
    void moveLeft();
    void moveRight();
}
