package com.alphawallet.app.entity;

import androidx.annotation.Nullable;

public class ServiceErrorException extends Exception {

    public enum ServiceErrorCode {
        UNKNOWN_ERROR, INVALID_DATA, KEY_STORE_ERROR, FAIL_TO_SAVE_IV_FILE, KEY_STORE_SECRET, USER_NOT_AUTHENTICATED, KEY_IS_GONE,
        IV_OR_ALIAS_NO_ON_DISK, INVALID_KEY
    }

    public final ServiceErrorCode code;

    public ServiceErrorException(ServiceErrorCode code, @Nullable String message, Throwable throwable) {
        super(message, throwable);
        this.code = code;
    }

    public ServiceErrorException(ServiceErrorCode code, @Nullable String message) {
        this(code, message, null);
    }

    public ServiceErrorException(ServiceErrorCode code) {
        this(code, null);
    }
}
