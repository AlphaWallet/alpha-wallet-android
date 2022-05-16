package com.alphawallet.app.entity;

import java.math.BigInteger;

/**
 * Created by JB on 1/04/2022.
 */
public class SyncDef
{
    public final BigInteger eventReadStartBlock;
    public final BigInteger eventReadEndBlock;
    public final Boolean upwardSync;
    public final EventSyncState state;

    public SyncDef(BigInteger eventReadStartBlock, BigInteger eventReadEndBlock, EventSyncState currentState, Boolean upwardSync)
    {
        this.eventReadStartBlock = eventReadStartBlock;
        this.eventReadEndBlock = eventReadEndBlock;
        this.state = currentState;
        this.upwardSync = upwardSync;
    }
}
