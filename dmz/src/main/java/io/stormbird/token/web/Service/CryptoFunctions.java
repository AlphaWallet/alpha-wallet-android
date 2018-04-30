package io.stormbird.token.web.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Arrays;

import io.stormbird.token.entity.CryptoFunctionsInterface;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

public class CryptoFunctions implements CryptoFunctionsInterface
{
    private static final Base64Encoder encoder = new Base64Encoder();

    @Override
    public byte[] Base64Decode(String message)
    {
        return decode(message);
    }

    @Override
    public byte[] Base64Encode(byte[] data)
    {
        return encode(data, 0, data.length);
    }

    @Override
    public BigInteger signedMessageToKey(byte[] data, byte[] signature) throws SignatureException
    {
        Sign.SignatureData sigData = sigFromByteArray(signature);
        return Sign.signedMessageToKey(data, sigData);
    }

    @Override
    public String getAddressFromKey(BigInteger recoveredKey)
    {
        return Keys.getAddress(recoveredKey);
    }

    public static Sign.SignatureData sigFromByteArray(byte[] sig)
    {
        byte   subv = (byte)(sig[64]);
        if (subv < 27) subv += 27;

        byte[] subrRev = Arrays.copyOfRange(sig, 0, 32);
        byte[] subsRev = Arrays.copyOfRange(sig, 32, 64);

        BigInteger r = new BigInteger(1, subrRev);
        BigInteger s = new BigInteger(1, subsRev);

        Sign.SignatureData ecSig = new Sign.SignatureData(subv, subrRev, subsRev);

        return ecSig;
    }

    /**
     * encode the input data producing a base 64 encoded byte array.
     *
     * @return a byte array containing the base 64 encoded data.
     */
    public static byte[] encode(
            byte[] data,
            int    off,
            int    length)
    {
        int len = (length + 2) / 3 * 4;
        ByteArrayOutputStream bOut = new ByteArrayOutputStream(len);

        try
        {
            encoder.encode(data, off, length, bOut);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return bOut.toByteArray();
    }

    public static byte[] decode(
            String    data)
    {
        int len = data.length() / 4 * 3;
        ByteArrayOutputStream bOut = new ByteArrayOutputStream(len);

        try
        {
            encoder.decode(data, bOut);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return bOut.toByteArray();
    }
}
