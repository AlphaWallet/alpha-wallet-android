package com.alphawallet.app.entity.tokens;

import android.app.Activity;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.TicketRangeElement;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionInput;
import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.BaseViewModel;
import com.alphawallet.token.entity.TicketRange;

import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by James on 27/01/2018.  It might seem counter intuitive
 * but here Ticket refers to a container of an asset class here, not
 * the right to seat somewhere in the venue. Therefore, there
 * shouldn't be List<Ticket> To understand this, imagine that one says
 * "I have two cryptocurrencies: Ether and Bitcoin, each amounts to a
 * hundred", and he pauses and said, "I also have two indices: FIFA
 * and Formuler-one, which, too, amounts to a hundred each".
 */

public class Ticket extends Token
{
    private final List<BigInteger> balanceArray;
    private boolean isMatchedInXML = false;

    public Ticket(TokenInfo tokenInfo, List<BigInteger> balances, long blancaTime, String networkName, ContractType type) {
        super(tokenInfo, BigDecimal.ZERO, blancaTime, networkName, type);
        this.balanceArray = balances;
        balance = balanceArray != null ? BigDecimal.valueOf(balanceArray.size()) : BigDecimal.ZERO;
        group = TokenGroup.NFT;
    }

    public Ticket(TokenInfo tokenInfo, String balances, long blancaTime, String networkName, ContractType type) {
        super(tokenInfo, BigDecimal.ZERO, blancaTime, networkName, type);
        this.balanceArray = stringHexToBigIntegerList(balances);
        balance = BigDecimal.valueOf(balanceArray.size());
        group = TokenGroup.NFT;
    }

    @Override
    public String getStringBalance() {
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
        if (contractType == ContractType.ERC875_LEGACY)
        {
            return R.string.erc875legacy;
        }
        else
        {
            return R.string.erc875;
        }
    }

    /*************************************
     *
     * Conversion functions used for manipulating indices
     *
     */


    /**
     * Convert a list of TicketID's into an Index list corresponding to those indices
     * @param ticketIds
     * @return
     */
    public List<BigInteger> ticketIdListToIndexList(List<BigInteger> ticketIds)
    {
        //read given indicies and convert into internal format, error checking to ensure
        List<BigInteger> idList = new ArrayList<>();

        try
        {
            for (BigInteger id : ticketIds)
            {
                if (id.compareTo(BigInteger.ZERO) != 0)
                {
                    int index = balanceArray.indexOf(id);
                    if (index > -1)
                    {
                        if (!idList.contains(BigInteger.valueOf(index))) //just make sure they didn't already add this one
                        {
                            idList.add(BigInteger.valueOf(index));
                        }
                    }
                    else
                    {
                        idList = null;
                        break;
                    }
                }
            }
        }
        catch (Exception e)
        {
            idList = null;
        }

        return idList;
    }

    /**
     * Convert a String list of ticket IDs into a list of ticket indices
     * @param userList
     * @return
     */
    @Override
    public List<BigInteger> ticketIdStringToIndexList(String userList)
    {
        List<BigInteger> idList = new ArrayList<>();

        String[] ids = userList.split(",");

        for (String id : ids)
        {
            //remove whitespace
            String     trim   = id.trim();
            BigInteger thisId = Numeric.toBigInt(trim);
            idList.add(thisId);
        }

        return tokenIdsToTokenIndices(idList);
    }

    private List<BigInteger> tokenIdsToTokenIndices(List<BigInteger> tokenIds)
    {
        List<BigInteger> inventoryCopy = new ArrayList<BigInteger>(balanceArray);
        List<BigInteger> indexList = new ArrayList<>();
        try
        {
            for (BigInteger id : tokenIds)
            {
                if (id.compareTo(BigInteger.ZERO) != 0)
                {
                    int index = inventoryCopy.indexOf(id);
                    if (index > -1)
                    {
                        inventoryCopy.set(index, BigInteger.ZERO);
                        BigInteger indexBi = BigInteger.valueOf(index);
                        if (!indexList.contains(indexBi))
                        {   //just make sure they didn't already add this one
                            indexList.add(indexBi);
                        }
                    }
                    else
                    {
                        indexList = null;
                        break;
                    }
                }
            }
        }
        catch (Exception e) {
            indexList = null;
        }

        return indexList;
    }

