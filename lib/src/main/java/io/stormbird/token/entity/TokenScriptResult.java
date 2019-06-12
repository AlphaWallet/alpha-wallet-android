package io.stormbird.token.entity;

import io.stormbird.token.tools.TokenDefinition;
import io.stormbird.token.util.DateTime;
import io.stormbird.token.util.DateTimeFactory;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
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
        public Map<BigInteger, String> tokenIdText;
        public Map<BigInteger, BigInteger> tokenIdValue;
        public Attribute(String attributeId, String name, BigInteger value, String text) {
            this.id = attributeId;
            this.name = name;
            this.text = text;
            this.value = value;
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

    public static void addPair(StringBuilder attrs, Attribute attr)
    {
        attrs.append(attr.id);
        attrs.append(": ");

        attrs.append("\"");
        attrs.append(attr.text);
        attrs.append("\"");

        attrs.append(",\n");
    }

    public static void addPair(StringBuilder attrs, String attrId, String attrValue)
    {
        attrs.append(attrId);
        attrs.append(": ");

//        if (attrId.equals("time"))
//        {
//            try
//            {
//                DateTime dt = DateTimeFactory.getDateTime(attrValue);
//                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
//                SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("hh:mm:ssZ");
//                String JSDate = dt.format(simpleDateFormat) + "T" + dt.format(simpleTimeFormat);
//
//                attrValue = "{ generalizedTime: \"" + attrValue + "\", date: new Date(\"" + JSDate + "\") }";// ((DateTime) dt).toString();
//                attrs.append(attrValue);
//            }
//            catch (ParseException e)
//            {
//                attrs.append("\"");
//                attrs.append(attrValue);
//                attrs.append("\"");
//            }
//        }
//        else
//        {
//            attrs.append("\"");
//            attrs.append(attrValue);
//            attrs.append("\"");
//        }

        if (attrValue != null && attrValue.charAt(0) != '{') attrs.append("\"");
        attrs.append(attrValue);
        if (attrValue != null && attrValue.charAt(0) != '{') attrs.append("\"");

        attrs.append(",\n");
    }
}
