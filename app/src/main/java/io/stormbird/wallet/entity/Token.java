package io.stormbird.wallet.entity;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import io.stormbird.token.entity.TicketRange;
import io.stormbird.wallet.R;
import io.stormbird.wallet.repository.EthereumNetworkRepository;
import io.stormbird.wallet.repository.entity.RealmToken;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.TokensService;
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
    private boolean requiresAuxRefresh = true;
    protected ContractType contractType;
    public long lastBlockCheck = 0;
    private final String shortNetworkName;
    public float balanceUpdateWeight;
    public float balanceUpdatePressure;
    public boolean balanceChanged;
    public boolean walletUIUpdateRequired;

    public String getNetworkName() { return shortNetworkName; }

    public TokenTicker ticker;
    protected Map<String, String> auxData;

    public Token(TokenInfo tokenInfo, BigDecimal balance, long updateBlancaTime, String networkName, ContractType type) {
        this.tokenInfo = tokenInfo;
        if (balance == null)
        {
            balance = BigDecimal.ZERO;
        }
        this.balance = balance;
        this.updateBlancaTime = updateBlancaTime;
        this.shortNetworkName = networkName;
        this.contractType = type;

        balanceUpdateWeight = calculateBalanceUpdateWeight();
        balanceUpdatePressure = 0.0f;
        balanceChanged = false;
        walletUIUpdateRequired = false;
    }

    public void transferPreviousData(Token oldToken)
    {
        if (oldToken != null)
        {
            lastBlockCheck = oldToken.lastBlockCheck;
        }
    }

    protected Token(Parcel in) {
        tokenInfo = in.readParcelable(TokenInfo.class.getClassLoader());
        balance = new BigDecimal(in.readString());
        updateBlancaTime = in.readLong();
        int readType = in.readInt();
        shortNetworkName = in.readString();
        balanceChanged = false;
        if (readType <= ContractType.CREATION.ordinal())
        {
            contractType = ContractType.values()[readType];
        }
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
        dest.writeString(balance == null ? "0" : balance.toString());
        dest.writeLong(updateBlancaTime);
        dest.writeInt(contractType.ordinal());
        dest.writeString(shortNetworkName);
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

    public void clickReact(BaseViewModel viewModel, Context context)
    {
        viewModel.showErc20TokenDetail(context, tokenInfo.address, tokenInfo.symbol, tokenInfo.decimals, this);
    }

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
        ethBalance = ethBalance.setScale(4, RoundingMode.HALF_DOWN).stripTrailingZeros();
        String value = ethBalance.compareTo(BigDecimal.ZERO) == 0 ? "0" : ethBalance.toPlainString();
        if (ethBalance.compareTo(BigDecimal.ZERO) == 0 && balance.compareTo(BigDecimal.ZERO) > 0)
        {
            ethBalance = balance.divide(decimalDivisor);
            //fractional value. How to represent?
            value = getMinimalString(ethBalance.toPlainString());
            if (value.length() > 6)
            {
                holder.balanceEth.setText(R.string.dust_value);
            }
            else
            {
                holder.balanceEth.setText(value);
            }
        }
        else
        {
            holder.balanceEth.setText(value);
        }

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

    private String getMinimalString(String value)
    {
        StringBuilder sb = new StringBuilder();
        for (char c : value.toCharArray())
        {
            switch (c)
            {
                case '.':
                case '0':
                    sb.append(c);
                    break;
                default:
                    sb.append(c);
                    return sb.toString();
            }
        }

        return sb.toString();
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
        return ourAddress.equalsIgnoreCase(checkAddress);
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
        return tokenInfo.symbol == null && tokenInfo.name == null;
    }

    public boolean checkTokenWallet(String address)
    {
        return tokenWallet.equalsIgnoreCase(address);
    }

    public void setTokenWallet(String address)
    {
        this.tokenWallet = address;
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

    public void setRealmLastBlock(RealmToken realmToken)
    {
        realmToken.setLastBlock(lastBlockCheck);
    }

    /**
     * Stub functions - these are intended to be overridden in inherited classes.
     */
    public void setInterfaceSpec(ContractType type) { contractType = type; }
    public ContractType getInterfaceSpec() { return contractType; }
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
    public int[] getTicketIndices(String ticketIds) { return new int[0]; }
    public boolean unspecifiedSpec() { return contractType == ContractType.NOT_SET; }

    public void displayTicketHolder(TicketRange range, View activity, AssetDefinitionService assetService, Context ctx) { }
    public List<BigInteger> getArrayBalance() { return new ArrayList<>(); }
    public boolean isMatchedInXML() { return false; }

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
        if (value.compareTo(BigDecimal.valueOf(0.0001)) < 0)
        {
            return "~0.00"; // very small amount of eth
        }
        else
        {
            return value.setScale(scale, RoundingMode.HALF_DOWN).stripTrailingZeros().toPlainString();
        }
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
        int networkId = assetService.getChainId(getAddress());
        if (networkId >= 1)
        {
            if (tokenInfo.name != null) return tokenInfo.name;
            else return assetService.getAssetDefinition(getAddress()).getTokenName();
        }
        else
        {
            return tokenInfo.name;
        }
    }

    public boolean hasRealValue()
    {
        switch (tokenInfo.chainId)
        {
            case EthereumNetworkRepository.MAINNET_ID:
            case EthereumNetworkRepository.POA_ID:
            case EthereumNetworkRepository.CLASSIC_ID:
            case EthereumNetworkRepository.XDAI_ID:
                return true;

            default:
                return false;
        }
    }

    private long getUpdateTime(long currentTime)
    {
        long updateTime = -1;
        //calculate balance update time
        if (!isTerminated() && currentTime >= 0)
        {
            if (hasRealValue())
            {
                if (isEthereum() || hasPositiveBalance())
                {
                    //schedule check in 20 seconds
                    updateTime = 1000*20;
                }
                else
                {
                    //zero balance and not base currency, use 40 second cycle.
                    updateTime = 1000*40;
                }
            }
            else
            {
                //testnet: TODO: check time since last transaction - if greater than 1 month slow update further
                if (isEthereum())
                {
                    //schedule check in 30 seconds
                    updateTime = 1000 * 40;
                }
                else if (hasPositiveBalance())
                {
                    //testnet positive balance and not base currency, use 120 second cycle.
                    updateTime = 1000 * 160;
                }
                else if (tokenInfo.name != null)
                {
                    //zero balance - very slow update cycle
                    updateTime = 1000 * 280;
                }
                else
                {
                    updateTime = 1000 * 360;
                }
            }
        }
        else
        {
            return -1;
        }

        updateTime += (long)((double)updateTime)*(0.6*Math.random()-0.2); //small variance to spread the checking load
        updateTime += currentTime;

        long millis = updateTime - System.currentTimeMillis();
        double seconds = (double)millis / 1000.0;
        Log.d("TOKEN", tokenInfo.name + " Reset in " + seconds);

        return updateTime;
    }

    private float calculateBalanceUpdateWeight()
    {
        float updateWeight = 0;
        //calculate balance update time
        if (!isTerminated() && !isBad())
        {
            if (hasRealValue())
            {
                if (isEthereum() || hasPositiveBalance())
                {
                    updateWeight = 1.0f;
                }
                else
                {
                    updateWeight = 0.5f;
                }
            }
            else
            {
                //testnet: TODO: check time since last transaction - if greater than 1 month slow update further
                if (isEthereum())
                {
                    updateWeight = 0.25f;
                }
                else if (hasPositiveBalance())
                {
                    updateWeight = 0.1f;
                }
                else if (tokenInfo.name != null)
                {
                    updateWeight = 0.05f;
                }
                else
                {
                    updateWeight = 0.01f;
                }
            }
        }

        Log.d("TOKEN", tokenInfo.name + " Update weight " + updateWeight);

        return updateWeight;
    }

    public boolean checkBalanceChange(List<BigInteger> balanceArray)
    {
        return false;
    }

    public boolean checkBalanceChange(BigDecimal balance)
    {
        if (balance != null && this.balance != null)
        {
            return !this.balance.equals(balance);
        }
        else
        {
            return false;
        }
    }

    public void updateBalanceCheckPressure()
    {
        if (!isTerminated())
        {
            balanceUpdatePressure += balanceUpdateWeight;
        }
    }

    public boolean walletUIUpdateRequired()
    {
        boolean requiresUpdate = walletUIUpdateRequired;
        walletUIUpdateRequired = false;
        return requiresUpdate;
    }

    public boolean requiresTransactionRefresh()
    {
        boolean requiresTransactionRefresh = balanceChanged;
        balanceChanged = false;
        if ((hasPositiveBalance() || isEthereum()) && lastBlockCheck == 0) //check transactions for native currency plus tokens with balance
        {
            lastBlockCheck = 1;
            requiresTransactionRefresh = true;
        }

        return requiresTransactionRefresh;
    }

    public boolean getIsSent(Transaction transaction)
    {
        return transaction.from.equalsIgnoreCase(tokenWallet);
    }
}