package com.alphawallet.app.entity.attestation;

import java.util.HashSet;

/**
 * Created by JB on 19/01/2023.
 */
public class Attestation
{
    private final String address;
    private String name;
    private final HashSet<Long> supportedChains = new HashSet<>();
    private String subTitle;
    private String id;

    public Attestation(String address)
    {
        this.address = address;
    }

    public String databaseKey(String hash)
    {
        return this.address + "-" + hash;
    }

    public boolean isSupportedChain(long chainId)
    {
        return (supportedChains.isEmpty() || supportedChains.contains(chainId));
    }

    public void addSupportedChain(long chainId)
    {
        supportedChains.add(chainId);
    }

    public String getSubTitle()
    {
        return subTitle;
    }

    public void setSubTitle(String subTitle)
    {
        this.subTitle = subTitle;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}

//Render attestation:
//1. Parse XML
//2. Extract params from XML
//3. Check Attestation Integrity
//4. Store attestation
//5. Fetch attestation from DB in wallet view - attestation needs to be compatible with TCM ?
//6. Write renderer
