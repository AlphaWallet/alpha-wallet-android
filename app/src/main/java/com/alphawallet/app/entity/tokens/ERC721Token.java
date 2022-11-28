package com.alphawallet.app.entity.tokens;

import static com.alphawallet.app.repository.TokenRepository.callSmartContractFunction;
import static com.alphawallet.app.repository.TokensRealmSource.databaseKey;
import static com.alphawallet.app.util.Utils.parseTokenId;
import static org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction;
import static org.web3j.tx.Contract.staticExtractEventParameters;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Pair;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.LogOverflowException;
import com.alphawallet.app.entity.SyncDef;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionInput;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.EventResult;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.repository.entity.RealmNFTAsset;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.service.TransactionsService;
import com.alphawallet.app.viewmodel.BaseViewModel;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.EventValues;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.Utils;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.BatchRequest;
import org.web3j.protocol.core.BatchResponse;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmResults;
import timber.log.Timber;

/**
 * Created by James on 3/10/2018.
 * Stormbird in Singapore
 */
public class ERC721Token extends Token
{
    private final Map<BigInteger, NFTAsset> tokenBalanceAssets;
    private static final Map<String, Boolean> balanceChecks = new ConcurrentHashMap<>();
    private boolean batchProcessingError;

    public ERC721Token(TokenInfo tokenInfo, Map<BigInteger, NFTAsset> balanceList, BigDecimal balance, long blancaTime, String networkName, ContractType type)
    {
        super(tokenInfo, balance, blancaTime, networkName, type);
        if (balanceList != null)
        {
            tokenBalanceAssets = balanceList;
        }
        else
        {
            tokenBalanceAssets = new ConcurrentHashMap<>();
        }
        setInterfaceSpec(type);
        group = TokenGroup.NFT;
        batchProcessingError = false;
    }

    @Override
    public Map<BigInteger, NFTAsset> getTokenAssets()
    {
        return tokenBalanceAssets;
    }

    @Override
    public void addAssetToTokenBalanceAssets(BigInteger tokenId, NFTAsset asset)
    {
        tokenBalanceAssets.put(tokenId, asset);
    }

    @Override
    public NFTAsset getAssetForToken(String tokenIdStr)
    {
        return tokenBalanceAssets.get(parseTokenId(tokenIdStr));
    }

    @Override
    public NFTAsset getAssetForToken(BigInteger tokenId)
    {
        return tokenBalanceAssets.get(tokenId);
    }

    @Override
    public int getContractType()
    {
        return R.string.erc721;
    }

    @Override
    public String getStringBalanceForUI(int scale)
    {
        if (balance.compareTo(BigDecimal.ZERO) > 0)
        {
            return balance.toString();
        }
        else
        {
            return "0";
        }
    }

    @Override
    public byte[] getTransferBytes(String to, ArrayList<Pair<BigInteger, NFTAsset>> transferData) throws NumberFormatException
    {
        if (transferData == null || transferData.size() != 1)
            return Numeric.hexStringToByteArray("0x");
        Function txFunc = getTransferFunction(to, new ArrayList<>(Collections.singleton(transferData.get(0).first)));
        String encodedFunction = FunctionEncoder.encode(txFunc);
        return Numeric.hexStringToByteArray(Numeric.cleanHexPrefix(encodedFunction));
    }

    @Override
    public Function getTransferFunction(String to, List<BigInteger> tokenIds) throws NumberFormatException
    {
        if (tokenIds.size() > 1)
        {
            throw new NumberFormatException("ERC721Ticket can't handle batched transfers");
        }

        Function function = null;
        List<Type> params;
        BigInteger tokenIdBI = tokenIds.get(0);
        List<TypeReference<?>> returnTypes = Collections.emptyList();
        if (tokenUsesLegacyTransfer())
        {
            params = Arrays.asList(new Address(to), new Uint256(tokenIdBI));
            function = new Function("transfer", params, returnTypes);
        }
        else
        {
            //function safeTransferFrom(address _from, address _to, uint256 _tokenId) external payable;
            params = Arrays.asList(new Address(getWallet()), new Address(to), new Uint256(tokenIdBI));
            function = new Function("safeTransferFrom", params, returnTypes);
        }
        return function;
    }

