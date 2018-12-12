package io.stormbird.wallet.entity;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import io.stormbird.token.entity.TicketRange;
import io.stormbird.token.tools.TokenDefinition;
import io.stormbird.wallet.R;
import io.stormbird.wallet.repository.entity.RealmToken;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.ui.widget.holder.TokenHolder;
import io.stormbird.wallet.viewmodel.BaseViewModel;

import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint16;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.stormbird.wallet.C.ETH_SYMBOL;
import static io.stormbird.wallet.interact.SetupTokensInteract.EXPIRED_CONTRACT;
import static io.stormbird.wallet.interact.SetupTokensInteract.UNKNOWN_CONTRACT;

public class Token implements Parcelable
{
    public final TokenInfo tokenInfo;
    public BigDecimal balance;
    public long updateBlancaTime;
    public boolean balanceIsLive = false;
    private String tokenWallet;
    private short tokenNetwork;
    private boolean requiresAuxRefresh = true;
    protected ContractType contractType;

    public TokenTicker ticker;
    protected Map<String, String> auxData;

    public Token(TokenInfo tokenInfo, BigDecimal balance, long updateBlancaTime) {
        this.tokenInfo = tokenInfo;
        this.balance = balance;
        this.updateBlancaTime = updateBlancaTime;
    }

