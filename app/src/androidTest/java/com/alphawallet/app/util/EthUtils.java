package com.alphawallet.app.util;

import com.alphawallet.app.C;
import com.alphawallet.app.service.AWHttpServiceWaterfall;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import okhttp3.OkHttpClient;

/**
 * Created by JB on 4/09/2022.
 */
public abstract class EthUtils
{
    public static Web3j buildWeb3j(String url, long chainId)
    {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(C.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .connectTimeout(C.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(C.WRITE_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        AWHttpServiceWaterfall svs = new AWHttpServiceWaterfall(new String[]{url}, chainId, client,null, null, null, false);
        return Web3j.build(svs);
    }

    public static String calculateContractAddress(String account, long nonce)
    {
        byte[] addressAsBytes = org.web3j.utils.Numeric.hexStringToByteArray(account);
        byte[] calculatedAddressAsBytes =
                Hash.sha3(RlpEncoder.encode(
                        new RlpList(
                                RlpString.create(addressAsBytes),
                                RlpString.create((nonce)))));

        calculatedAddressAsBytes = Arrays.copyOfRange(calculatedAddressAsBytes,
                12, calculatedAddressAsBytes.length);
        return Keys.toChecksumAddress(org.web3j.utils.Numeric.toHexString(calculatedAddressAsBytes));
    }

    public static Single<BigInteger> getLastTransactionNonce(Web3j web3j, String walletAddress)
    {
        return Single.fromCallable(() -> {
            try
            {
                EthGetTransactionCount ethGetTransactionCount = web3j
                        .ethGetTransactionCount(walletAddress, DefaultBlockParameterName.PENDING)
                        .send();
                return ethGetTransactionCount.getTransactionCount();
            }
            catch (Exception e)
            {
                return BigInteger.ZERO;
            }
        });
    }

    public static void transferFunds(Web3j web3j, Credentials credentials, String targetAddr, BigDecimal ethAmount)
    {
        try
        {
            TransactionReceipt transactionReceipt = Transfer.sendFunds(
                    web3j, credentials, targetAddr,
                    ethAmount, Convert.Unit.ETHER).send();

            System.out.println("TX: " + transactionReceipt.getTransactionHash());
        }
        catch (Exception e)
        {
            //
        }
    }

    public static long deployContract(Web3j web3j, Credentials credentials, String contractCode)
    {
        long nonceReturn = 0;
        try
        {
            BigInteger nonce = getLastTransactionNonce(web3j, credentials.getAddress()).blockingGet();

            RawTransaction rawTransaction = RawTransaction.createContractTransaction(nonce,
                    BigInteger.valueOf(20000000000L), BigInteger.valueOf(6721975), BigInteger.ZERO,
                    contractCode);

            byte[] signedDeployTransaction = TransactionEncoder.signMessage(rawTransaction, credentials);

            EthSendTransaction raw = web3j
                    .ethSendRawTransaction(org.web3j.utils.Numeric.toHexString(signedDeployTransaction)).send();

            System.out.println("Deploy hash: " + raw.getTransactionHash());

            nonceReturn = nonce.longValue();
        }
        catch (Exception e)
        {
            //
        }

        return nonceReturn;
    }
}
