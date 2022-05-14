package com.alphawallet.app.entity;

public class AddressInformation
{
    public String publicNameTag;

    public String contractAddress;

    public String uiWebsiteLink;

    public String publicNote;

    public AddressInformation(String publicNameTag, String contractAddress, String uiWebsiteLink, String publicNote)
    {
        this.publicNameTag = publicNameTag;
        this.contractAddress = contractAddress;
        this.uiWebsiteLink = uiWebsiteLink;
        this.publicNote = publicNote;
    }

    @Override
    public String toString()
    {
        return "AddressInformation{" +
                "publicNameTag='" + publicNameTag + '\'' +
                ", contractAddress='" + contractAddress + '\'' +
                ", uiWebsiteLink='" + uiWebsiteLink + '\'' +
                ", publicNote='" + publicNote + '\'' +
                '}';
    }
}
