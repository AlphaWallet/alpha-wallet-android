package com.alphawallet.app.entity.tokens;


import static com.alphawallet.app.repository.TokenRepository.callSmartContractFunctionArray;
import static com.alphawallet.app.util.Utils.parseTokenId;
import static org.web3j.tx.Contract.staticExtractEventParameters;

import android.app.Activity;
import android.util.Pair;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.LogOverflowException;
import com.alphawallet.app.entity.SyncDef;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionInput;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.opensea.AssetContract;
import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.app.repository.EventResult;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.repository.entity.RealmNFTAsset;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.service.TransactionsService;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.BaseViewModel;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.EventValues;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.realm.Realm;
import timber.log.Timber;

public class ERC1155Token extends Token
{
    private final Map<BigInteger, NFTAsset> assets;
    private AssetContract assetContract;

    public ERC1155Token(TokenInfo tokenInfo, Map<BigInteger, NFTAsset> balanceList, long blancaTime, String networkName)
    {
        super(tokenInfo, balanceList != null ? BigDecimal.valueOf(balanceList.keySet().size()) : BigDecimal.ZERO, blancaTime, networkName, ContractType.ERC1155);
        if (balanceList != null)
        {
            assets = balanceList;
        }
        else
        {
            assets = new ConcurrentHashMap<>();
        }
        setInterfaceSpec(ContractType.ERC1155);
        group = TokenGroup.NFT;
    }

    @Override
    public Map<BigInteger, NFTAsset> getTokenAssets()
    {
        return assets;
    }

    @Override
    public NFTAsset getAssetForToken(BigInteger tokenId)
    {
        return assets.get(tokenId);
    }

    @Override
    public NFTAsset getAssetForToken(String tokenIdStr)
    {
        return assets.get(parseTokenId(tokenIdStr));
    }

    public boolean isNonFungible()
    {
        return true;
    }

    @Override
    public int getContractType()
    {
        return R.string.erc1155;
    }

    @Override
    public void addAssetToTokenBalanceAssets(BigInteger tokenId, NFTAsset asset)
    {
        assets.put(tokenId, asset);
        balance = new BigDecimal(assets.keySet().size());
    }

    @Override
    public Map<BigInteger, NFTAsset> getCollectionMap()
    {
        Map<BigInteger, BigInteger> collectionBuilder = new HashMap<>();
        Map<BigInteger, NFTAsset> collectionMap = new HashMap<>();
        //run through all assets to fetch the list
        for (BigInteger tokenId : assets.keySet())
        {
            BigInteger baseTokenId = getBaseTokenId(tokenId);

            if (baseTokenId.compareTo(BigInteger.ZERO) > 0)
            {
                NFTAsset thisAsset = assets.get(tokenId);
                NFTAsset checkAsset;
                if (!collectionBuilder.containsKey(baseTokenId))
                {
                    checkAsset = new NFTAsset(thisAsset);
                    collectionBuilder.put(baseTokenId, tokenId);
                }
                else
                {
                    checkAsset = collectionMap.get(collectionBuilder.get(baseTokenId));
                }

                checkAsset.addCollectionToken(tokenId);
                collectionMap.put(collectionBuilder.get(baseTokenId), checkAsset);
            }
            else
            {
                collectionMap.put(tokenId, assets.get(tokenId)); //add token as-is
            }
        }

        return collectionMap;
    }

    @Override
    public BigDecimal getBalanceRaw()
    {
        return new BigDecimal(assets.size());
    }

