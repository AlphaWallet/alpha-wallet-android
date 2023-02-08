package com.alphawallet.hardware;

/**
 * Created by JB on 1/02/2023.
 */
public interface HardwareCallback
{
    void hardwareCardError(String errorMessage);

    void signedMessageFromHardware(SignatureFromKey returnSig);

    void onCardReadStart();
}
