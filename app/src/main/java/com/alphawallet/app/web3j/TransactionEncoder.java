package com.alphawallet.app.web3j;

import com.alphawallet.app.web3j.transactiontype.TransactionType;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.Sign;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Created by JB on 5/08/2021.
 */
public class TransactionEncoder
{
    private static final int CHAIN_ID_INC = 35;
    private static final int LOWER_REAL_V = 27;

    public static byte[] signMessage(RawTransaction rawTransaction, Credentials credentials) {
        byte[] encodedTransaction = encode(rawTransaction);
        Sign.SignatureData signatureData =
                Sign.signMessage(encodedTransaction, credentials.getEcKeyPair());

        return encode(rawTransaction, signatureData);
    }

    public static byte[] signMessage(
            RawTransaction rawTransaction, long chainId, Credentials credentials) {

        if (!rawTransaction.getType().equals(TransactionType.LEGACY)) {
            return signMessage(rawTransaction, credentials);
        }

        byte[] encodedTransaction = encode(rawTransaction, chainId);
        Sign.SignatureData signatureData =
                Sign.signMessage(encodedTransaction, credentials.getEcKeyPair());

        Sign.SignatureData eip155SignatureData = createEip155SignatureData(signatureData, chainId);
        return encode(rawTransaction, eip155SignatureData);
    }

    @Deprecated
    public static byte[] signMessage(
            RawTransaction rawTransaction, byte chainId, Credentials credentials) {
        return signMessage(rawTransaction, (long) chainId, credentials);
    }

    public static Sign.SignatureData createEip155SignatureData(
            Sign.SignatureData signatureData, long chainId) {
        BigInteger v = Numeric.toBigInt(signatureData.getV());
        v = v.subtract(BigInteger.valueOf(LOWER_REAL_V));
        v = v.add(BigInteger.valueOf(chainId).multiply(BigInteger.valueOf(2)));
        v = v.add(BigInteger.valueOf(CHAIN_ID_INC));

        return new Sign.SignatureData(v.toByteArray(), signatureData.getR(), signatureData.getS());
    }

    @Deprecated
    public static Sign.SignatureData createEip155SignatureData(
            Sign.SignatureData signatureData, byte chainId) {
        return createEip155SignatureData(signatureData, (long) chainId);
    }

    public static byte[] encode(RawTransaction rawTransaction) {
        return encode(rawTransaction, null);
    }

    public static byte[] encode(RawTransaction rawTransaction, long chainId) {
        Sign.SignatureData signatureData =
                new Sign.SignatureData(longToBytes(chainId), new byte[] {}, new byte[] {});
        return encode(rawTransaction, signatureData);
    }

    @Deprecated
    public static byte[] encode(RawTransaction rawTransaction, byte chainId) {
        return encode(rawTransaction, (long) chainId);
    }

    public static byte[] encode(RawTransaction rawTransaction, Sign.SignatureData signatureData) {
        List<RlpType> values = asRlpValues(rawTransaction, signatureData);
        RlpList rlpList = new RlpList(values);
        byte[] encoded = RlpEncoder.encode(rlpList);
        if (!rawTransaction.getType().equals(TransactionType.LEGACY)) {
            return ByteBuffer.allocate(encoded.length + 1)
                    .put(rawTransaction.getType().getRlpType())
                    .put(encoded)
                    .array();
        }
        return encoded;
    }

    private static byte[] longToBytes(long x) {
        ByteBuffer buffer;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
        {
            buffer = ByteBuffer.allocate(Long.BYTES);
        }
        else
        {
            buffer = ByteBuffer.allocate(64/8);
        }
        buffer.putLong(x);
        return buffer.array();
    }

    public static List<RlpType> asRlpValues(
            RawTransaction rawTransaction, Sign.SignatureData signatureData) {
        return rawTransaction.getTransaction().asRlpValues(signatureData);
    }
}
