package io.stormbird.wallet.repository.entity;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.stormbird.wallet.entity.Wallet;

/**
 * Created by James on 8/11/2018.
 * Stormbird in Singapore
 */
public class RealmWalletData extends RealmObject
{
    @PrimaryKey
    private String address;
    private String ENSName;
    private String balance;
    private String name;
    private int type;
    private long lastBackup;

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public String getENSName()
    {
        return ENSName;
    }

    public void setENSName(String ENSName)
    {
        this.ENSName = ENSName;
    }

    public String getBalance()
    {
        return balance;
    }

    public void setBalance(String balance)
    {
        this.balance = balance;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Wallet.WalletType getType() { return Wallet.WalletType.values()[type]; }
    public void setType(Wallet.WalletType type) { this.type = type.ordinal(); }

    public long getLastBackup()
    {
        return lastBackup;
    }
    public void setLastBackup(long lastBackup)
    {
        this.lastBackup = lastBackup;
    }
}
