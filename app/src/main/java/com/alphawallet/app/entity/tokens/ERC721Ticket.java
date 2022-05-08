package com.alphawallet.app.entity.tokens;

import android.app.Activity;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.TicketRangeElement;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionInput;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.app.repository.EventResult;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.BaseViewModel;
import com.alphawallet.token.entity.TicketRange;

import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ERC721Ticket extends Token
{
    private final List<BigInteger> balanceArray;

    public ERC721Ticket(TokenInfo tokenInfo, List<BigInteger> balances, long blancaTime, String networkName, ContractType type) {
        super(tokenInfo, BigDecimal.ZERO, blancaTime, networkName, type);
        this.balanceArray = balances;
        group = TokenGroup.NFT;
    }

    public ERC721Ticket(TokenInfo tokenInfo, String balances, long blancaTime, String networkName, ContractType type) {
        super(tokenInfo, BigDecimal.ZERO, blancaTime, networkName, type);
        this.balanceArray = stringHexToBigIntegerList(balances);
        group = TokenGroup.NFT;
    }

    @Override
    public String getStringBalanceForUI(int scale) {
        return String.valueOf(getTokenCount());
    }

    @Override
    public boolean hasPositiveBalance() {
        return (getTokenCount() > 0);
    }

    @Override
    public String getFullBalance() {
        if (balanceArray == null) return "no tokens";
        else return Utils.bigIntListToString(balanceArray, true);
    }

    @Override
    public Map<BigInteger, NFTAsset> getTokenAssets() {
        Map<BigInteger, NFTAsset> assets = new HashMap<>();
        for (BigInteger tokenId : balanceArray)
        {
            assets.put(tokenId, new NFTAsset(tokenId));
        }
        return assets;
    }

    /**
     * Given a string of hex ticket ID's, reduce the length of the string to 'quantity' items
     *
     * @return
     */
    public List<BigInteger> pruneIDList(String idListStr, int quantity)
    {
        //convert to list
        List<BigInteger> idList = stringHexToBigIntegerList(idListStr);
        /* weiwu: potentially we can do this but I am not sure if
         * order is important*/
        //List<BigInteger> idList = Observable.fromArray(idListStr.split(","))
        //       .map(s -> Numeric.toBigInt(s)).toList().blockingGet();
        if (quantity >= idList.size()) return idList;
        List<BigInteger> pruneList = new ArrayList<>();
        for (int i = 0; i < quantity; i++) pruneList.add(idList.get(i));

        return pruneList;
    }

    @Override
    public int getTokenCount()
    {
        int count = 0;
        if (balanceArray != null)
        {
            for (BigInteger id : balanceArray)
            {
                if (id.compareTo(BigInteger.ZERO) != 0) count++;
            }
        }
        return count;
    }

    @Override
    public void setRealmBalance(RealmToken realmToken)
    {
        realmToken.setBalance(Utils.bigIntListToString(balanceArray, true));
    }

    @Override
    public void clickReact(BaseViewModel viewModel, Activity activity)
    {
        viewModel.showTokenList(activity, this);
    }

    @Override
    public int getContractType()
    {
        return R.string.ERC721T;
    }

    public Function getPassToFunction(BigInteger expiry, List<BigInteger> tokenIds, int v, byte[] r, byte[] s, String recipient)
    {
        return new Function(
                "passTo",
                Arrays.asList(new org.web3j.abi.datatypes.generated.Uint256(expiry),
                              getDynArray(tokenIds),
                              new org.web3j.abi.datatypes.generated.Uint8(v),
                              new org.web3j.abi.datatypes.generated.Bytes32(r),
                              new org.web3j.abi.datatypes.generated.Bytes32(s),
                              new org.web3j.abi.datatypes.Address(recipient)),
                Collections.emptyList());
    }

    @Override
    public Function getTransferFunction(String to, List<BigInteger> tokenIds) throws NumberFormatException
    {
        if (tokenIds.size() > 1)
        {
            throw new NumberFormatException("ERC721Ticket currently doesn't handle batch transfers.");
        }

        return new Function(
                "safeTransferFrom",
                Arrays.asList(
                        new org.web3j.abi.datatypes.Address(this.getWallet()),
                        new org.web3j.abi.datatypes.Address(to),
                        new Uint256(tokenIds.get(0))
                             ), Collections.emptyList());
    }

    //Can only be ERC721 ticket if created as ERC721Ticket type
    @Override
    public boolean contractTypeValid()
    {
        return true;
    }

    @Override
    public boolean hasArrayBalance()
    {
        return true;
    }

    @Override
    public List<BigInteger> getArrayBalance() { return getNonZeroArrayBalance(); }

    @Override
    public List<BigInteger> getNonZeroArrayBalance()
    {
        List<BigInteger> nonZeroValues = new ArrayList<>();
        for (BigInteger value : balanceArray) if (value.compareTo(BigInteger.ZERO) != 0 && !nonZeroValues.contains(value)) nonZeroValues.add(value);
        return nonZeroValues;
    }

    @Override
    public String convertValue(String prefix, EventResult vResult, int precision)
    {
        precision += 1;
        String value = (vResult != null) ? vResult.value : "";
        if (value.length() > precision)
        {
            return prefix + "1";
        }
        else
        {
            return "#" + value;
        }
    }

    @Override
    public boolean getIsSent(Transaction transaction)
    {
        return transaction.isNFTSent(getWallet());
    }

    @Override
    public boolean isERC721Ticket() { return true; }
    @Override
    public boolean isNonFungible() { return true; }

    @Override
    public boolean groupWithToken(TicketRange currentGroupingRange, TicketRangeElement newElement, long currentGroupTime)
    {
        //don't group any ERC721 tickets in the asset view
        return false;
    }

    @Override
    public void addAssetToTokenBalanceAssets(BigInteger tokenId, NFTAsset asset)
    {
        balanceArray.add(tokenId);
    }

    @Override
    public String getTransferValue(TransactionInput txInput, int precision)
    {
        return getTransferValueRaw(txInput).toString();
    }

    @Override
    public BigInteger getTransferValueRaw(TransactionInput txInput)
    {
        if (txInput != null && txInput.arrayValues.size() > 1)
        {
            return BigInteger.valueOf(txInput.arrayValues.size());
        }
        else
        {
            return BigInteger.ONE;
        }
    }

    @Override
    public BigDecimal getBalanceRaw()
    {
        return new BigDecimal(getArrayBalance().size());
    }

    @Override
    public List<Integer> getStandardFunctions()
    {
        return Arrays.asList(R.string.action_use, R.string.action_transfer);
    }
}