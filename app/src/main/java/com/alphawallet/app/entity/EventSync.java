package com.alphawallet.app.entity;

import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.service.TransactionsService;

import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.Response;
import org.web3j.utils.Numeric;

import java.math.BigInteger;

import io.realm.Realm;
import timber.log.Timber;

/**
 * Created by JB on 23/04/2022.
 */
public class EventSync
{
    public static final long BLOCK_SEARCH_INTERVAL = 100000L;

    private final Token token;

    public EventSync(Token token)
    {
        this.token = token;
    }

    public SyncDef getSyncDef(Realm realm)
    {
        BigInteger currentBlock = TransactionsService.getCurrentBlock(token.tokenInfo.chainId);
        EventSyncState syncState = getCurrentTokenSyncState(realm);
        BigInteger lastBlockRead = BigInteger.valueOf(getLastEventRead(realm));
        long readBlockSize = getCurrentEventBlockSize(realm);
        BigInteger eventReadStartBlock;
        BigInteger eventReadEndBlock;

        if (currentBlock.equals(BigInteger.ZERO)) return null;

        boolean upwardSync = false;

        switch (syncState)
        {
            default:
            case DOWNWARD_SYNC_START:
                eventReadStartBlock = BigInteger.ONE;
                eventReadEndBlock = BigInteger.valueOf(-1L);
                //write the start point here
                writeStartSyncBlock(realm, currentBlock.longValue());
                break;
            case DOWNWARD_SYNC: //we needed to slow down the sync
                eventReadStartBlock = lastBlockRead.subtract(BigInteger.valueOf(readBlockSize));
                eventReadEndBlock = lastBlockRead;
                if (eventReadStartBlock.compareTo(BigInteger.ZERO) <= 0)
                {
                    eventReadStartBlock = BigInteger.ONE;
                    syncState = EventSyncState.DOWNWARD_SYNC_COMPLETE;
                }
                break;
            case UPWARD_SYNC_MAX: //we are syncing from the point we started the downward sync
                upwardSync = true;
                eventReadStartBlock = lastBlockRead;
                eventReadEndBlock = BigInteger.valueOf(-1L);
                break;
            case UPWARD_SYNC: //we encountered upward sync issues
                upwardSync = true;
                eventReadStartBlock = lastBlockRead;
                eventReadEndBlock = lastBlockRead.add(BigInteger.valueOf(readBlockSize));
                break;
        }

        return new SyncDef(eventReadStartBlock, eventReadEndBlock, syncState, upwardSync);
    }

    public boolean handleEthLogError(Response.Error error, DefaultBlockParameter startBlock, DefaultBlockParameter endBlock, SyncDef sync, Realm realm)
    {
        if (error.getCode() == -32005)
        {
            long newStartBlock;
            long newEndBlock;
            long blockSize;
            BigInteger startBlockVal = Numeric.toBigInt(startBlock.getValue());
            EventSyncState state;
            if (sync.upwardSync)
            {
                if (endBlock.getValue().equalsIgnoreCase("latest"))
                {
                    newStartBlock = startBlockVal.longValue();
                    newEndBlock = newStartBlock + BLOCK_SEARCH_INTERVAL;
                    blockSize = BLOCK_SEARCH_INTERVAL;
                }
                else
                {
                    BigInteger endBlockVal = Numeric.toBigInt(endBlock.getValue());
                    //in the process of scanning up, encountered too many events for this interval, so reduce interval
                    long currentBlockScan = endBlockVal.subtract(startBlockVal).longValue();
                    newStartBlock = startBlockVal.longValue();
                    newEndBlock = newStartBlock + currentBlockScan / 2;
                    blockSize = newEndBlock - newStartBlock;
                }

                state = EventSyncState.UPWARD_SYNC;
            }
            else
            {
                //Too many logs, reduce fetch size
                //Measure interval
                if (endBlock.getValue().equalsIgnoreCase("latest"))
                {
                    BigInteger currentBlock = TransactionsService.getCurrentBlock(token.tokenInfo.chainId);
                    blockSize = BLOCK_SEARCH_INTERVAL;
                    newEndBlock = currentBlock.longValue();
                }
                else
                {
                    //in the process of scanning down, encountered too many events for this interval, so reduce interval
                    newEndBlock = Numeric.toBigInt(endBlock.getValue()).longValue();
                    newStartBlock = reduceBlockSearch(newEndBlock, startBlockVal);
                    blockSize = newEndBlock - newStartBlock;
                }

                state = EventSyncState.DOWNWARD_SYNC;
            }

            updateEventReads(realm, newEndBlock, blockSize, state);

            //now call again
            return true;
        }
        else
        {
            //try again later with same settings, could be connection fault.
            Timber.w("Event fetch error: %s", error.getMessage());
            return false;
        }
    }

    private long reduceBlockSearch(long currentBlock, BigInteger startBlock)
    {
        if (startBlock.compareTo(BigInteger.ONE) == 0)
        {
            //initial search, apply full limit
            return currentBlock - BLOCK_SEARCH_INTERVAL;
        }
        else
        {
            //half existing interval, and try again
            return currentBlock - (currentBlock - startBlock.longValue())/2;
        }
    }

    private long getCurrentEventBlockSize(Realm instance)
    {
        RealmAuxData rd = instance.where(RealmAuxData.class)
                .equalTo("instanceKey", TokensRealmSource.databaseKey(token.tokenInfo.chainId, token.getAddress()))
                .findFirst();

        if (rd == null)
        {
            return BLOCK_SEARCH_INTERVAL;
        }
        else
        {
            return rd.getResultReceivedTime();
        }
    }

