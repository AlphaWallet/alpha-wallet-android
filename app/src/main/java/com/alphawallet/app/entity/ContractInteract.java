package com.alphawallet.app.entity;

import static com.alphawallet.app.repository.TokenRepository.callSmartContractFunction;

import android.text.TextUtils;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.service.IPFSService;
import com.alphawallet.app.util.Utils;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;

/**
 * Created by JB on 7/05/2022.
 */
public class ContractInteract
{
    private final Token token;
    protected static IPFSService client;

    public ContractInteract(Token token)
    {
        this.token = token;
    }

    public Single<String> getScriptFileURI()
    {
        return Single.fromCallable(() -> {
            String contractURI = callSmartContractFunction(token.tokenInfo.chainId, getScriptURI(), token.getAddress(), token.getWallet());
            return contractURI != null ? contractURI : "";
        }).observeOn(Schedulers.io());
    }

    private String loadMetaData(String tokenURI)
    {
        if (TextUtils.isEmpty(tokenURI)) return "";

        //check if this is direct metadata, some tokens do this
        if (Utils.isJson(tokenURI)) return tokenURI;

        setupClient();

        return client.getContent(tokenURI);
    }

    public NFTAsset fetchTokenMetadata(BigInteger tokenId)
    {
        //1. get TokenURI (check for non-standard URI - check "tokenURI" and "uri")
        String responseValue = callSmartContractFunction(token.tokenInfo.chainId, getTokenURI(tokenId), token.getAddress(), token.getWallet());
        if (responseValue == null) responseValue = callSmartContractFunction(token.tokenInfo.chainId, getTokenURI2(tokenId), token.getAddress(), token.getWallet());
        String metaData = loadMetaData(responseValue);
        if (!TextUtils.isEmpty(metaData))
        {
            return new NFTAsset(metaData);
        }
        else
        {
            return new NFTAsset();
        }
    }

    private Function getTokenURI(BigInteger tokenId)
    {
        return new Function("tokenURI",
                Arrays.asList(new Uint256(tokenId)),
                Arrays.asList(new TypeReference<Utf8String>() {}));
    }

    private Function getTokenURI2(BigInteger tokenId)
    {
        return new Function("uri",
                Arrays.asList(new Uint256(tokenId)),
                Arrays.asList(new TypeReference<Utf8String>() {}));
    }

    private static Function getScriptURI() {
        return new Function("scriptURI",
                Collections.emptyList(),
                Collections.singletonList(new TypeReference<Utf8String>() {}));
    }

    private static void setupClient()
    {
        if (client == null)
        {
            client = new IPFSService(
                    new OkHttpClient.Builder()
                    .connectTimeout(C.CONNECT_TIMEOUT*2, TimeUnit.SECONDS)
                    .readTimeout(C.READ_TIMEOUT*2, TimeUnit.SECONDS)
                    .writeTimeout(C.WRITE_TIMEOUT*2, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(false)
                    .build());
        }
    }
}
