package com.alphawallet.app.entity.attestation;

public enum SmartPassReturn
{
    ALREADY_IMPORTED("Already Imported"),
    IMPORT_SUCCESS("Import Success"),
    IMPORT_FAILED("Pass not valid"),
    NO_CONNECTION("Could not connect to SmartLayer, try again later");

    private final String property;

    SmartPassReturn(String property)
    {
        this.property = property;
    }

    public String getValue()
    {
        return property;
    }
}
