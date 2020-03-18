package com.alphawallet.app.entity.tokens;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.TicketRangeElement;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionOperation;
import com.alphawallet.app.entity.opensea.Asset;
import com.alphawallet.app.interact.SetupTokensInteract;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.ui.widget.holder.TokenHolder;
import com.alphawallet.app.viewmodel.BaseViewModel;
import com.alphawallet.app.web3.Web3TokenView;
import com.alphawallet.token.entity.TicketRange;
import com.alphawallet.token.entity.TokenScriptResult;
import com.alphawallet.token.tools.TokenDefinition;

import org.web3j.abi.datatypes.Function;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class Token implements Parcelable, Comparable<Token>
{
    public final TokenInfo tokenInfo;
    public BigDecimal balance;
    public BigDecimal pendingBalance;
    public long updateBlancaTime;
    private String tokenWallet;
    protected ContractType contractType;
    public long lastBlockCheck = 0;
    private final String shortNetworkName;
    public float balanceUpdateWeight;
    public boolean balanceChanged;
    public boolean walletUIUpdateRequired;
    public boolean hasTokenScript;
    public boolean hasDebugTokenscript;
    public boolean refreshCheck;
    public long lastTxCheck;
    public long lastTxUpdate;
    public long lastTxTime;

    public int iconifiedWebviewHeight;
    public int nonIconifiedWebviewHeight;
    private int nameWeight;

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
        this.lastTxUpdate = 0;
        this.lastTxCheck = 0;
        this.lastBlockCheck = 0;
        this.lastTxTime = 0;
        balanceUpdateWeight = calculateBalanceUpdateWeight();
        balanceChanged = false;
        walletUIUpdateRequired = false;
        hasTokenScript = false;
        refreshCheck = false;
        nameWeight = calculateTokenNameWeight();
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
            lastTxTime = oldToken.lastTxTime;
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
        lastTxTime = in.readLong();
        hasTokenScript = in.readByte() == 1;
        hasDebugTokenscript = in.readByte() == 1;
        nonIconifiedWebviewHeight = in.readInt();
        iconifiedWebviewHeight = in.readInt();
        nameWeight = in.readInt();

        balanceChanged = false;
        if (readType <= ContractType.CREATION.ordinal())
        {
            contractType = ContractType.values()[readType];
        }
    }

    public String getStringBalance() {
        BigDecimal correctedBalance = getCorrectedBalance(4);
        return correctedBalance.compareTo(BigDecimal.ZERO) == 0 ? "0" : correctedBalance.toPlainString();
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
        if (balance != null) return balance.toString();
        else return "0";
    }

    public Asset getAssetForToken(String tokenId) {
        return null;
    }

    public void addAssetToTokenBalanceAssets(Asset asset) {
        //only for ERC721, see override in ERC721Token
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
        dest.writeLong(lastTxTime);
        dest.writeByte(hasTokenScript?(byte)1:(byte)0);
        dest.writeByte(hasDebugTokenscript?(byte)1:(byte)0);
        dest.writeInt(nonIconifiedWebviewHeight);
        dest.writeInt(iconifiedWebviewHeight);
        dest.writeInt(nameWeight);
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
        realmToken.setUpdateTime(-1);
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
        String symbol = (tokenInfo.symbol == null || tokenInfo.symbol.length() == 0) ? "" : " (" + tokenInfo.symbol.toUpperCase() + ")";
        return name + symbol;
    }

    public String getFullName(AssetDefinitionService assetDefinition, int count)
    {
        //override contract name with TS defined name
        String name = assetDefinition.getTokenName(tokenInfo.chainId, tokenInfo.address, count);
        if (name != null) {
            String symbol = (tokenInfo.symbol == null || tokenInfo.symbol.length() == 0) ? "" : " (" + tokenInfo.symbol.toUpperCase() + ")";
            return name + symbol;
        } else {
            return getFullName();
        }
    }

    public String getSymbol()
    {
        if (tokenInfo.symbol == null) return "";
        else return tokenInfo.symbol.toUpperCase();
    }

    public void clickReact(BaseViewModel viewModel, Context context)
    {
        viewModel.showErc20TokenDetail(context, tokenInfo.address, tokenInfo.symbol, tokenInfo.decimals, this);
    }

    public BigDecimal getCorrectedBalance(int scale)
    {
        if (balance == null || balance.equals(BigDecimal.ZERO)) return BigDecimal.ZERO;
        BigDecimal decimalDivisor = new BigDecimal(Math.pow(10, tokenInfo.decimals));
        BigDecimal ethBalance = tokenInfo.decimals > 0
                ? balance.divide(decimalDivisor) : balance;
        return ethBalance.setScale(scale, RoundingMode.HALF_DOWN).stripTrailingZeros();
    }

    public void setupContent(TokenHolder holder, AssetDefinitionService definition)
    {
        BigDecimal ethBalance = getCorrectedBalance(4);
        String     value      = ethBalance.compareTo(BigDecimal.ZERO) == 0 ? "0" : ethBalance.toPlainString();
        if (ethBalance.compareTo(BigDecimal.ZERO) == 0 && balance.compareTo(BigDecimal.ZERO) > 0)
        {
            ethBalance = balance.divide(new BigDecimal(Math.pow(10, tokenInfo.decimals)));
            //fractional value. How to represent?
            value = getMinimalString(ethBalance.toPlainString());
            if (ethBalance.compareTo(BigDecimal.valueOf(0.000001)) < 0)
            {
                value = holder.getString(R.string.dust_value);
            }
        }

        holder.balanceEth.setText(value);

        if (isEthereum())
        {
            holder.textAppreciationSub.setText(R.string.appreciation);
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
        }

        addTokenName(holder, definition);

        //populate ticker if we have it
        if (ticker != null)
        {
            holder.layoutValueDetails.setVisibility(View.VISIBLE);
            holder.textAppreciationSub.setText(R.string.appreciation);
            holder.fillCurrency(ethBalance, ticker);
            holder.text24HoursSub.setText(R.string.twenty_four_hours);
            holder.currencyLabel.setText(ticker.priceSymbol);
        }
        else if (isEthereum())
        {
            holder.layoutValueDetails.setVisibility(View.VISIBLE);
            holder.animateTextWhileWaiting();
            holder.emptyTicker();
        }
        else
        {
            holder.layoutValueDetails.setVisibility(View.GONE);
        }

        holder.balanceEth.setVisibility(View.VISIBLE);
    }

    void addTokenName(TokenHolder holder, AssetDefinitionService definitionService)
    {
        String balance = holder.balanceEth.getText().toString();
        String symbolStr = tokenInfo.symbol != null ? tokenInfo.symbol.toUpperCase() : "";
        String nameTxt = TextUtils.isEmpty(tokenInfo.name)
                         ? symbolStr
                         : getFullName(definitionService, getTicketCount());

        String composite = balance + " " + nameTxt;
        holder.balanceEth.setText(composite);
    }

    public List<Asset> getTokenAssets() {
        return null;
    }

    public List<BigInteger> ticketIdStringToIndexList(String userList)
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
    public String bigIntListToString(List<BigInteger> idList, boolean keepZeros)
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
        lastTxUpdate = realmToken.getTXUpdateTime();
        lastTxCheck = realmToken.getTXUpdateTime();
        lastTxTime = realmToken.getLastTxTime();
        tokenInfo.isEnabled = realmToken.getEnabled();
    }

    public boolean checkBalanceChange(Token token)
    {
        return token != null && (!getFullBalance().equals(token.getFullBalance()) || !getFullName().equals(token.getFullName()));
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
        realmToken.setTXUpdateTime(lastTxUpdate);
        realmToken.setLastTxTime(lastTxTime);
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
    public Function getTransferFunction(String to, List<BigInteger> transferData) throws NumberFormatException
    {
        return null;
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

    public void checkIsMatchedInXML(AssetDefinitionService assetService) { }
    public int[] getTicketIndices(String ticketIds) { return new int[0]; }
    public boolean contractTypeValid() { return !(contractType == ContractType.NOT_SET || contractType == ContractType.OTHER); }
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

    /**
     * Determine how often we check balance of tokens.
     *
     * Note that if any transaction relating to a contract is received then we trigger a balance check
     *
     * @return
     */
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
                    updateWeight = 1.0f; //30 seconds
                }
                else
                {
                    updateWeight = 0.5f; //1 minute
                }
            }
            else
            {
                //testnet: TODO: check time since last transaction - if greater than 1 month slow update further
                if (isEthereum())
                {
                    updateWeight = 0.5f; //1 minute
                }
                else if (hasPositiveBalance())
                {
                    updateWeight = 0.3f; //100 seconds
                }
                else
                {
                    updateWeight = 0.1f; //5 minutes
                }
            }
        }
        return updateWeight;
    }

    public boolean checkBalanceChange(List<BigInteger> balanceArray)
    {
        return false;
    }

    public boolean checkBalanceChange(BigDecimal balance)
    {
        balanceUpdateWeight = calculateBalanceUpdateWeight();
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
        return walletUIUpdateRequired;
    }

    public boolean requiresTransactionRefresh(int updateChain)
    {
        boolean requiresUpdate = balanceChanged;
        balanceChanged = false;
        if ((hasPositiveBalance() || isEthereum()) && lastBlockCheck == 0) //check transactions for native currency plus tokens with balance
        {
            lastBlockCheck = 1;
        }

        long currentTime = System.currentTimeMillis();

        long multiplier = isEthereum() || EthereumNetworkRepository.isPriorityToken(this) ? 1 : 5;

        if (isEthereum() && tokenInfo.chainId == updateChain && (currentTime - lastTxCheck) > 10*1000) //check chain every 10 seconds while transaction is pending
        {
            lastTxCheck = currentTime;
            requiresUpdate = true;
        }

        //ensure chain transactions for the wallet are checked on a regular basis.
        if (checkBackgroundUpdate(currentTime) || (EthereumNetworkRepository.hasTicker(this) && hasPositiveBalance() && (currentTime - lastTxCheck) > multiplier*60*1000)) //need to check main chains once per minute
        {
            lastTxCheck = currentTime; //don't check again
            requiresUpdate = true;
        }

        return requiresUpdate;
    }

    private boolean checkBackgroundUpdate(long currentTime)
    {
        //check balance of empty mainnet chains once per hour
        return isEthereum() && !hasPositiveBalance() && (currentTime - lastTxCheck) > 60 * 60 * 1000;
    }

    public boolean getIsSent(Transaction transaction)
    {
        return transaction.from.equalsIgnoreCase(tokenWallet);
    }

    public String getWallet()
    {
        return tokenWallet;
    }

    public List<BigInteger> pruneIDList(String ticketIds, int quantity)
    {
        return new ArrayList<>();
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
    public boolean isERC721() { return false; }
    public boolean isNonFungible() { return false; }
    public boolean isERC20()
    {
        return contractType == ContractType.ERC20;
    }
    public boolean isEthereum()
    {
        return contractType == ContractType.ETHEREUM;
    }
    public boolean isERC721Ticket() { return false; }
    public boolean hasGroupedTransfer() { return false; } //Can the NFT token's transfer function handle multiple tokens?
    public boolean checkSelectionValidity(List<BigInteger> selection) //check a selection of ID's for Transfer/Redeem/Sell
    {
        return selection.size() != 0 && (selection.size() == 1 || hasGroupedTransfer());
    }


    public BigDecimal getCorrectedAmount(String newAmount)
    {
        if (newAmount == null || newAmount.length() == 0) return BigDecimal.ZERO;

        try
        {
            BigDecimal bd = new BigDecimal(newAmount);
            BigDecimal factor = new BigDecimal(Math.pow(10, tokenInfo.decimals));
            //.setScale(scale, RoundingMode.HALF_DOWN).stripTrailingZeros();
            return bd.multiply(factor).setScale(0, RoundingMode.DOWN).stripTrailingZeros();
        }
        catch (Exception e)
        {
            //
        }
        return BigDecimal.ZERO;
    }

    public String getShortName() {
        if (isTerminated() || isBad()) return "";
        return tokenInfo.name != null ? tokenInfo.name : tokenInfo.symbol != null ? tokenInfo.symbol : "";
    }

    public boolean groupWithToken(TicketRange currentRange, TicketRangeElement e, long currentTime)
    {
        //default is no grouping
        return false;
    }

    /**
     * This function takes a list of tokenIds, and returns a BigInteger list suitable for this token's transfer function
     * For most token contracts this is the list of tokenIds but for ERC875 this is the list converted to indices
     * @param tokenIds
     * @return
     */
    public List<BigInteger> getTransferListFormat(List<BigInteger> tokenIds)
    {
        return tokenIds;
    }
    protected org.web3j.abi.datatypes.DynamicArray getDynArray(List<BigInteger> indices)
    {
        return new org.web3j.abi.datatypes.DynamicArray<>(
                        org.web3j.abi.datatypes.generated.Uint256.class,
                        org.web3j.abi.Utils.typeMap(indices, org.web3j.abi.datatypes.generated.Uint256.class));
    }


    /**
     * Common TokenScript methods
     */

    public void displayTicketHolder(TicketRange range, View activity, AssetDefinitionService assetService, Context ctx) {
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
        //need to wait until the assetDefinitionService has finished loading assets
        assetService.getAssetDefinitionASync(tokenInfo.chainId, tokenInfo.address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(td -> renderTicketHolder(td, range, activity, assetService, ctx, iconified), this::loadingError).isDisposed();
    }

    private void loadingError(Throwable e)
    {
        e.printStackTrace();
    }

    private void renderTicketHolder(TokenDefinition td, TicketRange range, View activity, AssetDefinitionService assetService, Context ctx, boolean iconified)
    {
        if (td != null && td.holdingToken != null)
        {
            //use webview
            displayTokenscriptView(range, assetService, activity, ctx, iconified);
        }
        else
        {
            activity.findViewById(R.id.layout_legacy).setVisibility(View.VISIBLE);
            activity.findViewById(R.id.layout_webwrapper).setVisibility(View.GONE);

            TextView amount = activity.findViewById(R.id.amount);
            TextView name = activity.findViewById(R.id.name);

            String nameStr = getTokenTitle();
            String seatCount = String.format(Locale.getDefault(), "x%d", range.tokenIds.size());

            name.setText(nameStr);
            amount.setText(seatCount);
        }
    }

    protected void displayTokenscriptView(TicketRange range, AssetDefinitionService assetService, View activity, Context ctx, boolean iconified)
    {
        //get webview
        Web3TokenView tokenView = activity.findViewById(R.id.web3_tokenview);
        ProgressBar waitSpinner = activity.findViewById(R.id.progress_element);
        activity.findViewById(R.id.layout_webwrapper).setVisibility(View.VISIBLE);
        activity.findViewById(R.id.layout_legacy).setVisibility(View.GONE);

        waitSpinner.setVisibility(View.VISIBLE);
        renderTokenscriptView(range, assetService, waitSpinner, ctx, tokenView, iconified);
    }

    public void renderTokenscriptView(TicketRange range, AssetDefinitionService assetService, ProgressBar waitSpinner, Context ctx, Web3TokenView tokenView, boolean iconified)
    {
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
        if (waitSpinner != null) waitSpinner.setVisibility(View.GONE);
        tokenView.setVisibility(View.VISIBLE);

        String view = assetService.getTokenView(tokenInfo.chainId, getAddress(), iconified ? "item-view" : "view");
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

    public boolean checkTickerChange(Token check)
    {
        if (check.ticker == null && ticker == null) return false;
        else if (check.ticker == null || ticker == null) return true; //ticker situation changed
        else return !check.ticker.price.equals(ticker.price); //return true if ticker changed
    }

    @Override
    public boolean equals(Object v)
    {
        boolean retVal = false;

        if (v instanceof Token) {
            Token t = (Token) v;
            retVal = t != null && tokenInfo.chainId == t.tokenInfo.chainId && getAddress().equals(t.getAddress());
        }

        return retVal;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 17 * hash + (this.tokenInfo.name != null ? this.tokenInfo.name.hashCode() : 0);
        return hash;
    }


    @Override
    public int compareTo(@NonNull Token otherToken)
    {
        return nameWeight - otherToken.nameWeight;
    }

    public int getNameWeight()
    {
        return nameWeight;
    }

    private int calculateTokenNameWeight()
    {
        int weight = 1000; //ensure base eth types are always displayed first
        String tokenName = getFullName();
        int override = EthereumNetworkRepository.getPriorityOverride(this);
        if (override > 0) return override;
        if(isBad()) return 99999999;
        if(tokenName.length() == 0)
        {
            return Integer.MAX_VALUE;
        }

        int i = 4;
        int pos = 0;

        while (i >= 0 && pos < tokenName.length())
        {
            char c = tokenName.charAt(pos++);
            int w = tokeniseCharacter(c);
            if (w > 0)
            {
                int component = (int)Math.pow(26, i)*w;
                weight += component;
                i--;
            }
        }

        String address = com.alphawallet.token.tools.Numeric.cleanHexPrefix(getAddress());
        for (i = 0; i < address.length() && i < 2; i++)
        {
            char c = address.charAt(i);
            int w = c - '0';
            weight += w;
        }

        if (weight < 2) weight = 2;

        return weight;
    }

    private int tokeniseCharacter(char c)
    {
        int w = Character.toLowerCase(c) - 'a' + 1;
        if (w > 'z')
        {
            //could be ideographic, in which case we may want to display this first
            //just use a modulus
            w = w % 10;
        }
        else if (w < 0)
        {
            //must be a number
            w = 1 + (c - '0');
        }
        else
        {
            w += 10;
        }

        return w;
    }

    public long getUID()
    {
        String id = getAddress() + "-" + tokenInfo.chainId;
        return id.hashCode();
    }

    public void setHighestPriorityCheck()
    {
        balanceUpdateWeight = 10.0f;
    }
}