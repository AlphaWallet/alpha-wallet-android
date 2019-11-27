package com.alphawallet.app.entity.tokens;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.ERC875ContractTransaction;
import com.alphawallet.app.entity.TicketRangeElement;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionOperation;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.ui.widget.holder.TokenHolder;
import com.alphawallet.app.viewmodel.BaseViewModel;
import com.alphawallet.app.web3.Web3TokenView;
import com.alphawallet.token.entity.TicketRange;
import com.alphawallet.token.entity.TokenScriptResult;
import com.alphawallet.token.tools.TokenDefinition;

import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ERC721Ticket extends Token implements Parcelable {

    private final List<BigInteger> balanceArray;
    private boolean isMatchedInXML = false;

    public ERC721Ticket(TokenInfo tokenInfo, List<BigInteger> balances, long blancaTime, String networkName, ContractType type) {
        super(tokenInfo, BigDecimal.ZERO, blancaTime, networkName, type);
        this.balanceArray = balances;
    }

    public ERC721Ticket(TokenInfo tokenInfo, String balances, long blancaTime, String networkName, ContractType type) {
        super(tokenInfo, BigDecimal.ZERO, blancaTime, networkName, type);
        this.balanceArray = stringHexToBigIntegerList(balances);
    }

    private ERC721Ticket(Parcel in) {
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

    public static final Creator<ERC721Ticket> CREATOR = new Creator<ERC721Ticket>() {
        @Override
        public ERC721Ticket createFromParcel(Parcel in) {
            return new ERC721Ticket(in);
        }

        @Override
        public ERC721Ticket[] newArray(int size) {
            return new ERC721Ticket[size];
        }
    };

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

        tokenHolder.issuer.setText(R.string.ethereum);
        tokenHolder.contractType.setVisibility(View.GONE);
        tokenHolder.contractSeparator.setVisibility(View.GONE);
        tokenHolder.layoutValueDetails.setVisibility(View.GONE);
        tokenHolder.icon.setVisibility(View.GONE);
        tokenHolder.icon.setImageResource(R.drawable.ic_ethereum);
        tokenHolder.chainName.setVisibility(View.GONE);
        tokenHolder.extendedInfo.setVisibility(View.VISIBLE);
        tokenHolder.blockchain.setVisibility(View.GONE);

        String composite = getTicketCount() + " " + getFullName(asset, getTicketCount());
        tokenHolder.balanceEth.setText(composite);

        tokenHolder.layoutValueDetails.setVisibility(View.GONE);
    }

    @Override
    public int[] getTicketIndices(String ticketIds)
    {
       return null;
    }

    /*************************************
     *
     * Conversion functions used for manipulating indices
     *
     */

    /**
     * Convert a String list of ticket IDs into a list of ticket indices
     * @param userList
     * @return
     */
    @Override
    public List<Integer> ticketIdStringToIndexList(String userList)
    {
        return null;
    }

    private void blankTicketExtra(View activity)
    {
        try
        {
            TextView textVenue = activity.findViewById(R.id.venue);

            //textVenue.setVisibility(View.GONE);
            textVenue.setText("");
        }
        catch (Exception e)
        {
            Log.d("TICKET", e.getMessage());
        }
    }

    private void displayTokenscriptView(TicketRange range, AssetDefinitionService assetService, View activity, Context ctx, boolean iconified)
    {
        //get webview
        Web3TokenView tokenView = activity.findViewById(R.id.web3_tokenview);
        ProgressBar waitSpinner = activity.findViewById(R.id.progress_element);
        activity.findViewById(R.id.layout_webwrapper).setVisibility(View.VISIBLE);
        activity.findViewById(R.id.layout_legacy).setVisibility(View.GONE);

        waitSpinner.setVisibility(View.VISIBLE);
        BigInteger tokenId = range.tokenIds.get(0);

        final StringBuilder attrs = assetService.getTokenAttrs(this, tokenId, range.tokenIds.size());

        assetService.resolveAttrs(this, tokenId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(attr -> onAttr(attr, attrs), throwable -> onError(throwable, ctx, assetService, attrs, waitSpinner, tokenView, iconified),
                        () -> displayTicket(ctx, assetService, attrs, waitSpinner, tokenView, iconified))
                .isDisposed();
    }

    private void displayTicket(Context ctx, AssetDefinitionService assetService, StringBuilder attrs, ProgressBar waitSpinner, Web3TokenView tokenView, boolean iconified)
    {
        waitSpinner.setVisibility(View.GONE);
        tokenView.setVisibility(View.VISIBLE);

        String view = assetService.getTokenView(tokenInfo.chainId, getAddress(), iconified ? "view-iconified" : "view");
        String style = assetService.getTokenView(tokenInfo.chainId, getAddress(), "style");
        String viewData = tokenView.injectWeb3TokenInit(ctx, view, attrs.toString());
        viewData = tokenView.injectStyleData(viewData, style); //style injected last so it comes first

        String base64 = android.util.Base64.encodeToString(viewData.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
        tokenView.loadData(base64, "text/html; charset=utf-8", "base64");
    }

    private void onError(Throwable throwable, Context ctx, AssetDefinitionService assetService, StringBuilder attrs, ProgressBar waitSpinner, Web3TokenView tokenView, boolean iconified)
    {
        throwable.printStackTrace();
        displayTicket(ctx, assetService, attrs, waitSpinner, tokenView, iconified);
    }

    private void onAttr(TokenScriptResult.Attribute attribute, StringBuilder attrs)
    {
        TokenScriptResult.addPair(attrs, attribute.id, attribute.text);
    }

    public void displayTicketHolder(TicketRange range, View activity, AssetDefinitionService assetService, Context ctx)
    {
        displayTicketHolder(range, activity, assetService, ctx, false);
    }

    /**
     * This is a single method that populates any instance of graphic ticket anywhere
     *
     * @param range
     * @param activity
     * @param assetService
     * @param ctx needed to create date/time format objects
     */
    public void displayTicketHolder(TicketRange range, View activity, AssetDefinitionService assetService, Context ctx, boolean iconified)
    {
        TokenDefinition td = assetService.getAssetDefinition(tokenInfo.chainId, tokenInfo.address);
        if (td != null)
        {
            //use webview
            displayTokenscriptView(range, assetService, activity, ctx, iconified);
        }
        else
        {
            TextView amount = activity.findViewById(R.id.amount);
            TextView name = activity.findViewById(R.id.name);

            String nameStr = getTokenTitle();
            String seatCount = String.format(Locale.getDefault(), "x%d", range.tokenIds.size());

            name.setText(nameStr);
            amount.setText(seatCount);

            blankTicketExtra(activity);
        }
    }

    public void checkIsMatchedInXML(AssetDefinitionService assetService)
    {
        isMatchedInXML = assetService.hasDefinition(tokenInfo.chainId, tokenInfo.address);
    }

    @Override
    public boolean isMatchedInXML()
    {
        return isMatchedInXML;
    }

    public Function getTradeFunction(BigInteger expiry, List<BigInteger> tokenIds, int v, byte[] r, byte[] s)
    {
        return new Function(
                "trade",
                Arrays.asList(new org.web3j.abi.datatypes.generated.Uint256(expiry),
                        getDynArray(tokenIds),
                        new org.web3j.abi.datatypes.generated.Uint8(v),
                        new org.web3j.abi.datatypes.generated.Bytes32(r),
                        new org.web3j.abi.datatypes.generated.Bytes32(s)),
                Collections.emptyList());
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

    public Function getTransferFunction(String to, BigInteger tokenId)
    {
        return new Function(
                "safeTransferFrom",
                Arrays.asList(
                        new org.web3j.abi.datatypes.Address(this.getWallet()),
                        new org.web3j.abi.datatypes.Address(to),
                        new Uint256(tokenId)
                ), Collections.emptyList());
    }

    @Override
    public boolean unspecifiedSpec()
    {
        return contractType.equals(ContractType.ERC721_TICKET);
    }

    private org.web3j.abi.datatypes.DynamicArray getDynArray(List<BigInteger> tokenIds)
    {
        return new org.web3j.abi.datatypes.DynamicArray<>(
                org.web3j.abi.datatypes.generated.Uint256.class,
                org.web3j.abi.Utils.typeMap(tokenIds, org.web3j.abi.datatypes.generated.Uint256.class));
    }

    /**
     * Refresh transactions for TokenScript enabled tokens at startup, once per 5 minutes
     * and finally if the user does a refresh
     *
     * TODO: This heuristic becomes redundant once we enable event support
     * @return token requires a transaction refresh
     */
    @Override
    public boolean requiresTransactionRefresh()
    {
        boolean requiresUpdate = balanceChanged;
        balanceChanged = false;
        if (hasTokenScript && hasPositiveBalance())
        {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastTxCheck > 5*60*1000) //need to check transactions for function updates, at startup and every 5 minutes is good
            {
                lastTxCheck = currentTime;
                refreshCheck = false;
                requiresUpdate = true;
            }
        }

        return requiresUpdate;
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
    protected String addSuffix(String result, Transaction transaction)
    {
        return result;
    }

    @Override
    public boolean checkIntrinsicType()
    {
        return contractType == ContractType.ERC721_TICKET;
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

    /**
     * Detect a change of balance for ERC721 balance
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

        if (operation != null && operation.contract instanceof ERC875ContractTransaction)
        {
            ERC875ContractTransaction ct = (ERC875ContractTransaction) operation.contract;
            if (ct.type > 0) isSent = false;
        }
        return isSent;
    }

    @Override
    public boolean isERC875() {
        return false;
    }
    @Override
    public boolean isToken() {
        return false;
    }
    @Override
    public boolean isERC721Ticket() { return true; }

    @Override
    public List<BigInteger> getTicketsAsBigIntList(String ticketIds) {
        List<BigInteger> tokenIds = new ArrayList<>();
        String[] tickets = ticketIds.split(",");
        for(String ticket: tickets) {
            tokenIds.add(new BigInteger(ticket, 16));
        }
        return tokenIds;
    }

    @Override
    public boolean groupWithToken(TicketRange currentGroupingRange, TicketRangeElement newElement, long currentGroupTime)
    {
        //don't group any ERC721 tickets in the asset view
        return false;
    }
}