    @Override
    public void clickReact(BaseViewModel viewModel, Activity activity)
    {
        viewModel.showTokenList(activity, this);
    }

    @Override
    public int getTokenCount()
    {
        return balance.intValue();
    }

    @Override
    public String getFullBalance()
    {
        return balance.toString();
    }

    @Override
    public boolean isToken()
    {
        return false;
    }

    @Override
    public boolean hasArrayBalance()
    {
        return true;
    }

    @Override
    public boolean hasPositiveBalance()
    {
        return tokenBalanceAssets != null && tokenBalanceAssets.size() > 0;
    }

    @Override
    public void setRealmBalance(RealmToken realmToken)
    {
        realmToken.setBalance(balance.toString());
    }

    public boolean isERC721()
    {
        return true;
    }

    public boolean isNonFungible()
    {
        return true;
    }

    /**
     * This is a list of legacy contracts which are known to use the old ERC721 source,
     * which only had 'transfer' as the transfer function.
     *
     * @return
     */
    private boolean tokenUsesLegacyTransfer()
    {
        switch (tokenInfo.address.toLowerCase())
        {
            case "0x06012c8cf97bead5deae237070f9587f8e7a266d":
            case "0xabc7e6c01237e8eef355bba2bf925a730b714d5f":
            case "0x71c118b00759b0851785642541ceb0f4ceea0bd5":
            case "0x16baf0de678e52367adc69fd067e5edd1d33e3bf":
            case "0x7fdcd2a1e52f10c28cb7732f46393e297ecadda1":
                return true;

            default:
                return false;
        }
    }

    @Override
    public List<BigInteger> getArrayBalance()
    {
        return new ArrayList<>(tokenBalanceAssets.keySet());
    }

    @Override
    public boolean checkRealmBalanceChange(RealmToken realmToken)
    {
        if (contractType == null || contractType.ordinal() != realmToken.getInterfaceSpec())
            return true;
        String currentState = realmToken.getBalance();
        if (currentState == null) return true;
        if (lastTxTime > realmToken.getLastTxTime()) return true;
        if (!currentState.equals(balance.toString())) return true;
        //check balances
        for (NFTAsset a : tokenBalanceAssets.values())
        {
            if (!a.needsLoading() && !a.requiresReplacement()) return true;
        }
        return false;
    }

