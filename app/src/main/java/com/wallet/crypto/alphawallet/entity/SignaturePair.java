package com.wallet.crypto.alphawallet.entity;

import java.math.BigInteger;

/**
 * Created by James on 30/01/2018.
 */

public class SignaturePair {
    public final byte[] selection;
    public final byte[] signature;
    public final String message;

    public String signatureStr;
    public String selectionStr;

    //go from selection and signature to produce the sig and selection for QR
    public SignaturePair(String selection, byte[] signature)
    {
        //convert selection into optimised selection
        //first 8 bit value is the length of the message
        byte[] select = null;
        try
        {
            int trailingZeros = getTrailingZeros(selection);
            String truncatedValue = selection.substring(0, selection.length() - trailingZeros);

            //to create string
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%1$02d", truncatedValue.length()));
            sb.append(String.format("%1$02d", trailingZeros));
            sb.append(String.valueOf(truncatedValue));
            selectionStr = sb.toString();

            BigInteger bi = new BigInteger(signature);
            signatureStr  = bi.toString(10);

            //check if the produced number was negative
            if (signatureStr.charAt(0) == '-')
            {
                BigInteger twosComplement = twosComplement(bi);
                signatureStr  = twosComplement.toString(10);
            }
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

    public static BigInteger twosComplement(BigInteger original)
    {
        // for negative BigInteger, top byte is negative
        byte[] contents = original.toByteArray();

        // prepend byte of opposite sign
        byte[] result = new byte[contents.length + 1];
        System.arraycopy(contents, 0, result, 1, contents.length);
        result[0] = (contents[0] < 0) ? 0 : (byte)-1;

        // this will be two's complement
        return new BigInteger(result);
    }

    //got from a received byte[] message to produce selection and signature inputs
    public SignaturePair(String qrMessage, String timeMessage)
    {
        //convert selection from optimised string
        //first char is length
        String lengthStr = qrMessage.substring(0,2);
        int selectionLength = Integer.parseInt(lengthStr);
        String trailingZerosStr = qrMessage.substring(2,4);
        int trailingZeros = Integer.parseInt(trailingZerosStr);

        byte[] selectionCandidate = null;
        byte[] signatureCandidate = null;
        String messageCandidate = null;
        try
        {
            //get selection int
            String selectionStr = qrMessage.substring(4, 4 + selectionLength);
            int selectionInt = Integer.parseInt(selectionStr);
            StringBuilder selectionHex = new StringBuilder();
            selectionHex.append(Integer.toHexString(selectionInt));

            //populate trailing zeros
            for (int i = 0; i < trailingZeros; i++)
            {
                selectionHex.append("0");
            }
            selectionCandidate = selectionHex.toString().getBytes();

            //final number is the sig
            String sigStr = qrMessage.substring(4 + selectionLength);
            //convert to BigInteger
            BigInteger sigBi = new BigInteger(sigStr, 10);
            //Now convert sig back to Byte
            signatureCandidate = sigBi.toByteArray();

            if (signatureCandidate.length > 65)
            {
                byte[] sigCopy = new byte[65];
                //prune the first digit
                System.arraycopy(signatureCandidate, 1, sigCopy, 0, 65);
                signatureCandidate = sigCopy;
            }

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
