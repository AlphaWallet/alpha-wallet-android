package com.alphawallet.app.repository.entity;

import com.alphawallet.app.entity.TokensMapping;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class RealmTokenMapping extends RealmObject {
    public String address;
    public Long chainId;
    public String group;

    public RealmTokenMapping() {
        super();
    }

    public RealmTokenMapping(String address, Long chainId, String group) {
        super();
        this.address = address;
        this.chainId = chainId;
        this.group = group;
    }
}
