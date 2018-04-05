package io.awallet.crypto.alphawallet.entity;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.repository.entity.RealmToken;
import io.awallet.crypto.alphawallet.ui.widget.entity.TicketRange;
import io.awallet.crypto.alphawallet.ui.widget.holder.TokenHolder;
import io.awallet.crypto.alphawallet.viewmodel.BaseViewModel;

import org.web3j.abi.datatypes.Int;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jnr.ffi.annotations.In;

/**
 * Created by James on 27/01/2018.
 */

public class Ticket extends Token implements Parcelable
{
    public final List<Integer> balanceArray;
    private List<Integer> burnArray;

    public Ticket(TokenInfo tokenInfo, List<Integer> balances, List<Integer> burned, long blancaTime) {
        super(tokenInfo, BigDecimal.ZERO, blancaTime);
        this.balanceArray = balances;
        burnArray = burned;
    }

    public Ticket(TokenInfo tokenInfo, String balances, String burnList, long blancaTime) {
        super(tokenInfo, BigDecimal.ZERO, blancaTime);
        this.balanceArray = parseIDListInteger(balances);
        burnArray = parseIDListInteger(burnList, true);
    }
//
//    public Ticket(TokenInfo tokenInfo, String balances, String burnList, long blancaTime) {
//        super(tokenInfo, BigDecimal.ZERO, blancaTime);
//        this.balanceArray   = parseIDListInteger(balances);
//        this.burnArray      = parseIDListInteger(burnList);
//    }

    private Ticket(Parcel in) {
        super(in);
        Object[] readObjArray = in.readArray(Object.class.getClassLoader());
        Object[] readBurnArray = in.readArray(Object.class.getClassLoader());
        balanceArray = new ArrayList<Integer>();
        burnArray = new ArrayList<Integer>();
        for (Object o : readObjArray)
        {
            Integer val = (Integer)o;
            balanceArray.add(val);
        }

        //check to see if burn notice is needed
        for (Object o : readBurnArray)
        {
            Integer val = (Integer)o;
            burnArray.add(val);
        }
    }

    @Override
    public String getStringBalance() {
        return populateIDs(balanceArray, false);
    }

