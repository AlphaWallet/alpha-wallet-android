package com.alphawallet.token.entity;

import com.google.gson.Gson;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by James on 14/05/2019.
 * Stormbird in Sydney
 */
public class TokenScriptResult
{
    public static final class Attribute {
        public final String id;
        public String name;
        public String text;
        public final BigInteger value;
        public final boolean userInput;
        public Attribute(String attributeId, String name, BigInteger value, String text) {
            this.id = attributeId;
            this.name = name;
            this.text = text;
            this.value = value;
            this.userInput = false;
        }

        public Attribute(String attributeId, String name, BigInteger value, String text, boolean userInput) {
            this.id = attributeId;
            this.name = name;
            this.text = text;
            this.value = value;
            this.userInput = userInput;
        }
    }

    private Map<String, Attribute> attrs = new HashMap<>();

    public void setAttribute(String key, Attribute attr)
    {
        attrs.put(key, attr);
    }

    public Map<String, TokenScriptResult.Attribute> getAttributes()
    {
        return attrs;
    }

    public Attribute getAttribute(String attributeId) {
        if (attrs != null)
        {
            return attrs.get(attributeId);
        }
        else
        {
            return null;
        }
    }

    public static <T> void addPair(StringBuilder attrs, String attrId, T attrValue)
    {
        attrs.append(attrId);
        attrs.append(": ");

        if (attrValue == null)
        {
            attrs.append("\"\"");
        }
        else if (attrValue instanceof BigInteger)
        {
            attrs.append("\"");
            attrs.append(((BigInteger)attrValue).toString(10));
            attrs.append("\"");
        }
        else if (attrValue instanceof List)
        {
            attrs.append("\'");
            attrs.append(new Gson().toJson(attrValue));
            attrs.append("\'");
        }
        else
        {
            String attrValueStr = (String) attrValue;
            if (attrValueStr.length() == 0 || (attrValueStr.charAt(0) != '{')) attrs.append("\"");
            attrs.append(attrValueStr);
            if (attrValueStr.length() == 0 || (attrValueStr.charAt(0) != '{')) attrs.append("\"");
        }

        attrs.append(",\n");
    }
}
