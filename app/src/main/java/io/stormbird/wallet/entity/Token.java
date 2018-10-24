package io.stormbird.wallet.entity;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import io.stormbird.token.tools.TokenDefinition;
import io.stormbird.wallet.R;
import io.stormbird.wallet.repository.entity.RealmToken;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.ui.widget.holder.TokenHolder;
import io.stormbird.wallet.viewmodel.BaseViewModel;

import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static io.stormbird.wallet.C.ETH_SYMBOL;
import static io.stormbird.wallet.interact.SetupTokensInteract.EXPIRED_CONTRACT;
import static io.stormbird.wallet.interact.SetupTokensInteract.UNKNOWN_CONTRACT;

public class Token implements Parcelable
{
    public final TokenInfo tokenInfo;
    public BigDecimal balance;
    public long updateBlancaTime;
    public boolean balanceIsLive = false;
    public boolean isERC20 = false; //TODO: when we see ERC20 functions in transaction decoder switch this on
    private boolean isEth = false;
    private String tokenWallet;
    private short tokenNetwork;

    public TokenTicker ticker;

    public Token(TokenInfo tokenInfo, BigDecimal balance, long updateBlancaTime) {
        this.tokenInfo = tokenInfo;
        this.balance = balance;
        this.updateBlancaTime = updateBlancaTime;
    }

    protected Token(Parcel in) {
        tokenInfo = in.readParcelable(TokenInfo.class.getClassLoader());
        balance = new BigDecimal(in.readString());
        updateBlancaTime = in.readLong();
    }

    public String getStringBalance() {
        //should apply BigDecimal conversion here
        if (balance != null) return balance.toString();
        else return "0";
    }

    public boolean hasPositiveBalance() {
        if (balance != null) return !balance.equals(BigDecimal.ZERO);
        else return false;
    }

    public boolean independentUpdate()
    {
        return false;
    }

    public String getFullBalance() {
        return getStringBalance();
    }

    public String getBurnListStr() {
        return "";
    }

    public static final Creator<Token> CREATOR = new Creator<Token>() {
        @Override
        public Token createFromParcel(Parcel in) {
            return new Token(in);
        }

        @Override
        public Token[] newArray(int size) {
            return new Token[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(tokenInfo, flags);
        dest.writeString(balance.toString());
        dest.writeLong(updateBlancaTime);
    }

    public void setRealmBalance(RealmToken realmToken)
    {
        if (balance != null)
        {
            realmToken.setBalance(balance.toString());
        }
        else
        {
            realmToken.setBalance("0");
        }
    }

    public void setIsTerminated(RealmToken realmToken)
    {
        realmToken.setUpdatedTime(-1);
        updateBlancaTime = -1;
    }
    public boolean isTerminated() { return (updateBlancaTime == -1); }

    public String getAddress() {
        return tokenInfo.address;
    }
    public String getFullName()
    {
        if (isTerminated()) return EXPIRED_CONTRACT;
        if (isBad()) return UNKNOWN_CONTRACT;
        return tokenInfo.name + (tokenInfo.symbol != null && tokenInfo.symbol.length() > 0 ? "(" + tokenInfo.symbol.toUpperCase() + ")" : "");
    }

    public BigInteger getIntAddress() { return Numeric.toBigInt(tokenInfo.address); }

    public void clickReact(BaseViewModel viewModel, Context context)
    {
        viewModel.showSendToken(context, tokenInfo.address, tokenInfo.symbol, tokenInfo.decimals, this);
    }

    public String populateIDs(List<Integer> d, boolean keepZeros)
    {
        return "";
    }
    public static final String EMPTY_BALANCE = "\u2014\u2014";

    public boolean needsUpdate()
    {
        long now = System.currentTimeMillis();
        long diff = (now - updateBlancaTime) / 1000; //seconds

        if (diff > 50) // value is stale
        {
            Log.d("TOKEN", tokenInfo.name + " DIFF: " + diff);
            return balanceIsLive;
        }
        else
        {
            return !balanceIsLive;
        }
    }

    /**
     * This function should check if the balance of the token is stale or not
     * However the recycler view is subject to its own rules and laws, which I haven't decoded.
     * This is a TODO.
     * @param ctx
     * @param holder
     */
    public void checkUpdateTimeValid(Context ctx, TokenHolder holder)
    {
        long now = System.currentTimeMillis();
        long diff = (now - updateBlancaTime) / 1000; //seconds

        if (diff > 50) // value is stale
        {
            Log.d("TOKEN", tokenInfo.name + " DIFF: " + diff);
            holder.balanceEth.setTextColor(ContextCompat.getColor(ctx, R.color.holo_blue));
            holder.symbol.setTextColor(ContextCompat.getColor(ctx, R.color.holo_blue));
            balanceIsLive = false;
        }
        else
        {
            holder.balanceEth.setTextColor(ContextCompat.getColor(ctx, R.color.black));
            holder.symbol.setTextColor(ContextCompat.getColor(ctx, R.color.black));
            balanceIsLive = true;
        }
    }

    public void setupContent(TokenHolder holder, AssetDefinitionService definition)
    {
        BigDecimal decimalDivisor = new BigDecimal(Math.pow(10, tokenInfo.decimals));
        BigDecimal ethBalance = tokenInfo.decimals > 0
                ? balance.divide(decimalDivisor) : balance;
        ethBalance = ethBalance.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
        String value = ethBalance.compareTo(BigDecimal.ZERO) == 0 ? "0" : ethBalance.toPlainString();
        holder.balanceEth.setText(value);
        holder.issuer.setText(R.string.ethereum);

        if (isEthereum())
        {
            holder.textAppreciationSub.setText(R.string.appreciation);
            holder.icon.setVisibility(View.GONE);
            holder.text24HoursSub.setText(R.string.twenty_four_hours);
            holder.contractType.setVisibility(View.GONE);
            holder.contractSeparator.setVisibility(View.GONE);
            holder.layoutValueDetails.setVisibility(View.VISIBLE);
        }
        else
        {
            holder.contractType.setVisibility(View.VISIBLE);
            holder.contractSeparator.setVisibility(View.VISIBLE);
            holder.contractType.setText(R.string.erc20);
            holder.layoutValueDetails.setVisibility(View.GONE);
            //currently we don't collect the value of ERC20 tokens
            //TODO: get ticker for ERC20 tokens
        }

        //populate ticker if we have it
        if (ticker != null)
        {
            holder.layoutValueDetails.setVisibility(View.VISIBLE);
            holder.textAppreciationSub.setText(R.string.appreciation);
            holder.fillCurrency(ethBalance, ticker);
            holder.text24HoursSub.setText(R.string.twenty_four_hours);
        }

        holder.balanceEth.setVisibility(View.VISIBLE);
        holder.arrayBalance.setVisibility(View.GONE);
    }

    public void setRealmBurn(RealmToken realmToken, List<Integer> burnList)
    {

    }

    public List<Integer> ticketIdStringToIndexList(String userList)
    {
        return null;
    }

    public List<Integer> stringIntsToIntegerList(String userList)
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
                idList.add(intId);
            }
        }
        catch (Exception e)
        {
            idList = new ArrayList<>();
        }

