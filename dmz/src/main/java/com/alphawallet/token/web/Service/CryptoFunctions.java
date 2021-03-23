package com.alphawallet.token.web.Service;

import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Arrays;

import com.alphawallet.token.entity.CryptoFunctionsInterface;
import com.alphawallet.token.entity.ProviderTypedData;
import com.alphawallet.token.web.Ethereum.web3j.StructuredDataEncoder;

import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

public class CryptoFunctions implements CryptoFunctionsInterface
{
    @Override
    public byte[] Base64Decode(String message)
    {
        return Base64.getUrlDecoder().decode(message);
    }

    @Override
    public byte[] Base64Encode(byte[] data)
    {
        return Base64.getUrlEncoder().encode(data);
    }

    @Override
    public BigInteger signedMessageToKey(byte[] data, byte[] signature) throws SignatureException
    {
        Sign.SignatureData sigData = sigFromByteArray(signature);
        if (sigData == null) return BigInteger.ZERO;
        else return Sign.signedMessageToKey(data, sigData);
    }

    @Override
    public String getAddressFromKey(BigInteger recoveredKey)
    {
        return Keys.getAddress(recoveredKey);
    }

    @Override
    public byte[] keccak256(byte[] message)
    {
        return Hash.sha3(message);
    }

    @Override
    public CharSequence formatTypedMessage(ProviderTypedData[] rawData)
    {
        //produce readable text to display in the signing prompt
        StringBuilder sb = new StringBuilder();
        boolean firstVal = true;
        for (ProviderTypedData data : rawData)
        {
            if (!firstVal) sb.append("\n");
            sb.append(data.name).append(":");
            sb.append("\n  ").append(data.value.toString());
            firstVal = false;
        }

        return sb.toString();
    }

    @Override
    public CharSequence formatEIP721Message(String messageData)
    {
        CharSequence msgData = "";
        try
        {
            StructuredDataEncoder eip721Object = new StructuredDataEncoder(messageData);

            HashMap<String, Object> messageMap = (HashMap<String, Object>) eip721Object.jsonMessageObject.getMessage();
            StringBuilder sb = new StringBuilder();
            for (String entry : messageMap.keySet())
            {
                sb.append(entry).append(":").append("\n");
                Object v = messageMap.get(entry);
                if (v instanceof LinkedHashMap)
                {
                    HashMap<String, Object> valueMap = (HashMap<String, Object>) messageMap.get(entry);
                    for (String paramName : valueMap.keySet())
                    {
                        String value = valueMap.get(paramName).toString();
                        sb.append(" ").append(paramName).append(": ");
                        sb.append(value).append("\n");
                    }
                }
                else
                {
                    sb.append(" ").append(v.toString()).append("\n");
                }
            }

            msgData = sb.toString();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return msgData;
    }

    @Override
    public byte[] getStructuredData(String messageData)
    {
        try
        {
            StructuredDataEncoder eip721Object = new StructuredDataEncoder(messageData);
            return eip721Object.getStructuredData();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return new byte[0];
    }

    public static Sign.SignatureData sigFromByteArray(byte[] sig)
    {
        if (sig.length < 64 || sig.length > 65) return null;

        byte   subv = sig[64];
        if (subv < 27) subv += 27;

        byte[] subrRev = Arrays.copyOfRange(sig, 0, 32);
        byte[] subsRev = Arrays.copyOfRange(sig, 32, 64);

        BigInteger r = new BigInteger(1, subrRev);
        BigInteger s = new BigInteger(1, subsRev);

        return new Sign.SignatureData(subv, subrRev, subsRev);
    }
}
