package com.alphawallet.app.entity.tokens;

import static com.alphawallet.app.util.Utils.parseTokenId;
import static org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction;

import android.app.Activity;
import android.util.Pair;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionInput;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.viewmodel.BaseViewModel;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by James on 3/10/2018.
 * Stormbird in Singapore
 */
public class ERC721Token extends Token
{
    private final Map<BigInteger, NFTAsset> tokenBalanceAssets;

    public ERC721Token(TokenInfo tokenInfo, Map<BigInteger, NFTAsset> balanceList, BigDecimal balance, long blancaTime, String networkName, ContractType type) {
        super(tokenInfo, balance, blancaTime, networkName, type);
        if (balanceList != null)
        {
            tokenBalanceAssets = balanceList;
        }
        else
        {
            tokenBalanceAssets = new ConcurrentHashMap<>();
        }
        setInterfaceSpec(type);
        group = TokenGroup.NFT;
    }

    @Override
    public Map<BigInteger, NFTAsset> getTokenAssets() {
        return tokenBalanceAssets;
    }

    @Override
    public void addAssetToTokenBalanceAssets(BigInteger tokenId, NFTAsset asset) {
        tokenBalanceAssets.put(tokenId, asset);
    }

    @Override
    public NFTAsset getAssetForToken(String tokenIdStr)
    {
        return tokenBalanceAssets.get(parseTokenId(tokenIdStr));
    }

    @Override
    public NFTAsset getAssetForToken(BigInteger tokenId)
    {
        return tokenBalanceAssets.get(tokenId);
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
    public byte[] getTransferBytes(String to, ArrayList<Pair<BigInteger, NFTAsset>> transferData) throws NumberFormatException
    {
        if (transferData == null || transferData.size() != 1) return Numeric.hexStringToByteArray("0x");
        Function txFunc = getTransferFunction(to, new ArrayList<>(Collections.singleton(transferData.get(0).first)));
        String encodedFunction = FunctionEncoder.encode(txFunc);
        return Numeric.hexStringToByteArray(Numeric.cleanHexPrefix(encodedFunction));
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
    public int getTokenCount()
    {
        return balance.intValue();
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
        return new ArrayList<>(tokenBalanceAssets.keySet());
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
        for (NFTAsset a : tokenBalanceAssets.values())
        {
            if (!a.needsLoading() && !a.requiresReplacement()) return true;
        }
        return false;
    }

    @Override
    public boolean checkBalanceChange(Token oldToken)
    {
        if (super.checkBalanceChange(oldToken)) return true;
        if (getTokenAssets().size() != oldToken.getTokenAssets().size()) return true;
        for (BigInteger tokenId : tokenBalanceAssets.keySet())
        {
            NFTAsset newAsset = tokenBalanceAssets.get(tokenId);
            NFTAsset oldAsset = oldToken.getAssetForToken(tokenId);
            if (newAsset == null || oldAsset == null || !newAsset.equals(oldAsset))
            {
                return true;
            }
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
        for (NFTAsset a : tokenBalanceAssets.values())
        {
            if (!a.isBlank()) onlyHasTokenId = false;
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

    /**
     * Create a live balance of assets. Only need to query assets that have dropped out
     * From where this is called, the current assets are those loaded from opensea call
     * If there is a token that previously was there, but now isn't, it could be because
     * the opensea call was split or that the owner transferred the token
     * @param assetMap Loaded Assets from Realm
     * @return map of currently known live assets
     */
    @Override
    public Map<BigInteger, NFTAsset> queryAssets(Map<BigInteger, NFTAsset> assetMap)
    {
        final Web3j web3j = TokenRepository.getWeb3jService(tokenInfo.chainId);

        //check all tokens in this contract
        assetMap.putAll(tokenBalanceAssets);

        //now check balance for all tokenIds (note that ERC1155 has a batch balance check, ERC721 does not)
        for (Map.Entry<BigInteger, NFTAsset> entry : assetMap.entrySet())
        {
            BigInteger checkId = entry.getKey();
            NFTAsset checkAsset = entry.getValue();

            //check balance
            String owner = callSmartContractFunction(web3j, ownerOf(checkId), getAddress(), getWallet());
            if (owner == null) //play it safe. If there's no 'ownerOf' for an ERC721, it's something custom like ENS
            {
                checkAsset.setBalance(BigDecimal.ONE);
            }
            else if (owner.toLowerCase().equals(getWallet()))
            {
                checkAsset.setBalance(BigDecimal.ONE);
            }
            else
            {
                checkAsset.setBalance(BigDecimal.ZERO);
            }

            //add back into asset map
            tokenBalanceAssets.put(checkId, checkAsset);
        }

        return tokenBalanceAssets;
    }

    @Override
    public List<BigInteger> getChangeList(Map<BigInteger, NFTAsset> assetMap)
    {
        //detect asset removal
        List<BigInteger> oldAssetIdList = new ArrayList<>(assetMap.keySet());
        oldAssetIdList.removeAll(tokenBalanceAssets.keySet());

        List<BigInteger> changeList = new ArrayList<>(oldAssetIdList);

        //Now detect differences or new tokens
        for (BigInteger tokenId : tokenBalanceAssets.keySet())
        {
            NFTAsset newAsset = tokenBalanceAssets.get(tokenId);
            NFTAsset oldAsset = assetMap.get(tokenId);

            if (oldAsset == null || newAsset.hashCode() != oldAsset.hashCode())
            {
                changeList.add(tokenId);
            }
        }

        return changeList;
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

    private static Function ownerOf(BigInteger token) {
        return new Function(
                "ownerOf",
                Collections.singletonList(new Uint256(token)),
                Collections.singletonList(new TypeReference<Address>() {}));
    }

    @Override
    public List<Integer> getStandardFunctions()
    {
        return Arrays.asList(R.string.action_transfer);
    }
}
