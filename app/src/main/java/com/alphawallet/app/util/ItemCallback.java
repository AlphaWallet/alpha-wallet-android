package com.alphawallet.app.util;

/** Simple callback functional interface for any object*/
public interface ItemCallback<T> {
    public void call(T object);
}
