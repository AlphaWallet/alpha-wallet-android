package com.alphawallet.app.entity.tokens;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionInput;
import com.alphawallet.app.entity.opensea.Asset;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.viewmodel.BaseViewModel;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by James on 3/10/2018.
 * Stormbird in Singapore
 */
public class ERC721Token extends Token implements Parcelable
{
    private List<Asset> tokenBalanceAssets;

    public ERC721Token(TokenInfo tokenInfo, List<Asset> balanceList, long blancaTime, String networkName, ContractType type) {
        super(tokenInfo, BigDecimal.ZERO, blancaTime, networkName, type);
        if (balanceList != null)
        {
            tokenBalanceAssets = balanceList;
        }
        else
        {
            tokenBalanceAssets = new ArrayList<>();
        }
        setInterfaceSpec(type);
    }

    @Override
    public List<Asset> getTokenAssets() {
        return tokenBalanceAssets;
    }

    @Override
    public void addAssetToTokenBalanceAssets(Asset asset) {
        for (Asset a : tokenBalanceAssets) //don't add the same assets twice (should this be a map?)
        {
            if (a.getTokenId().equalsIgnoreCase(asset.getTokenId()))
            {
                return;
            }
        }
        tokenBalanceAssets.add(asset);
    }

    @Override
    public Asset getAssetForToken(String tokenId) {
        for(Asset asset : tokenBalanceAssets)
        {
            if(asset.getTokenId().equals(tokenId))
            {
                return asset;
            }
        }
        return null;
    }

    private ERC721Token(Parcel in) {
        super(in);
        tokenBalanceAssets = new ArrayList<>();
        //read in the element list
        int size = in.readInt();
        for (; size > 0; size--)
        {
            Asset asset = in.readParcelable(Asset.class.getClassLoader());
            tokenBalanceAssets.add(asset);
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
        for (Asset asset : tokenBalanceAssets)
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
    public String getStringBalance() {
        return String.valueOf(tokenBalanceAssets.size());
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
    public void clickReact(BaseViewModel viewModel, Context context)
    {
        viewModel.showTokenList(context, this);
    }

    @Override
    public int getTicketCount()
    {
        return tokenBalanceAssets.size();
    }

    @Override
    public String getFullBalance()
    {
        boolean firstItem = true;
        StringBuilder sb = new StringBuilder();
        for (Asset item : tokenBalanceAssets)
        {
            if (!firstItem) sb.append(",");
            sb.append(item.getTokenId());
            firstItem = false;
        }
        return sb.toString();
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
        realmToken.setBalance(getFullBalance());
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
        for (Asset a : tokenBalanceAssets)
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
        if (!currentState.equalsIgnoreCase(getFullBalance())) return true;
        return false;
    }

    @Override
    public String convertValue(String value, int precision)
    {
        return value;
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
        for (Asset a : tokenBalanceAssets)
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
        return getTransferValueRaw(txInput).toString();
    }

    @Override
    public BigInteger getTransferValueRaw(TransactionInput txInput)
    {
        return BigInteger.ONE;
    }

    @Override
    public BigDecimal getBalanceRaw()
    {
        return new BigDecimal(getArrayBalance().size());
    }
}
