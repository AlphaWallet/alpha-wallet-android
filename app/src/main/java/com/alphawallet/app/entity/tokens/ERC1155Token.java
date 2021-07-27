package com.alphawallet.app.entity.tokens;


import android.os.Parcelable;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.ERC1155TransferEvent;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.repository.entity.RealmNFTAsset;
import com.alphawallet.app.repository.entity.RealmToken;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.EventValues;
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;

import static com.alphawallet.app.repository.TokensRealmSource.databaseKey;
import static org.web3j.tx.Contract.staticExtractEventParameters;

public class ERC1155Token extends Token implements Parcelable
{
    private final Map<BigInteger, NFTAsset> assets;
    private BigInteger lastEventBlockRead;

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

    public void setEventBlockRead(BigInteger blockNumber)
    {
        lastEventBlockRead = blockNumber;
    }

    @Override
    public Map<BigInteger, NFTAsset> getTokenAssets() {
        return assets;
    }


        /**
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
    public void updateBalance(Realm realm)
    {
        try
        {
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
    }
}
