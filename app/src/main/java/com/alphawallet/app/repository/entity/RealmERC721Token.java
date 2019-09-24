package com.alphawallet.app.repository.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import com.alphawallet.app.entity.opensea.Asset;

/**
 * Created by James on 22/10/2018.
 * Stormbird in Singapore
 */
public class RealmERC721Token extends RealmObject
{
    @PrimaryKey
    private String address;
    private String name;
    private String symbol;
    private long addedTime;
    private long updatedTime;
    private String tokenIdList;
    private String schemaName;
    private int chainId;
    private int contractType;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public int getAddressChain() {
        if (address.contains("-"))
        {
            String chain = address.split("-")[1];
            if (chain.length() > 0 && Character.isDigit(chain.charAt(0)))
            {
                return Integer.parseInt(chain);
            }
        }

        return 0;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getAddedTime() {
        return addedTime;
    }

    public void setAddedTime(long addedTime) {
        this.addedTime = addedTime;
    }

    public long getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(long updatedTime) {
        this.updatedTime = updatedTime;
    }

    public List<String> getTokenIdList()
    {
        String[] list = tokenIdList.split(",");
        List<String> tokens = new ArrayList<>();
        Collections.addAll(tokens, list);
        return tokens;
    }

    public void setTokenIdList(List<Asset> balance) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Asset asset : balance)
        {
            if (!first) sb.append(",");
            sb.append(asset.getTokenId());
            first = false;
        }

        this.tokenIdList = sb.toString();
    }

    public String getSchemaName()
    {
        return schemaName;
    }

    public void setSchemaName(String schemaName)
    {
        this.schemaName = schemaName;
    }

    public int getChainId()
    {
        return chainId;
    }

    public void setChainId(int chainId)
    {
        this.chainId = chainId;
    }

    public int getContractType()
    {
        return contractType;
    }

    public void setContractType(int contractType)
    {
        this.contractType = contractType;
    }

    public boolean isTokenId()
    {
        if (address.contains("-"))
        {
            String[] split = address.split("-");
            //address:0x16baf0de678e52367adc69fd067e5edd1d33e3bf-4-6488
            return split.length > 2 && Character.isDigit(split[2].charAt(0));
        }

        return false;
    }
}
