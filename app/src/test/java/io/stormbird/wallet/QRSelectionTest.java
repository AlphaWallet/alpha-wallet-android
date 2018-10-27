package io.stormbird.wallet;

import android.util.Log;

import org.junit.Test;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;

import io.stormbird.wallet.entity.MessagePair;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.SignaturePair;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.TokenTransaction;
import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.CreateTransactionInteract;
import io.stormbird.wallet.interact.SignatureGenerateInteract;
import io.stormbird.wallet.repository.TransactionRepositoryType;
import io.stormbird.wallet.repository.WalletRepositoryType;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.stormbird.token.entity.SalesOrderMalformed;

import static org.junit.Assert.assertTrue;

public class QRSelectionTest
{
    @Inject
    TransactionRepositoryType transactionRepository;

    SignatureGenerateInteract signatureGenerateInteract;


    final String CONTRACT_ADDR  = "0xbc9a1026a4bc6f0ba8bbe486d1d09da5732b39e4";


    private ECKeyPair testKey;

    private class QREncoding
    {
        List<Integer> indices = new ArrayList<>();
        SignaturePair sigPair;
        long localTime;
    }

    @Test
    public void QRSelectionTest()
    {
        //roll a new key
        testKey = ECKeyPair.create("Test Key".getBytes());

        transactionRepository = new TransactionRepositoryType() {

            @Override
            public Observable<Transaction[]> fetchCachedTransactions(NetworkInfo network, Wallet wallet)
            {
                return null;
            }

            @Override
            public Observable<Transaction[]> fetchTransaction(Wallet wallet) {
                return null;
            }

            @Override
            public Observable<Transaction[]> fetchNetworkTransaction(Wallet wallet, long lastBlock)
            {
                return null;
            }

            @Override
            public Observable<TokenTransaction[]> fetchTokenTransaction(Wallet wallet, Token token, long lastBlock) {
                return null;
            }

            @Override
            public Maybe<Transaction> findTransaction(Wallet wallet, String transactionHash) {
                return null;
            }

            @Override
            public Single<String> createTransaction(Wallet from, String toAddress, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data, String password) {
                return null;
            }

            @Override
            public Single<String> createTransaction(Wallet from, BigInteger gasPrice, BigInteger gasLimit, String data, String password)
            {
                return null;
            }

            @Override
            public Single<byte[]> getSignature(Wallet wallet, byte[] message, String password) {
                return null;
            }

            @Override
            public Single<byte[]> getSignatureFast(Wallet wallet, byte[] message, String pass) {
                return Single.fromCallable(() -> {
                    //sign using the local key
                    Sign.SignatureData sigData = Sign.signMessage(message, testKey);

                    byte[] sig = new byte[65];

                    try {
                        System.arraycopy(sigData.getR(), 0, sig, 0, 32);
                        System.arraycopy(sigData.getS(), 0, sig, 32, 32);
                        sig[64] = (byte) (int) sigData.getV();
                    }
                    catch (IndexOutOfBoundsException e)
                    {
                        throw new SalesOrderMalformed("Signature shorter than expected 256");
                    }

                    return sig;
                });
            }

            @Override
            public void unlockAccount(Wallet signer, String signerPassword) throws Exception {

            }

            @Override
            public void lockAccount(Wallet signer, String signerPassword) throws Exception {

            }

            @Override
            public Single<Transaction[]> storeTransactions(NetworkInfo networkInfo, Wallet wallet, Transaction[] txList)
            {
                return null;
            }
        };

        signatureGenerateInteract = new SignatureGenerateInteract(null)
        {
            @Override
            //TODO: Sign message here not in the additional field
            public Single<MessagePair> getMessage(List<Integer> indexList, String contract) {
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
        final int indicesCount = 8*2;
        final int combinations = (int)Math.pow(2, indicesCount);

        for (int i = 1; i < combinations; i += 1) // pick all the combinations, even though it slows the test down
        {
            QREncoding qr = new QREncoding();
            qrList.add(qr);
            qr.localTime = System.currentTimeMillis() / (30*1000);
            //generate an entry
            //1 generate the indices
            //consume the bitfield
            BigInteger k = BigInteger.valueOf(i);
            int radix = k.getLowestSetBit();
            while (!k.equals(BigInteger.ZERO))
            {
                if (k.testBit(radix))
                {
                    qr.indices.add(radix+1);
                    k = k.clearBit(radix);
                }
                radix++;
            }

            MessagePair messagePair = signatureGenerateInteract
                    .getMessage(qr.indices, CONTRACT_ADDR).blockingGet();

            //now sign
            byte[] sig = transactionRepository
                    .getSignatureFast(null, messagePair.message.getBytes(), "hackintosh").blockingGet();

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

                long localTime = qr.localTime;

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

    public static Sign.SignatureData sigFromBase64Fix(byte[] sig) throws Exception
    {
        byte   subv = (byte)(sig[64] + 27);
        if (subv > 30) subv -= 27;

        byte[] subrRev = Arrays.copyOfRange(sig, 0, 32);
        byte[] subsRev = Arrays.copyOfRange(sig, 32, 64);
        Sign.SignatureData ecSig = new Sign.SignatureData(subv, subrRev, subsRev);

        return ecSig;
    }
}
