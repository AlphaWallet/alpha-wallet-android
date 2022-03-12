package com.alphawallet.app.entity.tokens;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Pair;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.TicketRangeElement;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionInput;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.opensea.AssetContract;
import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.entity.ENSHandler;
import com.alphawallet.app.ui.widget.entity.StatusType;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.BaseViewModel;
import com.alphawallet.token.entity.TicketRange;
import com.alphawallet.token.entity.TokenScriptResult;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import static android.text.Html.FROM_HTML_MODE_COMPACT;
import static com.alphawallet.app.repository.TokenRepository.callSmartContractFunction;

public class Token
{
    public final static int TOKEN_BALANCE_PRECISION = 4;
    public final static int TOKEN_BALANCE_FOCUS_PRECISION = 5;

    protected static OkHttpClient client;

    public final TokenInfo tokenInfo;
    public BigDecimal balance;
    public BigDecimal pendingBalance;
    public long updateBlancaTime;
    private String tokenWallet;
    protected ContractType contractType;
    public long lastBlockCheck = 0;
    private final String shortNetworkName;
    public boolean balanceChanged;
    public boolean walletUIUpdateRequired;
    public boolean hasTokenScript;
    public long lastTxCheck;
    public long lastTxTime;
    public int itemViewHeight;
    public TokenGroup group;

    private final Map<BigInteger, Map<String, TokenScriptResult.Attribute>> resultMap = new ConcurrentHashMap<>(); //Build result map for function parse, per tokenId
    private Map<BigInteger, List<String>> functionAvailabilityMap = null;

    public String getNetworkName() { return shortNetworkName; }

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
        this.lastTxCheck = 0;
        this.lastBlockCheck = 0;
        this.lastTxTime = 0;
        balanceChanged = false;
        walletUIUpdateRequired = false;
        hasTokenScript = false;
        resultMap.clear();

