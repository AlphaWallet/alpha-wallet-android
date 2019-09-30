package com.alphawallet.app.ui.widget.entity;

/**
 * Created by James on 17/11/2018.
 * Stormbird in Singapore
 */
public interface ItemClickListener
{
    void onItemClick(String url);
    default void onItemLongClick(String url) { }  //only override this if extra handling is needed
}