    protected Token(Parcel in) {
        tokenInfo = in.readParcelable(TokenInfo.class.getClassLoader());
        balance = new BigDecimal(in.readString());
        updateBlancaTime = in.readLong();
        int size = in.readInt();
        if (size > 0)
        {
            auxData = new ConcurrentHashMap<>();
            for (; size > 0; size--)
            {
                String key = in.readString();
                String value = in.readString();
                auxData.put(key, value);
            }
        }
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
        int size = (auxData == null ? 0 : auxData.size());
        dest.writeInt(size);
        if (size > 0)
        {
            for (String key : auxData.keySet())
            {
                dest.writeString(key);
                dest.writeString(auxData.get(key));
            }
        }
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

    public List<Integer> ticketIdStringToIndexList(String userList)
    {
        return null;
    }

    public String intArrayToString(List<BigInteger> idList, boolean keepZeros)
    {
        return "";
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

    public boolean isCurrency()
    {
        return true;
    }

    public void addAuxDataResult(String id, String result)
    {
        if (auxData == null) auxData = new ConcurrentHashMap<>();
        if (result == null) auxData.remove(id);
        else auxData.put(id, result);
    }

    public boolean checkRealmBalanceChange(RealmToken realmToken)
    {
        if (contractType == null || contractType.ordinal() != realmToken.getInterfaceSpec()) return true;
        String currentState = realmToken.getBalance();
        if (currentState == null) return true;
        if (tokenInfo.name != null && realmToken.getName() == null) return true; //signal to update database if correct name has been fetched (node timeout etc)
        if (tokenInfo.name == null && realmToken.getName() != null) return true;
        if (tokenInfo.symbol == null && realmToken.getSymbol() != null) return true;
        if (tokenInfo.name != null && realmToken.getName() != null) return true;
        if (tokenInfo.symbol != null && realmToken.getSymbol() == null) return true;
        if (tokenInfo.name != null && (!tokenInfo.name.equals(realmToken.getName()) || !tokenInfo.symbol.equals(realmToken.getSymbol()))) return true;
        if (tokenInfo.isStormbird != realmToken.isStormbird()) return true;
        if (checkAuxDataChanged(realmToken)) return true;
        String currentBalance = getFullBalance();
        return !currentState.equals(currentBalance);
    }

    //Detects if the auxData held in Realm is different from the current auxData.
    private boolean checkAuxDataChanged(RealmToken realmToken)
    {
        if (auxData != null && (realmToken.getAuxData() == null || realmToken.getAuxData().length() <= 4)) return true;
        else if (auxData != null && realmToken.getAuxData().length() > 4)
        {
            Map<String, String> currentRealmData = restoreAuxData(realmToken.getAuxData());
            for (String key : auxData.keySet())
            {
                if (!currentRealmData.containsKey(key)) return true;
                else if (!auxData.get(key).equals(currentRealmData.get(key))) return true;
            }
        }

        return false;
    }

    private Map<String, String> restoreAuxData(String data)
    {
        Map<String, String> aux = null;
        if (data != null && data.length() > 0)
        {
            String[] set = data.split(",");
            aux = new ConcurrentHashMap<>();
            for (int i = 0; i < (set.length - 1); i+=2)
            {
                aux.put(set[i], set[i+1]);
            }
        }

        return aux;
    }

    public void setRealmAuxData(RealmToken realmToken)
    {
        //first form the data
        if (auxData != null)
        {
            StringBuilder auxDataStr = new StringBuilder();
            for (String key : auxData.keySet())
            {
                auxDataStr.append(key);
                auxDataStr.append(",");
                auxDataStr.append(auxData.get(key));
                auxDataStr.append(",");
            }

            realmToken.setAuxData(auxDataStr.toString());
        }
    }

    public void restoreAuxDataFromRealm(RealmToken realmToken)
    {
        String values = realmToken.getAuxData();
        auxData = restoreAuxData(values);
    }

    public void setIsEthereum()
    {
        contractType = ContractType.ETHEREUM;
    }
    public boolean isEthereum()
    {
        return contractType == ContractType.ETHEREUM;
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

    public void patchAuxData(Token token)
    {
        auxData = token.auxData;
        if (auxData != null) requiresAuxRefresh = false;
    }

    public boolean checkBalanceChange(Token token)
    {
        return !getFullBalance().equals(token.getFullBalance());
    }

    public void setRealmInterfaceSpec(RealmToken realmToken)
    {
        if (isEthereum()) contractType = ContractType.ETHEREUM;
        realmToken.setInterfaceSpec(contractType.ordinal());
    }

    public void setInterfaceSpecFromRealm(RealmToken realm)
    {
        if (realm.getInterfaceSpec() > ContractType.CREATION.ordinal())
        {
            //need to re-sync this contract
            this.contractType = ContractType.NOT_SET;
        }
        else
        {
            this.contractType = ContractType.values()[realm.getInterfaceSpec()];
        }
    }

    /**
     * Stub functions - these are intended to be overridden in inherited classes.
     */
    public void setInterfaceSpec(ContractType type) { contractType = type; }
    public ContractType getInterfaceSpec() { return contractType; }
    public boolean isOldSpec() { return false; }
    public List<BigInteger> stringHexToBigIntegerList(String integerString)
    {
        return null;
    }
    public int interfaceOrdinal()
    {
        return 0;
    }
    public BigInteger getTokenID(int index)
    {
        return BigInteger.valueOf(-1);
    }
    public void auxDataRefreshed()
    {
        requiresAuxRefresh = false;
    }
    public void setRequireAuxRefresh()
    {
        requiresAuxRefresh = true;
    }
    public boolean requiresAuxRefresh()
    {
        return (requiresAuxRefresh);
    }
    public Function getTransferFunction(String to, String tokenId)
    {
        return null;
    }
    public void checkIsMatchedInXML(AssetDefinitionService assetService) { }
    public void setRealmBurn(RealmToken realmToken, List<Integer> burnList) { }
    public int[] getTicketIndices(String ticketIds) { return new int[0]; }
    public boolean unspecifiedSpec() { return contractType == ContractType.NOT_SET; };
    public void displayTicketHolder(TicketRange range, View activity, AssetDefinitionService assetService, Context ctx) { }
    public List<BigInteger> getArrayBalance() { return new ArrayList<>(); }
    public void addToBurnList(List<Uint16> burnList) { }
    public List<Integer> getBurnList() { return null; }

    public String getOperationName(Transaction transaction, Context ctx)
    {
        String name = null;
        try
        {
            if (transaction.operations != null && transaction.operations.length > 0)
            {
                TransactionOperation operation = transaction.operations[0];
                name = operation.getOperationName(ctx);
            }
            else
            {
                if (transaction.from.equals(tokenWallet))
                {
                    name = ctx.getString(R.string.sent);
                }
                else
                {
                    name = ctx.getString(R.string.received);
                }
            }
        }
        catch (NumberFormatException e)
        {
            //Silent fail, number was invalid just display default
        }

        return name;
    }

    /**
     * Universal scaled value method
     * @param valueStr
     * @param decimals
     * @return
     */
    public static String getScaledValue(String valueStr, long decimals) {
        // Perform decimal conversion
        BigDecimal value = new BigDecimal(valueStr);
        value = value.divide(new BigDecimal(Math.pow(10, decimals)));
        int scale = 4;
        return value.setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    public String getTransactionValue(Transaction transaction, Context context)
    {
        String result = "0";
        if (transaction.error.equals("1"))
        {
            return "";
        }
        else if (transaction.operations != null && transaction.operations.length > 0)
        {
            result = transaction.operations[0].getValue(tokenInfo.decimals);
        }
        else if (!transaction.value.equals("0"))
        {
            result = getScaledValue(transaction.value, tokenInfo.decimals);
        }

        if (result.length() > 0 && tokenInfo.symbol != null)
        {
            result = result + " " + tokenInfo.symbol;
        }

        if (result.length() > 0 && !result.equals("0"))
        {
            result = addSuffix(result, transaction);
        }

        return result;
    }

    protected String addSuffix(String result, Transaction transaction)
    {
        if (transaction.from.equals(tokenWallet))
        {
            result = "-" + result;
        }
        else
        {
            result = "+" + result;
        }

        return result;
    }

    public boolean checkIntrinsicType()
    {
        return (contractType == ContractType.ETHEREUM || contractType == ContractType.ERC20 || contractType == ContractType.OTHER);
    }

    public boolean isERC20()
    {
        return contractType == ContractType.ERC20;
    }

    public boolean hasArrayBalance()
    {
        return false;
    }

    public String getTokenName(AssetDefinitionService assetService)
    {
        //see if this token is covered by any contract
        int networkId = assetService.getNetworkId(getAddress());
        if (networkId >= 1)
        {
            return assetService.getAssetDefinition(getAddress()).getTokenName();
        }
        else
        {
            return tokenInfo.name;
        }
    }
}