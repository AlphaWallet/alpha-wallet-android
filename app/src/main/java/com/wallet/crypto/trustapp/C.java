package com.wallet.crypto.trustapp;

public abstract class C {

    public static final int IMPORT_REQUEST_CODE = 1001;
    public static final int EXPORT_REQUEST_CODE = 1002;
    public static final int SHARE_REQUEST_CODE = 1003;

    public interface ErrorCode {

		int UNKNOWN = 1;
		int CANT_GET_STORE_PASSWORD = 2;
	}

    public interface Key {
	    String WALLET = "wallet";
    }
}
