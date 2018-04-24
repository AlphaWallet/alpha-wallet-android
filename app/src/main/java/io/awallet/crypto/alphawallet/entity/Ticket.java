package io.awallet.crypto.alphawallet.entity;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.repository.AssetDefinition;
import io.awallet.crypto.alphawallet.repository.entity.NonFungibleToken;
import io.awallet.crypto.alphawallet.repository.entity.RealmToken;
import io.awallet.crypto.alphawallet.ui.BaseActivity;
import io.awallet.crypto.alphawallet.ui.RedeemSignatureDisplayActivity;
import io.awallet.crypto.alphawallet.ui.widget.entity.TicketRange;
import io.awallet.crypto.alphawallet.ui.widget.holder.BaseTicketHolder;
import io.awallet.crypto.alphawallet.ui.widget.holder.BinderViewHolder;
import io.awallet.crypto.alphawallet.ui.widget.holder.TicketHolder;
import io.awallet.crypto.alphawallet.ui.widget.holder.TokenHolder;
import io.awallet.crypto.alphawallet.viewmodel.BaseViewModel;

import org.web3j.abi.datatypes.generated.Uint16;
import org.web3j.utils.Numeric;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

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
    public final List<BigInteger> balanceArray;
    private List<Integer> burnIndices;
    private boolean isLiveTicket = false;

    public Ticket(TokenInfo tokenInfo, List<BigInteger> balances, List<Integer> burned, long blancaTime) {
        super(tokenInfo, BigDecimal.ZERO, blancaTime);
        this.balanceArray = balances;
        burnIndices = burned;
    }

    public Ticket(TokenInfo tokenInfo, String balances, String burnList, long blancaTime) {
        super(tokenInfo, BigDecimal.ZERO, blancaTime);
        this.balanceArray = stringHexToBigIntegerList(balances);
        burnIndices = stringIntsToIntegerList(burnList);
    }

    private Ticket(Parcel in) {
        super(in);
        Object[] readObjArray = in.readArray(Object.class.getClassLoader());
        Object[] readBurnArray = in.readArray(Object.class.getClassLoader());
        balanceArray = new ArrayList<>();
        burnIndices = new ArrayList<Integer>();
        for (Object o : readObjArray)
        {
            BigInteger val = (BigInteger)o;
            balanceArray.add(val);
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
        return intArrayToString(balanceArray, false);
    }

    @Override
    public boolean hasPositiveBalance() {
        return (getTicketCount() > 0);
    }

    @Override
    public String getFullBalance() {
        if (balanceArray == null) return "no tokens";
        else return intArrayToString(balanceArray, true);
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
        dest.writeArray(burnIndices.toArray());
    }

    /**
     * Given a string of hex ticket ID's, reduce the length of the string to 'quantity' items
     *
     * @return
     */
    public String pruneIDList(String idListStr, int quantity)
    {
        //convert to list
        List<BigInteger> idList = stringHexToBigIntegerList(idListStr);
        /* weiwu: potentially we can do this but I am not sure if
	 * order is important*/
        //List<BigInteger> idList = Observable.fromArray(idListStr.split(","))
        //       .map(s -> Numeric.toBigInt(s)).toList().blockingGet();
        for (int i = (idList.size() - 1); i >= quantity; i--)
        {
            idList.remove(i);
        }

        return intArrayToString(idList, true);
    }

    //Burn handling
    public void addToBurnList(List<Uint16> burnUpdate)
    {
        for (Uint16 b : burnUpdate) {
            Integer index = b.getValue().intValue();

            //lookup index
            if (balanceArray.size() > index)
            {
                BigInteger value = balanceArray.get(index);
                if (value.compareTo(BigInteger.ZERO) != 0 && !burnIndices.contains(index)) {
                    burnIndices.add(index);
                }
            }
        }
    }

    @Override
    public int getTicketCount()
    {
        int count = 0;
        for (BigInteger id : balanceArray)
        {
            if (id.compareTo(BigInteger.ZERO) != 0) count++;
        }
        return count;
    }

    @Override
    public void setRealmBalance(RealmToken realmToken)
    {
        realmToken.setBalance(intArrayToString(balanceArray, true));
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
        tokenHolder.textAppreciation.setText("--");
        if (isLiveTicket())
        {
            tokenHolder.issuer.setText(R.string.shengkai);
            tokenHolder.contractType.setVisibility(View.VISIBLE);
            tokenHolder.contractSeparator.setVisibility(View.VISIBLE);
            tokenHolder.contractType.setText(R.string.erc875);
        }

        tokenHolder.text24HoursSub.setText(R.string.burned);
        tokenHolder.text24Hours.setText(String.valueOf(burnIndices.size()));
        tokenHolder.textAppreciationSub.setText(R.string.marketplace);
        tokenHolder.arrayBalance.setText(String.valueOf(getTicketCount()));
    }

    public String populateRange(TicketRange range)
    {
        return intArrayToString(range.tokenIds, false);
    }

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

    public String getTicketInfo(NonFungibleToken nonFungibleToken)
    {
        String teamA = nonFungibleToken.getAttribute("countryA").text;
        String teamB = nonFungibleToken.getAttribute("countryB").text;
        String time = nonFungibleToken.getDate("hh:mm");
        String locality = nonFungibleToken.getAttribute("locality").text;
        return time + "  " + locality + "\n\n" + teamA + " vs " + teamB;
    }


    /*************************************
     *
     * Conversion functions used for manipulating tickets
     *
     */

    /**
     * Convert a list of ticket indices into a CSV string of the corresponding TicketIDs
     * @param checkedIndexList
     * @return
     */
    //produce a String containing all the TicketID (BigInteger) entries from a list of indices
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
                    if (!first) sb.append(",");
                    BigInteger ticketID = balanceArray.get(i);
                    sb.append(Numeric.toHexStringNoPrefix(ticketID));
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
     * Convert a CSV string of Hex values into a BigInteger List
     * @param integerString CSV string of hex ticket id's
     * @return
     */
    public List<BigInteger> stringHexToBigIntegerList(String integerString)
    {
        List<BigInteger> idList = new ArrayList<>();

        try
        {
            String[] ids = integerString.split(",");

            for (String id : ids)
            {
                //remove whitespace
                String trim = id.trim();
                BigInteger val = Numeric.toBigInt(trim);
                idList.add(val);
            }
        }
        catch (Exception e)
        {
            idList = new ArrayList<>();
        }

        return idList;
    }

    /**
     * Given a set of indices generate a list of BigInteger Ticket ID's
     * @param prunedIndices
     * @return
     */
    public List<BigInteger> indexArrayToTicketId(int[] prunedIndices)
    {
        List<BigInteger> idList = new ArrayList<>();
        for (int i : prunedIndices)
        {
            if (i < balanceArray.size()) {
                BigInteger ticketID = balanceArray.get(i);
                idList.add(ticketID);
            }
        }

        return idList;
    }

    /**
     * Convert a list of TicketID's into an Index list corresponding to those tickets
     * @param ticketIds
     * @return
     */
    public List<Integer> ticketIdListToIndexList(List<BigInteger> ticketIds)
    {
        //read given indicies and convert into internal format, error checking to ensure
        List<Integer> idList = new ArrayList<>();

        try
        {
            for (BigInteger id : ticketIds)
            {
                if (id.compareTo(BigInteger.ZERO) != 0)
                {
                    int index = balanceArray.indexOf(id);
                    if (index > -1)
                    {
                        if (!idList.contains(index)) //just make sure they didn't already add this one
                        {
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
    public List<Integer> ticketIdStringToIndexList(String userList)
    {
        //read given indicies and convert into internal format, error checking to ensure
        List<Integer> idList = new ArrayList<>();

        try
        {
            String[] ids = userList.split(",");

            for (String id : ids) {
                //remove whitespace
                String trim = id.trim();
                BigInteger thisId = Numeric.toBigInt(trim);

                if (thisId.compareTo(BigInteger.ZERO) != 0)
                {
                    int index = balanceArray.indexOf(thisId);
                    if (index > -1)
                    {
                        if (!idList.contains(index))
                        {   //just make sure they didn't already add this one
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

    /**
     * Produce a string CSV of integer IDs given an input list of values
     * @param idList
     * @param keepZeros
     * @return
     */
    public String intArrayToString(List<BigInteger> idList, boolean keepZeros)
    {
        if (idList == null) return "";
        String displayIDs = "";
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (BigInteger id : idList)
        {
            if (!keepZeros && id.compareTo(BigInteger.ZERO) == 0) continue;
            if (!first)
            {
                sb.append(",");
            }
            first = false;

            sb.append(Numeric.toHexStringNoPrefix(id));
            displayIDs = sb.toString();
        }

        return displayIDs;
    }

    /**
     * Setup the single view of a ticket. Since this is repeatedly used and is exclusive info to a ticket it should go in here
     * @param range TicketRange
     * @param activity BaseActivity of Holder of ticket
     */
    public void displayTicketHolder(TicketRange range, BaseActivity activity)
    {
        TextView textAmount = activity.findViewById(R.id.amount);
        TextView textTicketName = activity.findViewById(R.id.name);
        TextView textVenue = activity.findViewById(R.id.venue);
        TextView textDate = activity.findViewById(R.id.date);
        TextView textRange = activity.findViewById(R.id.tickettext);
        TextView textCat = activity.findViewById(R.id.cattext);
        TextView ticketDetails = activity.findViewById(R.id.ticket_details);

        int numberOfTickets = range.tokenIds.size();
        if (numberOfTickets > 0)
        {
            try
            {
                BigInteger firstTicket = range.tokenIds.get(0);
                AssetDefinition assetDefinition = new AssetDefinition("TicketingContract.xml", activity.getResources());
                NonFungibleToken nonFungibleToken = new NonFungibleToken(firstTicket, assetDefinition);
                String venue = nonFungibleToken.getAttribute("venue").text;
                String date = nonFungibleToken.getDate("dd MMM");
                String seatCount = String.format(Locale.getDefault(), "x%d", range.tokenIds.size());

                textAmount.setText(seatCount);
                textTicketName.setText(getFullName());
                textVenue.setText(venue);
                textDate.setText(date);
                textRange.setText(nonFungibleToken.getRangeStr(range));
                textCat.setText(nonFungibleToken.getAttribute("category").text);
                ticketDetails.setText(getTicketInfo(nonFungibleToken));
            }
            catch (IOException | SAXException e)
            {
                e.printStackTrace();
                //TODO: Handle error
            }
        }
    }

    public String getXMLProperty(String property, BaseActivity activity)
    {
        String value;
        try
        {
            AssetDefinition ad = new AssetDefinition("TicketingContract.xml", activity.getResources());
            value = ad.networkInfo.get(property);
        }
        catch (IOException | SAXException e)
        {
            e.printStackTrace();
            //TODO: Handle error
            value = "";
        }

        return value;
    }

    public boolean isLiveTicket()
    {
        return isLiveTicket;
    }

    public void setLiveTicket()
    {
        isLiveTicket = true;
    }
}