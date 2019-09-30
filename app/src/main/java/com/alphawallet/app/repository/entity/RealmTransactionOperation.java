package com.alphawallet.app.repository.entity;

import io.realm.RealmObject;

public class RealmTransactionOperation extends RealmObject {
    private String transactionId;
    private String viewType;
    private String from;
    private String to;
    private String value;
    private RealmTransactionContract contract;

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getViewType() {
        return viewType;
    }

    public void setViewType(String viewType) {
        this.viewType = viewType;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public RealmTransactionContract getContract() {
        return contract;
    }

    public void setContract(RealmTransactionContract contract) {
        this.contract = contract;
    }
}
