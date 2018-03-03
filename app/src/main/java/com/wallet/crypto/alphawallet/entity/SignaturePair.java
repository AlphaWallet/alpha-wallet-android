package com.wallet.crypto.alphawallet.entity;

import org.web3j.utils.Numeric;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by James on 30/01/2018.
 */

public class SignaturePair {
    public byte[] signature;
    public final String message;

    public String signatureStr;
    public String selectionStr;

    public SignaturePair(String selection, byte[] sig, String message)
    {
        selectionStr = selection;
        signature = rearrangeToVRS(sig);

        BigInteger bi = new BigInteger(signature);
        signatureStr  = bi.toString(10);
        this.message = message;
    }

    /**
     * Helper function to get the large number for sending QR code
     */
    public String formQRMessage()
    {
        return selectionStr + signatureStr;
    }

    public static String generateSelection(List<Integer> indexList)
    {
        String selection = null;

        try {
            if (indexList != null && indexList.size() > 0) {
                Collections.sort(indexList); //ensure index list is sorted
                final int NIBBLE = 4;
                Integer lowestValue = indexList.get(0);
                int zeroCount = lowestValue / NIBBLE;
                //now reduce the index to base of this value
                int correctionFactor = zeroCount * NIBBLE;
                Integer highestValue = indexList.get(indexList.size() - 1); //TODO: Check for highest value out of range of bitfield

                BigInteger bitFieldLookup = BigInteger.ZERO;
                for (Integer i : indexList) {
                    BigInteger adder = BigInteger.valueOf(2).pow(i - correctionFactor);
                    bitFieldLookup = bitFieldLookup.add(adder);
                }

                String truncatedValueDecimal = bitFieldLookup.toString(10); //decimal of reduced bitfield

                //to create string
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("%1$02d", truncatedValueDecimal.length()));
                sb.append(String.format("%1$02d", zeroCount));
                sb.append(String.valueOf(truncatedValueDecimal));

                selection = sb.toString();
            }
        }
        catch (Exception e)
        {
            selection = null;
        }

        return(selection);
    }

    public static List<Integer> buildIndexList(String selection) {
        List<Integer> intList = new ArrayList<>();
        try {
            final int NIBBLE = 4;
            //one: convert to bigint
            String lengthStr = selection.substring(0, 2);
            int selectionLength = Integer.parseInt(lengthStr);
            String trailingZerosStr = selection.substring(2, 4);
            int trailingZeros = Integer.parseInt(trailingZerosStr);
            int correctionFactor = trailingZeros*NIBBLE;

            String selectionStr = selection.substring(4, 4 + selectionLength);
            BigInteger bitField = new BigInteger(selectionStr, 10);

            int radix = bitField.getLowestSetBit();
            while (!bitField.equals(BigInteger.ZERO)) {
                if (bitField.testBit(radix)) {
                    intList.add(radix + correctionFactor); //because we need to encode index zero as the first bit
                    bitField = bitField.clearBit(radix);
                }
                radix++;
            }
        }
        catch (Exception e)
        {

        }

        return intList;
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

    /**
     * Re-arrange signature to VRS (Connor Web3j format) to ensure we don't run into problems with two's compliment representation of signature in decimal
     * @param sigRSV Signature in RSV
     * @return signature in VRS
     */
    private byte[] rearrangeToVRS(byte[] sigRSV)
    {
        ByteArrayOutputStream sigVRS = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(sigVRS);

        try
        {
            ByteArrayInputStream input = new ByteArrayInputStream(sigRSV);
            DataInputStream ds = new DataInputStream(input);
            byte[] r = new byte[32];
            byte[] s = new byte[32];
            int v;

            ds.read(r);
            ds.read(s);
            v = ds.readByte();

            out.writeByte(v+27);
            out.write(r);
            out.write(s);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return sigVRS.toByteArray();
    }

    //got from a received byte[] message to produce selection and signature inputs
    public SignaturePair(String qrMessage, String timeMessage) {
        //convert selection from optimised string

        String lengthStr = qrMessage.substring(0, 2);
        int selectionLength = Integer.parseInt(lengthStr);
        String trailingZerosStr = qrMessage.substring(2, 4);
        int trailingZeros = Integer.parseInt(trailingZerosStr);
        selectionStr = qrMessage.substring(0, 4 + selectionLength);
        signatureStr = qrMessage.substring(4 + selectionLength);
        message = selectionStr + "," + timeMessage;

        BigInteger sigBi = new BigInteger(signatureStr, 10);
        //Now convert sig back to Byte
        signature = sigBi.toByteArray();

        if (signature.length > 65) {
            byte[] sigCopy = new byte[65];
            //prune the first digit
            System.arraycopy(signature, 1, sigCopy, 0, 65);
            signature = sigCopy;
        }
    }
}
