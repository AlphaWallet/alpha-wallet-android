package com.langitwallet.app.repository.entity;

import android.util.Base64;

import com.langitwallet.app.util.Utils;

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
    private String collectionId;    //for quick lookup. //TODO: This may change when we receive an updated TokenScript
    private String attestation;
    private String identifierHash;  //this is only populated if there's a TokenScript.
                                    //the formula is keccak256(chainId + collectionID + idFields)
                                    //if it matches an existing attestation we replace

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTokenAddress()
    {
        if (address.contains("-"))
        {
            return address.split("-")[0];
        }
        else
        {
            return address;
        }
    }

    public String getAttestationID()
    {
        int secondIndex = address.indexOf("-");
        secondIndex = secondIndex != -1 ? address.indexOf("-", secondIndex + 1) : -1;
        if (secondIndex != -1)
        {
            return address.substring(secondIndex+1);
        }
        else
        {
            return "";
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

    public String getIdentifierHash()
    {
        return identifierHash;
    }

    public void setIdentifierHash(String identifierHash)
    {
        this.identifierHash = identifierHash;
    }

    public String getCollectionId()
    {
        return collectionId;
    }

    public void setCollectionId(String collectionId)
    {
        this.collectionId = collectionId;
    }
}
