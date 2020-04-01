package com.alphawallet.token.entity;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by JB on 21/03/2020.
 */
public class EventDefinition
{
    public ContractInfo originContract;
    public String attributeId; //TransactionResult: method
    public String eventName;
    public Module eventModule;
    public String filter;
    public String select;
    public BigInteger readBlock;
    public boolean hasNewEvent = false;

    public String getFilterTopicValue()
    {
        // (\+\d{4}|\-\d{4})
        Matcher m = Pattern.compile("\\$\\{([^}]+)\\}").matcher(filter);
        String item = m.find() ? m.group(1) : null;
        return item;
    }

    public String getFilterTopicIndex()
    {
        String[] item = filter.split("=");
        return item[0];
    }

    public int getTopicIndex(String filterTopic)
    {
        if (eventModule == null || filterTopic == null) return -1;
        return eventModule.getTopicIndex(filterTopic);
    }

    public int getSelectIndex(boolean indexed)
    {
        int index = 0;
        boolean found = false;
        for (String label : eventModule.getArgNames(indexed))
        {
            if (label.equals(select))
            {
                found = true;
                break;
            }
            else
            {
                index++;
            }
        }

        return found ? index : -1;
    }
}