    protected EventSyncState getCurrentTokenSyncState(Realm instance)
    {
        RealmAuxData rd = instance.where(RealmAuxData.class)
                .equalTo("instanceKey", TokensRealmSource.databaseKey(token.tokenInfo.chainId, token.getAddress()))
                .findFirst();

        if (rd == null)
        {
            writeCurrentTokenSyncState(instance, EventSyncState.DOWNWARD_SYNC_START);
            return EventSyncState.DOWNWARD_SYNC_START;
        }
        else
        {
            int state = rd.getTokenId().intValue();
            if (state >= EventSyncState.DOWNWARD_SYNC_START.ordinal() || state < EventSyncState.TOP_LIMIT.ordinal())
            {
                return EventSyncState.values()[state];
            }
            else
            {
                writeCurrentTokenSyncState(instance, EventSyncState.DOWNWARD_SYNC_START);
                return EventSyncState.DOWNWARD_SYNC_START;
            }
        }
    }

    public void writeCurrentTokenSyncState(Realm realm, EventSyncState newState)
    {
        if (realm == null) return;
        realm.executeTransaction(r -> {
            String key = TokensRealmSource.databaseKey(token.tokenInfo.chainId, token.getAddress());
            RealmAuxData rd = r.where(RealmAuxData.class)
                    .equalTo("instanceKey", key)
                    .findFirst();

            if (rd == null)
            {
                rd = r.createObject(RealmAuxData.class, key); //create asset in realm
            }

            rd.setTokenId(String.valueOf(newState.ordinal()));
            r.insertOrUpdate(rd);
        });
    }

    protected long getLastEventRead(Realm instance)
    {
        RealmAuxData rd = instance.where(RealmAuxData.class)
                .equalTo("instanceKey", TokensRealmSource.databaseKey(token.tokenInfo.chainId, token.getAddress()))
                .findFirst();

        if (rd == null)
        {
            return -1L;
        }
        else
        {
            return rd.getResultTime();
        }
    }

    private long getSyncStart(Realm instance)
    {
        RealmAuxData rd = instance.where(RealmAuxData.class)
                .equalTo("instanceKey", TokensRealmSource.databaseKey(token.tokenInfo.chainId, token.getAddress()))
                .findFirst();

        if (rd == null)
        {
            return TransactionsService.getCurrentBlock(token.tokenInfo.chainId).longValue();
        }
        else
        {
            return Long.parseLong(rd.getFunctionId());
        }
    }

    protected void writeStartSyncBlock(Realm realm, long currentBlock)
    {
        if (realm == null) return;
        realm.executeTransaction(r -> {
            String key = TokensRealmSource.databaseKey(token.tokenInfo.chainId, token.getAddress());
            RealmAuxData rd = r.where(RealmAuxData.class)
                    .equalTo("instanceKey", key)
                    .findFirst();

            if (rd == null)
            {
                rd = r.createObject(RealmAuxData.class, key); //create asset in realm
            }

            rd.setFunctionId(String.valueOf(currentBlock));
            r.insertOrUpdate(rd);
        });
    }

    public void updateEventReads(Realm realm, SyncDef sync, BigInteger currentBlock, int evReads)
    {
        switch (sync.state)
        {
            //update current read-from block (either we completed a successful
            case UPWARD_SYNC_MAX: //completed read from last sync to latest
            case DOWNWARD_SYNC_START: //completed read from 1 to latest
                sync = new SyncDef(BigInteger.ONE, currentBlock,
                        EventSyncState.UPWARD_SYNC_MAX, true);
                break;
            case DOWNWARD_SYNC_COMPLETE: //finished the event read
                //next time, start where we originally synced from
                updateEventReads(realm, getSyncStart(realm), BLOCK_SEARCH_INTERVAL, EventSyncState.UPWARD_SYNC_MAX);
                return;
            case DOWNWARD_SYNC: //successful intermediate downward sync
            case UPWARD_SYNC: //successful intermediate upward sync
            case TOP_LIMIT:
                break;
        }

        updateEventReads(realm, sync.eventReadEndBlock.longValue(), calcNewIntervalSize(sync, evReads), sync.state);
    }

    private void updateEventReads(Realm realm, long lastRead, long readInterval, EventSyncState state)
    {
        if (realm == null) return;
        realm.executeTransaction(r -> {
            String key = TokensRealmSource.databaseKey(token.tokenInfo.chainId, token.getAddress());
            RealmAuxData rd = r.where(RealmAuxData.class)
                    .equalTo("instanceKey", key)
                    .findFirst();

            if (rd == null)
            {
                rd = r.createObject(RealmAuxData.class, key); //create asset in realm
            }

            rd.setResultTime(lastRead);
            rd.setResultReceivedTime(readInterval);
            rd.setTokenId(String.valueOf(state.ordinal()));

            r.insertOrUpdate(rd);
        });
    }

    // If we're syncing downwards, work out what event block size we should read next
    private long calcNewIntervalSize(SyncDef sync, int evReads)
    {
        if (sync.upwardSync) return BLOCK_SEARCH_INTERVAL;
        long endBlock = sync.eventReadEndBlock.longValue() == -1 ? TransactionsService.getCurrentBlock(token.tokenInfo.chainId).longValue()
                : sync.eventReadEndBlock.longValue();
        long currentReadSize = endBlock - sync.eventReadStartBlock.longValue();
        long maxLogReads = EthereumNetworkBase.getMaxEventFetch(token.tokenInfo.chainId).longValue();
        // under the log limit?
        if (evReads == 0)
        {
            currentReadSize *= 4;
        }
        else if (evReads < 1000)
        {
            //increase block read size
            currentReadSize *= 2;
        }
        else if ((maxLogReads - evReads) > maxLogReads*0.25)
        {
            currentReadSize += BLOCK_SEARCH_INTERVAL;
        }

        return currentReadSize;
    }
}
