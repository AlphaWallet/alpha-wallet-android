package com.alphawallet.token.entity;

import java.math.BigInteger;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by JB on 21/03/2020.
 */
public class EventDefinition
{
    public ContractInfo contract;
    public String attributeName; //TransactionResult: method
    public String eventName;
    public Module eventModule;
    public String filter;
    public String select;
    public BigInteger readBlock;
    public boolean hasNewEvent = false;
    public Attribute parentAttribute;

    public String getFilterTopicValue()
    {
        // This regex splits up the "filterArgName=${filterValue}" directive and gets the 'filterValue'
        Matcher m = Pattern.compile("\\$\\{([^}]+)\\}").matcher(filter);
        return (m.find() && m.groupCount() >= 1) ? m.group(1) : null;
    }

    public String getFilterTopicIndex()
    {
        // Get the filter name from the directive and strip whitespace
        String[] item = filter.split("=");
        return item[0].replaceAll("\\s+", "");
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
