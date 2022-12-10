package com.alphawallet.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.MessagePair;
import com.alphawallet.app.entity.SignaturePair;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionData;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.cryptokeys.SignatureFromKey;
import com.alphawallet.app.interact.SignatureGenerateInteract;
import com.alphawallet.app.repository.TransactionRepositoryType;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.token.entity.SalesOrderMalformed;
import com.alphawallet.token.entity.Signable;

import org.junit.Test;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Single;
import io.realm.Realm;

public class QRSelectionTest
{
    @Inject
    TransactionRepositoryType transactionRepository;

    SignatureGenerateInteract signatureGenerateInteract;


    final String CONTRACT_ADDR = "0xbc9a1026a4bc6f0ba8bbe486d1d09da5732b39e4";


    private ECKeyPair testKey;

    private class QREncoding
    {
        List<BigInteger> indices = new ArrayList<>();
        SignaturePair sigPair;
    }

    @Test
    public void QRSelectionTest()
    {
        //Use a different key each time
        SecureRandom sr = new SecureRandom();
        byte[] keySeed = new byte[32];
        sr.nextBytes(keySeed);
        testKey = ECKeyPair.create(keySeed);

        transactionRepository = new TransactionRepositoryType()
        {
            @Override
            public Single<TransactionData> createTransactionWithSig(Wallet from, String toAddress, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, long nonce, byte[] data, long chainId)
            {
                return null;
            }

            @Override
            public Single<TransactionData> create1559TransactionWithSig(Wallet from, String toAddress, BigInteger subunitAmount, BigInteger gasLimit, BigInteger gasPremium, BigInteger gasMax, long nonce, byte[] data, long chainId) {
                return null;
            }

            @Override
            public Single<TransactionData> getSignatureForTransaction(Wallet from, String toAddress, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, long nonce, byte[] data, long chainId)
            {
                return null;
            }

            @Override
            public Single<SignatureFromKey> getSignature(Wallet wallet, Signable sign)
            {
                return null;
            }

            @Override
            public Single<byte[]> getSignatureFast(Wallet wallet, String pass, byte[] message)
            {
                return Single.fromCallable(() -> {
                    //sign using the local key
                    Sign.SignatureData sigData = Sign.signMessage(message, testKey);

                    byte[] sig = new byte[65];

                    try
                    {
                        System.arraycopy(sigData.getR(), 0, sig, 0, 32);
                        System.arraycopy(sigData.getS(), 0, sig, 32, 32);
                        System.arraycopy(sigData.getV(), 0, sig, 64, 1);
                    }
                    catch (IndexOutOfBoundsException e)
                    {
                        throw new SalesOrderMalformed("Signature shorter than expected 256");
                    }

                    return sig;
                });
            }

            @Override
            public Transaction fetchCachedTransaction(String walletAddr, String hash)
            {
                return null;
            }

            @Override
            public long fetchTxCompletionTime(String walletAddr, String hash)
            {
                return 0;
            }

            @Override
            public Single<String> resendTransaction(Wallet from, String to, BigInteger subunitAmount, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, byte[] data, long chainId)
            {
                return Single.fromCallable(() -> { return ""; });
            }

            @Override
            public Single<ActivityMeta[]> fetchCachedTransactionMetas(Wallet wallet,
                                                                      List<Long> networkFilters, long fetchTime, int fetchLimit)
            {
                return null;
            }

            @Override
            public Single<ActivityMeta[]> fetchCachedTransactionMetas(Wallet wallet, long chainId,
                                                                      String tokenAddress,
                                                                      int historyCount)
            {
                return null;
            }

            @Override
            public Single<ActivityMeta[]> fetchEventMetas(Wallet wallet,
                                                          List<Long> networkFilters)
            {
                return null;
            }

            @Override
            public Realm getRealmInstance(Wallet wallet)
            {
                return null;
            }

            @Override
            public RealmAuxData fetchCachedEvent(String walletAddress, String eventKey)
            {
                return null;
            }

            @Override
            public void restartService()
            {

            }
        };

        signatureGenerateInteract = new SignatureGenerateInteract(null)
        {
            @Override
            //TODO: Sign message here not in the additional field
            public Single<MessagePair> getMessage(List<BigInteger> indexList, String contract, ContractType contractType)
            {
                return Single.fromCallable(() -> {
                    String selectionStr = SignaturePair.generateSelection(indexList);
                    long currentTime = System.currentTimeMillis();
                    long minsT = currentTime / (30 * 1000);
                    int minsTime = (int) minsT;
                    String plainMessage = selectionStr + "," + minsTime + "," + contract.toLowerCase();  //This is the plain text message that gets signed
                    return new MessagePair(selectionStr, plainMessage);
                });
            }
        };

        List<QREncoding> qrList = new ArrayList<>();

        //test key address
        String testAddress = "0x" + Keys.getAddress(testKey.getPublicKey());

        //generate all ticket redeem combos up to index 256, then check signature and regenerate the selection
        final int indicesCount = 8 * 2;
        final int combinations = (int) Math.pow(2, indicesCount);

        for (int i = 1; i < combinations; i += 1) // pick all the combinations, even though it slows the test down
        {
            QREncoding qr = new QREncoding();
            qrList.add(qr);

            //generate an entry
            //1 generate the indices
            //consume the bitfield
            BigInteger k = BigInteger.valueOf(i);
            int radix = k.getLowestSetBit();
            while (!k.equals(BigInteger.ZERO))
            {
                if (k.testBit(radix))
                {
                    qr.indices.add(BigInteger.valueOf(radix + 1));
                    k = k.clearBit(radix);
                }
                radix++;
            }

            MessagePair messagePair = signatureGenerateInteract
                    .getMessage(qr.indices, CONTRACT_ADDR, ContractType.ERC875).blockingGet();

            byte[] sig = transactionRepository
                    .getSignatureFast(null, "hackintosh", messagePair.message.getBytes()).blockingGet();

            qr.sigPair = new SignaturePair(messagePair.selection, sig, messagePair.message);
        }

        try
        {
            //now check we can recover all the selections and their signings
            for (QREncoding qr : qrList)
            {
                //form the QR string
                String qrMessage = qr.sigPair.formQRMessage();

                // |
                // |       Imagine this string is being encoded on one phone, and scanned by another
                // v

                //read time from message
                long localTime = getTimeFromMessage(qr);

                //now using this alone recompose the selection and check the signature and contract
                //extract the sig pair
                SignaturePair sPair = new SignaturePair(qrMessage, String.valueOf(localTime), CONTRACT_ADDR);
                List<Integer> selectionRecreate = SignaturePair.buildIndexList(sPair.selectionStr);
                Sign.SignatureData sigData = sigFromBase64Fix(sPair.signature);

                //check the signature corresponds to the test address
                String addressHex = "0x" + ecRecoverAddress(sPair.message.getBytes(), sigData);
                // compare BigInteger and Integer. this is quicker than using stream->collect
                assertEquals(qr.indices.toString(), selectionRecreate.toString());
                assertTrue(addressHex.equals(testAddress));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private long getTimeFromMessage(QREncoding qr)
    {
        String[] split = qr.sigPair.message.split(",");
        String timeStr = split[1];
        return Long.parseLong(timeStr);
    }

    /*
     * This is a reference decoder for the new ERC721 token QR encoding
     */
    public static void decodeTest(String s)
    {
        try (InputStream bas  = new ByteArrayInputStream(s.getBytes()))
        {
            byte[] protocolBuf = new byte[1];
            byte[] lengthBuf = new byte[2];
            bas.read(protocolBuf);
            bas.read(lengthBuf);
            int tokenIdlength = Integer.valueOf(new String(lengthBuf));
            byte[] tokenIdBuf = new byte[tokenIdlength];
            bas.read(tokenIdBuf);
            BigInteger tokenId = new BigInteger(new String(tokenIdBuf));
            byte[] signatureBuf = new byte[bas.available()];
            bas.read(signatureBuf); //signature as string bytes
            BigInteger signatureBi = new BigInteger(new String(signatureBuf));

            System.out.println("ctest[TokenID2: 0x" + tokenId.toString(16));
            System.out.println("ctest[Sig2    : 0x" + signatureBi.toString(16));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private String ecRecoverAddress(byte[] data, Sign.SignatureData signature) //get the hex string address from the sig and data
    {
        String address = "";
        try
        {
            BigInteger recoveredKey = Sign.signedMessageToKey(data, signature); //get embedded address
            address = Keys.getAddress(recoveredKey);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return address;
    }

    public static Sign.SignatureData sigFromBase64Fix(byte[] sig) {
        byte subv = (byte) (sig[64] + 27);
        if (subv > 30)
            subv -= 27;

        byte[] subrRev = Arrays.copyOfRange(sig, 0, 32);
        byte[] subsRev = Arrays.copyOfRange(sig, 32, 64);

        return new Sign.SignatureData(subv, subrRev, subsRev);
    }
}