        if (group == null) group = TokenGroup.ASSET; //default to Asset
    }

    public String getStringBalance()
    {
        int decimals = 18;
        if (tokenInfo != null) decimals = tokenInfo.decimals;
        String balanceStr = BalanceUtils.getScaledValueScientific(balance, decimals);
        if (balanceStr.equals("0") && balance.compareTo(BigDecimal.ZERO) > 0) { balanceStr = "~0"; }
        return balanceStr;
    }

    public boolean hasPositiveBalance() {
        if (balance != null) return !balance.equals(BigDecimal.ZERO);
        else return false;
    }

    public String getFullBalance() {
        if (balance != null) return balance.toString();
        else return "0";
    }

    public NFTAsset getAssetForToken(String tokenId) {
        return null;
    }
    public NFTAsset getAssetForToken(BigInteger tokenId) {
        return null;
    }
    public List<BigInteger> getUniqueTokenIds()
    {
        List<BigInteger> uniqueIds = new ArrayList<>();
        if (isNonFungible())
        {
            for (BigInteger id : getArrayBalance())
            {
                if (!uniqueIds.contains(id) && !id.equals(BigInteger.ZERO)) uniqueIds.add(id);
            }
        }
        else
        {
            uniqueIds.add(BigInteger.ZERO);
        }

        return uniqueIds;
    }

    public void addAssetToTokenBalanceAssets(BigInteger tokenId, NFTAsset asset) {
        //only for ERC721, see override in ERC721Token
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

    //Used for custom balance updates
    public BigDecimal updateBalance(Realm realm) { return BigDecimal.ZERO; }

    public boolean isTerminated() { return (updateBlancaTime == -1); }

    public String getAddress() {
        return tokenInfo.address;
    }

    public String getFullName()
    {
        if (isTerminated()) return TokensService.EXPIRED_CONTRACT;
        if (isBad()) return TokensService.UNKNOWN_CONTRACT;
        String name = tokenInfo.name == null ? "" : tokenInfo.name;
        String symbol = (tokenInfo.symbol == null || tokenInfo.symbol.length() == 0) ? "" : " (" + tokenInfo.symbol.toUpperCase() + ")";
        return sanitiseString(name + symbol);
    }

    public String getFullName(AssetDefinitionService assetDefinition, int count)
    {
        //override contract name with TS defined name
        String name = assetDefinition != null ? assetDefinition.getTokenName(tokenInfo.chainId, tokenInfo.address, count) : null;
        if (name != null) {
            String symbol = (tokenInfo.symbol == null || tokenInfo.symbol.length() == 0) ? "" : " (" + tokenInfo.symbol.toUpperCase() + ")";
            return sanitiseString(name + symbol);
        } else {
            return sanitiseString(getFullName());
        }
    }

    public String getTSName(AssetDefinitionService assetDefinition, int count) {
        String name = assetDefinition != null ? assetDefinition.getTokenName(tokenInfo.chainId, tokenInfo.address, count) : null;
        if (name != null) {
            return sanitiseString(name);
        }
        return null;
    }

    public String getName(AssetDefinitionService assetDefinition, int count)
    {
        //override contract name with TS defined name
        String name = assetDefinition != null ? assetDefinition.getTokenName(tokenInfo.chainId, tokenInfo.address, count) : null;
        if (name != null) {
            return sanitiseString(name);
        } else {
            return getName();
        }
    }

    public String getName()
    {
        if (TextUtils.isEmpty(tokenInfo.name) || tokenInfo.name.length() < tokenInfo.symbol.length()) {
            return sanitiseString(!TextUtils.isEmpty(tokenInfo.symbol) ? tokenInfo.symbol : "");
        } else {
            return sanitiseString(!TextUtils.isEmpty(tokenInfo.name) ? tokenInfo.name : "");
        }
    }

    private String sanitiseString(String str)
    {
        StringBuilder sb = new StringBuilder();
        for (char ch : str.toCharArray())
        {
            if (Character.isAlphabetic(ch) || Character.isDigit(ch) || Character.isIdeographic(ch) || ch < 65530)
            {
                sb.append(ch);
            }
            else
            {
                sb.append("*");
            }
        }

        str = sb.toString();

        //Don't convert to Html if name contains any kind of link; protect user from potential link spam
        if (str.toLowerCase().contains("<a href="))
        {
            return str;
        }
        else
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            {
                return Html.fromHtml(str, FROM_HTML_MODE_COMPACT).toString();
            }
            else
            {
                //noinspection deprecation
                return Html.fromHtml(str).toString();
            }
        }
    }

    private String getShortestNameOrSymbol()
    {
        int symbolLength = !TextUtils.isEmpty(tokenInfo.symbol) ? tokenInfo.symbol.length() : 0;
        int nameLength = !TextUtils.isEmpty(tokenInfo.name) ? tokenInfo.name.length() : 0;
        if (symbolLength == 0 && nameLength == 0)
        {
            return "";
        }
        else if (nameLength > 0 && (symbolLength > nameLength || symbolLength == 0))
        {
            return tokenInfo.name;
        }
        else
        {
            return tokenInfo.symbol;
        }
    }

    public String getSymbol()
    {
        return getShortestNameOrSymbol();
    }

    public String getShortSymbol()
    {
        return Utils.getShortSymbol(getShortestNameOrSymbol());
    }

    public void clickReact(BaseViewModel viewModel, Activity context)
    {
        viewModel.showErc20TokenDetail(context, tokenInfo.address, tokenInfo.symbol, tokenInfo.decimals, this);
    }

    public BigDecimal getCorrectedBalance(int scale)
    {
        if (balance == null || balance.equals(BigDecimal.ZERO)) return BigDecimal.ZERO;
        int decimals = 18;
        if (tokenInfo != null) decimals = tokenInfo.decimals;
        BigDecimal decimalDivisor = BigDecimal.valueOf(Math.pow(10, decimals));
        return decimals > 0
               ? balance.divide(decimalDivisor, scale, RoundingMode.DOWN).stripTrailingZeros() : balance;
    }

    public int getContractType()
    {
        switch (contractType)
        {
            case ERC20:
                return R.string.erc20;
            case ETHEREUM:
                return 0; //don't display 'ethereum' as contract type
            default:
                return 0;
        }
    }

    public Map<BigInteger, NFTAsset> getTokenAssets() {
        return null;
    }

    public Map<BigInteger, NFTAsset> getCollectionMap() { return null; }

    public List<BigInteger> ticketIdStringToIndexList(String userList)
    {
        return null;
    }

    public int getTokenCount()
    {
        return balance.intValue();
    }

    public boolean isToken()
    {
        return !(contractType == ContractType.ETHEREUM_INVISIBLE || contractType == ContractType.ETHEREUM);
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

    public boolean checkBalanceChange(Token oldToken)
    {
        return !getFullBalance().equals(oldToken.getFullBalance()) || !getFullName().equals(oldToken.getFullName());
    }

    public void setIsEthereum()
    {
        contractType = ContractType.ETHEREUM;
    }

    public boolean isBad()
    {
        return tokenInfo == null || (tokenInfo.symbol == null && tokenInfo.name == null);
    }

    public void setTokenWallet(String address)
    {
        this.tokenWallet = address;
    }

    public void setupRealmToken(RealmToken realmToken)
    {
        lastBlockCheck = realmToken.getLastBlock();
        lastTxCheck = realmToken.getLastTxTime();
        lastTxTime = realmToken.getLastTxTime();
        tokenInfo.isEnabled = realmToken.getEnabled();
    }

    public String getPendingDiff()
    {
        if (!isEthereum() || pendingBalance == null || balance.equals(pendingBalance)) return null;
        else
        {
            String prefix = "";
            BigDecimal diff = pendingBalance.subtract(balance);
            if (diff.compareTo(BigDecimal.ZERO) > 0) prefix = "+";
            String diffStr = prefix + BalanceUtils.getScaledValue(diff, tokenInfo.decimals, TOKEN_BALANCE_PRECISION);
            if (diffStr.startsWith("~")) diffStr = null;
            return diffStr;
        }
    }

    public void setRealmInterfaceSpec(RealmToken realmToken)
    {
        if (isEthereum() && realmToken.getInterfaceSpec() != ContractType.ETHEREUM_INVISIBLE.ordinal())
        {
            contractType = ContractType.ETHEREUM;
        }
        realmToken.setInterfaceSpec(contractType.ordinal());
    }

    public void setRealmLastBlock(RealmToken realmToken)
    {
        realmToken.setLastBlock(lastBlockCheck);
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
    public Function getTransferFunction(String to, List<BigInteger> transferData) throws NumberFormatException
    {
        return null;
    }
    public byte[] getTransferBytes(String to, ArrayList<Pair<BigInteger, NFTAsset>> transferData)
    {
        return Numeric.hexStringToByteArray("0x");
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

    public boolean contractTypeValid() { return !(contractType == ContractType.NOT_SET || contractType == ContractType.OTHER); }
    public List<BigInteger> getArrayBalance() { return new ArrayList<>(); }
    public List<BigInteger> getNonZeroArrayBalance() { return new ArrayList<>(Arrays.asList(BigInteger.ZERO)); }
    public String getOperationName(Transaction transaction, Context ctx)
    {
        String name;
        if (isEthereum() && !transaction.hasInput())
        {
            if (transaction.value.equals("0") && transaction.hasInput())
            {
                name = ctx.getString(R.string.contract_call);
            }
            else if (transaction.from.equalsIgnoreCase(tokenWallet))
            {
                name = ctx.getString(R.string.sent);
            }
            else
            {
                name = ctx.getString(R.string.received);
            }
        }
        else
        {
            name = transaction.getOperationName(ctx, this, getWallet());
        }


        return name;
    }

    /**
     * Balance in human readable form, fixed decimal width eg 14.5000
     * @return formatted balance
     */
    public String getFixedFormattedBalance()
    {
        return BalanceUtils.getScaledValueFixed(balance, tokenInfo.decimals, TOKEN_BALANCE_PRECISION);
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

    public String convertValue(String prefix, String value, int precision)
    {
        return prefix + BalanceUtils.getScaledValueFixed(new BigDecimal(value),
                tokenInfo.decimals, precision);
    }

    /**
     * Fetch the base native value attached to this transaction
     * @param transaction
     * @return
     */
    public String getTransactionValue(Transaction transaction, int precision)
    {
        if (transaction.hasError()) return "";
        else if (transaction.value.equals("0") || transaction.value.equals("0x0")) return "0";
        return transaction.getPrefix(this) + BalanceUtils.getScaledValueFixed(new BigDecimal(transaction.value), tokenInfo.decimals, precision);
    }

    /**
     * Fetch the Token value of this transaction eg +20 DAI, or if a native chain transaction then return the value
     *
     * TODO: Refactor when Class Token refactor stage 2 is done
     *
     * @param transaction
     * @return
     */
    public String getTransactionResultValue(Transaction transaction, int precision)
    {
        if (isEthereum() && !transaction.hasInput())
        {
            //basic eth transaction
            return getTransactionValue(transaction, precision) + " " + getSymbol();
        }
        else if (transaction.hasInput())
        {
            //smart contract call
            return transaction.getOperationResult(this, precision);
        }
        else
        {
            return "";
        }
    }

    public boolean shouldShowSymbol(Transaction transaction)
    {
        return ((isEthereum() && !transaction.hasInput()) || (transaction.shouldShowSymbol(this)));
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

    public boolean getIsSent(Transaction transaction)
    {
        if (isEthereum())
        {
            return transaction.from.equalsIgnoreCase(tokenWallet);
        }
        else
        {
            return transaction.getIsSent(tokenWallet);
        }
    }

    public String getWallet()
    {
        return tokenWallet;
    }

    public List<BigInteger> pruneIDList(String ticketIds, int quantity)
    {
        return new ArrayList<>();
    }

    public boolean equals(Token token)
    {
        return token != null && tokenInfo.chainId == token.tokenInfo.chainId && getAddress().equalsIgnoreCase(token.getAddress());
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
        return selection != null && selection.size() != 0 && (selection.size() == 1 || hasGroupedTransfer());
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

    public boolean checkBalanceType()
    {
        return true;
    }

    public String getTransactionDetail(Context ctx, Transaction tx, TokensService tService)
    {
        if (isEthereum())
        {
            return ctx.getString(R.string.operation_definition, ctx.getString(getToFromText(tx)), ENSHandler.matchENSOrFormat(ctx, getTransactionDestination(tx)));
        }
        else
        {
            return tx.getOperationDetail(ctx, this, tService);
        }
    }

    public String getTransactionDestination(Transaction transaction)
    {
        if (isEthereum())
        {
            if (transaction.from.equalsIgnoreCase(tokenWallet))
            {
                return transaction.to;
            }
            else
            {
                return transaction.from;
            }
        }
        else
        {
            return transaction.getDestination(this);
        }
    }

    public StatusType ethereumTxImage(Transaction tx)
    {
        return tx.from.equalsIgnoreCase(tokenWallet) ? (tx.to.equals(tx.from) ? StatusType.SELF : StatusType.SENT)
                : StatusType.RECEIVE;
    }

    public StatusType getTxStatus(Transaction transaction)
    {
        StatusType status = transaction.getTransactionStatus();
        if (status == null)
        {
            if (isEthereum())
            {
                status = ethereumTxImage(transaction);
            }
            else
            {
                status = transaction.getOperationImage(this);
            }
        }

        return status;
    }

    public TokenScriptResult.Attribute getAttributeResult(String attrId, BigInteger tokenId)
    {
        if (resultMap.containsKey(tokenId))
        {
            return resultMap.get(tokenId).get(attrId);
        }
        else
        {
            return null;
        }
    }

    public void setAttributeResult(BigInteger tokenId, TokenScriptResult.Attribute attrResult)
    {
        Map<String, TokenScriptResult.Attribute> resultSet = resultMap.get(tokenId);
        if (resultSet == null)
        {
            resultSet = new ConcurrentHashMap<>();
            resultMap.put(tokenId, resultSet);
        }

        resultSet.put(attrResult.id, attrResult);
    }

    public void clearResultMap()
    {
        resultMap.clear();
    }

    public void setFunctionAvailability(Map<BigInteger, List<String>> availabilityMap)
    {
        functionAvailabilityMap = availabilityMap;
    }

    public boolean isFunctionAvailable(BigInteger tokenId, String functionName)
    {
        List<String> mapForToken = functionAvailabilityMap.get(tokenId);
        if (mapForToken != null)
        {
            return mapForToken.contains(functionName);
        }
        else
        {
            return false;
        }
    }

    public long getTransactionCheckInterval()
    {
        if (hasRealValue() && hasPositiveBalance())
        {
            return 1* DateUtils.MINUTE_IN_MILLIS;
        }
        else if (hasPositiveBalance())
        {
            return 150* DateUtils.SECOND_IN_MILLIS;
        }
        else
        {
            return 0;
        }
    }

    public boolean needsTransactionCheck()
    {
        switch (getInterfaceSpec())
        {
            case ERC875_LEGACY:
            case ERC875:
            case ETHEREUM:
            case ERC721_TICKET:
                return true;
            case CURRENCY:
            case DELETED_ACCOUNT:
            case OTHER:
            case NOT_SET:
            case ERC721:
            case ERC721_LEGACY:
            case ERC721_UNDETERMINED:
            case CREATION:
            case ERC20:
            default:
                return false;
        }
    }

    public int getToFromText(Transaction transaction)
    {
        if (isEthereum())
        {
            if (getIsSent(transaction))
            {
                return R.string.to;
            }
            else
            {
                return R.string.from_op;
            }
        }
        else
        {
            return transaction.getOperationToFrom(tokenWallet);
        }
    }

    /**
     * Given a transaction which is a Token Transfer of some kind, return the amount of tokens or token ID if NFT
     * Should be overriden for each distinct token type
     *
     * @param txInput
     * @param transactionBalancePrecision
     * @return
     */
    public String getTransferValue(TransactionInput txInput, int transactionBalancePrecision)
    {
        BigInteger bi = getTransferValueRaw(txInput);
        if (bi.compareTo(BigInteger.ZERO) > 0)
        {
            return BalanceUtils.getScaledValueMinimal(new BigDecimal(bi),
                    tokenInfo.decimals, transactionBalancePrecision);
        }
        else
        {
            return "0";
        }
    }

    public BigInteger getTransferValueRaw(TransactionInput txInput)
    {
        if (txInput != null && txInput.miscData.size() > 0)
        {
            return new BigInteger(txInput.miscData.get(0), 16);
        }
        else
        {
            return BigInteger.ZERO;
        }
    }

    public BigDecimal getBalanceRaw()
    {
        return balance;
    }

    public boolean mayRequireRefresh()
    {
        return (!TextUtils.isEmpty(tokenInfo.name) && tokenInfo.name.contains("?"))
                || (!TextUtils.isEmpty(tokenInfo.symbol) && tokenInfo.symbol.contains("?"));
    }

    public List<BigInteger> getChangeList(Map<BigInteger, NFTAsset> assetMap)
    {
        return new ArrayList<>();
    }

    public void setAssetContract(AssetContract contract) {  }
    public AssetContract getAssetContract() { return null; }

    public List<Integer> getStandardFunctions()
    {
        return Arrays.asList(R.string.action_send, R.string.action_receive);
    }

    public List<NFTAsset> getAssetListFromTransaction(Transaction tx)
    {
        return new ArrayList<>();
    }

    public Map<BigInteger, NFTAsset> queryAssets(Map<BigInteger, NFTAsset> assetMap)
    {
        return assetMap;
    }

    /**
     * Token Metadata handling
     */

    public NFTAsset fetchTokenMetadata(BigInteger tokenId)
    {
        //1. get TokenURI (check for non-standard URI - check "tokenURI" and "uri")
        String responseValue = callSmartContractFunction(tokenInfo.chainId, getTokenURI(tokenId), getAddress(), getWallet());
        if (responseValue == null) responseValue = callSmartContractFunction(tokenInfo.chainId, getTokenURI2(tokenId), getAddress(), getWallet());
        String metaData = loadMetaData(responseValue);
        if (!TextUtils.isEmpty(metaData))
        {
            return new NFTAsset(metaData);
        }
        else
        {
            return new NFTAsset();
        }
    }

    private Function getTokenURI(BigInteger tokenId)
    {
        return new Function("tokenURI",
                Arrays.asList(new Uint256(tokenId)),
                Arrays.asList(new TypeReference<Utf8String>() {}));
    }

    private Function getTokenURI2(BigInteger tokenId)
    {
        return new Function("uri",
                Arrays.asList(new Uint256(tokenId)),
                Arrays.asList(new TypeReference<Utf8String>() {}));
    }

    private String loadMetaData(String tokenURI)
    {
        if (TextUtils.isEmpty(tokenURI)) return "";

        setupClient();

        Request request = new Request.Builder()
                    .url(Utils.parseIPFS(tokenURI))
                    .get()
                    .build();

        try (okhttp3.Response response = client.newCall(request).execute())
        {
            return response.body().string();
        }
        catch (Exception e)
        {
            //
        }

        return "";
    }

    private static void setupClient()
    {
        if (client == null)
        {
            client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();
        }
    }

    public boolean checkInfoRequiresUpdate(RealmToken realmToken)
    {
        if (TextUtils.isEmpty(realmToken.getName()) || (!TextUtils.isEmpty(tokenInfo.name) && !tokenInfo.name.equals(realmToken.getName()))) { return true; }
        if (TextUtils.isEmpty(realmToken.getSymbol()) || (!TextUtils.isEmpty(tokenInfo.symbol) && !tokenInfo.symbol.equals(realmToken.getSymbol()))) { return true; }
        if (realmToken.getContractType() != contractType) { return true; }
        return realmToken.getDecimals() != tokenInfo.decimals;
    }

    public boolean isBatchTransferAvailable()
    {
        return false;
    }
}