package com.alphawallet.app.repository.entity;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.service.KeyService;

public class RealmKeyType extends RealmObject
{
    @PrimaryKey
    private String address;
    private byte type;
    private byte authLevel;
    private long lastBackup;
    private long dateAdded;
    private String modulus; //Added for future possibility that we use HD key modulus, so DB doesn't need to be re-initialised

    public String getAddress()
    {
        return address;
    }
    public void setAddress(String address)
    {
        this.address = address;
    }

    public WalletType getType() { return WalletType.values()[type]; }
    public void setType(WalletType type) { this.type = (byte)type.ordinal(); }

    public KeyService.AuthenticationLevel getAuthLevel() { return KeyService.AuthenticationLevel.values()[authLevel]; }
    public void setAuthLevel(KeyService.AuthenticationLevel authLevel) { this.authLevel = (byte)authLevel.ordinal(); }

    public long getLastBackup()
    {
        return lastBackup;
    }
    public void setLastBackup(long lastBackup)
    {
        this.lastBackup = lastBackup;
    }

    public long getDateAdded()
    {
        return dateAdded;
    }
    public void setDateAdded(long dateAdded)
    {
        this.dateAdded = dateAdded;
    }

    public String getKeyModulus()
    {
        return modulus;
    }

    public void setKeyModulus(String modulus)
    {
        this.modulus = modulus;
    }
}
