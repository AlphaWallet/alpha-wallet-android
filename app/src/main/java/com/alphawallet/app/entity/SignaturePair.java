package com.alphawallet.app.entity;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by James on 30/01/2018. Signature Pair refers to the
 * following data structure [selection of tokens][signature]. The word
 * "Signature Pair" has nothing to do with the pair of numbers that
 * makes up signature (𝑟, 𝑠), nor does it has anything to do with
 * Elliptic-Curve pairing. The "Pair" word is here refer to a Java
 * concept where you combine data to pass to the observer. And the
 * Pairing is actually not done in this class, because other data not
 * in the pair (like timestamp) are to be bound together outside of
 * the class.
 */

public class SignaturePair
{
    private static final int SELECTION_DESIGNATOR_SIZE = 2; //gives the fixed length of the selection length encoding
    private static final int TRAILING_ZEROES_SIZE = 3; //gives the fixed length of the trailing zeroes after selection

    public byte[] selection;
    public byte[] signature;
    public final String message;

    public String signatureStr;
    public String selectionStr;

    /**
     * You might expect the code to combine the message and signature
     * to be in this class - it is not.
     * @param selection what is returned from generate
     * @param sig       65 bytes of signature
     * @param message   the message being signed, including the 'selection'.
     */
    public SignaturePair(String selection, byte[] sig, String message)
    {
        selectionStr = selection;
        signature = sig;

        BigInteger bi = new BigInteger(1, signature);
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

    /**
     * Generate a compact string representation of the indices of an
     * ERC875 asset.  Notice that this function is not used in this
     * class. It is used to return the selectionStr to be used as a
     * parameter of the constructor */
    public static String generateSelection(List<Integer> indexList)
    {
        String selection = null;
        Collections.sort(indexList); // just to find the lowest value
        // since sorting is not needed to make the bitFieldLookup
        Integer lowestValue = indexList.get(0);
        final int NIBBLE = 4;
        int zeroCount = lowestValue / NIBBLE;
        // now reduce the index to base of this value
        int correctionFactor = zeroCount * NIBBLE;
        // TODO: Check for highest value out of range of bitfield. Like this:
        Integer highestValue = indexList.get(indexList.size() - 1);


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
        String formatDecimals = "%1$0" + SELECTION_DESIGNATOR_SIZE + "d";
        String formatZeros = "%1$0" + TRAILING_ZEROES_SIZE + "d";
        sb.append(String.format(formatDecimals, truncatedValueDecimal.length()));
        sb.append(String.format(formatZeros, zeroCount));
        sb.append(String.valueOf(truncatedValueDecimal));

        return sb.toString();
    }

    /**
     * The reverse of generateSelection - used in scanning the QR code.
     */
    public static List<Integer> buildIndexList(String selection) {
        List<Integer> intList = new ArrayList<>();
        final int NIBBLE = 4;
        //one: convert to bigint
        String lengthStr = selection.substring(0, SELECTION_DESIGNATOR_SIZE);
        int selectionLength = Integer.parseInt(lengthStr);
        String trailingZerosStr = selection.substring(SELECTION_DESIGNATOR_SIZE, SELECTION_DESIGNATOR_SIZE + TRAILING_ZEROES_SIZE);
        int trailingZeros = Integer.parseInt(trailingZerosStr);
        int correctionFactor = trailingZeros * NIBBLE;

        String selectionStr = selection.substring(SELECTION_DESIGNATOR_SIZE + TRAILING_ZEROES_SIZE,
                                                  SELECTION_DESIGNATOR_SIZE + TRAILING_ZEROES_SIZE + selectionLength);
        BigInteger bitField = new BigInteger(selectionStr, 10);

        int radix = bitField.getLowestSetBit();
        while (!bitField.equals(BigInteger.ZERO))
        {
            if (bitField.testBit(radix))
            {
                intList.add(radix + correctionFactor); //because we need to encode index zero as the first bit
                bitField = bitField.clearBit(radix);
            }
            radix++;
        }

        return intList;
    }

    //got from a received byte[] message to produce selection and signature inputs
    public SignaturePair(String qrMessage, String timeMessage, String contractAddr)
    {
        //convert selection from optimised string
        String lengthStr = qrMessage.substring(0, SELECTION_DESIGNATOR_SIZE);
        int selectionLength = Integer.parseInt(lengthStr);
        String trailingZerosStr = qrMessage.substring(SELECTION_DESIGNATOR_SIZE, SELECTION_DESIGNATOR_SIZE + TRAILING_ZEROES_SIZE);
        int trailingZeros = Integer.parseInt(trailingZerosStr);
        selectionStr = qrMessage.substring(0, SELECTION_DESIGNATOR_SIZE + TRAILING_ZEROES_SIZE + selectionLength);
        signatureStr = qrMessage.substring(SELECTION_DESIGNATOR_SIZE + TRAILING_ZEROES_SIZE + selectionLength);
        message = selectionStr + "," + timeMessage + "," + contractAddr.toLowerCase();
        selection = selectionStr.getBytes();

        BigInteger sigBi = new BigInteger(signatureStr, 10);
        //Now convert sig back to Byte
        signature = sigBi.toByteArray();

        if (signature.length < 65)
        {
            int offset = 65 - signature.length;
            byte[] sigCopy = new byte[65];
            System.arraycopy(signature, 0, sigCopy, offset, 65-offset);
            for (int i = 0; i < offset; i++)
            {
                sigCopy[i] = 0;
            }
            signature = sigCopy;
        }
        else if (signature.length > 65)
        {
            byte[] sigCopy = new byte[65];
            //prune the first digit
            System.arraycopy(signature, 1, sigCopy, 0, 65);
            signature = sigCopy;
        }
    }

    public boolean isValid()
    {
        return selectionStr != null && selectionStr.length() != 0;
    }
}
