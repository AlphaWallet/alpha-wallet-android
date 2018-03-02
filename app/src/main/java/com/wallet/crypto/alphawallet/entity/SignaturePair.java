package com.wallet.crypto.alphawallet.entity;

import java.math.BigInteger;
import java.util.ArrayList;
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

    public SignaturePair(String selection, byte[] sig, String digest)
    {
        selectionStr = selection;
        signature = sig;
        BigInteger bi = new BigInteger(signature);
        signatureStr  = bi.toString(10);
        message = digest;

        //check if the produced number was negative
        if (signatureStr.charAt(0) == '-')
        {
            BigInteger twosComplement = twosComplement(bi);
            signatureStr  = twosComplement.toString(10);
        }
    }

    /* James made this to generate a compact string representation of the indices of an ERC875 asset.*/
    public static String generateSelection(List<Integer> indexList)
    {
        String selection = null;

        try {
            if (indexList != null && indexList.size() > 0) {
                Collections.sort(indexList); // Indices needs to be sorted to find its bitfield representation
                final int NIBBLE = 4;
                Integer lowestValue = indexList.get(0);
                int zeroCount = lowestValue / NIBBLE;
                // now reduce the index to base of this value
                int correctionFactor = zeroCount * NIBBLE;
                Integer highestValue = indexList.get(indexList.size() - 1); //TODO: Check for highest value out of range of bitfield

                /* the method here is easier to express with matrix programming like this:
                indexList = indexList - correctionFactor # reduce every element of the list by an int
                selection = sum(2^indexList)             # raise every element and add the result back */
                BigInteger bitFieldLookup = BigInteger.ZERO;
                for (Integer i : indexList) {
                    BigInteger adder = BigInteger.valueOf(2).pow(i - correctionFactor);
                    bitFieldLookup = bitFieldLookup.add(adder);
                }

                String truncatedValueDecimal = bitFieldLookup.toString(10); //decimal of reduced bitfield

                // to create string
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
