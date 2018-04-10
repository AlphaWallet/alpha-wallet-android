package io.awallet.crypto.alphawallet.entity;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.repository.entity.NonFungibleToken;
import io.awallet.crypto.alphawallet.repository.entity.RealmToken;
import io.awallet.crypto.alphawallet.ui.widget.entity.TicketRange;
import io.awallet.crypto.alphawallet.ui.widget.holder.TokenHolder;
import io.awallet.crypto.alphawallet.viewmodel.BaseViewModel;

import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by James on 27/01/2018.  It might seem counter intuitive
 * but here Ticket refers to a container of an asset class here, not
 * the right to seat somewhere in the venue. Therefore, there
 * shouldn't be List<Ticket> To understand this, imagine that one says
 * "I have two cryptocurrencies: Ether and Bitcoin, each amounts to a
 * hundred", and he pauses and said, "I also have two tickets: FIFA
 * and Formuler-one, which, too, amounts to a hundred each".
 */

public class Ticket extends Token implements Parcelable
{
    public final List<Bytes32> balanceArray;
    public final List<BigInteger> balanceArrayI;
    private List<Integer> burnIndices;

    public Ticket(TokenInfo tokenInfo, List<Bytes32> balances, List<Integer> burned, long blancaTime) {
        super(tokenInfo, BigDecimal.ZERO, blancaTime);
        this.balanceArray = balances;
        balanceArrayI = new ArrayList<>();
        for (Bytes32 cv : balanceArray) balanceArrayI.add(Numeric.toBigInt(cv.getValue()));
        burnIndices = burned;
    }

    public Ticket(TokenInfo tokenInfo, String balances, String burnList, long blancaTime) {
        super(tokenInfo, BigDecimal.ZERO, blancaTime);
        this.balanceArray = stringToTicketIDList(balances);
        balanceArrayI = new ArrayList<>();
        for (Bytes32 cv : balanceArray) balanceArrayI.add(Numeric.toBigInt(cv.getValue()));
        burnIndices = parseIDListInteger(burnList, true);
    }

    private Ticket(Parcel in) {
        super(in);
        Object[] readObjArray = in.readArray(Object.class.getClassLoader());
        Object[] readBurnArray = in.readArray(Object.class.getClassLoader());
        balanceArray = new ArrayList<Bytes32>();
        balanceArrayI = new ArrayList<>();
        burnIndices = new ArrayList<Integer>();
        for (Object o : readObjArray)
        {
            BigInteger val = (BigInteger)o;
            balanceArrayI.add(val);
            balanceArray.add(new Bytes32(Numeric.toBytesPadded(val, 32)));
        }

        //check to see if burn notice is needed
        for (Object o : readBurnArray)
        {
            Integer val = (Integer)o;
            burnIndices.add(val);
        }
    }

    @Override
    public String getStringBalance() {
        return ticketIdToString(balanceArray, false);
    }

    @Override
    public String getFullBalance() {
        if (balanceArray == null) return "no tokens";
        else return ticketIdToString(balanceArray, true);
    }

