package com.wallet.crypto.trustapp.entity;

public class ApiErrorException extends Exception {
    private final ErrorEnvelope errorEnvelope;

    public ApiErrorException(ErrorEnvelope errorEnvelope) {
        super(errorEnvelope.message);

        this.errorEnvelope = errorEnvelope;
    }
}
