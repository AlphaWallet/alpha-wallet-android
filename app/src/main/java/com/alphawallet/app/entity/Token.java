package com.alphawallet.app.entity;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;

import com.alphawallet.app.interact.SetupTokensInteract;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.ui.widget.holder.TokenHolder;
import com.alphawallet.app.viewmodel.BaseViewModel;

import com.alphawallet.token.entity.TicketRange;
import com.alphawallet.app.R;

import org.web3j.abi.datatypes.Function;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Token implements Parcelable
{
    public final TokenInfo tokenInfo;
    public BigDecimal balance;
    public BigDecimal pendingBalance;
    public long updateBlancaTime;
    private String tokenWallet;
    protected ContractType contractType;
    public long lastBlockCheck = 0;
    public long lastTxCheck = 0;
    private final String shortNetworkName;
    public float balanceUpdateWeight;
    public boolean balanceChanged;
    public boolean walletUIUpdateRequired;
    public boolean hasTokenScript;
    public boolean hasDebugTokenscript;
    public boolean refreshCheck;
    public long    lastTxUpdate = 0;

    public String getNetworkName() { return shortNetworkName; }

    public TokenTicker ticker;

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
        this.pendingBalance = balance;
        this.lastBlockCheck = 0;
        this.lastTxCheck = System.currentTimeMillis() - (long)((Math.random()*60*1000));
        this.lastTxUpdate = 0;

        balanceUpdateWeight = calculateBalanceUpdateWeight();
        balanceChanged = false;
        walletUIUpdateRequired = false;
        hasTokenScript = false;
        refreshCheck = false;
    }

    public void transferPreviousData(Token oldToken)
    {
        if (oldToken != null)
        {
            lastBlockCheck = oldToken.lastBlockCheck;
            pendingBalance = oldToken.pendingBalance;
            lastTxCheck = oldToken.lastTxCheck;
            lastTxUpdate = oldToken.lastTxUpdate;
            balanceChanged = oldToken.balanceChanged;
            hasTokenScript = oldToken.hasTokenScript;
            hasDebugTokenscript = oldToken.hasDebugTokenscript;
        }
        refreshCheck = false;
    }

    protected Token(Parcel in) {
        tokenInfo = in.readParcelable(TokenInfo.class.getClassLoader());
        balance = new BigDecimal(in.readString());
        updateBlancaTime = in.readLong();
        int readType = in.readInt();
        shortNetworkName = in.readString();
        pendingBalance = new BigDecimal(in.readString());
        tokenWallet = in.readString();
        lastBlockCheck = in.readLong();
        lastTxCheck = in.readLong();
        lastTxUpdate = in.readLong();
        int readTS = in.readByte();
        int readDTS = in.readByte();
        hasTokenScript = readTS == 1;
        hasDebugTokenscript = readDTS == 1;
        balanceChanged = false;
        if (readType <= ContractType.CREATION.ordinal())
        {
            contractType = ContractType.values()[readType];
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
        dest.writeString(pendingBalance == null ? "0" : pendingBalance.toString());
        dest.writeString(tokenWallet);
        dest.writeLong(lastBlockCheck);
        dest.writeLong(lastTxCheck);
        dest.writeLong(lastTxUpdate);
        dest.writeByte(hasTokenScript?(byte)1:(byte)0);
        dest.writeByte(hasDebugTokenscript?(byte)1:(byte)0);
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
        realmToken.setAddedTime(-1);
        updateBlancaTime = -1;
    }
    public boolean isTerminated() { return (updateBlancaTime == -1); }

    public String getAddress() {
        return tokenInfo.address;
    }
    public String getFullName()
    {
        if (isTerminated()) return SetupTokensInteract.EXPIRED_CONTRACT;
        if (isBad()) return SetupTokensInteract.UNKNOWN_CONTRACT;
        String name = tokenInfo.name == null ? "" : tokenInfo.name;
        String symbol = tokenInfo.symbol == null ? "" : "(" + tokenInfo.symbol.toUpperCase() + ")";
        return name + symbol;
    }

    public String getFullName(AssetDefinitionService assetDefinition, int count)
    {
        String name = getFullName();
        String tokenTypeName = assetDefinition.getTokenName(tokenInfo.chainId, tokenInfo.address, count);
        if (name != null && tokenTypeName != null && !name.contains(tokenTypeName)) name = name + " " + tokenTypeName;

        return name;
    }

    public void clickReact(BaseViewModel viewModel, Context context)
    {
        viewModel.showErc20TokenDetail(context, tokenInfo.address, tokenInfo.symbol, tokenInfo.decimals, this);
    }

    public BigDecimal getCorrectedBalance(int scale)
    {
        if (balance.equals(BigDecimal.ZERO)) return BigDecimal.ZERO;
        BigDecimal decimalDivisor = new BigDecimal(Math.pow(10, tokenInfo.decimals));
        BigDecimal ethBalance = tokenInfo.decimals > 0
                ? balance.divide(decimalDivisor) : balance;
        return ethBalance.setScale(scale, RoundingMode.HALF_DOWN).stripTrailingZeros();
    }

    public void setupContent(TokenHolder holder, AssetDefinitionService definition)
    {
        BigDecimal ethBalance = getCorrectedBalance(4);
        String value = ethBalance.compareTo(BigDecimal.ZERO) == 0 ? "0" : ethBalance.toPlainString();
        if (ethBalance.compareTo(BigDecimal.ZERO) == 0 && balance.compareTo(BigDecimal.ZERO) > 0)
        {
            ethBalance = balance.divide(new BigDecimal(Math.pow(10, tokenInfo.decimals)));
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

            //For tokens with a long balance, wrap the name onto a new line so as not to appear cluttered
            if (value.length() > 8)
            {
                holder.symbol.setText("");
                String symbolStr = tokenInfo.symbol != null ? tokenInfo.symbol.toUpperCase() : "";
                holder.symbolAux.setVisibility(View.VISIBLE);
                holder.symbolAux.setText(TextUtils.isEmpty(tokenInfo.name)
                        ? symbolStr
                        : getFullName());
            }

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
            holder.currencyLabel.setText(ticker.priceSymbol);
        }

        holder.balanceEth.setVisibility(View.VISIBLE);
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

    public boolean isToken()
    {
        return (contractType != ContractType.ETHEREUM);
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
        String currentBalance = getFullBalance();
        return !currentState.equals(currentBalance);
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
        return tokenWallet != null && tokenWallet.equalsIgnoreCase(address);
    }

    public void setTokenWallet(String address)
    {
        this.tokenWallet = address;
    }

    public void setupRealmToken(RealmToken realmToken)
    {
        lastBlockCheck = realmToken.getLastBlock();
        lastTxUpdate = realmToken.getUpdatedTime();
    }

    public boolean checkBalanceChange(Token token)
    {
        return token != null && !getFullBalance().equals(token.getFullBalance());
    }

    public String getPendingDiff()
    {
        if (pendingBalance == null || balance.equals(pendingBalance)) return null;
        else
        {
            String prefix = "";
            BigDecimal diff = pendingBalance.subtract(balance);
            if (diff.compareTo(BigDecimal.ZERO) > 0) prefix = "+";
            String diffStr = prefix + getScaledValue(diff, tokenInfo.decimals);
            if (diffStr.startsWith("~")) diffStr = null;
            return diffStr;
        }
    }

    public boolean checkPendingChange(Token tokenUpdate)
    {
        return pendingBalance.equals(tokenUpdate.pendingBalance);
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
        realmToken.setUpdatedTime(lastTxUpdate);
    }

    /**
     * Stub functions - these are intended to be overridden in inherited classes.
     */
    public void setInterfaceSpec(ContractType type)
    {
        contractType = type;
    }
    public ContractType getInterfaceSpec() { return contractType; }
    public int interfaceOrdinal()
    {
        return 0;
    }
    public BigInteger getTokenID(int index)
    {
        return BigInteger.valueOf(-1);
    }
    public Function getTransferFunction(String to, String tokenId)
    {
        return null;
    }
    public void checkIsMatchedInXML(AssetDefinitionService assetService) { }
    public int[] getTicketIndices(String ticketIds) { return new int[0]; }
    public boolean unspecifiedSpec() { return contractType == ContractType.NOT_SET; }

    public void displayTicketHolder(TicketRange range, View activity, AssetDefinitionService assetService, Context ctx, boolean iconified) { }
    public void displayTicketHolder(TicketRange range, View activity, AssetDefinitionService assetService, Context ctx) { }
    public List<BigInteger> getArrayBalance() { return new ArrayList<>(); }
    public List<BigInteger> getNonZeroArrayBalance() { return new ArrayList<>(Arrays.asList(BigInteger.ZERO)); }
    public boolean isMatchedInXML() { return false; }

    public String getOperationName(Transaction transaction, Context ctx)
    {
        String name = null;
        try
        {
            if (transaction.blockNumber != null && transaction.blockNumber.equals("0"))
            {
                name = ctx.getString(R.string.status_pending);
            }
            else if (transaction.operations != null && transaction.operations.length > 0)
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

    public String getScaledBalance()
    {
        return getScaledValue(balance, tokenInfo.decimals);
    }

    public static String getScaledValue(BigDecimal value, long decimals) { return getScaledValue(value, decimals, false); }

    public static String getScaledValue(BigDecimal value, long decimals, boolean stripZeros)
    {
        value = value.divide(new BigDecimal(Math.pow(10, decimals)));
        int scale = 4;
        if (value.equals(BigDecimal.ZERO))
        {
            if (stripZeros) return "0";
            else return "0.0000";
        }
        if (value.abs().compareTo(BigDecimal.valueOf(0.0001)) < 0)
        {
            if (stripZeros) return "~0.00";
            else return "0.0000";
        }
        else if (stripZeros)
        {
            return value.setScale(scale, RoundingMode.HALF_DOWN).stripTrailingZeros().toPlainString();
        }
        else
        {
            return value.setScale(scale, RoundingMode.HALF_DOWN).toPlainString();
        }
    }

    /**
     * Universal scaled value method
     * @param valueStr
     * @param decimals
     * @return
     */
    public static String getScaledValue(String valueStr, long decimals) {
        // Perform decimal conversion
        if (decimals > 1 && valueStr != null && valueStr.length() > 0 && Character.isDigit(valueStr.charAt(0)))
        {
            BigDecimal value = new BigDecimal(valueStr);
            return getScaledValue(value, decimals); //represent balance transfers according to 'decimals' contract indicator property
        }
        else if (valueStr != null)
        {
            return valueStr;
        }
        else
        {
            return "0";
        }
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

        if (result.length() > 0 && !result.equals("0") && !result.startsWith("~"))
        {
            result = addSuffix(result, transaction);
        }

        return result;
    }

    protected String addSuffix(String result, Transaction transaction)
    {
        if (transaction.from.equalsIgnoreCase(tokenWallet.toLowerCase()))
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

    public String getTokenName(AssetDefinitionService assetService, int count)
    {
        //see if this token is covered by any contract
        if (assetService.hasDefinition(tokenInfo.chainId, tokenInfo.address))
        {
            if (tokenInfo.name != null) return tokenInfo.name;
            else return assetService.getAssetDefinition(tokenInfo.chainId, getAddress()).getTokenName(count);
        }
        else
        {
            return tokenInfo.name;
        }
    }

    public boolean hasRealValue()
    {
        return EthereumNetworkRepository.hasRealValue(tokenInfo.chainId);
    }

    protected float calculateBalanceUpdateWeight()
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

        //Log.d("TOKEN", tokenInfo.name + " Update weight " + updateWeight);

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

    public boolean walletUIUpdateRequired()
    {
        boolean requiresUpdate = walletUIUpdateRequired;
        walletUIUpdateRequired = false;
        return requiresUpdate;
    }

    public boolean requiresTransactionRefresh()
    {
        boolean requiresUpdate = balanceChanged;
        balanceChanged = false;
        if ((hasPositiveBalance() || isEthereum()) && lastBlockCheck == 0) //check transactions for native currency plus tokens with balance
        {
            lastBlockCheck = 1;
        }

        long currentTime = System.currentTimeMillis();

        long multiplier = isEthereum() || EthereumNetworkRepository.isPriorityToken(this) ? 1 : 5;

        //ensure chain transactions for the wallet are checked on a regular basis.
        if (EthereumNetworkRepository.hasTicker(this) && hasPositiveBalance() && (currentTime - lastTxCheck) > multiplier*60*1000) //need to check main chains once per minute
        {
            lastTxCheck = currentTime; //don't check again
            requiresUpdate = true;
        }

        return requiresUpdate;
    }

    public boolean getIsSent(Transaction transaction)
    {
        return transaction.from.equalsIgnoreCase(tokenWallet);
    }

    public String getWallet()
    {
        return tokenWallet;
    }

    public String pruneIDList(String ticketIds, int quantity)
    {
        return "";
    }

    public void setFocus(boolean focus)
    {
        if (focus)
        {
            balanceUpdateWeight = 2.0f;
        }
        else
        {
            balanceUpdateWeight = calculateBalanceUpdateWeight();
        }
    }

    public boolean balanceIncrease(Token token)
    {
        return balance != null && token.balance != null && balance.compareTo(token.balance) > 0;
    }

    public boolean equals(Token token)
    {
        return token != null && tokenInfo.chainId == token.tokenInfo.chainId && getAddress().equals(token.getAddress());
    }

    public void zeroiseBalance()
    {
        balance = BigDecimal.ZERO;
        pendingBalance = BigDecimal.ZERO;
    }

    public String getTokenTitle()
    {
        return tokenInfo.name;
    }

    public boolean isERC875() { return false; }
}