package com.alphawallet.app.repository.entity;

import com.alphawallet.app.entity.EIP1559FeeOracleResult;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by JB on 19/01/2022.
 */
public class Realm1559Gas extends RealmObject
{
    @PrimaryKey
    private long chainId;

    private long timeStamp;
    private String resultData; //JSON format string

    public Map<Integer, EIP1559FeeOracleResult> getResult()
    {
        Type entry = new TypeToken<Map<Integer, EIP1559FeeOracleResult>>() {}.getType();
        return new Gson().fromJson(resultData, entry);
    }

    public void setResultData(Map<Integer, EIP1559FeeOracleResult> result, long ts)
    {
        //form JSON string and write to DB
        resultData = new Gson().toJson(result);
        timeStamp = ts;
    }

    public long getTimeStamp()
    {
        return timeStamp;
    }
}
