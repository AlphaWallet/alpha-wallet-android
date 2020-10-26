package com.alphawallet.attestation;

import static org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
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

public class SmartContract {
  private static final String ATTESTATION_CHECKING_CONTRACT = "0x8D653D288346921B9099E74E61e2fD8054689524";
  private static final String ZERO_ADDRESS = "0x0000000000000000000000000000000000000000";

  //test contract
  public boolean testEncoding(ProofOfExponent exp)
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
