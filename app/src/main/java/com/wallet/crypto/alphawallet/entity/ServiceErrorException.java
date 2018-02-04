package com.wallet.crypto.trustapp.entity;

import android.support.annotation.Nullable;

public class ServiceErrorException extends Exception {

    public static final int UNKNOWN_ERROR = -1;
    public static final int INVALID_DATA = 1;
    public static final int KEY_STORE_ERROR = 1001;
    public static final int FAIL_TO_SAVE_IV_FILE = 1002;
    public static final int KEY_STORE_SECRET = 1003;
    public static final int USER_NOT_AUTHENTICATED = 1004;
    public static final int KEY_IS_GONE = 1005;
    public static final int IV_OR_ALIAS_NO_ON_DISK = 1006;
    public static final int INVALID_KEY = 1007;

    public final int code;

    public ServiceErrorException(int code, @Nullable String message, Throwable throwable) {
        super(message, throwable);
        this.code = code;
    }

    public ServiceErrorException(int code, @Nullable String message) {
        this(code, message, null);
    }

    public ServiceErrorException(int code) {
        this(code, null);
    }
}
