package io.stormbird.wallet.entity;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.stormbird.token.util.DateTime;
import io.stormbird.token.util.DateTimeFactory;

import io.stormbird.wallet.util.Utils;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint16;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import io.stormbird.token.entity.NonFungibleToken;
import io.stormbird.token.entity.TicketRange;
import io.stormbird.wallet.R;
import io.stormbird.wallet.repository.entity.RealmToken;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.ui.BaseActivity;
import io.stormbird.wallet.ui.widget.holder.TokenHolder;
import io.stormbird.wallet.viewmodel.BaseViewModel;

import static io.stormbird.wallet.util.Utils.isAlNum;

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
    private final List<BigInteger> balanceArray;
    private boolean isMatchedInXML = false;

    public Ticket(TokenInfo tokenInfo, List<BigInteger> balances, long blancaTime, String networkName, ContractType type) {
        super(tokenInfo, BigDecimal.ZERO, blancaTime, networkName, type);
        this.balanceArray = balances;
    }

    public Ticket(TokenInfo tokenInfo, String balances, long blancaTime, String networkName, ContractType type) {
        super(tokenInfo, BigDecimal.ZERO, blancaTime, networkName, type);
        this.balanceArray = stringHexToBigIntegerList(balances);
    }

    private Ticket(Parcel in) {
        super(in);
        balanceArray = new ArrayList<>();
        int objSize = in.readInt();
        int interfaceOrdinal = in.readInt();
        contractType = ContractType.values()[interfaceOrdinal];
        if (objSize > 0)
        {
            Object[] readObjArray = in.readArray(Object.class.getClassLoader());
            for (Object o : readObjArray)
            {
                BigInteger val = (BigInteger)o;
                balanceArray.add(val);
            }
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
        dest.writeInt(balanceArray.size());
        dest.writeInt(contractType.ordinal());
        if (balanceArray.size() > 0) dest.writeArray(balanceArray.toArray());
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

    @Override
    public int getTicketCount()
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
        realmToken.setBalance(intArrayToString(balanceArray, true));
    }

    @Override
    public void clickReact(BaseViewModel viewModel, Context context)
    {
        viewModel.showRedeemToken(context, this);
    }

    @Override
    public void setupContent(TokenHolder tokenHolder, AssetDefinitionService asset)
    {
        tokenHolder.balanceCurrency.setText("--");
        tokenHolder.textAppreciation.setText("--");

        tokenHolder.contractType.setVisibility(View.VISIBLE);
        tokenHolder.contractSeparator.setVisibility(View.VISIBLE);
        if (contractType == ContractType.ERC875LEGACY)
        {
            tokenHolder.contractType.setText(R.string.erc875legacy);
        }
        else
        {
            tokenHolder.contractType.setText(R.string.erc875);
        }

        tokenHolder.balanceEth.setText(String.valueOf(getTicketCount()));
        tokenHolder.layoutValueDetails.setVisibility(View.GONE);
    }

    public String populateRange(TicketRange range)
    {
        return intArrayToString(range.tokenIds, false);
    }

    @Override
    public int[] getTicketIndices(String ticketIds)
    {
        List<Integer> indexList = ticketIdStringToIndexList(ticketIds);
        int[] indicies = new int[indexList.size()];
        int i = 0;
        for (Iterator<Integer> iterator = indexList.iterator(); iterator.hasNext(); i++) {
            indicies[i] = iterator.next();
        }
        return indicies;
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
    @Override
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
        List<BigInteger> inventoryCopy = new ArrayList<BigInteger>(balanceArray);

        try
        {
            String[] ids = userList.split(",");

            for (String id : ids) {
                //remove whitespace
                String trim = id.trim();
                BigInteger thisId = Numeric.toBigInt(trim);

                if (thisId.compareTo(BigInteger.ZERO) != 0)
                {
                    int index = inventoryCopy.indexOf(thisId);
                    if (index > -1)
                    {
                        inventoryCopy.set(index, BigInteger.ZERO);
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

    private void blankTicketExtra(View activity)
    {
        try
        {
            TextView textVenue = activity.findViewById(R.id.venue);
            TextView textDate = activity.findViewById(R.id.date);
            TextView textRange = activity.findViewById(R.id.tickettext);
            TextView textCat = activity.findViewById(R.id.cattext);
            TextView ticketDetails = activity.findViewById(R.id.ticket_details);
            LinearLayout ticketLayout = activity.findViewById(R.id.ticketlayout);
            LinearLayout catLayout = activity.findViewById(R.id.catlayout);
            LinearLayout dateLayout = activity.findViewById(R.id.datelayout);
            LinearLayout bottomPart = activity.findViewById(R.id.bottom_part);

            //textVenue.setVisibility(View.GONE);
            textVenue.setText("");
            textDate.setText("");
            textRange.setText("");
            textCat.setText("");
            ticketDetails.setText("");
            ticketLayout.setVisibility(View.GONE);
            catLayout.setVisibility(View.GONE);
            dateLayout.setVisibility(View.GONE);
            bottomPart.setVisibility(View.GONE);
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
        DateFormat date = android.text.format.DateFormat.getMediumDateFormat(ctx);
        DateFormat time = android.text.format.DateFormat.getTimeFormat(ctx);

        TextView amount = activity.findViewById(R.id.amount);
        TextView name = activity.findViewById(R.id.name);
        TextView venue = activity.findViewById(R.id.venue);
        TextView ticketDate = activity.findViewById(R.id.date);
        TextView ticketRange = activity.findViewById(R.id.tickettext);
        TextView cat = activity.findViewById(R.id.cattext);
        TextView details = activity.findViewById(R.id.ticket_details);
        TextView ticketTime = activity.findViewById(R.id.time);
        LinearLayout dateLayout = activity.findViewById(R.id.datelayout);
        LinearLayout ticketLayout = activity.findViewById(R.id.ticketlayout);
        LinearLayout catLayout = activity.findViewById(R.id.catlayout);
        boolean detailsShown = false;

        int numberOfTickets = range.tokenIds.size();
        if (numberOfTickets > 0)
        {
            BigInteger firstTicket = range.tokenIds.get(0);
            NonFungibleToken nonFungibleToken = assetService.getNonFungibleToken(range.contractAddress, firstTicket);

            String nameStr = getTokenTitle(nonFungibleToken);
            String venueStr = (nonFungibleToken != null && nonFungibleToken.getAttribute("venue") != null)
                    ? nonFungibleToken.getAttribute("venue").text : "";
            String seatCount = String.format(Locale.getDefault(), "x%d", range.tokenIds.size());

            String textFieldVs = null;
            String textFieldNumero = null;
            String detailsText = "";
            long eventTime = 0;

            // TODO: we should be checking the contract functions for individual ticket ranges
            // TODO: Work on a coded placement system
            if (checkDynamic())
            {
                if (auxData.containsKey("building")) nameStr = auxData.get("building");
                venueStr += auxData.get("street");
                venueStr += ", ";
                venueStr += auxData.get("state");
                textFieldVs = (auxData.get("expired").equals("true")) ? "expired" : "valid";
                if (nonFungibleToken != null && nonFungibleToken.getAttribute("section") != null)
                    textFieldNumero = nonFungibleToken.getAttribute("section").value.toString(10);

                if (auxData.get("location") != null)
                {
                    detailsText = auxData.get("location");
                }

                if (auxData.get("expiry") != null)
                {
                    eventTime = Long.valueOf(auxData.get("expiry"));
                }
            }
            else if (nonFungibleToken != null)
            {
                String countryA = null;
                String countryB = null;

                if (nonFungibleToken.getAttribute("countryA") != null)
                    countryA = nonFungibleToken.getAttribute("countryA").text;
                if (nonFungibleToken.getAttribute("countryB") != null)
                    countryB = nonFungibleToken.getAttribute("countryB").text;

                if (isAlNum(countryA)) textFieldVs = countryA;
                if (isAlNum(countryB)) textFieldVs = (countryA != null) ?
                        countryA + "-" + countryB : countryB;

                if (nonFungibleToken.getAttribute("match") != null)
                {
                    String matchTxt = nonFungibleToken.getAttribute("match").name;
                    String matchVal = nonFungibleToken.getAttribute("match").text;

                    if (!matchVal.equals("0"))
                    {
                        String firstChar = matchTxt.length() > 0 ? matchTxt.substring(0,1) : "M";
                        textFieldNumero = firstChar + matchVal;
                    }
                }

                if (nonFungibleToken.getAttribute("locality") != null)
                {
                    detailsText = nonFungibleToken.getAttribute("locality").name + ": " +
                            nonFungibleToken.getAttribute("locality").text;
                }
            }

            if (nonFungibleToken != null && eventTime == 0 && nonFungibleToken.getAttribute("time") != null)
            {
                detailsShown = true;
                dateLayout.setVisibility(View.VISIBLE);
                DateTime eventDateTime;

                try
                {
                    eventDateTime = DateTimeFactory.getDateTime(nonFungibleToken.getAttribute("time"));
                }
                catch (ParseException | IllegalArgumentException e)
                {
                    detailsShown = false;
                    eventDateTime = DateTimeFactory.getCurrentTime();
                }

                ticketDate.setText(eventDateTime.format(date));
                if (eventDateTime.isZoned())
                {
                    ticketTime.setText(eventDateTime.format(time));
                    ticketTime.setVisibility(View.VISIBLE);
                }
            }
            else
            {
                dateLayout.setVisibility(View.GONE);
            }

            name.setText(nameStr);
            amount.setText(seatCount);
            venue.setText(venueStr);
            details.setText(detailsText);

            if (textFieldVs == null)
            {
                ticketLayout.setVisibility(View.GONE);
            }
            else
            {
                detailsShown = true;
                ticketLayout.setVisibility(View.VISIBLE);
                ticketRange.setText(textFieldVs);
            }

            if (textFieldNumero == null)
            {
                catLayout.setVisibility(View.GONE);
            }
            else
            {
                detailsShown = true;
                catLayout.setVisibility(View.VISIBLE);
                cat.setText(textFieldNumero);
            }

            if (!detailsShown)
            {
                //remove all info
                blankTicketExtra(activity);
            }
        }
    }

    private boolean checkDynamic()
    {
        boolean isDynamic = false;
        if (auxData != null && auxData.size() > 0)
        {
            if (auxData.containsKey("street") || auxData.containsKey("building") || auxData.containsKey("state"))
                isDynamic = true;
        }

        return isDynamic;
    }

    private String getTokenTitle(NonFungibleToken nonFungibleToken)
    {
        String tokenTitle = getFullName();
        if (nonFungibleToken != null && nonFungibleToken.getAttribute("category") != null)
        {
            String assetCategory = nonFungibleToken.getAttribute("category").text;
            if (isAlNum(assetCategory)) tokenTitle = assetCategory;
        }

        return tokenTitle;
    }

    public void checkIsMatchedInXML(AssetDefinitionService assetService)
    {
        int networkId = assetService.getChainId(getAddress());
        isMatchedInXML = networkId >= 1;
    }

    @Override
    public boolean isMatchedInXML()
    {
        return isMatchedInXML;
    }

    @Override
    public void patchAuxData(Token token)
    {
        if (token instanceof Ticket)
        {
            this.contractType = ContractType.values()[token.interfaceOrdinal()];
        }
        super.patchAuxData(token);
    }

    public Function getTradeFunction(BigInteger expiry, List<BigInteger> indices, int v, byte[] r, byte[] s)
    {
        return new Function(
                "trade",
                Arrays.asList(new org.web3j.abi.datatypes.generated.Uint256(expiry),
                                    getDynArray(indices),
                                    new org.web3j.abi.datatypes.generated.Uint8(v),
                                    new org.web3j.abi.datatypes.generated.Bytes32(r),
                                    new org.web3j.abi.datatypes.generated.Bytes32(s)),
                Collections.emptyList());
    }

    public Function getSpawnPassToFunction(BigInteger expiry, List<BigInteger> tokenIds, int v, byte[] r, byte[] s, String recipient)
    {
        return new Function(
                "spawnPassTo",
                Arrays.asList(new org.web3j.abi.datatypes.generated.Uint256(expiry),
                              getDynArray(tokenIds),
                              new org.web3j.abi.datatypes.generated.Uint8(v),
                              new org.web3j.abi.datatypes.generated.Bytes32(r),
                              new org.web3j.abi.datatypes.generated.Bytes32(s),
                              new org.web3j.abi.datatypes.Address(recipient)),
                Collections.emptyList());
    }

    public Function getTransferFunction(String to, List<BigInteger> indices)
    {
        return new Function(
                "transfer",
                Arrays.asList(new org.web3j.abi.datatypes.Address(to),
                                    getDynArray(indices)
                                ),
                Collections.emptyList());
    }

    @Override
    public boolean unspecifiedSpec()
    {
        switch (contractType)
        {
            case ERC875:
            case ERC875LEGACY:
                return false;
            default:
                return true;
        }
    }

    private org.web3j.abi.datatypes.DynamicArray getDynArray(List<BigInteger> indices)
    {
        org.web3j.abi.datatypes.DynamicArray dynArray;

        switch (contractType)
        {
            case ERC875LEGACY:
                dynArray = new org.web3j.abi.datatypes.DynamicArray<>(
                        org.web3j.abi.Utils.typeMap(indices, org.web3j.abi.datatypes.generated.Uint16.class));
                break;
            case ERC875:
            default:
                dynArray = new org.web3j.abi.datatypes.DynamicArray<>(
                        org.web3j.abi.Utils.typeMap(indices, org.web3j.abi.datatypes.generated.Uint256.class));
                break;
        }

        return dynArray;
    }

    @Override
    public int interfaceOrdinal()
    {
        return contractType.ordinal();
    }

    @Override
    public BigInteger getTokenID(int index)
    {
        if (balanceArray.size() > index && index >= 0) return balanceArray.get(index);
        else return BigInteger.valueOf(-1);
    }

    @Override
    public boolean isCurrency() {
        return false;
    }

    @Override
    protected String addSuffix(String result, Transaction transaction)
    {
        return result;
    }

    private enum InterfaceType
    {
        NotSpecified, UsingUint16, UsingUint256
    }

    @Override
    public boolean checkIntrinsicType()
    {
        return (contractType == ContractType.ERC875 || contractType == ContractType.ERC875LEGACY);
    }

    @Override
    public boolean hasArrayBalance()
    {
        return true;
    }

    @Override
    public List<BigInteger> getArrayBalance() { return balanceArray; }

    /**
     * Detect a change of balance for ERC875 balance
     * @param balanceArray
     * @return
     */
    @Override
    public boolean checkBalanceChange(List<BigInteger> balanceArray)
    {
        if (balanceArray.size() != this.balanceArray.size()) return true; //quick check for new tokens
        for (int index = 0; index < balanceArray.size(); index++) //see if spawnable token ID has changed
        {
            if (!balanceArray.get(index).equals(this.balanceArray.get(index))) return true;
        }
        return false;
    }

    @Override
    public boolean getIsSent(Transaction transaction)
    {
        boolean isSent = true;
        TransactionOperation operation = transaction.operations == null
                || transaction.operations.length == 0 ? null : transaction.operations[0];

        if (operation != null)
        {
            ERC875ContractTransaction ct = (ERC875ContractTransaction) operation.contract;
            if (ct.type > 0) isSent = false;
        }
        return isSent;
    }
}
