package com.wallet.crypto.trustapp.entity;

import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * Created by James on 30/01/2018.
 */

public class SignaturePair {
    public final byte[] selection;
    public final byte[] signature;
    public final String message;

    //go from selection and signature to produce the sig and selection for QR
    public SignaturePair(String selection, byte[] signature)
    {
        //convert selection into optimised selection
        //first 8 bit value is the length of the message
        byte[] select = null;
        try
        {
            ByteArrayOutputStream sBuilder = new ByteArrayOutputStream();
            DataOutputStream ds = new DataOutputStream(sBuilder);
            int trailingZeros = getTrailingZeros(selection);
            String truncatedValue = selection.substring(0, selection.length() - trailingZeros);
            int newLength = truncatedValue.length(); //only need to know the length of the truncated selection value
            ds.writeByte(newLength);
            ds.writeByte(trailingZeros);
            ds.write(truncatedValue.getBytes());
            ds.flush();
            select = sBuilder.toByteArray();
        }
        catch (Exception e)
        {
            select = null;
        }
        finally
        {
            this.signature = signature;
            this.selection = select;
            this.message = null;
        }
    }

    //got from a received byte[] message to produce selection and signature inputs
    public SignaturePair(String qrMessage, String timeMessage)
    {
        //convert selection into optimised selection
        //qrMessage is base64 message, first convert back to bytes
        byte[] byteMessage = Base64.decode(qrMessage, Base64.DEFAULT);
        byte[] selectionCandidate = null;
        byte[] signatureCandidate = null;
        String messageCandidate = null;
        try
        {
            ByteArrayInputStream sBuilder = new ByteArrayInputStream(byteMessage);
            DataInputStream ds = new DataInputStream(sBuilder);
            int selectionLength = ds.readByte();
            int trailingZeros = ds.readByte();
            byte[] selectionBytes = new byte[selectionLength + trailingZeros];
            ds.read(selectionBytes, 0, selectionLength);
            //populate trailing zeros
            for (int i = 0; i < trailingZeros; i++)
            {
                selectionBytes[i + selectionLength] = '0';
            }
            selectionCandidate = selectionBytes;

            int remaining = ds.available();

            signatureCandidate = new byte[remaining];

            //now read signature
            ds.readFully(signatureCandidate);

            //now create the message
            StringBuilder sb = new StringBuilder();
            sb.append(new String(selectionCandidate));
            sb.append(",");
            sb.append(timeMessage);

            messageCandidate = sb.toString();
        }
        catch (Exception e)
        {
            selectionCandidate = null;
            signatureCandidate = null;
            messageCandidate = null;
        }
        finally
        {
            this.signature = signatureCandidate;
            this.selection = selectionCandidate;
            this.message = messageCandidate;
        }
    }

    private int getTrailingZeros(String s)
    {
        int i = (s.length() - 1);
        int trailingZeros = 0;
        while (s.charAt(i--) == '0')
        {
            trailingZeros++;
        }

        return trailingZeros;
    }
}
