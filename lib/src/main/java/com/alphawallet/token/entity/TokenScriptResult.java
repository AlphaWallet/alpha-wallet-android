package com.alphawallet.token.entity;

import static com.alphawallet.token.tools.TokenDefinition.Syntax.IA5String;

import com.alphawallet.token.tools.TokenDefinition;
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
        public final TokenDefinition.Syntax syntax;

        public Attribute(String attributeId, String name, BigInteger value, String text) {
            this.id = attributeId;
            this.name = name;
            this.text = text;
            this.value = value;
            this.userInput = false;
            this.syntax = IA5String;
        }

        public Attribute(String attributeId, String name, BigInteger value, String text, boolean userInput) {
            this.id = attributeId;
            this.name = name;
            this.text = text;
            this.value = value;
            this.userInput = userInput;
            this.syntax = IA5String;
        }

        public Attribute(com.alphawallet.token.entity.Attribute attr, BigInteger value, String text)
        {
            this.id = attr.name;
            this.name = attr.label;
            this.text = text;
            this.value = value;
            this.userInput = false;
            syntax = attr.syntax;
        }

        public String attrValue()
        {
            StringBuilder sb = new StringBuilder();

            switch (syntax)
            {
                case IA5String:
                case DirectoryString:
                case BitString:
                case CountryString:
                case JPEG:
                default:
                    if (text.length() == 0 || (text.charAt(0) != '{')) sb.append("\"");
                    sb.append(text);
                    if (text.length() == 0 || (text.charAt(0) != '{')) sb.append("\"");
                    break;

                case Integer:
                    if (value != null)
                    {
                        sb.append((value).toString(10));
                    }
                    else
                    {
                        sb.append("0");
                    }
                    break;
                case GeneralizedTime:
                    break;
                case Boolean:
                    if (text.equalsIgnoreCase("TRUE"))
                    {
                        sb.append("true");
                    }
                    else
                    {
                        sb.append("false");
                    }
                    break;
                case NumericString:
                    sb.append(text);
                    break;
            }

            return sb.toString();
        }

        public boolean requiresQuoteWrap()
        {
            return switch (syntax)
            {
                case DirectoryString, IA5String, NumericString, BitString, JPEG -> true;
                default -> false;
            };
        }
    }

    private final Map<String, Attribute> attrs = new HashMap<>();

    public void setAttribute(String key, Attribute attr)
    {
        attrs.put(key, attr);
    }

    public Map<String, TokenScriptResult.Attribute> getAttributes()
    {
        return attrs;
    }

    public Attribute getAttribute(String attributeId)
    {
        return attrs.get(attributeId);
    }

    public static <T> void addPair(StringBuilder attrs, Attribute attr)
    {
        attrs.append(attr.id);
        attrs.append(": ");
        if (attr.requiresQuoteWrap())
        {
            attrs.append("\"");
        }
        attrs.append(attr.attrValue());
        if (attr.requiresQuoteWrap())
        {
            attrs.append("\"");
        }
        attrs.append(",\n");
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
            attrs.append(((BigInteger) attrValue).toString(10));
        }
        else if (attrValue instanceof Integer)
        {
            attrs.append(attrValue);
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
