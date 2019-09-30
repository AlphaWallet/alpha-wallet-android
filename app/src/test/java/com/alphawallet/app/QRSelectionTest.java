package com.alphawallet.app;

import org.junit.Test;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.MessagePair;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.SignaturePair;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.TokenInfo;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionData;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.interact.SignatureGenerateInteract;
import com.alphawallet.app.repository.TransactionRepositoryType;
import io.reactivex.Observable;
import io.reactivex.Single;
import com.alphawallet.token.entity.SalesOrderMalformed;

import static org.junit.Assert.assertTrue;

public class QRSelectionTest
{
    @Inject
    TransactionRepositoryType transactionRepository;

    SignatureGenerateInteract signatureGenerateInteract;


    final String CONTRACT_ADDR = "0xbc9a1026a4bc6f0ba8bbe486d1d09da5732b39e4";


    private ECKeyPair testKey;

    private class QREncoding
    {
        List<Integer> indices = new ArrayList<>();
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
            public Observable<Transaction[]> fetchCachedTransactions(Wallet wallet, int maxTransactions)
            {
                return null;
            }

            @Override
            public Observable<Transaction[]> fetchNetworkTransaction(NetworkInfo network, String tokenAddress, long lastBlock, String userAddress)
            {
                return null;
            }

            @Override
            public Single<String> createTransaction(Wallet from, String toAddress, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data, int chainId)
            {
                return null;
            }

            @Override
            public Single<String> createTransaction(Wallet from, BigInteger gasPrice, BigInteger gasLimit, String data, int chainId)
            {
                return null;
            }

            @Override
            public Single<TransactionData> createTransactionWithSig(Wallet from, String toAddress, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data, int chainId)
            {
                return null;
            }

            @Override
            public Single<TransactionData> createTransactionWithSig(Wallet from, BigInteger gasPrice, BigInteger gasLimit, String data, int chainId)
            {
                return null;
            }

            @Override
            public Single<byte[]> getSignature(Wallet wallet, byte[] message, int chainId)
            {
                return null;
            }

            @Override
            public Single<byte[]> getSignatureFast(Wallet wallet, String pass, byte[] message, int chainId)
            {
                return Single.fromCallable(() -> {
                    //sign using the local key
                    Sign.SignatureData sigData = Sign.signMessage(message, testKey);

                    byte[] sig = new byte[65];

                    try
                    {
                        System.arraycopy(sigData.getR(), 0, sig, 0, 32);
                        System.arraycopy(sigData.getS(), 0, sig, 32, 32);
                        sig[64] = sigData.getV();//[0];
                    }
                    catch (IndexOutOfBoundsException e)
                    {
                        throw new SalesOrderMalformed("Signature shorter than expected 256");
                    }

                    return sig;
                });
            }

            @Override
            public Single<Transaction[]> storeTransactions(Wallet wallet, Transaction[] txList)
            {
                return null;
            }

            @Override
            public Single<Transaction[]> fetchTransactionsFromStorage(Wallet wallet, Token token, int count)
            {
                return null;
            }

            @Override
            public Single<ContractType> queryInterfaceSpec(String address, TokenInfo tokenInfo)
            {
                return null;
            }

            @Override
            public Transaction fetchCachedTransaction(String walletAddr, String hash)
            {
                return null;
            }
        };

        signatureGenerateInteract = new SignatureGenerateInteract(null)
        {
            @Override
            //TODO: Sign message here not in the additional field
            public Single<MessagePair> getMessage(List<Integer> indexList, String contract)
            {
                return Single.fromCallable(() -> {
                    String selectionStr = SignaturePair.generateSelection(indexList);
                    long currentTime = System.currentTimeMillis();
                    long minsT = currentTime / (30 * 1000);
                    int minsTime = (int) minsT;
                    String plainMessage = selectionStr + "," + String.valueOf(minsTime) + "," + contract.toLowerCase();  //This is the plain text message that gets signed
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
                    qr.indices.add(radix + 1);
                    k = k.clearBit(radix);
                }
                radix++;
            }

            MessagePair messagePair = signatureGenerateInteract
                    .getMessage(qr.indices, CONTRACT_ADDR).blockingGet();

            byte[] sig = transactionRepository
                    .getSignatureFast(null, "hackintosh", messagePair.message.getBytes(), 1).blockingGet();

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
                assertTrue(selectionRecreate.equals(qr.indices));
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
