package com.alphawallet.app.entity.tokens;


import android.app.Activity;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Pair;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.ERC1155TransferEvent;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionInput;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.opensea.AssetContract;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.repository.entity.RealmNFTAsset;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.BaseViewModel;
import com.google.gson.Gson;

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
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.realm.Realm;

import static com.alphawallet.app.repository.TokenRepository.callSmartContractFunction;
import static com.alphawallet.app.repository.TokenRepository.callSmartContractFunctionArray;
import static com.alphawallet.app.repository.TokensRealmSource.databaseKey;
import static com.alphawallet.app.util.Utils.parseTokenId;
import static org.web3j.tx.Contract.staticExtractEventParameters;

public class ERC1155Token extends Token implements Parcelable
{
    private final Map<BigInteger, NFTAsset> assets;
    private BigInteger lastEventBlockRead;
    private AssetContract assetContract;

    public ERC1155Token(TokenInfo tokenInfo, Map<BigInteger, NFTAsset> balanceList, long blancaTime, String networkName) {
        super(tokenInfo, balanceList != null ? BigDecimal.valueOf(balanceList.keySet().size()) : BigDecimal.ZERO, blancaTime, networkName, ContractType.ERC1155);
        lastEventBlockRead = BigInteger.ZERO;
        if (balanceList != null)
        {
            assets = balanceList;
        }
        else
        {
            assets = new HashMap<>();
        }
        setInterfaceSpec(ContractType.ERC1155);
    }

    private ERC1155Token(Parcel in) {
        super(in);
        String assetJSON = in.readString();
        if (!TextUtils.isEmpty(assetJSON)) assetContract = new Gson().fromJson(assetJSON, AssetContract.class);
        assets = new HashMap<>();
        //read in the element list
        int size = in.readInt();
        for (; size > 0; size--)
        {
            BigInteger tokenId = new BigInteger(in.readString(), Character.MAX_RADIX);
            NFTAsset asset = in.readParcelable(NFTAsset.class.getClassLoader());
            assets.put(tokenId, asset);
        }
    }

