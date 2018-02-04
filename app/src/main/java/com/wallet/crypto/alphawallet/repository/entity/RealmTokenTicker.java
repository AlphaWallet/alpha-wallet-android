package com.wallet.crypto.alphawallet.repository.entity;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class RealmTokenTicker extends RealmObject {
    @PrimaryKey
    private String contract;
    private String price;
    private String percentChange24h;
    private long createdTime;
    private String id;
    private String image;
    private long updatedTime;

    //So far Realm doesn't support extending RealmTokenInfo as a base class :(
    private String venue = null;
    private String date = null;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContract() {
        return contract;
    }

    public void setContract(String contract) {
        this.contract = contract;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getPercentChange24h() {
        return percentChange24h;
    }

    public void setPercentChange24h(String percentChange24h) {
        this.percentChange24h = percentChange24h;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public long getUpdatedTime() {
        return updatedTime;
    }

    public String getVenue() { return venue; }

    public void setVenue(String venue)
    {
        this.venue = venue;
    }

    public String getDate()
    {
        return date;
    }

    public void setDate(String date)
    {
        this.date = date;
    }

    public void setUpdatedTime(long updatedTime) {
        this.updatedTime = updatedTime;
    }
}
