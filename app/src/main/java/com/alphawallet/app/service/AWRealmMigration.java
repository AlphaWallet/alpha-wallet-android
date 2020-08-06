package com.alphawallet.app.service;

import com.alphawallet.app.repository.entity.RealmAuxData;

import io.realm.DynamicRealm;
import io.realm.DynamicRealmObject;
import io.realm.FieldAttribute;
import io.realm.RealmMigration;
import io.realm.RealmObjectSchema;
import io.realm.RealmSchema;


/**
 * Important! If you make a change to any of the realm objects (eg RealmToken) you need to perform a DataBase migration
 * NB: Ensure the primitive types match up. EG if you used long timeObject; in the DataBase class then use long.class here, don't use Long.class!
 */
public class AWRealmMigration implements RealmMigration
{
    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion)
    {
        RealmSchema schema = realm.getSchema();
        if (oldVersion == 4)
        {
            RealmObjectSchema realmTicker = schema.get("RealmTokenTicker");
            if (!realmTicker.hasField("currencySymbol")) realmTicker.addField("currencySymbol", String.class);
            oldVersion++;
        }
        //Note: these version updates drop through; eg if oldVersion was 4, then the above code AND this code will execute
        if (oldVersion == 5)
        {
            RealmObjectSchema realmToken = schema.get("RealmToken");
            if (!realmToken.hasField("lastTxTime")) realmToken.addField("lastTxTime", long.class); //add the last transaction update time, used to check tokenscript cached result validity
            oldVersion++;
        }

        //Version 6
        if (oldVersion == 6)
        {
            schema.create("RealmCertificateData")
                    .addField("instanceKey", String.class, FieldAttribute.PRIMARY_KEY)
                    .addField("result", String.class)
                    .addField("subject", String.class)
                    .addField("keyName", String.class)
                    .addField("keyType", String.class)
                    .addField("issuer", String.class)
                    .addField("certificateName", String.class)
                    .addField("type", int.class);
            oldVersion++;
        }

        if (oldVersion == 7)
        {
            RealmObjectSchema realmData = schema.get("RealmAuxData");
            if (!realmData.hasField("tokenAddress")) realmData.addField("tokenAddress", String.class);
            if (!realmData.hasField("resultReceivedTime")) realmData.addField("resultReceivedTime", long.class);
            oldVersion += 2;
        }
        else if (oldVersion == 8)
        {
            RealmObjectSchema realmData = schema.get("RealmAuxData");
            if (!realmData.hasField("resultReceivedTime")) realmData.addField("resultReceivedTime", long.class)
                                    .transform(obj -> obj.set("resultReceivedTime", 0L));
            oldVersion++;
        }
    }

    @Override
    public int hashCode()
    {
        return AWRealmMigration.class.hashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        return object instanceof AWRealmMigration;
    }
}