    public static final Creator<ERC1155Token> CREATOR = new Creator<ERC1155Token>() {
        @Override
        public ERC1155Token createFromParcel(Parcel in) {
            return new ERC1155Token(in);
        }

        @Override
        public ERC1155Token[] newArray(int size) {
            return new ERC1155Token[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(assetContract != null ? assetContract.getJSON() : "");
        dest.writeInt(assets.size());
        for (BigInteger assetKey : assets.keySet())
        {
            dest.writeString(assetKey.toString(Character.MAX_RADIX));
            dest.writeParcelable(assets.get(assetKey), flags);
        }
    }

    public void setEventBlockRead(BigInteger blockNumber)
    {
        lastEventBlockRead = blockNumber;
    }

    @Override
    public Map<BigInteger, NFTAsset> getTokenAssets() {
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

    //TODO: Handle various collection formats; may need to specify collection
    @Override
    public Map<BigInteger, NFTAsset> getTokenAssetMap(BigInteger tokenId) {
        return new HashMap<BigInteger, NFTAsset>()
            {{ put(tokenId, assets.get(tokenId));}};
    }

    public boolean isNonFungible() { return true; }

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
    public byte[] getTransferBytes(String to, List<BigInteger> tokenIds) throws NumberFormatException
    {
        Function txFunc = getTransferFunction(to, tokenIds);
        String encodedFunction = FunctionEncoder.encode(txFunc);
        return Numeric.hexStringToByteArray(Numeric.cleanHexPrefix(encodedFunction));
    }

    @Override
    public byte[] getTransferBytes(String to, ArrayList<Pair<BigInteger, NFTAsset>> transferData) throws NumberFormatException
    {
        Function txFunc = getTransferFunction(to, transferData);
        String encodedFunction = FunctionEncoder.encode(txFunc);
        return Numeric.hexStringToByteArray(Numeric.cleanHexPrefix(encodedFunction));
    }

    @Override
    public boolean hasGroupedTransfer() { return true; }

    public Function getTransferFunction(String to, ArrayList<Pair<BigInteger, NFTAsset>> transferData) throws NumberFormatException
    {
        Function               function;
        List<Type>             params;
        List<TypeReference<?>> returnTypes = Collections.emptyList();
        //Map<BigInteger, BigInteger> idMap = Utils.getIdMap(tokenIds);

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
        Function               function    = null;
        List<Type>             params;
        BigInteger             tokenIdBI   = tokenIds.get(0);
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

    //Must not be called on main thread
    private void updateBalances(Realm realm)
    {
        if (updateBatchBalance(realm)) { return; }
        updateIndividualBalances(realm);
    }

    private boolean updateBatchBalance(Realm realm)
    {
        boolean updated = false;
        Function balanceOfBatch = balanceOfBatch(getWallet(), assets.keySet());
        List<Uint256> balances = callSmartContractFunctionArray(tokenInfo.chainId, balanceOfBatch, getAddress(), getWallet());
        //fill in balances
        if (balances != null && balances.size() > 0)
        {
            int index = 0;
            for (BigInteger tokenId : assets.keySet())
            {
                if (assets.get(tokenId).setBalance(new BigDecimal(balances.get(index).getValue())) && !updated)
                {
                    updated = true;
                }
                index++;
            }

            if (updated) { updateRealmBalances(realm); }
            return true;
        }
        else
        {
            return false;
        }
    }

    private void updateIndividualBalances(Realm realm)
    {
        boolean updated = false;
        for (BigInteger tokenId : assets.keySet())
        {
            try
            {
                Function balanceOf = balanceOfSingle(getWallet(), tokenId);
                String response = callSmartContractFunction(tokenInfo.chainId, balanceOf, getAddress(), getWallet());
                BigInteger balance = new BigInteger(response);
                //TODO: Allow for decimal
                if (assets.get(tokenId).setBalance(new BigDecimal(balance)) && !updated) { updated = true; }
            }
            catch (Exception e)
            {
                if (BuildConfig.DEBUG) e.printStackTrace();
            }
        }

        if (updated)
        {
            updateRealmBalances(realm);
        }
    }

    private void updateRealmBalances(Realm realm)
    {
        realm.executeTransaction(r -> {
            for (BigInteger tokenId : assets.keySet())
            {
                String key = RealmNFTAsset.databaseKey(this, tokenId);
                RealmNFTAsset realmAsset = realm.where(RealmNFTAsset.class)
                        .equalTo("tokenIdAddr", key)
                        .findFirst();

                if (realmAsset == null) { continue; } //only update established assets
                realmAsset.setBalance(assets.get(tokenId).getBalance());
            }
        });
    }

    @Override
    public boolean checkRealmBalanceChange(RealmToken realmToken)
    {
        String currentState = realmToken.getBalance();
        if (!lastEventBlockRead.equals(realmToken.getErc1155BlockRead())) return true;
        if (currentState == null || !currentState.equals(getBalanceRaw().toString())) return true;
        //check balances
        for (NFTAsset a : assets.values())
        {
            if (!a.needsLoading() && !a.requiresReplacement()) return true;
        }
        return false;
    }

    @Override
    public void setRealmBalance(RealmToken realmToken)
    {
        realmToken.setBalance(getBalanceRaw().toString());
    }

    @Override
    public String getStringBalance()
    {
        return getBalanceRaw().toString();
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
            return BigInteger.valueOf(txInput.arrayValues.size()/2);
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
                int halfIndex = tx.transactionInput.arrayValues.size()/2;
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

    /*
     @dev Either `TransferSingle` or `TransferBatch` MUST emit when tokens are transferred, including zero value transfers as well as minting or burning (see "Safe Transfer Rules" section of the standard).
     The `_operator` argument MUST be msg.sender.
     The `_from` argument MUST be the address of the holder whose balance is decreased.
     The `_to` argument MUST be the address of the recipient whose balance is increased.
     The `_id` argument MUST be the token type being transferred.
     The `_value` argument MUST be the number of tokens the holder balance is decreased by and match what the recipient balance is increased by.
     When minting/creating tokens, the `_from` argument MUST be set to `0x0` (i.e. zero address).
     When burning/destroying tokens, the `_to` argument MUST be set to `0x0` (i.e. zero address).
     */
    //event TransferSingle(address indexed _operator, address indexed _from, address indexed _to, uint256 _id, uint256 _value);

    /**
     @dev Either `TransferSingle` or `TransferBatch` MUST emit when tokens are transferred, including zero value transfers as well as minting or burning (see "Safe Transfer Rules" section of the standard).
     The `_operator` argument MUST be msg.sender.
     The `_from` argument MUST be the address of the holder whose balance is decreased.
     The `_to` argument MUST be the address of the recipient whose balance is increased.
     The `_ids` argument MUST be the list of tokens being transferred.
     The `_values` argument MUST be the list of number of tokens (matching the list and order of tokens specified in _ids) the holder balance is decreased by and match what the recipient balance is increased by.
     When minting/creating tokens, the `_from` argument MUST be set to `0x0` (i.e. zero address).
     When burning/destroying tokens, the `_to` argument MUST be set to `0x0` (i.e. zero address).
     */
    //event TransferBatch(address indexed _operator, address indexed _from, address indexed _to, uint256[] _ids, uint256[] _values);

    //get balance
    private Event getBalanceUpdateEvents()
    {
        //event TransferSingle(address indexed _operator, address indexed _from, address indexed _to, uint256 _id, uint256 _value);
        List<TypeReference<?>> paramList = new ArrayList<>();
        paramList.add(new TypeReference<Address>(true) { });
        paramList.add(new TypeReference<Address>(true) { });
        paramList.add(new TypeReference<Address>(true) { });
        paramList.add(new TypeReference<Uint256>(false) { });
        paramList.add(new TypeReference<Uint256>(false) { });

        return new Event("TransferSingle", paramList);
    }

    private EthFilter getReceiveBalanceFilter(Event event, DefaultBlockParameter startBlock)
    {
        final org.web3j.protocol.core.methods.request.EthFilter filter =
                new org.web3j.protocol.core.methods.request.EthFilter(
                        startBlock,
                        DefaultBlockParameterName.LATEST,
                        tokenInfo.address) // retort contract address
                        .addSingleTopic(EventEncoder.encode(event));// commit event format

        filter.addSingleTopic(null);
        filter.addSingleTopic(null);
        filter.addSingleTopic("0x" + TypeEncoder.encode(new Address(getWallet()))); //listen for events 'to'.
        return filter;
    }

    private EthFilter getSendBalanceFilter(Event event, DefaultBlockParameter startBlock)
    {
        final org.web3j.protocol.core.methods.request.EthFilter filter =
                new org.web3j.protocol.core.methods.request.EthFilter(
                        startBlock,
                        DefaultBlockParameterName.LATEST,
                        tokenInfo.address) // retort contract address
                        .addSingleTopic(EventEncoder.encode(event));// commit event format

        filter.addSingleTopic(null);
        filter.addSingleTopic("0x" + TypeEncoder.encode(new Address(getWallet()))); //listen for events 'from'.
        filter.addSingleTopic(null);
        return filter;
    }

    @Override
    public BigDecimal updateBalance(Realm realm)
    {
        try
        {
            updateBalances(realm);
            if (true) return new BigDecimal(assets.keySet().size());

            final Web3j web3j = TokenRepository.getWeb3jService(tokenInfo.chainId);

            List<ERC1155TransferEvent> txEvents = new ArrayList<>();
            DefaultBlockParameter startBlock = DefaultBlockParameter.valueOf(lastEventBlockRead);
            final Event event = getBalanceUpdateEvents();
            EthFilter filter = getReceiveBalanceFilter(event, startBlock);
            EthFilter filterOut = getSendBalanceFilter(event, startBlock);
            EthLog receiveLogs = web3j.ethGetLogs(filter).send();
            EthLog sendLogs = web3j.ethGetLogs(filterOut).send();

            if (receiveLogs.getLogs().size() > 0)
            {
                for (EthLog.LogResult<?> ethLog : receiveLogs.getLogs())
                {
                    String block = ((Log) ethLog.get()).getBlockNumberRaw();
                    if (block == null || block.length() == 0) continue;
                    BigInteger blockNumber = new BigInteger(Numeric.cleanHexPrefix(block), 16);
                    final EventValues eventValues = staticExtractEventParameters(event, (Log) ethLog.get());
                    String fromAddress = eventValues.getIndexedValues().get(1).getValue().toString();
                    String toAddress = eventValues.getIndexedValues().get(2).getValue().toString();
                    BigInteger _id = new BigInteger(eventValues.getNonIndexedValues().get(0).getValue().toString());
                    BigInteger value = new BigInteger(eventValues.getNonIndexedValues().get(1).getValue().toString());

                    txEvents.add(new ERC1155TransferEvent(blockNumber, toAddress, fromAddress, _id, value, toAddress.equalsIgnoreCase(getAddress())));
                }
            }

            if (sendLogs.getLogs().size() > 0)
            {
                for (EthLog.LogResult<?> ethLog : sendLogs.getLogs())
                {
                    String block = ((Log) ethLog.get()).getBlockNumberRaw();
                    if (block == null || block.length() == 0) continue;
                    BigInteger blockNumber = new BigInteger(Numeric.cleanHexPrefix(block), 16);
                    final EventValues eventValues = staticExtractEventParameters(event, (Log) ethLog.get());
                    String fromAddress = eventValues.getIndexedValues().get(1).getValue().toString();
                    String toAddress = eventValues.getIndexedValues().get(2).getValue().toString();
                    BigInteger _id = new BigInteger(eventValues.getNonIndexedValues().get(0).getValue().toString());
                    BigInteger value = new BigInteger(eventValues.getNonIndexedValues().get(1).getValue().toString());

                    txEvents.add(new ERC1155TransferEvent(blockNumber, toAddress, fromAddress, _id, value, toAddress.equalsIgnoreCase(getAddress())));
                }
            }

            Collections.sort(txEvents);

            //now move through the recorded events to get the current balance
            for (ERC1155TransferEvent ev : txEvents)
            {
                NFTAsset thisAsset = assets.get(ev.tokenId);
                if (thisAsset == null) thisAsset = new NFTAsset();
                //add or subtract
                thisAsset.setBalance(thisAsset.getBalance().add(new BigDecimal(ev.value)));
                assets.put(ev.tokenId, thisAsset);
            }

            if (txEvents.size() > 0)
            {
                lastEventBlockRead = txEvents.get(txEvents.size() - 1).blockNumber;
                //write to token

                //now update the NFTAsset balances
                realm.executeTransactionAsync(r -> {
                    RealmToken realmToken = r.where(RealmToken.class)
                            .equalTo("address", databaseKey(tokenInfo.chainId, getAddress()))
                            .findFirst();

                    if (realmToken != null)
                    {
                        realmToken.setErc1155BlockRead(lastEventBlockRead);
                    }

                    for (BigInteger tokenId : assets.keySet())
                    {
                        NFTAsset thisAsset = assets.get(tokenId);

                        String key = RealmNFTAsset.databaseKey(this, tokenId);
                        RealmNFTAsset realmAsset = realm.where(RealmNFTAsset.class)
                                .equalTo("tokenIdAddr", key)
                                .findFirst();

                        if (thisAsset.getBalance().equals(BigDecimal.ZERO))
                        {
                            //delete asset
                            if (realmAsset != null) realmAsset.deleteFromRealm();
                        }
                        else
                        {
                            if (realmAsset == null)
                            {
                                realmAsset = realm.createObject(RealmNFTAsset.class, key);
                                realmAsset.setMetaData(thisAsset.jsonMetaData());
                            }

                            realmAsset.setBalance(thisAsset.getBalance());
                        }
                    }
                });
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return new BigDecimal(assets.keySet().size());
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
}
