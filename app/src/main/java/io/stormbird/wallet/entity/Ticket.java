package io.stormbird.wallet.entity;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.web3j.abi.datatypes.generated.Uint16;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import io.stormbird.token.entity.NonFungibleToken;
import io.stormbird.token.entity.TicketRange;
import io.stormbird.token.util.ZonedDateTime;
import io.stormbird.wallet.R;
import io.stormbird.wallet.repository.entity.RealmToken;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.ui.BaseActivity;
import io.stormbird.wallet.ui.widget.holder.TokenHolder;
import io.stormbird.wallet.viewmodel.BaseViewModel;

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
    private boolean isMatchedInXML = false;

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
    public void setupContent(TokenHolder tokenHolder, AssetDefinitionService asset)
    {
        tokenHolder.fillIcon(null, R.mipmap.ic_alpha);
        tokenHolder.balanceEth.setVisibility(View.GONE);
        tokenHolder.balanceCurrency.setText("--");
        tokenHolder.arrayBalance.setVisibility(View.VISIBLE);
        tokenHolder.textAppreciation.setText("--");

        tokenHolder.issuer.setText(asset.getIssuerName(getAddress()));
        tokenHolder.contractType.setVisibility(View.VISIBLE);
        tokenHolder.contractSeparator.setVisibility(View.VISIBLE);
        tokenHolder.contractType.setText(R.string.erc875);

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
        String time = nonFungibleToken.getAttribute("time").text;
        String locality = nonFungibleToken.getAttribute("locality").text;
        try {
            ZonedDateTime datetime = new ZonedDateTime(time);
            time = datetime.format(new SimpleDateFormat("hh:mm", Locale.ENGLISH));
        } catch (ParseException e) {
            // time is returned as un-parsed, original string
        }
        return time + locality + "\n\n" + teamA + " vs " + teamB;
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
     * Routine to blank a ticket on a page. It can be static because it doesn't use any class members
     * It will throw an exception if given an activity page with no ticket on it
     * @param activity
     * @param blankingString
     */
    public static void blankTicketHolder(int blankingString, BaseActivity activity)
    {
        try
        {
            TextView textAmount = activity.findViewById(R.id.amount);
            TextView textTicketName = activity.findViewById(R.id.name);
            TextView textVenue = activity.findViewById(R.id.venue);
            TextView textDate = activity.findViewById(R.id.date);
            TextView textRange = activity.findViewById(R.id.tickettext);
            TextView textCat = activity.findViewById(R.id.cattext);
            TextView ticketDetails = activity.findViewById(R.id.ticket_details);

            textAmount.setText("");
            textTicketName.setText(blankingString);
            textVenue.setText("");
            textDate.setText("");
            textRange.setText("");
            textCat.setText("");
            ticketDetails.setText("");
        }
        catch (Exception e)
        {
            Log.d("TICKET", e.getMessage());
        }
    }

    /**
     * This is a single method that populates any instance of graphic ticket anywhere
     *
     * @param range
     * @param activity
     * @param assetService
     * @param ctx needed to create date/time format objects
     */
    public void displayTicketHolder(TicketRange range, View activity, AssetDefinitionService assetService, Context ctx)
    {
        DateFormat date = android.text.format.DateFormat.getLongDateFormat(ctx);
        DateFormat time = android.text.format.DateFormat.getTimeFormat(ctx);

        TextView amount = activity.findViewById(R.id.amount);
        TextView name = activity.findViewById(R.id.name);
        TextView venue = activity.findViewById(R.id.venue);
        TextView ticketDate = activity.findViewById(R.id.date);
        TextView ticketRange = activity.findViewById(R.id.tickettext);
        TextView cat = activity.findViewById(R.id.cattext);
        TextView details = activity.findViewById(R.id.ticket_details);
        TextView ticketTime = activity.findViewById(R.id.time);

        int numberOfTickets = range.tokenIds.size();
        if (numberOfTickets > 0)
        {
            BigInteger firstTicket = range.tokenIds.get(0);
            NonFungibleToken nonFungibleToken = assetService.getNonFungibleToken(firstTicket);

            String venueStr = nonFungibleToken.getAttribute("venue").text;
            String nameStr = getTokenTitle(nonFungibleToken); //nonFungibleToken.getAttribute("category").text;

            String seatCount = String.format(Locale.getDefault(), "x%d", range.tokenIds.size());

            name.setText(nameStr);
            amount.setText(seatCount);
            venue.setText(venueStr);
            ticketRange.setText(
                    nonFungibleToken.getAttribute("countryA").text + "-" +
                            nonFungibleToken.getAttribute("countryB").text
            );
            cat.setText("M" + nonFungibleToken.getAttribute("match").text);
            details.setText(
                    nonFungibleToken.getAttribute("locality").name + ": " +
                            nonFungibleToken.getAttribute("locality").text
            );
            try
            {
                String eventTime = nonFungibleToken.getAttribute("time").text;

                if (eventTime != null)
                {
                    ZonedDateTime datetime = new ZonedDateTime(eventTime);
                    ticketDate.setText(datetime.format(date));
                    ticketTime.setText(datetime.format(time));
                }
                else
                {
                    Date current = new Date();
                    ticketDate.setText(date.format(current));
                    ticketTime.setText(time.format(current));
                }
            }
            catch (ParseException e)
            {
                ticketDate.setText("N.A.");
            }
        }
    }

    private String getTokenTitle(NonFungibleToken nonFungibleToken)
    {
        String tokenTitle = nonFungibleToken.getAttribute("category").text;
        if (tokenTitle == null || tokenTitle.length() == 0)
        {
            tokenTitle = getFullName();
        }

        return tokenTitle;
    }

    public String getTokenName(AssetDefinitionService assetService)
    {
        //see if this token is covered by any contract
        int networkId = assetService.getAssetDefinition().getNetworkFromContract(getAddress());
        if (networkId >= 1)
        {
            return assetService.getAssetDefinition().getTokenName();
        }
        else
        {
            return tokenInfo.name;
        }
    }

//    public int getXMLTokenNetwork(BaseActivity activity)
//    {
//        TokenDefinition td = getTokenDefinition(activity);
//        if (td != null) return td.getNetworkId();
//        else return 1;
//    }
//
//    public String getXMLContractAddress(BaseActivity activity, int networkId)
//    {
//        TokenDefinition td = getTokenDefinition(activity);
//        if (td != null) return td.getContractAddress(networkId);
//        else return "0x";
//    }
//
//    public String getXMLTokenName()
//    {
//        TokenDefinition td = getTokenDefinition(activity);
//        if (td != null) return td.
//                getTokenName();
//        else return "Generic Token";
//    }

//    private TokenDefinition getTokenDefinition(BaseActivity activity)
//    {
//        try
//        {
//            return new TokenDefinition(
//                    activity.getResources().getAssets().open("TicketingContract.xml"),
//                    activity.getResources().getConfiguration().locale);
//        }
//        catch (IOException e)
//        {
//            //react to file not found
//            return null;
//        }
//        catch (SAXException e)
//        {
//            //react to interpretation exception
//            return null;
//        }
//    }

    public void checkIsMatchedInXML(AssetDefinitionService assetService)
    {
        int networkId = assetService.getAssetDefinition().getNetworkFromContract(getAddress());
        isMatchedInXML = networkId >= 1;
    }

    public boolean isMatchedInXML()
    {
        return isMatchedInXML;
    }
}
