package com.alphawallet.hardware;

import android.app.Activity;

/**
 * Created by JB on 2/02/2023.
 *
 * Empty stub to ensure the repo builds if no hardware is implemented
 */
public class HardwareDevice //Should implement the hardware callback from the device SDK
{
    private final HardwareCallback callback;
    private byte[] digestBytes;

    // Constructor - add the init code here
    public HardwareDevice(HardwareCallback callback)
    {
        this.callback = callback;
    }

    public boolean isStub() //If you implement hardware set to false.
    {
        return true;
    }

    public String getPlaceCardMessage(Activity activity) //string to be used to ask user to hold card to phone
    {
        return null;
    }

    //Set the signing data which will be signed later by the card (when card is ready to sign - see onDetectedCard below)
    public void setSigningData(byte[] signingData)
    {
        digestBytes = signingData; //NB digestBytes must have already been hashed
    }

    public void activateReader(Activity activity)
    {

    }

    public void deactivateReader()
    {

    }

    // Add your Hardware SDK callbacks here which might be something like this:
    /*
    @Override
    public void onDetectedCard(NFC status)
    {
        callback.onCardReadStart(); // so you can display a pop-up to let user know card reading is in progress
        SignatureFromKey returnSig = new SignatureFromKey();

        try
        {
            returnSig.signature = cardManager.signBytes(digestBytes);
            returnSig.sigType = SignatureReturnType.SIGNATURE_GENERATED;
        }
        catch (Exception e)
        {
            callback.hardwareCardError(e.getMessage());
        }

        callback.signedMessageFromHardware(returnSig);
    }
    */
}