    @Override
    public boolean checkBalanceChange(Token oldToken)
    {
        if (super.checkBalanceChange(oldToken)) return true;
        if (getTokenAssets().size() != oldToken.getTokenAssets().size()) return true;
        for (BigInteger tokenId : tokenBalanceAssets.keySet())
        {
            NFTAsset newAsset = tokenBalanceAssets.get(tokenId);
            NFTAsset oldAsset = oldToken.getAssetForToken(tokenId);
            if (newAsset == null || oldAsset == null || !newAsset.equals(oldAsset))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public String convertValue(String prefix, EventResult vResult, int precision)
    {
        precision++;
        String value = vResult != null ? vResult.value : "0";
        if (value.length() > precision)
        {
            return prefix + "1";
        }
        else
        {
            return "#" + value;
        }
    }


    //determine token balance
    //1 determine first transaction


    //get balance
    private Event getTransferEvents()
    {
        //event Transfer(address indexed from, address indexed to, uint256 indexed tokenId);
        List<TypeReference<?>> paramList = new ArrayList<>();
        paramList.add(new TypeReference<Address>(true)
        {
        });
        paramList.add(new TypeReference<Address>(true)
        {
        });
        paramList.add(new TypeReference<Uint256>(true)
        {
        });

        return new Event("Transfer", paramList);
    }

    /**
     * Uses both events and balance call. Each call to updateBalance uses 3 Node calls:
     * 1. Get new Transfer events since last call
     * 2.
     * <p>
     * Once we have the current balance for potential tokens the database is updated to reflect the current status
     * <p>
     * Note that this function is used even for contracts covered by OpenSea: This is because we could be looking at
     * a contract 'join' between successive opensea reads. With accounts with huge quantity of NFT, this happens a lot
     *
     * @param realm
     * @return
     */
    @Override
    public BigDecimal updateBalance(Realm realm)
    {
        if (balanceChecks.containsKey(tokenInfo.address))
        {
            return balance;
        }

        //first get current block
        SyncDef sync = eventSync.getSyncDef(realm);
        if (sync == null) return balance;

        DefaultBlockParameter startBlock = DefaultBlockParameter.valueOf(sync.eventReadStartBlock);
        DefaultBlockParameter endBlock = DefaultBlockParameter.valueOf(sync.eventReadEndBlock);
        if (sync.eventReadEndBlock.compareTo(BigInteger.valueOf(-1L)) == 0) endBlock = DefaultBlockParameterName.LATEST;

        //take a note of the current block#
        BigInteger currentBlock = TransactionsService.getCurrentBlock(tokenInfo.chainId);

        try
        {
            balanceChecks.put(tokenInfo.address, true); //set checking
            final Web3j web3j = TokenRepository.getWeb3jService(tokenInfo.chainId);
            if (contractType == ContractType.ERC721_ENUMERABLE)
            {
                updateEnumerableBalance(web3j, realm);
            }

            Pair<Integer, Pair<HashSet<BigInteger>, HashSet<BigInteger>>> evRead = eventSync.processTransferEvents(web3j,
                    getTransferEvents(), startBlock, endBlock, realm);

           eventSync.updateEventReads(realm, sync, currentBlock, evRead.first); //means our event read was fine

            //No need to go any further if this is enumerable
            if (contractType == ContractType.ERC721_ENUMERABLE) return balance;

            HashSet<BigInteger> allMovingTokens = new HashSet<>(evRead.second.first);
            allMovingTokens.addAll(evRead.second.second);

            if (allMovingTokens.size() == 0 && balance.intValue() != tokenBalanceAssets.size()) //if there's a mismatch, check all current assets
            {
                allMovingTokens.addAll(tokenBalanceAssets.keySet());
            }

            HashSet<BigInteger> tokenIdsHeld = checkBalances(web3j, allMovingTokens);
            updateRealmBalance(realm, tokenIdsHeld, allMovingTokens);
        }
        catch (LogOverflowException e)
        {
            //handle log read overflow; reduce search size
            if (eventSync.handleEthLogError(e.error, startBlock, endBlock, sync, realm))
            {
                //recurse until we find a good value
                updateBalance(realm);
            }
        }
        catch (Exception e)
        {
            Timber.w(e);
        }
        finally
        {
            balanceChecks.remove(tokenInfo.address);
        }

        //check for possible issues
        if (endBlock == DefaultBlockParameterName.LATEST && balance.compareTo(BigDecimal.valueOf(tokenBalanceAssets.size())) != 0)
        {
            //possible mismatch, scan from beginning again
            eventSync.resetEventReads(realm);
        }

        return balance;
    }

    /***********
     * For ERC721Enumerable interface
     **********/
    private void updateEnumerableBalance(Web3j web3j, Realm realm) throws IOException
    {
        HashSet<BigInteger> tokenIdsHeld = new HashSet<>();
        //get enumerable balance
        //find tokenIds held
        long currentBalance = balance != null ? balance.longValue() : 0;

        if (EthereumNetworkBase.getBatchProcessingLimit(tokenInfo.chainId) > 0 && !batchProcessingError && currentBalance > 1) //no need to do batch query for 1
        {
            updateEnumerableBatchBalance(web3j, currentBalance, tokenIdsHeld, realm);
        }
        else
        {
            for (long tokenIndex = 0; tokenIndex < currentBalance; tokenIndex++)
            {
                // find tokenId from index
                String tokenId = callSmartContractFunction(tokenInfo.chainId, tokenOfOwnerByIndex(BigInteger.valueOf(tokenIndex)), getAddress(), getWallet());
                if (tokenId == null) continue;
                tokenIdsHeld.add(new BigInteger(tokenId));
            }
        }

        updateRealmForEnumerable(realm, tokenIdsHeld);
    }

    private void updateEnumerableBatchBalance(Web3j web3j, long currentBalance, HashSet<BigInteger> tokenIdsHeld, Realm realm) throws IOException
    {
        BatchRequest requests = web3j.newBatch();

        for (long tokenIndex = 0; tokenIndex < currentBalance; tokenIndex++)
        {
            requests.add(getContractCall(web3j, tokenOfOwnerByIndex(BigInteger.valueOf(tokenIndex)), getAddress()));
            if (requests.getRequests().size() >= EthereumNetworkBase.getBatchProcessingLimit(tokenInfo.chainId))
            {
                //do this send
                handleEnumerableRequests(requests, tokenIdsHeld);
                requests = web3j.newBatch();
            }
        }

        if (requests.getRequests().size() > 0)
        {
            //do final call
            handleEnumerableRequests(requests, tokenIdsHeld);
        }

        if (batchProcessingError)
        {
            updateEnumerableBalance(web3j, realm);
        }
    }

    private void handleEnumerableRequests(BatchRequest requests, HashSet<BigInteger> tokenIdsHeld) throws IOException
    {
        BatchResponse responses = requests.send();
        if (responses.getResponses().size() != requests.getRequests().size())
        {
            batchProcessingError = true;
            return;
        }

        //process responses
        for (Response<?> rsp : responses.getResponses())
        {
            BigInteger tokenId = getTokenId(rsp);
            if (tokenId == null) continue;
            tokenIdsHeld.add(tokenId);
        }
    }

    private BigInteger getTokenId(Response<?> rsp)
    {
        List<TypeReference<Type>> outputParams = Utils.convert(Collections.singletonList(new TypeReference<Uint256>() {}));
        List<Type> responseValues = FunctionReturnDecoder.decode(((EthCall)rsp).getValue(), outputParams);
        if (!responseValues.isEmpty())
        {
            String tokenIdStr = responseValues.get(0).getValue().toString();
            if (!TextUtils.isEmpty(tokenIdStr)) return new BigInteger(tokenIdStr);
        }

        return null;
    }

    private void updateRealmBalance(Realm realm, Set<BigInteger> tokenIds, Set<BigInteger> allMovingTokens)
    {
        boolean updated = false;
        //fill in balances
        HashSet<BigInteger> removedTokens = new HashSet<>(allMovingTokens);
        if (tokenIds != null && tokenIds.size() > 0)
        {
            for (BigInteger tokenId : tokenIds)
            {
                NFTAsset asset = tokenBalanceAssets.get(tokenId);
                if (asset == null)
                {
                    tokenBalanceAssets.put(tokenId, new NFTAsset(tokenId));
                    updated = true;
                }
                removedTokens.remove(tokenId);
            }

            if (updated)
            {
                updateRealmBalances(realm, tokenIds);
            }
        }

        removeRealmBalance(realm, removedTokens);
    }

    private void removeRealmBalance(Realm realm, HashSet<BigInteger> removedTokens)
    {
        if (removedTokens.size() == 0) return;
        realm.executeTransaction(r -> {
            for (BigInteger tokenId : removedTokens)
            {
                String key = RealmNFTAsset.databaseKey(this, tokenId);
                RealmNFTAsset realmAsset = realm.where(RealmNFTAsset.class)
                        .equalTo("tokenIdAddr", key)
                        .findFirst();

                if (realmAsset != null)
                {
                    realmAsset.deleteFromRealm();
                }
            }
        });
    }

    private void updateRealmForEnumerable(Realm realm, HashSet<BigInteger> currentTokens)
    {
        HashSet<BigInteger> storedBalance = new HashSet<>();
        RealmResults<RealmNFTAsset> results = realm.where(RealmNFTAsset.class)
                .like("tokenIdAddr", databaseKey(this) + "-*", Case.INSENSITIVE)
                .findAll();

        for (RealmNFTAsset t : results)
        {
            storedBalance.add(new BigInteger(t.getTokenId()));
        }

        if (!currentTokens.equals(storedBalance))
        {
            realm.executeTransaction(r -> {
                results.deleteAllFromRealm();
                for (BigInteger tokenId : currentTokens)
                {
                    String key = RealmNFTAsset.databaseKey(this, tokenId);
                    RealmNFTAsset realmAsset = realm.where(RealmNFTAsset.class)
                            .equalTo("tokenIdAddr", key)
                            .findFirst();

                    if (realmAsset == null)
                    {
                        realmAsset = r.createObject(RealmNFTAsset.class, key); //create asset in realm
                        realmAsset.setMetaData(new NFTAsset(tokenId).jsonMetaData());
                        r.insertOrUpdate(realmAsset);
                    }
                }
            });
        }
    }

    private void updateRealmBalances(Realm realm, Set<BigInteger> tokenIds)
    {
        if (realm == null) return;
        realm.executeTransaction(r -> {
            for (BigInteger tokenId : tokenIds)
            {
                String key = RealmNFTAsset.databaseKey(this, tokenId);
                RealmNFTAsset realmAsset = realm.where(RealmNFTAsset.class)
                        .equalTo("tokenIdAddr", key)
                        .findFirst();

                if (realmAsset == null)
                {
                    realmAsset = r.createObject(RealmNFTAsset.class, key); //create asset in realm
                    realmAsset.setMetaData(tokenBalanceAssets.get(tokenId).jsonMetaData());
                    r.insertOrUpdate(realmAsset);
                }
            }
        });
    }

    @Override
    public HashSet<BigInteger> processLogsAndStoreTransferEvents(EthLog receiveLogs, Event event, HashSet<String> txHashes, Realm realm)
    {
        HashSet<BigInteger> tokenIds = new HashSet<>();
        for (EthLog.LogResult<?> ethLog : receiveLogs.getLogs())
        {
            String block = ((Log) ethLog.get()).getBlockNumberRaw();
            if (block == null || block.length() == 0) continue;
            String txHash = ((Log) ethLog.get()).getTransactionHash();

            final EventValues eventValues = staticExtractEventParameters(event, (Log) ethLog.get());
            Pair<List<BigInteger>, List<BigInteger>> idResult = eventSync.getEventIdResult(eventValues.getIndexedValues().get(2), null);
            tokenIds.addAll(idResult.first);

            // generating transfer record and storing it
            String from = eventValues.getIndexedValues().get(0).getValue().toString();  // from address
            String to = eventValues.getIndexedValues().get(1).getValue().toString();    // to address
            eventSync.storeTransferData(realm, from, to, idResult, txHash);
            txHashes.add(txHash);
        }
        return tokenIds;
    }

    private HashSet<BigInteger> checkBalances(Web3j web3j, HashSet<BigInteger> eventIds) throws IOException
    {
        HashSet<BigInteger> heldTokens = new HashSet<>();
        if (EthereumNetworkBase.getBatchProcessingLimit(tokenInfo.chainId) > 0 && !batchProcessingError && eventIds.size() > 1) return checkBatchBalances(web3j, eventIds);

        for (BigInteger tokenId : eventIds)
        {
            String owner = callSmartContractFunction(tokenInfo.chainId, ownerOf(tokenId), getAddress(), getWallet());
            if (owner == null || owner.equalsIgnoreCase(getWallet()))
            {
                heldTokens.add(tokenId);
            }
        }

        return heldTokens;
    }

    private HashSet<BigInteger> checkBatchBalances(Web3j web3j, HashSet<BigInteger> eventIds) throws IOException
    {
        HashSet<BigInteger> heldTokens = new HashSet<>();
        List<BigInteger> balanceIds = new ArrayList<>();
        BatchRequest requests = web3j.newBatch();
        for (BigInteger tokenId : eventIds)
        {
            requests.add(getContractCall(web3j, ownerOf(tokenId), getAddress()));
            balanceIds.add(tokenId);
            if (requests.getRequests().size() >= EthereumNetworkBase.getBatchProcessingLimit(tokenInfo.chainId))
            {
                //do this send
                handleRequests(requests, balanceIds, heldTokens);
                requests = web3j.newBatch();
            }
        }

        if (requests.getRequests().size() > 0)
        {
            //do final call
            handleRequests(requests, balanceIds, heldTokens);
        }

        if (batchProcessingError)
        {
            return checkBalances(web3j, eventIds);
        }
        else
        {
            return heldTokens;
        }
    }

    private void handleRequests(BatchRequest requests, List<BigInteger> balanceIds, HashSet<BigInteger> heldTokens) throws IOException
    {
        int index = 0;
        BatchResponse responses = requests.send();
        if (responses.getResponses().size() != requests.getRequests().size())
        {
            batchProcessingError = true;
            return;
        }

        //process responses
        for (Response<?> rsp : responses.getResponses())
        {
            BigInteger tokenId = balanceIds.get(index);
            if (isOwner(rsp, tokenId))
            {
                heldTokens.add(tokenId);
            }

            index++;
        }

        balanceIds.clear();
    }

    private boolean isOwner(Response<?> rsp, BigInteger tokenId)
    {
        EthCall response = (EthCall) rsp;
        Function function = ownerOf(tokenId);
        List<Type> responseValues = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        if (!responseValues.isEmpty())
        {
            String owner = responseValues.get(0).getValue().toString();
            return (!owner.isEmpty() && owner.equalsIgnoreCase(getWallet()));
        }
        else
        {
            return false;
        }
    }

    @Override
    public EthFilter getReceiveBalanceFilter(Event event, DefaultBlockParameter startBlock, DefaultBlockParameter endBlock)
    {
        final org.web3j.protocol.core.methods.request.EthFilter filter =
                new org.web3j.protocol.core.methods.request.EthFilter(
                        startBlock,
                        endBlock,
                        tokenInfo.address) // contract address
                        .addSingleTopic(EventEncoder.encode(event));// transfer event format

        filter.addSingleTopic(null);
        filter.addSingleTopic("0x" + TypeEncoder.encode(new Address(getWallet()))); //listen for events 'to' our wallet, we can check balance at end
        filter.addSingleTopic(null);
        return filter;
    }

    @Override
    public EthFilter getSendBalanceFilter(Event event, DefaultBlockParameter startBlock, DefaultBlockParameter endBlock)
    {
        final org.web3j.protocol.core.methods.request.EthFilter filter =
                new org.web3j.protocol.core.methods.request.EthFilter(
                        startBlock,
                        endBlock,
                        tokenInfo.address)  // contract address
                        .addSingleTopic(EventEncoder.encode(event));// transfer event format

        filter.addSingleTopic("0x" + TypeEncoder.encode(new Address(getWallet()))); //listen for events 'from' our wallet
        filter.addSingleTopic(null);
        filter.addSingleTopic(null);
        return filter;
    }

    /**
     * Returns false if the Asset balance appears to be entries with only TokenId - indicating an ERC721Ticket
     *
     * @return
     */
    @Override
    public boolean checkBalanceType()
    {
        boolean onlyHasTokenId = true;
        //if elements contain asset with only assetId then most likely this is a ticket.
        for (NFTAsset a : tokenBalanceAssets.values())
        {
            if (!a.isBlank()) onlyHasTokenId = false;
        }

        return tokenBalanceAssets.size() == 0 || !onlyHasTokenId;
    }

    public String getTransferID(Transaction tx)
    {
        if (tx.transactionInput != null && tx.transactionInput.miscData.size() > 0)
        {
            String tokenHex = tx.transactionInput.miscData.get(0);
            if (tokenHex.length() > 0)
            {
                BigInteger id = new BigInteger(tokenHex, 16);
                tokenHex = id.toString();
                if (tokenHex.length() < 7)
                {
                    return id.toString(16);
                }
            }
        }

        return "0";
    }

    @Override
    public String getTransferValue(TransactionInput txInput, int precision)
    {
        precision++;
        //return the tokenId from the transfer if possible
        try
        {
            BigInteger tokenId = new BigInteger(txInput.miscData.get(0), 16);
            String tokenIdStr = tokenId.toString();
            if (tokenIdStr.length() > precision)
            {
                return "1";
            }
            else
            {
                return "#" + tokenId.toString();
            }
        }
        catch (Exception e)
        {
            //
        }

        return getTransferValueRaw(txInput).toString();
    }

    @Override
    public BigInteger getTransferValueRaw(TransactionInput txInput)
    {
        //return the tokenId from the transfer if possible
        try
        {
            return new BigInteger(txInput.miscData.get(0), 16);
        }
        catch (Exception e)
        {
            //
        }

        return BigInteger.ONE;
    }

    @Override
    public BigDecimal getBalanceRaw()
    {
        return balance;
    }

    /**
     * Create a live balance of assets. Only need to query assets that have dropped out
     * From where this is called, the current assets are those loaded from opensea call
     * If there is a token that previously was there, but now isn't, it could be because
     * the opensea call was split or that the owner transferred the token
     *
     * @param assetMap Loaded Assets from Realm
     * @return map of currently known live assets
     */
    @Override
    public Map<BigInteger, NFTAsset> queryAssets(Map<BigInteger, NFTAsset> assetMap)
    {
        //check all tokens in this contract
        assetMap.putAll(tokenBalanceAssets);

        //now check balance for all tokenIds (note that ERC1155 has a batch balance check, ERC721 does not)
        for (Map.Entry<BigInteger, NFTAsset> entry : assetMap.entrySet())
        {
            BigInteger checkId = entry.getKey();
            NFTAsset checkAsset = entry.getValue();

            //check balance
            String owner = callSmartContractFunction(tokenInfo.chainId, ownerOf(checkId), getAddress(), getWallet());
            if (owner == null) //play it safe. If there's no 'ownerOf' for an ERC721, it's something custom like ENS
            {
                checkAsset.setBalance(BigDecimal.ONE);
            }
            else if (owner.equalsIgnoreCase(getWallet()))
            {
                checkAsset.setBalance(BigDecimal.ONE);
            }
            else
            {
                checkAsset.setBalance(BigDecimal.ZERO);
            }

            //add back into asset map
            tokenBalanceAssets.put(checkId, checkAsset);
        }

        return tokenBalanceAssets;
    }

    @Override
    public List<BigInteger> getChangeList(Map<BigInteger, NFTAsset> assetMap)
    {
        //detect asset removal
        List<BigInteger> oldAssetIdList = new ArrayList<>(assetMap.keySet());
        oldAssetIdList.removeAll(tokenBalanceAssets.keySet());

        List<BigInteger> changeList = new ArrayList<>(oldAssetIdList);

        //Now detect differences or new tokens
        for (BigInteger tokenId : tokenBalanceAssets.keySet())
        {
            NFTAsset newAsset = tokenBalanceAssets.get(tokenId);
            NFTAsset oldAsset = assetMap.get(tokenId);

            if (oldAsset == null || newAsset.hashCode() != oldAsset.hashCode())
            {
                changeList.add(tokenId);
            }
        }

        return changeList;
    }

    private Request<?, EthCall> getContractCall(Web3j web3j, Function function, String contractAddress)
    {
        String encodedFunction = FunctionEncoder.encode(function);
        org.web3j.protocol.core.methods.request.Transaction transaction
                = createEthCallTransaction(getWallet(), contractAddress, encodedFunction);
        return web3j.ethCall(transaction, DefaultBlockParameterName.LATEST);
    }

    private static Function ownerOf(BigInteger token)
    {
        return new Function(
                "ownerOf",
                Collections.singletonList(new Uint256(token)),
                Collections.singletonList(new TypeReference<Address>()
                {
                }));
    }

    private Function tokenOfOwnerByIndex(BigInteger index)
    {
        return new Function("tokenOfOwnerByIndex",
                Arrays.asList(new Address(getWallet()), new Uint256(index)),
                Collections.singletonList(new TypeReference<Uint256>() {}));
    }

    @Override
    public List<Integer> getStandardFunctions()
    {
        return Arrays.asList(R.string.action_transfer);
    }
}