    public void checkIsMatchedInXML(AssetDefinitionService assetService)
    {
        isMatchedInXML = assetService.hasDefinition(tokenInfo.chainId, tokenInfo.address);
    }

    @Override
    public Function getTransferFunction(String to, List<BigInteger> tokenIndices) throws NumberFormatException
    {
        return new Function(
                "transfer",
                Arrays.asList(new org.web3j.abi.datatypes.Address(to),
                        getDynArray(tokenIndices)
                ),
                Collections.emptyList());
    }

    @Override
    public boolean contractTypeValid()
    {
        switch (contractType)
        {
            case ERC875:
            case ERC875_LEGACY:
                return true;
            default:
                return false;
        }
    }

    @Override
    protected org.web3j.abi.datatypes.DynamicArray getDynArray(List<BigInteger> indices)
    {
        DynamicArray dynArray;

        switch (contractType)
        {
            case ERC875_LEGACY:
                dynArray = new org.web3j.abi.datatypes.DynamicArray<>(
                        org.web3j.abi.datatypes.generated.Uint16.class,
                        org.web3j.abi.Utils.typeMap(indices, org.web3j.abi.datatypes.generated.Uint16.class));
                break;
            case ERC875:
            default:
                dynArray = new org.web3j.abi.datatypes.DynamicArray<>(
                        org.web3j.abi.datatypes.generated.Uint256.class,
                        org.web3j.abi.Utils.typeMap(indices, org.web3j.abi.datatypes.generated.Uint256.class));
                break;
        }

        return dynArray;
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
    public List<BigInteger> getArrayBalance() { return balanceArray; }

    @Override
    public List<BigInteger> getNonZeroArrayBalance()
    {
        List<BigInteger> nonZeroValues = new ArrayList<>();
        for (BigInteger value : balanceArray) if (value.compareTo(BigInteger.ZERO) != 0 && !nonZeroValues.contains(value)) nonZeroValues.add(value);
        return nonZeroValues;
    }

    @Override
    public boolean getIsSent(Transaction transaction)
    {
        return transaction.isNFTSent(getWallet());
    }

    @Override
    public boolean isERC875() { return true; }
    public boolean isNonFungible() { return true; }
    @Override
    public boolean hasGroupedTransfer() { return true; }

    @Override
    public boolean groupWithToken(TicketRange currentGroupingRange, TicketRangeElement newElement, long currentGroupTime)
    {
        if (currentGroupingRange.tokenIds.size() == 0) return false;

        return currentGroupingRange.tokenIds.get(0)
                .equals(newElement.id) || (newElement.time != 0 && newElement.time == currentGroupTime);
    }

    /**
     * This function should return a String list of IDs suitable for submission to the token's transfer function
     * For ERC875 it is a list of indices, so convert this list of TokenIDs to indices
     * @param CSVstringIdList
     * @return
     */

    public String getTransferListFormat(String CSVstringIdList)
    {
        List<BigInteger> indexList = ticketIdStringToIndexList(CSVstringIdList); //convert the list of tokenID to indices.
        return Utils.bigIntListToString(indexList, true);
    }

    /**
     * This function takes a list of tokenIds, and returns a BigInteger list suitable for this token's transfer function
     * For ERC875 it is a list of indices, so convert this list of TokenIDs to indices
     * @param tokenIds
     * @return
     */
    @Override
    public List<BigInteger> getTransferListFormat(List<BigInteger> tokenIds)
    {
        return tokenIdsToTokenIndices(tokenIds);
    }

    @Override
    public String convertValue(String prefix, String value, int precision)
    {
        return prefix + value;
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
        return Arrays.asList(R.string.action_use, R.string.action_transfer, R.string.action_sell);
    }
}