    @Override
    public List<Integer> getStandardFunctions()
    {
        return Arrays.asList(R.string.action_transfer);
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

    @Override
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

    @Override
    public byte[] getTransferBytes(String to, ArrayList<Pair<BigInteger, NFTAsset>> transferData) throws NumberFormatException
    {
        Function txFunc = getTransferFunction(to, transferData);
        String encodedFunction = FunctionEncoder.encode(txFunc);
        return Numeric.hexStringToByteArray(Numeric.cleanHexPrefix(encodedFunction));
    }

    @Override
    public boolean hasGroupedTransfer()
    {
        return true;
    }

    public Function getTransferFunction(String to, ArrayList<Pair<BigInteger, NFTAsset>> transferData) throws NumberFormatException
    {
        Function function;
        List<Type> params;
        List<TypeReference<?>> returnTypes = Collections.emptyList();

        if (transferData.size() == 1)
        {
            params = Arrays.asList(new Address(getWallet()), new Address(to), new Uint256(transferData.get(0).first),
                    new Uint256(transferData.get(0).second.getSelectedBalance().toBigInteger()), new DynamicBytes(new byte[0]));
            function = new Function("safeTransferFrom", params, returnTypes);
        }
        else
        {
            List<Uint256> idList = new ArrayList<>(transferData.size());
            List<Uint256> amounts = new ArrayList<>(transferData.size());
            for (int i = 0; i < transferData.size(); i++)
            {
                idList.add(new Uint256(transferData.get(i).first));
                amounts.add(new Uint256(transferData.get(i).second.getSelectedBalance().toBigInteger()));
            }

            params = Arrays.asList(new Address(getWallet()), new Address(to), new DynamicArray<>(Uint256.class, idList),
                    new DynamicArray<>(Uint256.class, amounts), new DynamicBytes(new byte[0]));
            function = new Function("safeBatchTransferFrom", params, returnTypes);
        }
        return function;
    }

    @Override
    public Function getTransferFunction(String to, List<BigInteger> tokenIds) throws NumberFormatException
    {
        Function function;
        List<Type> params;
        BigInteger tokenIdBI = tokenIds.get(0);
        List<TypeReference<?>> returnTypes = Collections.emptyList();
        Map<BigInteger, BigInteger> idMap = Utils.getIdMap(tokenIds);

        if (idMap.keySet().size() == 1)
        {
            params = Arrays.asList(new Address(getWallet()), new Address(to), new Uint256(tokenIdBI),
                    new Uint256(idMap.get(tokenIdBI)), new DynamicBytes(new byte[0]));
            function = new Function("safeTransferFrom", params, returnTypes);
        }
        else
        {
            List<Uint256> idList = new ArrayList<>(idMap.keySet().size());
            List<Uint256> amounts = new ArrayList<>(idMap.keySet().size());
            for (BigInteger tokenId : idMap.keySet())
            {
                idList.add(new Uint256(tokenId));
                amounts.add(new Uint256(idMap.get(tokenId)));
            }

            params = Arrays.asList(new Address(getWallet()), new Address(to), new DynamicArray<>(Uint256.class, idList),
                    new DynamicArray<>(Uint256.class, amounts), new DynamicBytes(new byte[0]));
            function = new Function("safeBatchTransferFrom", params, returnTypes);
        }
        return function;
    }

    @Override
    public List<BigInteger> getChangeList(Map<BigInteger, NFTAsset> assetMap)
    {
        //detect asset removal
        List<BigInteger> oldAssetIdList = new ArrayList<>(assetMap.keySet());
        oldAssetIdList.removeAll(assets.keySet());

        List<BigInteger> changeList = new ArrayList<>(oldAssetIdList);

        //Now detect differences or new tokens
        for (BigInteger tokenId : assets.keySet())
        {
            NFTAsset newAsset = assets.get(tokenId);
            NFTAsset oldAsset = assetMap.get(tokenId);

            if (oldAsset == null || newAsset.hashCode() != oldAsset.hashCode())
            {
                changeList.add(tokenId);
            }
        }

        return changeList;
    }

    private List<Uint256> fetchBalances(Set<BigInteger> tokenIds)
    {
        Function balanceOfBatch = balanceOfBatch(getWallet(), tokenIds);
        return callSmartContractFunctionArray(tokenInfo.chainId, balanceOfBatch, getAddress(), getWallet());
    }

    @Override
    public Map<BigInteger, NFTAsset> queryAssets(Map<BigInteger, NFTAsset> assetMap)
    {
        //first see if there's no change; if this is the case we can skip
        if (assetsUnchanged(assetMap)) return assets;

        //add all known tokens in
        Map<BigInteger, NFTAsset> sum = new HashMap<>(assetMap);
        sum.putAll(assets);
        Set<BigInteger> tokenIds = sum.keySet();
        Function balanceOfBatch = balanceOfBatch(getWallet(), tokenIds);
        List<Uint256> balances = callSmartContractFunctionArray(tokenInfo.chainId, balanceOfBatch, getAddress(), getWallet());
        Map<BigInteger, NFTAsset> updatedAssetMap;

        if (balances != null && balances.size() > 0)
        {
            updatedAssetMap = new HashMap<>();
            int index = 0;
            for (BigInteger tokenId : tokenIds)
            {
                NFTAsset thisAsset = new NFTAsset(sum.get(tokenId));
                BigInteger balance = balances.get(index).getValue();
                thisAsset.setBalance(new BigDecimal(balance));
                updatedAssetMap.put(tokenId, thisAsset);

                index++;
            }
        }
        else
        {
            updatedAssetMap = assets;
        }

        return updatedAssetMap;
    }

    /**
     * See if any asset has disappeared. If not, then no need to check
     * Note: this takes into account that the map size is the same, but a new asset appeared and an old disappeared
     * (cannot just compare map sizes)
     * Also - need to call this in case any new asset has an associated balance
     *
     * @param assetMap
     * @return
     */
    private boolean assetsUnchanged(Map<BigInteger, NFTAsset> assetMap)
    {
        boolean assetsUnchanged = true;
        for (BigInteger tokenId : assetMap.keySet())
        {
            if (!assets.containsKey(tokenId))
            {
                assetsUnchanged = false;
                break;
            }
        }

        for (BigInteger tokenId : assets.keySet())
        {
            if (!assetMap.containsKey(tokenId))
            {
                assetsUnchanged = false;
                break;
            }
        }

        return assetsUnchanged;
    }

    private void updateRealmBalance(Realm realm, Set<BigInteger> tokenIds, List<Uint256> balances)
    {
        boolean updated = false;
        //fill in balances
        if (balances != null && balances.size() > 0)
        {
            int index = 0;
            for (BigInteger tokenId : tokenIds)
            {
                NFTAsset asset = assets.get(tokenId);
                BigDecimal newBalance = new BigDecimal(balances.get(index).getValue());
                if (asset == null)
                {
                    NFTAsset newAsset = new NFTAsset(tokenId);
                    newAsset.setBalance(newBalance);
                    assets.put(tokenId, newAsset);
                    updated = true;
                }
                else if (asset.setBalance(newBalance))
                {
                    updated = true;
                }

                if (realm == null && newBalance.equals(BigDecimal.ZERO))
                {
                    assets.remove(tokenId);
                }
                index++;
            }

            if (updated)
            {
                updateRealmBalances(realm);
            }
        }
    }

    private void updateRealmBalances(Realm realm)
    {
        if (realm == null) return;
        realm.executeTransaction(r -> {
            for (Map.Entry<BigInteger, NFTAsset> entry : assets.entrySet())
            {
                BigInteger tokenId = entry.getKey();
                NFTAsset asset = entry.getValue();
                String key = RealmNFTAsset.databaseKey(this, tokenId);
                RealmNFTAsset realmAsset = realm.where(RealmNFTAsset.class)
                        .equalTo("tokenIdAddr", key)
                        .findFirst();

                if (realmAsset == null && asset.getBalance().equals(BigDecimal.ZERO)) continue; // no recorded token, no balance, skip

                if (realmAsset == null)
                {
                    realmAsset = r.createObject(RealmNFTAsset.class, key); //create asset in realm
                    realmAsset.setMetaData(asset.jsonMetaData());
                }

                if (asset.getBalance().equals(BigDecimal.ZERO)) //remove asset no longer in balance
                {
                    realmAsset.deleteFromRealm();
                    assets.remove(tokenId);
                }
                else
                {
                    realmAsset.setBalance(entry.getValue().getBalance()); //update realm balance
                    r.insertOrUpdate(realmAsset);
                }
            }
        });
    }

    @Override
    public boolean checkRealmBalanceChange(RealmToken realmToken)
    {
        String currentState = realmToken.getBalance();
        if (currentState == null || !currentState.equals(getBalanceRaw().toString())) return true;
        //check balances
        for (NFTAsset a : assets.values())
        {
            if (!a.needsLoading() && !a.requiresReplacement())
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean checkBalanceChange(Token oldToken)
    {
        if (super.checkBalanceChange(oldToken)) return true;
        if (getTokenAssets().size() != oldToken.getTokenAssets().size()) return true;
        for (BigInteger tokenId : assets.keySet())
        {
            NFTAsset newAsset = assets.get(tokenId);
            NFTAsset oldAsset = oldToken.getAssetForToken(tokenId);
            if (newAsset == null || oldAsset == null || !newAsset.equals(oldAsset))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setRealmBalance(RealmToken realmToken)
    {
        realmToken.setBalance(getBalanceRaw().toString());
    }

    @Override
    public String getStringBalanceForUI(int scale)
    {
        return balance.toString();
    }

    @Override
    public void clickReact(BaseViewModel viewModel, Activity activity)
    {
        viewModel.showTokenList(activity, this);
    }

    @Override
    public void setAssetContract(AssetContract contract)
    {
        assetContract = contract;
    }

    @Override
    public AssetContract getAssetContract()
    {
        return assetContract;
    }

    @Override
    public BigInteger getTransferValueRaw(TransactionInput txInput)
    {
        if (txInput.arrayValues != null)
        {
            return BigInteger.valueOf(txInput.arrayValues.size() / 2);
        }
        else
        {
            return BigInteger.ONE;
        }
    }

    @Override
    public List<NFTAsset> getAssetListFromTransaction(Transaction tx)
    {
        List<NFTAsset> assetList = new ArrayList<>();
        if (tx == null || tx.transactionInput == null) return assetList;
        NFTAsset asset;
        //given a transaction get the associated Asset map (with quantities)
        switch (tx.transactionInput.functionData.functionName)
        {
            case "safeTransferFrom":
                BigInteger tokenId = new BigInteger(tx.transactionInput.miscData.get(0), 16);
                BigInteger count = new BigInteger(tx.transactionInput.miscData.get(1), 16);
                asset = new NFTAsset(assets.get(tokenId));
                asset.setSelectedBalance(new BigDecimal(count));
                assetList.add(asset);
                break;
            case "safeBatchTransferFrom":
                int halfIndex = tx.transactionInput.arrayValues.size() / 2;
                for (int i = 0; i < halfIndex; i++)
                {
                    asset = new NFTAsset(assets.get(tx.transactionInput.arrayValues.get(i)));
                    int amountIndex = i + halfIndex;
                    asset.setSelectedBalance(new BigDecimal(tx.transactionInput.arrayValues.get(amountIndex)));
                    assetList.add(asset);
                }
                break;
            default:
                break;
        }

        return assetList;
    }

    //get balance
    private Event getBalanceUpdateEvents()
    {
        //event TransferSingle(address indexed _operator, address indexed _from, address indexed _to, uint256 _id, uint256 _value);
        List<TypeReference<?>> paramList = new ArrayList<>();
        paramList.add(new TypeReference<Address>(true) {});
        paramList.add(new TypeReference<Address>(true) {}); // from (1)
        paramList.add(new TypeReference<Address>(true) {}); // to   (2)
        paramList.add(new TypeReference<Uint256>(false) {});
        paramList.add(new TypeReference<Uint256>(false) {});

        return new Event("TransferSingle", paramList);
    }

    private Event getBatchBalanceUpdateEvents()
    {
        //event TransferBatch(address indexed _operator, address indexed _from, address indexed _to, uint256[] _ids, uint256[] _values);
        List<TypeReference<?>> paramList = new ArrayList<>();
        paramList.add(new TypeReference<Address>(true) {});
        paramList.add(new TypeReference<Address>(true) {});
        paramList.add(new TypeReference<Address>(true) {});
        paramList.add(new TypeReference<DynamicArray<Uint256>>(false) {});
        paramList.add(new TypeReference<DynamicArray<Uint256>>(false) {});

        return new Event("TransferBatch", paramList);
    }

    @Override
    public EthFilter getReceiveBalanceFilter(Event event, DefaultBlockParameter startBlock, DefaultBlockParameter endBlock)
    {
        final org.web3j.protocol.core.methods.request.EthFilter filter =
                new org.web3j.protocol.core.methods.request.EthFilter(
                        startBlock,
                        endBlock,
                        tokenInfo.address) // contract address
                        .addSingleTopic(EventEncoder.encode(event));// event format

        filter.addSingleTopic(null);
        filter.addSingleTopic(null);
        filter.addSingleTopic("0x" + TypeEncoder.encode(new Address(getWallet()))); //listen for events 'to' the wallet
        return filter;
    }

    @Override
    public EthFilter getSendBalanceFilter(Event event, DefaultBlockParameter startBlock, DefaultBlockParameter endBlock)
    {
        final org.web3j.protocol.core.methods.request.EthFilter filter =
                new org.web3j.protocol.core.methods.request.EthFilter(
                        startBlock,
                        endBlock,
                        tokenInfo.address) // contract address
                        .addSingleTopic(EventEncoder.encode(event)); // send event format

        filter.addSingleTopic(null);
        filter.addSingleTopic("0x" + TypeEncoder.encode(new Address(getWallet()))); //listen for events 'from' the wallet
        filter.addSingleTopic(null);
        return filter;
    }

    /**
     * Uses both events and balance call. Each call to updateBalance uses 3 Node calls:
     * 1. Get new TransferSingle events since last call
     * 2. Get new TransferBatch events since last call
     * 3. Call ERC1155 contract function balanceOfBatch on all tokenIds
     *
     * Once we have the current balance for potential tokens the database is updated to reflect the current status
     *
     * Note that this function is used even for contracts covered by OpenSea: This is because we could be looking at
     * a contract 'join' between successive opensea reads. With accounts with huge quantity of NFT, this happens a lot
     *
     * @param realm
     * @return
     */
    @Override
    public BigDecimal updateBalance(Realm realm)
    {
        SyncDef sync = eventSync.getSyncDef(realm);
        if (sync == null) return balance;

        DefaultBlockParameter startBlock = DefaultBlockParameter.valueOf(sync.eventReadStartBlock);
        DefaultBlockParameter endBlock = DefaultBlockParameter.valueOf(sync.eventReadEndBlock);
        if (sync.eventReadEndBlock.compareTo(BigInteger.valueOf(-1L)) == 0) endBlock = DefaultBlockParameterName.LATEST;

        //take a note of the current block#
        BigInteger currentBlock = TransactionsService.getCurrentBlock(tokenInfo.chainId);

        try
        {
            final Web3j web3j = TokenRepository.getWeb3jService(tokenInfo.chainId);

            Pair<Integer, Pair<HashSet<BigInteger>, HashSet<BigInteger>>> evRead = eventSync.processTransferEvents(web3j,
                    getBalanceUpdateEvents(), startBlock, endBlock, realm);

            Pair<Integer, Pair<HashSet<BigInteger>, HashSet<BigInteger>>> batchRead = eventSync.processTransferEvents(web3j,
                    getBatchBalanceUpdateEvents(), startBlock, endBlock, realm);

            // All tokenIds which have passed through the owner address
            evRead.second.first.addAll(evRead.second.second);
            evRead.second.first.addAll(batchRead.second.first);
            evRead.second.first.addAll(batchRead.second.second);

            //combine the tokenIds with existing assets
            evRead.second.first.addAll(assets.keySet());

            //update balances of all
            List<Uint256> balances = fetchBalances(evRead.second.first);
            //update realm
            updateRealmBalance(realm, evRead.second.first, balances);

            //update read points
            eventSync.updateEventReads(realm, sync, currentBlock, evRead.first); //means our event read was fine
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
            Timber.e(e);
        }

        return new BigDecimal(assets.keySet().size());
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
            Pair<List<BigInteger>, List<BigInteger>> idResult = eventSync.getEventIdResult(eventValues.getNonIndexedValues().get(0),
                    eventValues.getNonIndexedValues().get(1));
            tokenIds.addAll(idResult.first);

            // generating transfer record and storing it
            String from = eventValues.getIndexedValues().get(1).getValue().toString();  // from address
            String to = eventValues.getIndexedValues().get(2).getValue().toString();    // to address
            eventSync.storeTransferData(realm, from, to, idResult, txHash);
            txHashes.add(txHash);
        }

        return tokenIds;
    }

    private Function balanceOfBatch(String address, Set<BigInteger> tokenIds)
    {
        //create address list
        List<Address> batchAddresses = new ArrayList<>(tokenIds.size());
        List<Uint256> tokenIdList = new ArrayList<>(tokenIds.size());
        for (BigInteger id : tokenIds)
        {
            batchAddresses.add(new Address(address));
            tokenIdList.add(new Uint256(id));
        } //populate list of addresses with wallet address

        return new Function("balanceOfBatch",
                Arrays.asList(
                        new DynamicArray<>(Address.class, batchAddresses),
                        new DynamicArray<>(Uint256.class, tokenIdList)),
                Collections.singletonList(new TypeReference<DynamicArray<Uint256>>() {}));
    }

    private Function balanceOfSingle(String address, BigInteger tokenId)
    {
        return new Function(
                "balanceOf",
                Arrays.asList(
                        new org.web3j.abi.datatypes.Address(address),
                        new Uint256(tokenId)),
                Collections.singletonList(new TypeReference<Uint256>() {}));
    }

    /**
     * Handle Fungibility types.
     * Adapted from rules given in the contract here: https://github.com/enjin/erc-1155/blob/master/contracts/ERC1155MixedFungible.sol
     * @param tokenId
     * @return
     */
    private static BigInteger getBaseTokenId(BigInteger tokenId)
    {
        return tokenId.shiftRight(96); //Top 20 bytes (bottom 5 is NFT tokenId)
    }

    public static BigInteger getNFTTokenId(BigInteger tokenId)
    {
        return tokenId.and(Numeric.toBigInt("0xFFFFFFFFFF")); //apply bitmask, bottom 5 bytes
    }

    /**
     * This is a heuristic based on the ERC1155 suggested rules here: https://github.com/enjin/erc-1155/blob/master/contracts/ERC1155MixedFungible.sol
     * See if there's a base ID, if there is, then see if the NFT tokenID is below 0xFFFF and non zero, which means the mask is 0x000000FFFF
     * The consequence of mis-identification here is only that the token type description is not correct.
     * @param tokenId
     * @return
     */
    public static boolean isNFT(BigInteger tokenId)
    {
        return getBaseTokenId(tokenId).compareTo(BigInteger.ZERO) > 0
                && getNFTTokenId(tokenId).compareTo(BigInteger.valueOf(0xFFFF)) < 0
                && getNFTTokenId(tokenId).compareTo(BigInteger.ZERO) > 0;
    }

    @Override
    public boolean isBatchTransferAvailable()
    {
        return true;
    }
}
