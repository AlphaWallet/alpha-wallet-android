package com.alphawallet.app.entity;

import org.web3j.abi.datatypes.generated.Uint16;
import java.util.List;

public class TransferFromEventResponse
{
    public String _from;

    public String _to;

    public List<Uint16> _indices;
}