    public static final Creator<Ticket> CREATOR = new Creator<Ticket>() {
        @Override
        public Ticket createFromParcel(Parcel in) {
            return new Ticket(in);
        }

        @Override
        public Ticket[] newArray(int size) {
            return new Ticket[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        //List<BigInteger> vals = new ArrayList<>();
        //for (Bytes32 cv : balanceArray) vals.add(Numeric.toBigInt(cv.getValue()));
        dest.writeArray(balanceArrayI.toArray());
        dest.writeArray(burnIndices.toArray());
    }

    //produce a String containing all the BYTES32 entries from a list of indices
    public String parseList(List<Integer> checkedIndexList) {
        StringBuilder sb = new StringBuilder();

        boolean first = true;
        if (checkedIndexList != null)
        {
            for (Integer i : checkedIndexList)
            {
                //reverse lookup the selected IDs
                if (i < balanceArray.size())
                {
                    if (!first) sb.append(", ");
                    Bytes32 ticketID = balanceArray.get(i);
                    sb.append(Numeric.toHexString(ticketID.getValue()));
                    first = false;
                }
            }
        }
        else {
            sb.append("none");
        }

        return sb.toString();
    }

    /**
     * Given a string list of Integer Indices generate the BigInteger list required for interaction with Ethereum contract
     * @param userList
     * @return
     */
    public List<BigInteger> parseIDListBI(String userList)
    {
        List<BigInteger> idList = new ArrayList<>();

        try
        {
            String[] ids = userList.split(",");

            for (String id : ids)
            {
                //remove whitespace
                String trim = id.trim();
                BigInteger thisId = BigInteger.valueOf(Integer.parseInt(trim));
                idList.add(thisId);
            }
        }
        catch (Exception e)
        {
            idList = null;
        }

        return idList;
    }

    public List<Bytes32> stringToTicketIDList(String ticketStringList)
    {
        List<Bytes32> idList = new ArrayList<>();

        try
        {
            String[] ids = ticketStringList.split(",");

            for (String id : ids)
            {
                //remove whitespace
                String trim = id.trim();
                Bytes32 val = new Bytes32(Numeric.toBytesPadded(Numeric.toBigInt(trim), 32));
                idList.add(val);
            }
        }
        catch (Exception e)
        {
            idList = new ArrayList<>();
        }

        return idList;
    }

    public List<Integer> parseIDListInteger(String userList)
    {
        return parseIDListInteger(userList, false);
    }

    public List<Integer> parseIDListInteger(String userList, boolean removeDuplicates)
    {
        List<Integer> idList = new ArrayList<>();

        try
        {
            String[] ids = userList.split(",");

            for (String id : ids)
            {
                //remove whitespace
                String trim = id.trim();
                Integer intId = Integer.parseInt(trim);
                if (removeDuplicates) {
                    if (!idList.contains(intId)) idList.add(intId);
                }
                else {
                    idList.add(intId);
                }
            }
        }
        catch (Exception e)
        {
            idList = new ArrayList<>();
        }

        return idList;
    }

    /**
     * Given a set of indices generate a list of Bytes32 ID's
     * @param prunedIndices
     * @return
     */
    @Override
    public List<Bytes32> indexToIDList(int[] prunedIndices)
    {
        List<Bytes32> idList = new ArrayList<>();
        for (int i : prunedIndices)
        {
            if (i < balanceArray.size()) {
                Bytes32 ticketID = balanceArray.get(i);
                idList.add(ticketID);
            }
        }

        return idList;
    }

    public String arrayToString(int[] prunedIndices)
    {
        return ticketIdToString(prunedIndices);
    }

    public List<Integer> ticketIdToTicketIndex(List<Bytes32> ticketIds)
    {
        //read given indicies and convert into internal format, error checking to ensure
        List<Integer> idList = new ArrayList<>();

        try
        {
            for (Bytes32 id : ticketIds) {
                if (Numeric.toBigInt(id.getValue()).compareTo(BigInteger.ZERO) != 0) {
                    int index = balanceArray.indexOf(id);
                    if (index > -1) {
                        if (!idList.contains(index)) {  //just make sure they didn't already add this one
                            idList.add(index);
                        }
                    } else {
                        idList = null;
                        break;
                    }
                }
            }
        }
        catch (Exception e) {
            idList = null;
        }

        return idList;
    }

    /**
     * Function to convert a String list of ticket IDs into ticket indices for the account address given
     * @param userList
     * @return
     */
    @Override
    public List<Integer> ticketIdStringToIndexList(String userList) //ticketIdStringToIndexList
    {
        //read given indicies and convert into internal format, error checking to ensure
        List<Integer> idList = new ArrayList<>();

        try
        {
            String[] ids = userList.split(",");

            for (String id : ids) {
                //remove whitespace
                String trim = id.trim();
                Bytes32 thisId = new Bytes32(Numeric.toBytesPadded(Numeric.toBigInt(trim), 32));

                if (Numeric.toBigInt(thisId.getValue()).compareTo(BigInteger.ZERO) != 0)
                {
                    int index = balanceArray.indexOf(thisId);
                    if (index > -1)
                    {
                        if (!idList.contains(index))
                        {  //just make sure they didn't already add this one
                            idList.add(index);
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
        catch (Exception e) {
            idList = null;
        }

        return idList;
    }

    //Burn handling
    public void addToBurnList(List<BigInteger> burnUpdate)
    {
        for (BigInteger b : burnUpdate) {
            Integer index = b.intValue();

            //lookup index
            if (balanceArray.size() > index)
            {
                Bytes32 value = balanceArray.get(index);
                if (Numeric.toBigInt(value.getValue()).compareTo(BigInteger.ZERO) != 0 && !burnIndices.contains(index)) {
                    burnIndices.add(index);
                }
            }
        }
    }

    /**
     * Produce a string CSV of integer IDs given an input list of values
     * @param idArray
     * @param keepZeros
     * @return
     */
    @Override
    public String ticketIdToString(List<Bytes32> idArray, boolean keepZeros)
    {
        if (idArray == null) return "";
        String displayIDs = "";
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (Bytes32 id : idArray)
        {
            if (!keepZeros && Numeric.toBigInt(id.getValue()).compareTo(BigInteger.ZERO) == 0) continue;
            if (!first)
            {
                sb.append(", ");
            }
            first = false;

            sb.append(Numeric.toHexString(id.getValue(), 0, id.getValue().length, false));
            displayIDs = sb.toString();
        }

        return displayIDs;
    }

    /**
     * Produce a string CSV of integer IDs given an input list of
     * @param idArray int[] array of indices
     * @return
     */
    @Override
    public String ticketIdToString(int[] idArray)
    {
        if (idArray == null) return "";
        String displayIDs = "";
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (Integer id : idArray)
        {
            if (id == 0) continue;
            if (!first)
            {
                sb.append(", ");
            }
            first = false;

            sb.append(id.toString());
            displayIDs = sb.toString();
        }

        return displayIDs;
    }

    /**
     *
     * @return
     */
    public String pruneIDList(String idListStr, int quantity)
    {
        //convert to list
        List<Bytes32> idList = stringToTicketIDList(idListStr);
        for (int i = (idList.size() - 1); i >= quantity; i--)
        {
            idList.remove(i);
        }

        return ticketIdToString(idList, true);
    }

    @Override
    public int getTicketCount()
    {
        int count = 0;
        for (Bytes32 id : balanceArray)
        {
            if (Numeric.toBigInt(id.getValue()).compareTo(BigInteger.ZERO) != 0) count++;
        }
        return count;
    }

    @Override
    public void setRealmBalance(RealmToken realmToken)
    {
        realmToken.setBalance(ticketIdToString(balanceArray, true));
    }

    @Override
    public void setRealmBurn(RealmToken realmToken, List<Integer> burnList)
    {
        realmToken.setBurnList(integerListToString(burnList, false));
    }

    @Override
    public void clickReact(BaseViewModel viewModel, Context context)
    {
        viewModel.showRedeemToken(context, this);
    }

    @Override
    public void setupContent(TokenHolder tokenHolder)
    {
        tokenHolder.fillIcon(null, R.mipmap.ic_alpha);
        tokenHolder.balanceEth.setVisibility(View.GONE);
        tokenHolder.balanceCurrency.setText("--");
        tokenHolder.arrayBalance.setVisibility(View.VISIBLE);
        tokenHolder.issuer.setText(TicketDecode.getIssuer());
        tokenHolder.text24HoursSub.setText(R.string.burned);
        tokenHolder.text24Hours.setText(String.valueOf(burnIndices.size()));
        tokenHolder.textAppreciationSub.setText(R.string.marketplace);
        tokenHolder.arrayBalance.setText(String.valueOf(getTicketCount()));
    }

    public String populateRange(TicketRange range)
    {
        return ticketIdToString(range.tokenIds, false);
    }

    @Override
    public int[] getTicketIndicies(String ticketIds)
    {
        List<Integer> indexList = ticketIdStringToIndexList(ticketIds);
        int[] indicies = new int[indexList.size()];
        int i = 0;
        for (Iterator<Integer> iterator = indexList.iterator(); iterator.hasNext(); i++) {
            indicies[i] = (int)iterator.next();
        }
        return indicies;
    }

    public List<Integer> getBurnList()
    {
        return burnIndices;
    }

    @Override
    public String getBurnListStr() {
        return integerListToString(burnIndices, false);
    }
}
