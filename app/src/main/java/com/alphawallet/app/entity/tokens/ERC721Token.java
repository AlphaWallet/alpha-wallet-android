package com.alphawallet.app.entity.tokens;

import android.app.Activity;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionInput;
import com.alphawallet.app.entity.opensea.Asset;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.BaseViewModel;
import com.alphawallet.token.tools.Numeric;

import org.bouncycastle.jcajce.provider.digest.SHA3;
import org.json.JSONObject;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthCall;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import wallet.core.jni.Hash;

import static org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction;

/**
 * Created by James on 3/10/2018.
 * Stormbird in Singapore
 */
public class ERC721Token extends Token implements Parcelable
{
    private final Map<BigInteger, Asset> tokenBalanceAssets;
    private static OkHttpClient client;

    public ERC721Token(TokenInfo tokenInfo, Map<BigInteger, Asset> balanceList, BigDecimal balance, long blancaTime, String networkName, ContractType type) {
        super(tokenInfo, balance, blancaTime, networkName, type);
        if (balanceList != null)
        {
            tokenBalanceAssets = balanceList;
        }
        else
        {
            tokenBalanceAssets = new HashMap<>();
        }
        setInterfaceSpec(type);
    }

    @Override
    public Map<BigInteger, Asset> getTokenAssets() {
        return tokenBalanceAssets;
    }

    @Override
    public void addAssetToTokenBalanceAssets(Asset asset) {
        BigInteger tokenId = parseTokenId(asset.getTokenId());
        tokenBalanceAssets.put(tokenId, asset);
    }

    @Override
    public Asset getAssetForToken(String tokenIdStr)
    {
        return tokenBalanceAssets.get(parseTokenId(tokenIdStr));
    }

    private ERC721Token(Parcel in) {
        super(in);
        tokenBalanceAssets = new HashMap<>();
        //read in the element list
        int size = in.readInt();
        for (; size > 0; size--)
        {
            Asset asset = in.readParcelable(Asset.class.getClassLoader());
            tokenBalanceAssets.put(parseTokenId(asset.getTokenId()), asset);
        }
    }

