package com.alphawallet.app.repository.entity;

import android.text.TextUtils;
import android.util.Base64;

import com.alphawallet.app.util.Utils;
import com.alphawallet.token.tools.Numeric;

import java.util.HashSet;
import java.util.List;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by JB on 19/01/2023.
 */
public class RealmAttestation extends RealmObject
{
    @PrimaryKey
    private String address;
    private String name;
    private String chains;
    private String subTitle;
    private String id;
    private String hash;
    private String attestation;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTokenAddress()
    {
        String tAddress = address;
        if (tAddress.contains("-"))
        {
            return tAddress.split("-")[0];
        }
        else
        {
            return address;
        }
    }

    public String getAttestationKey()
    {
        return address;
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

    public String getHash()
    {
        return hash;
    }

    public void setHash(String hash)
    {
        this.hash = hash;
    }

    public List<Long> getChains()
    {
        return Utils.longListToArray(chains);
    }

    public void addChain(long chainId)
    {
        HashSet<Long> knownChains = new HashSet<>(Utils.longListToArray(chains));
        knownChains.add(chainId);
        this.chains = Utils.longArrayToString(knownChains.toArray(new Long[0]));
    }

    public void setChain(long chainId)
    {
        this.chains = Long.toString(chainId);
    }

    public void setAttestation(byte[] attestation)
    {
        this.attestation = Base64.encodeToString(attestation, Base64.DEFAULT);
    }

    public void setAttestationLink(String attestation)
    {
        //should be in hex
        this.attestation = attestation;
    }

    public String getAttestationLink()
    {
        return attestation;
    }

    public byte[] getAttestation()
    {
        return Base64.decode(attestation, Base64.DEFAULT);
    }

    public boolean supportsChain(List<Long> networkFilters)
    {
        HashSet<Long> knownChains = new HashSet<>(Utils.longListToArray(chains));

        for (long chainId : knownChains)
        {
            if (networkFilters.contains(chainId))
            {
                return true;
            }
        }

        return false;
    }
}