        return idList;
    }

    public String integerListToString(List<Integer> intList, boolean keepZeros)
    {
        if (intList == null) return "";
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (Integer id : intList)
        {
            if (!keepZeros && id == 0) continue;
            if (!first)sb.append(",");
            sb.append(String.valueOf(id));
            first = false;
        }

        return sb.toString();
    }

    public int getTicketCount()
    {
        return balance.intValue();
    }

    public boolean addressMatches(String contractAddress)
    {
        String checkAddress = Numeric.cleanHexPrefix(contractAddress);
        String ourAddress = Numeric.cleanHexPrefix(getAddress());
        if (ourAddress.equalsIgnoreCase(checkAddress))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean isCurrency() {
        return !tokenInfo.isStormbird;
    }

    public List<Integer> indexToIDList(int[] prunedIndices)
    {
        return null;
    }

    public boolean checkRealmBalanceChange(RealmToken realmToken)
    {
        String currentState = realmToken.getBalance();
        if (currentState == null) return true;
        if (tokenInfo.name != null && realmToken.getName() == null) return true; //signal to update database if correct name has been fetched (node timeout etc)
        if (tokenInfo.isStormbird != realmToken.isStormbird()) return true;
        String currentBalance = getFullBalance();
        return !currentState.equals(currentBalance);
    }

    public void setIsEthereum()
    {
        isEth = true;
    }
    public boolean isEthereum()
    {
        return (tokenInfo != null && tokenInfo.symbol != null && isEth);
    }

    public boolean isBad()
    {
        return tokenInfo.name == null || tokenInfo.name.length() < 2;
    }

    public boolean checkTokenWallet(String address)
    {
        return tokenWallet.equals(address);
    }

    public boolean checkTokenNetwork(int currentNetwork)// setTokenWallet(String tokenWallet)
    {
        return tokenNetwork == currentNetwork;
    }

    public void setTokenWallet(String address)
    {
        this.tokenWallet = address;
    }

    public void setTokenNetwork(int tokenNetwork)
    {
        this.tokenNetwork = (short)tokenNetwork;
    }
}