    public static final Creator<ERC721Token> CREATOR = new Creator<ERC721Token>() {
        @Override
        public ERC721Token createFromParcel(Parcel in) {
            return new ERC721Token(in);
        }

        @Override
        public ERC721Token[] newArray(int size) {
            return new ERC721Token[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(tokenBalanceAssets.size());
        for (Asset asset : tokenBalanceAssets.values())
        {
            dest.writeParcelable(asset, flags);
        }
    }

    @Override
    public boolean independentUpdate()
    {
        return true;
    }

    @Override
    public int getContractType()
    {
        return R.string.erc721;
    }

    @Override
    public String getStringBalance()
    {
        if (balance.compareTo(BigDecimal.ZERO) > 0) { return balance.toString(); }
        else { return "0"; }
    }

    @Override
    public Function getTransferFunction(String to, List<BigInteger> tokenIds) throws NumberFormatException
    {
        if (tokenIds.size() > 1)
        {
            throw new NumberFormatException("ERC721Ticket can't handle batched transfers");
        }

        Function               function    = null;
        List<Type>             params;
        BigInteger             tokenIdBI   = tokenIds.get(0);
        List<TypeReference<?>> returnTypes = Collections.emptyList();
        if (tokenUsesLegacyTransfer())
        {
            params = Arrays.asList(new Address(to), new Uint256(tokenIdBI));
            function = new Function("transfer", params, returnTypes);
        }
        else
        {
            //function safeTransferFrom(address _from, address _to, uint256 _tokenId) external payable;
            params = Arrays.asList(new Address(getWallet()), new Address(to), new Uint256(tokenIdBI));
            function = new Function("safeTransferFrom", params, returnTypes);
        }
        return function;
    }

    @Override
    public void clickReact(BaseViewModel viewModel, Activity activity)
    {
        viewModel.showTokenList(activity, this);
    }

    @Override
    public int getTicketCount()
    {
        return tokenBalanceAssets.size();
    }

    @Override
    public String getFullBalance()
    {
        return balance.toString();
    }

    @Override
    public boolean isToken() {
        return false;
    }

    @Override
    public boolean hasArrayBalance()
    {
        return true;
    }

    @Override
    public boolean hasPositiveBalance()
    {
        return tokenBalanceAssets != null && tokenBalanceAssets.size() > 0;
    }

    @Override
    public void setRealmBalance(RealmToken realmToken)
    {
        realmToken.setBalance(balance.toString());
    }

    public boolean isERC721() { return true; }
    public boolean isNonFungible() { return true; }

    /**
     * This is a list of legacy contracts which are known to use the old ERC721 source,
     * which only had 'transfer' as the transfer function.
     * @return
     */
    private boolean tokenUsesLegacyTransfer()
    {
        switch (tokenInfo.address.toLowerCase())
        {
            case "0x06012c8cf97bead5deae237070f9587f8e7a266d":
            case "0xabc7e6c01237e8eef355bba2bf925a730b714d5f":
            case "0x71c118b00759b0851785642541ceb0f4ceea0bd5":
            case "0x16baf0de678e52367adc69fd067e5edd1d33e3bf":
            case "0x7fdcd2a1e52f10c28cb7732f46393e297ecadda1":
                return true;

            default:
                return false;
        }
    }

    @Override
    public List<BigInteger> getArrayBalance()
    {
        List<BigInteger> balanceAsArray = new ArrayList<>();
        for (Asset a : tokenBalanceAssets.values())
        {
            try
            {
                BigInteger tokenIdBI = new BigInteger(a.getTokenId());
                balanceAsArray.add(tokenIdBI);
            }
            catch (NumberFormatException e)
            {
                //
            }
        }

        return balanceAsArray;
    }

    @Override
    public boolean checkRealmBalanceChange(RealmToken realmToken)
    {
        if (contractType == null || contractType.ordinal() != realmToken.getInterfaceSpec()) return true;
        String currentState = realmToken.getBalance();
        if (currentState == null) return true;
        if (lastTxTime > realmToken.getLastTxTime()) return true;
        if (!currentState.equals(balance.toString())) return true;
        //check balances
        for (Asset a : tokenBalanceAssets.values())
        {
            if (!a.needsLoading() && !a.requiresReplacement()) return true;
        }
        return false;
    }

    @Override
    public String convertValue(String prefix, String value, int precision)
    {
        precision++;
        if (value.length() > precision)
        {
            return prefix + "1";
        }
        else
        {
            return "#" + value;
        }
    }

    /**
     * Returns false if the Asset balance appears to be entries with only TokenId - indicating an ERC721Ticket
     * @return
     */
    @Override
    public boolean checkBalanceType()
    {
        boolean onlyHasTokenId = true;
        //if elements contain asset with only assetId then most likely this is a ticket.
        for (Asset a : tokenBalanceAssets.values())
        {
            if (!a.hasIdOnly()) onlyHasTokenId = false;
        }

        return tokenBalanceAssets.size() == 0 || !onlyHasTokenId;
    }

    public String getTransferID(Transaction tx)
    {
        if (tx.transactionInput != null && tx.transactionInput.miscData.size() > 0)
        {
            String tokenHex = tx.transactionInput.miscData.get(0);
            if (tokenHex.length() > 0)
            {
                BigInteger id = new BigInteger(tokenHex, 16);
                tokenHex = id.toString();
                if (tokenHex.length() < 7)
                {
                    return id.toString(16);
                }
            }
        }

        return "0";
    }

    @Override
    public String getTransferValue(TransactionInput txInput, int precision)
    {
        precision++;
        //return the tokenId from the transfer if possible
        try
        {
            BigInteger tokenId = new BigInteger(txInput.miscData.get(0), 16);
            String tokenIdStr = tokenId.toString();
            if (tokenIdStr.length() > precision)
            {
                return "1";
            }
            else
            {
                return "#" + tokenId.toString();
            }
        }
        catch (Exception e)
        {
            //
        }

        return getTransferValueRaw(txInput).toString();
    }

    @Override
    public BigInteger getTransferValueRaw(TransactionInput txInput)
    {
        //return the tokenId from the transfer if possible
        try
        {
            return new BigInteger(txInput.miscData.get(0), 16);
        }
        catch (Exception e)
        {
            //
        }

        return BigInteger.ONE;
    }

    @Override
    public BigDecimal getBalanceRaw()
    {
        return balance;
    }

    private BigInteger parseTokenId(String tokenIdStr)
    {
        BigInteger tokenId;
        try
        {
            tokenId = new BigInteger(tokenIdStr);
        }
        catch (Exception e)
        {
            tokenId = BigInteger.ZERO;
        }

        return tokenId;
    }

    @Override
    public void removeBalance(String tokenID)
    {
        tokenBalanceAssets.remove(parseTokenId(tokenID));
    }

    private static String getHashBalance(ERC721Token token)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try
        {
            for (Asset item : token.tokenBalanceAssets.values())
            {
                baos.write(item.getTokenId().getBytes());
                if (item.getImagePreviewUrl() != null) baos.write(item.getImagePreviewUrl().getBytes());
                if (item.getImageOriginalUrl() != null) baos.write(item.getImageOriginalUrl().getBytes());
                if (item.getName() != null) baos.write(item.getName().getBytes());
                if (item.getDescription() != null) baos.write(item.getDescription().getBytes());
                if (item.getTraits() != null) baos.write(item.getTraits().hashCode());
            }
        }
        catch (Exception e)
        {
            return String.valueOf(token.tokenBalanceAssets.size());
        }

        return Numeric.toHexString(Hash.keccak256(baos.toByteArray()));
    }

    @Override
    public Asset fetchTokenMetadata(BigInteger tokenId)
    {
        //1. get TokenURI (check for non-standard URI - check "tokenURI" and "uri")
        Web3j web3j = TokenRepository.getWeb3jService(tokenInfo.chainId);
        String responseValue = callSmartContractFunction(web3j, getTokenURI(tokenId), getAddress(), getWallet());
        if (responseValue == null) responseValue = callSmartContractFunction(web3j, getTokenURI2(tokenId), getAddress(), getWallet());
        JSONObject metaData = loadMetaData(responseValue);
        if (metaData != null)
        {
            return Asset.fromMetaData(metaData, tokenId, this);
        }
        else
        {
            return new Asset(tokenId);
        }
    }

    private String callSmartContractFunction(Web3j web3j,
                                             Function function, String contractAddress, String walletAddr)
    {
        String encodedFunction = FunctionEncoder.encode(function);

        try
        {
            org.web3j.protocol.core.methods.request.Transaction transaction
                    = createEthCallTransaction(walletAddr, contractAddress, encodedFunction);
            EthCall response = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();

            List<Type> responseValues = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());

            if (!responseValues.isEmpty())
            {
                return responseValues.get(0).getValue().toString();
            }
        }
        catch (Exception e)
        {
            //
        }

        return null;
    }

    private Function getTokenURI(BigInteger tokenId)
    {
        return new Function("tokenURI",
                Arrays.asList(new Uint256(tokenId)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
    }

    private Function getTokenURI2(BigInteger tokenId)
    {
        return new Function("uri",
                Arrays.asList(new Uint256(tokenId)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
    }

    private JSONObject loadMetaData(String tokenURI)
    {
        JSONObject metaData = null;
        setupClient();

        try
        {
            Request request = new Request.Builder()
                    .url(Utils.parseIPFS(tokenURI))
                    .get()
                    .build();

            okhttp3.Response response = client.newCall(request).execute();
            metaData = new JSONObject(response.body().string());
        }
        catch (Exception e)
        {
            //
        }

        return metaData;
    }

    private static void setupClient()
    {
        if (client == null)
        {
            client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();
        }
    }
}
