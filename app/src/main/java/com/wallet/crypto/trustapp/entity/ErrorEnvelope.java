package com.wallet.crypto.trustapp.entity;

import android.support.annotation.Nullable;

import com.wallet.crypto.trustapp.C;

public class ErrorEnvelope {
	public final int code;
	@Nullable
	public final String message;

	public ErrorEnvelope(@Nullable String message) {
		this(C.ErrorCode.UNKNOWN, message);
	}

	public ErrorEnvelope(int code, @Nullable String message) {
		this.code = code;
		this.message = message;
	}
}
