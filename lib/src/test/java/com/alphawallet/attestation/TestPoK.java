package com.alphawallet.attestation;

import com.alphawallet.attestation.IdentifierAttestation.AttestationType;

import org.junit.Assert;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

import static org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction;

public class TestPoK {

  @org.junit.Test
  public void TestSunshine() {
    AttestationCrypto crypto = new AttestationCrypto(new SecureRandom());
    ProofOfExponent pok = crypto.constructProof("hello", AttestationType.PHONE, BigInteger.TEN);
    Assert.assertTrue(pok.verify());
    testEncoding(pok);
    ProofOfExponent newPok = new ProofOfExponent(pok.getDerEncoding());
    Assert.assertTrue(newPok.verify());
    Assert.assertEquals(pok.getBase(), newPok.getBase());
    Assert.assertEquals(pok.getRiddle(), newPok.getRiddle());
    Assert.assertEquals(pok.getPoint(), newPok.getPoint());
    Assert.assertEquals(pok.getChallenge(), newPok.getChallenge());
    Assert.assertArrayEquals(pok.getDerEncoding(), newPok.getDerEncoding());

    ProofOfExponent newConstructor = new ProofOfExponent(pok.getBase(), pok.getRiddle(), pok.getPoint(), pok.getChallenge());
    Assert.assertArrayEquals(pok.getDerEncoding(), newConstructor.getDerEncoding());
  }

  @org.junit.Test
  public void TestNegative() {
    AttestationCrypto crypto = new AttestationCrypto(new SecureRandom());
    ProofOfExponent pok = crypto.constructProof("hello", AttestationType.PHONE, BigInteger.TEN);
    Assert.assertTrue(pok.verify());
    ProofOfExponent newPok;
    newPok = new ProofOfExponent(pok.getBase(), pok.getRiddle(), pok.getPoint(), pok.getChallenge().add(BigInteger.ONE));
    Assert.assertFalse(newPok.verify());
    newPok = new ProofOfExponent(pok.getBase(), pok.getRiddle(), pok.getPoint().multiply(new BigInteger("2")), pok.getChallenge());
    Assert.assertFalse(newPok.verify());
    newPok = new ProofOfExponent(pok.getBase().multiply(new BigInteger("2")), pok.getRiddle(), pok.getPoint(), pok.getChallenge());
    Assert.assertFalse(newPok.verify());
    newPok = new ProofOfExponent(pok.getBase(), pok.getRiddle().multiply(new BigInteger("2")), pok.getPoint(), pok.getChallenge());
    Assert.assertFalse(newPok.verify());
  }

  @org.junit.Test
  public void TestContract()
  {
    AttestationCrypto crypto = new AttestationCrypto(new SecureRandom());

    SecureRandom rand = new SecureRandom();

    for (int i = 0; i < 30; i++)
    {
      byte[] bytes = new byte[32];
      rand.nextBytes(bytes);
      BigInteger rVal = new BigInteger(bytes);
      ProofOfExponent pok = crypto.constructProof("hello", AttestationType.PHONE, rVal);
      Assert.assertTrue(pok.verify());
      Assert.assertTrue(testEncoding(pok));
    }

    //now check fail
    for (int i = 0; i < 5; i++)
    {
      byte[] bytes = new byte[32];
      rand.nextBytes(bytes);
      BigInteger rVal = new BigInteger(bytes);
      ProofOfExponent pok = crypto.constructProof("hello", AttestationType.PHONE, rVal);
      Assert.assertTrue(pok.verify());
      ProofOfExponent newPok = new ProofOfExponent(pok.getBase(), pok.getRiddle(), pok.getPoint(), pok.getChallenge().add(BigInteger.ONE));
      Assert.assertFalse(testEncoding(newPok));
    }
  }

  private static final String ATTESTATION_CHECKING_CONTRACT = "0x8D653D288346921B9099E74E61e2fD8054689524";
  private static final String ZERO_ADDRESS = "0x0000000000000000000000000000000000000000";

  //test contract
  private boolean testEncoding(ProofOfExponent exp)
  {
    Web3j web3j = getWeb3j();

    boolean result = false;

    try
    {
      Function function = checkEncoding(exp.getDerEncoding());
      String responseValue = callSmartContractFunction(web3j, function, ATTESTATION_CHECKING_CONTRACT);
      List<Type> responseValues = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());

      if (!responseValues.isEmpty())
      {
        if (!((boolean) responseValues.get(0).getValue()))
        {
          System.out.println("Check failed");
        }
        else
        {
          System.out.println("Check passed");
          result = true;
        }
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    return result;
  }

  private String callSmartContractFunction(Web3j web3j,
                                           Function function, String contractAddress)
  {
    String encodedFunction = FunctionEncoder.encode(function);

    try
    {
      org.web3j.protocol.core.methods.request.Transaction transaction
              = createEthCallTransaction(ZERO_ADDRESS, contractAddress, encodedFunction);
      EthCall response = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();

      return response.getValue();
    }
    catch (IOException e)
    {
      return null;
    }
    catch (Exception e)
    {
      e.printStackTrace();
      return null;
    }
  }

  private static Function checkEncoding(byte[] encoding) {
    return new Function(
            "decodeAttestation",
            Collections.singletonList(new DynamicBytes(encoding)),
            Collections.singletonList(new TypeReference<Bool>() {}));
  }


  private OkHttpClient buildClient()
  {
    return new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build();
  }

  private Web3j getWeb3j()
  {
    //Infura
    HttpService nodeService = new HttpService("https://kovan.infura.io/v3/da3717f25f824cc1baa32d812386d93f", buildClient(), false);
    return Web3j.build(nodeService);
  }
}
