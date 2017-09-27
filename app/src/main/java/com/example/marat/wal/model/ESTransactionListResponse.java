package com.example.marat.wal.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by marat on 9/26/17.
 */

public class ESTransactionListResponse {
    @SerializedName("result")
    private List<ESTransaction> transactionList = new ArrayList<ESTransaction>();

    public List<ESTransaction> getTransactionList() {
        return transactionList;
    }
}
