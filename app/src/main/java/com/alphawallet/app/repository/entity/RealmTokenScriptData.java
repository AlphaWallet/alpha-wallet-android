package com.alphawallet.app.repository.entity;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

/**
 * Created by JB on 17/08/2020.
 */
public class RealmTokenScriptData extends RealmObject
{
    @PrimaryKey
    private String instanceKey;
    private String fileHash; //uniquely identify tokenscript - script MD5 hash
    private String filePath;
    private String names; //CSV list of token names allowing for plurals. //TODO: replace with RealmMap when available
    private String viewList; //CSV list of event views //TODO: replace with RealmMap when available
    private boolean hasEvents; //TokenScript has events

    public long getChainId()
    {
        String chainId = instanceKey.split("-")[1];
        if (Character.isDigit(chainId.charAt(0))) return Long.parseLong(chainId);
        else return MAINNET_ID;
    }

    public String getOriginTokenAddress()
    {
        return instanceKey.split("-")[0];
    }

    public String getFilePath()
    {
        return filePath;
    }

    public void setFilePath(String filePath)
    {
        this.filePath = filePath;
    }

    public String getName(int count)
    {
        Map<String, String> nameMap = getValueMap(names);
        String value = null;
        switch (count)
        {
            case 1:
                if (nameMap.containsKey("one")) value = nameMap.get("one");
                break;
            default:
                value = nameMap.get("other");
            case 2:
                if (value == null) value = nameMap.get("two");
                if (value == null) value = getName(1); //still don't have anything, try singular
                break;
        }

        if (value == null && nameMap.values().size() > 0)
        {
            value = nameMap.values().iterator().next();
        }

        return value;
    }

    private Map<String, String> getValueMap(String values)
    {
        Map<String, String> nameMap = new HashMap<>();
        if (TextUtils.isEmpty(values)) return nameMap;

        String[] nameList = values.split(",");
        boolean state = true;
        String key = null;
        for (String s : nameList)
        {
            if (state)
            {
                key = s;
                state = false;
            }
            else
            {
                nameMap.put(key, s);
                state = true;
            }
        }

        return nameMap;
    }

    public void setNames(String names)
    {
        this.names = names;
    }

    public List<String> getViewList()
    {
        List<String> viewNames = new ArrayList<>();
        if (TextUtils.isEmpty(viewList)) return viewNames;

        String[] views = viewList.split(",");
        viewNames.addAll(Arrays.asList(views));
        return viewNames;
    }

    public void setViewList(String viewList)
    {
        this.viewList = viewList;
    }

    public void setFileHash(String fileHash)
    {
        this.fileHash = fileHash;
    }

    public String getFileHash()
    {
        return this.fileHash;
    }

    public boolean hasEvents()
    {
        return hasEvents;
    }

    public void setHasEvents(boolean hasEvents)
    {
        this.hasEvents = hasEvents;
    }
}
