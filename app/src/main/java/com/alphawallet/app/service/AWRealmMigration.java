package com.alphawallet.app.service;

import com.alphawallet.app.repository.entity.RealmCertificateData;

import io.realm.DynamicRealm;
import io.realm.FieldAttribute;
import io.realm.RealmMigration;
import io.realm.RealmObjectSchema;
import io.realm.RealmSchema;

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
                    .addField("result", String.class, FieldAttribute.INDEXED)
                    .addField("subject", String.class, FieldAttribute.INDEXED)
                    .addField("keyName", String.class, FieldAttribute.INDEXED)
                    .addField("keyType", String.class, FieldAttribute.INDEXED)
                    .addField("issuer", String.class, FieldAttribute.INDEXED)
                    .addField("certificateName", String.class, FieldAttribute.INDEXED)
                    .addField("type", int.class, FieldAttribute.INDEXED);
            oldVersion++;
        }
    }
}