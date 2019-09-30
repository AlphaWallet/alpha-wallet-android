package com.alphawallet.app.repository.entity;

import java.util.ArrayList;
import java.util.List;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import com.alphawallet.app.entity.opensea.Trait;

/**
 * Created by James on 23/10/2018.
 * Stormbird in Singapore
 */
public class RealmERC721Asset extends RealmObject
{
    @PrimaryKey
    private String tokenIdAddr;
    private String imagePreviewUrl;
    private String name;
    private String description;
    private String externalLink;
    private String backgroundColor;
    private String traits;

    public String getTokenId()
    {
        String[] str = tokenIdAddr.split("-");
        if (str.length > 1)
        {
            return str[str.length-1];
        }
        else
        {
            return tokenIdAddr;
        }
    }

    public String getImagePreviewUrl()
    {
        return imagePreviewUrl;
    }

    public void setImagePreviewUrl(String imagePreviewUrl)
    {
        this.imagePreviewUrl = imagePreviewUrl;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getExternalLink()
    {
        return externalLink;
    }

    public void setExternalLink(String externalLink)
    {
        this.externalLink = externalLink;
    }

    public String getBackgroundColor()
    {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor)
    {
        this.backgroundColor = backgroundColor;
    }

    public List<Trait> getTraits()
    {
        List<Trait> traitList = new ArrayList<>();
        String[] traits = this.traits.split(",");
        for (int i = 0; i < traits.length; i+=2)
        {
            if ((i+1) == traits.length) break; //protect against out-by-one error
            Trait t = new Trait(traits[i], traits[i+1]);
            traitList.add(t);
        }

        return traitList;
    }

    public void setTraits(List<Trait> traits)
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Trait t : traits)
        {
            if (!first) sb.append(",");
            sb.append(t.getTraitType());
            sb.append(",");
            sb.append(t.getValue());
            first = false;
        }

        this.traits = sb.toString();
    }

    public static String tokenIdAddrName(String tokenId, String addr)
    {
        return addr + "-" + tokenId;
    }
}