    @Override
    public String getFullBalance() {
        if (balanceArray == null) return "no tokens";
        else return populateIDs(balanceArray, true);
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
        dest.writeArray(balanceArray.toArray());
        dest.writeArray(burnArray.toArray());
    }

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
                    int thisTicketId = balanceArray.get(i);
                    sb.append(String.valueOf(thisTicketId));
                    first = false;
                }
            }
        }
        else {
            sb.append("none");
        }

        return sb.toString();
    }

    public String checkBalance(String selection)
    {
        StringBuilder sb = new StringBuilder();
        //convert selection to index list
        List<org.web3j.abi.datatypes.generated.Int16> selectionIndex = parseIDList(selection);
        //add correct entries
        boolean first = true;
        for (org.web3j.abi.datatypes.generated.Int16 id : selectionIndex) {
            if (balanceArray.contains(id.getValue().intValue()) && !burnArray.contains(id.getValue().intValue())) {
                if (!first) sb.append(", ");
                sb.append(String.valueOf(id.getValue().toString(10)));
                first = false;
            }
        }

        return sb.toString();
    }

    private List<org.web3j.abi.datatypes.generated.Int16> parseIDList(String userList)
    {
        List<org.web3j.abi.datatypes.generated.Int16> idList = new ArrayList<>();

        try
        {
            String[] ids = userList.split(",");

            for (String id : ids)
            {
                //remove whitespace
                String trim = id.trim();
                org.web3j.abi.datatypes.generated.Int16 thisId = new org.web3j.abi.datatypes.generated.Int16(Integer.parseInt(trim));
                idList.add(thisId);
            }
        }
        catch (Exception e)
        {
            idList = null;
        }

        return idList;
    }

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

    public List<Integer> indexToIDList(int[] prunedIndices)
    {
        List<Integer> idList = new ArrayList<>();
        for (int i : prunedIndices)
        {
            if (i < balanceArray.size()) {
                Integer ticketID = balanceArray.get(i);
                idList.add(ticketID);
            }
        }

        return idList;
    }

    public String arrayToString(int[] prunedIndices)
    {
        return populateIDs(prunedIndices);
    }

    public List<Integer> ticketIdToTicketIndex(List<Integer> ticketIds)
    {
        //read given indicies and convert into internal format, error checking to ensure
        List<Integer> idList = new ArrayList<>();

        try
        {
            for (Integer id : ticketIds) {
                if (id > 0) {
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
     * Function to convert a list of ticket IDs into ticket indices for the account address given
     * @param userList
     * @return
     */
    @Override
    public List<Integer> parseIndexList(String userList)
    {
        //read given indicies and convert into internal format, error checking to ensure
        List<Integer> idList = new ArrayList<>();

        try
        {
            String[] ids = userList.split(",");

            for (String id : ids) {
                //remove whitespace
                String trim = id.trim();
                Integer thisId = Integer.parseInt(trim);

                if (thisId > 0) {
                    int index = balanceArray.indexOf(thisId);
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

    //Burn handling
    public void addToBurnList(List<BigInteger> burnIndicies)
    {
        for (BigInteger b : burnIndicies) {
            Integer index = b.intValue();

            //lookup index
            if (balanceArray.size() > index)
            {
                Integer value = balanceArray.get(index);
                if (value > 0 && !burnArray.contains(value)) {
                    burnArray.add(value);
                }
            }
        }
    }

    public List<Integer> getValidIndicies() {
        List<Integer> validIndicies = new ArrayList<>();
        for (Integer ticketIndex : balanceArray)
        {
            if (!burnArray.contains(ticketIndex)) {
                validIndicies.add(ticketIndex);
            }
        }

        return validIndicies;
    }

    /**
     * Produce a string CSV of integer IDs given an input list of
     * @param idArray
     * @param keepZeros
     * @return
     */
    @Override
    public String populateIDs(List<Integer> idArray, boolean keepZeros)
    {
        if (idArray == null) return "";
        String displayIDs = "";
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (Integer id : idArray)
        {
            if (!keepZeros && id == 0) continue;
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
     * Produce a string CSV of integer IDs given an input list of
     * @param idArray int[] array of indices
     * @return
     */
    @Override
    public String populateIDs(int[] idArray)
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

    @Override
    public int getTicketCount()
    {
        int count = 0;
        for (Integer id : balanceArray)
        {
            if (id > 0) count++;
        }
        return count;
    }

    @Override
    public void setRealmBalance(RealmToken realmToken)
    {
        realmToken.setBalance(populateIDs(balanceArray, true));
    }

    @Override
    public void setRealmBurn(RealmToken realmToken, List<Integer> burnList)
    {
        realmToken.setBurnList(populateIDs(burnList, false));
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
        tokenHolder.text24Hours.setText(String.valueOf(burnArray.size()));
        tokenHolder.textAppreciationSub.setText(R.string.marketplace);

        //String ids = populateIDs(((Ticket)(tokenHolder.token)).balanceArray, false);
//        tokenHolder.arrayBalance.setText(String.valueOf(getTicketCount()) + " Tickets");
        tokenHolder.arrayBalance.setText(String.valueOf(getTicketCount()));
    }

    public String populateRange(TicketRange range)
    {
        return populateIDs(range.tokenIds, false);
    }

    private int getIndexOf(int id)
    {
        if (balanceArray.contains(id))
        {
            return balanceArray.indexOf(id);
        }
        else
        {
            return -1;
        }
    }

    @Override
    public int[] getTicketIndicies(String ticketIds)
    {
        List<Integer> indexList = parseIndexList(ticketIds);
        int[] indicies = new int[indexList.size()];
        int i = 0;
        for (Iterator<Integer> iterator = indexList.iterator(); iterator.hasNext(); i++) {
            indicies[i] = (int)iterator.next();
        }
        return indicies;
    }

    public List<Integer> getBurnList()
    {
        return burnArray;
    }

    @Override
    public String getBurnListStr() {
        return populateIDs(burnArray, false);
    }
}
