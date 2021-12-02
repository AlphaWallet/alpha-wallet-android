package com.alphawallet.app.repository.entity;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class RealmAToken extends RealmObject {
    @PrimaryKey
    public String address;
}
