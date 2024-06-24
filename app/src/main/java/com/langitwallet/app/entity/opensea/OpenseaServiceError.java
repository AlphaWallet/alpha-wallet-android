package com.langitwallet.app.entity.opensea;

import com.langitwallet.app.entity.ErrorEnvelope;

/**
 * Created by James on 20/12/2018.
 * Stormbird in Singapore
 */

public class OpenseaServiceError extends Exception {
    public final ErrorEnvelope error;

    public OpenseaServiceError(String message) {
        super(message);

        error = new ErrorEnvelope(message);
    }
}
