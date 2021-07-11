package com.alphawallet.app.entity.tokens;


import android.os.Parcelable;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.nftassets.NFTAsset;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class ERC1155Token extends Token implements Parcelable
{
    private Map<BigInteger, ERC1155Asset> assets;

    public ERC1155Token(TokenInfo tokenInfo, Map<BigInteger, ERC1155Asset> balanceList, long blancaTime, String networkName) {
        super(tokenInfo, balanceList != null ? BigDecimal.valueOf(balanceList.keySet().size()) : BigDecimal.ZERO, blancaTime, networkName, ContractType.ERC1155);
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

    @Override
    public Map<BigInteger, NFTAsset> getTokenAssets() {
        return null;//tokenBalanceAssets;
    }

    public Map<BigInteger, ERC1155Asset> getAssets()
    {
        return assets;
    }

    public void setAssets(Map<BigInteger, ERC1155Asset> assets)
    {
        this.assets = assets;
    }
}
