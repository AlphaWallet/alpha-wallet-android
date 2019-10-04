package com.alphawallet.token.entity;

import com.alphawallet.token.tools.TokenDefinition;
import com.alphawallet.token.util.DateTime;
import com.alphawallet.token.util.DateTimeFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.w3c.dom.Node.ELEMENT_NODE;

/**
 * Created by James on 9/05/2019.
 * Stormbird in Sydney
 */

public class AttributeType {
    public BigInteger bitmask;    // TODO: BigInteger !== BitInt. Test edge conditions.
    public String name;  // TODO: should be polyglot because user change change language in the run
    public String id;
    public int bitshift = 0;
    public TokenDefinition.Syntax syntax;
    public As as;
    public Map<BigInteger, String> members;
    private TokenDefinition definition;
    public FunctionDefinition function = null;
    public boolean userInput = false;

    public AttributeType(Element attr, TokenDefinition def)
    {
        definition = def;
        id = attr.getAttribute("id");
        as = As.Unsigned; //default value
        try {
            switch (attr.getAttribute("syntax")) { // We don't validate syntax here; schema does it.
                case "1.3.6.1.4.1.1466.115.121.1.6":
                    syntax = TokenDefinition.Syntax.BitString;
                    break;
                case "1.3.6.1.4.1.1466.115.121.1.7":
                    syntax = TokenDefinition.Syntax.Boolean;
                    break;
                case "1.3.6.1.4.1.1466.115.121.1.11":
                    syntax = TokenDefinition.Syntax.CountryString;
                    break;
                case "1.3.6.1.4.1.1466.115.121.1.28":
                    syntax = TokenDefinition.Syntax.JPEG;
                    break;
                case "1.3.6.1.4.1.1466.115.121.1.36":
                    syntax = TokenDefinition.Syntax.NumericString;
                    break;
                case "1.3.6.1.4.1.1466.115.121.1.24":
                    syntax = TokenDefinition.Syntax.GeneralizedTime;
                    break;
                case "1.3.6.1.4.1.1466.115.121.1.26":
                    syntax = TokenDefinition.Syntax.IA5String;
                    break;
                case "1.3.6.1.4.1.1466.115.121.1.27":
                    syntax = TokenDefinition.Syntax.Integer;
                    break;
                default: // unknown syntax treat as Directory String
                    syntax = TokenDefinition.Syntax.DirectoryString;
            }
        } catch (NullPointerException e) { // missing <syntax>
            syntax = TokenDefinition.Syntax.DirectoryString; // 1.3.6.1.4.1.1466.115.121.1.15
        }
        bitmask = null;
        for(Node node = attr.getFirstChild();
            node!=null; node=node.getNextSibling()){
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element origin = (Element) node;
                String label = node.getLocalName();
                switch (label)
                {
                    case "origins":
                        handleOrigins(origin);
                        break;
                    case "name":
                        name = definition.getLocalisedString(origin, "string");
                        break;
                    case "mapping":
                        populate(origin);
                        break;
                }
                switch(origin.getAttribute("contract").toLowerCase()) {
                    case "holding-contract":
                        setAs(As.Mapping);
                        // TODO: Syntax is not checked
                        //getFunctions(origin);
                        break;
                    default:
                        break;
                }
            }
        }
        if (bitmask != null ) {
            while (bitmask.mod(BigInteger.ONE.shiftLeft(++bitshift)).equals(BigInteger.ZERO)) ; // !!
            bitshift--;
        }
    }

    private void handleOrigins(Element origin)
    {
        for(Node node = origin.getFirstChild();
            node!=null; node=node.getNextSibling())
        {
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                Element resolve = (Element) node;
                setAs(definition.parseAs(resolve));
                switch (node.getLocalName())
                {
                    case "ethereum":
                        function = definition.parseFunction(resolve, syntax);
                        //drop through (no break)
                    case "token-id":
                        //this value is obtained from the token id
                        setAs(definition.parseAs(resolve));
                        populate(resolve); //check for mappings
                        if (function != null) function.as = definition.parseAs(resolve);
                        if (resolve.hasAttribute("bitmask")) {
                            bitmask = new BigInteger(resolve.getAttribute("bitmask"), 16);
                        }
                        break;
                    case "user-entry":
                        userInput = true;
                        setAs(definition.parseAs(resolve));
                        if (resolve.hasAttribute("bitmask")) {
                            bitmask = new BigInteger(resolve.getAttribute("bitmask"), 16);
                        }
                        break;
                }
            }
        }
    }

    private void populate(Element origin) {
        Element option;
        for (Node n = origin.getFirstChild(); n != null; n = n.getNextSibling())
        {
            if (n.getNodeType() == ELEMENT_NODE)
            {
                Element element = (Element) n;
                if (element.getLocalName().equals("mapping"))
                {
                    members = new HashMap<>();
                    setAs(As.Mapping);

                    NodeList nList = origin.getElementsByTagNameNS(definition.nameSpace, "option");
                    for (int i = 0; i < nList.getLength(); i++) {
                        option = (Element) nList.item(i);
                        members.put(new BigInteger(option.getAttribute("key")), definition.getLocalisedString(option, "value"));
                    }
                }
            }
        }
    }

    public String getSyntaxVal(String data)
    {
        if (data == null) return null;
        switch (syntax)
        {
            case DirectoryString:
                return data;
            case IA5String:
                return data;
            case Integer:
                //convert to integer
                if (Character.isDigit(data.charAt(0)))
                {
                    return data;
                }
                else
                {
                    //convert from byte value
                    return new BigInteger(data.getBytes()).toString();
                }
            case GeneralizedTime:
                try
                {
                    //ensure data is alphanum
                    data = checkAlphaNum(data);

                    DateTime dt = DateTimeFactory.getDateTime(data);
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("hh:mm:ssZ");
                    String generalizedTime = dt.format(simpleDateFormat) + "T" + dt.format(simpleTimeFormat);
                    return "{ generalizedTime: \"" + data + "\", date: new Date(\"" + generalizedTime + "\") }";
                }
                catch (ParseException e)
                {
                    return data;
                }
            case Boolean:
                if (data.length() == 0) return "FALSE";
                if (Character.isDigit(data.charAt(0)))
                {
                    return (data.charAt(0) == '0') ? "FALSE" : "TRUE";
                }
                else if (data.charAt(0) == 0) return "FALSE";
                else if (data.charAt(0) == 1) return "TRUE";
                else return data;
            case BitString:
                return data;
            case CountryString:
                return data;
            case JPEG:
                return data;
            case NumericString:
                if (data == null)
                {
                    return "0";
                }
                else if (data.startsWith("0x"))
                {
                    data = data.substring(2);
                }
                return data;
            default:
                return data;
        }
    }

    private String checkAlphaNum(String data)
    {
        for (char ch : data.toCharArray())
        {
            if (!(Character.isAlphabetic(ch) || Character.isDigit(ch) || ch == '+' || ch == '-' || Character.isWhitespace(ch)))
            {
                //set to current time
                SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssZ", Locale.ENGLISH);
                data = format.format(new Date(System.currentTimeMillis()));
                break;
            }
        }

        return data;
    }

    /**
     * Converts bitshifted/masked token numeric data into corresponding string.
     * eg. Attr is 'venue'; choices are "1" -> "Kaliningrad Stadium", "2" -> "Volgograd Arena" etc.
     * NB 'time' is Unix EPOCH, which is also a mapping.
     * Since the value may not have a corresponding mapping, but is a valid time we should still return the time value
     * and interpret it as a local time
     *
     * Also - some NF tokens which share a contract with others (eg World Cup, Meetup invites) will have mappings
     * which intentionally have zero value - eg 'Match' has no lookup value for a meeting. Returning null is a guide for the
     * token layout not to show the value.
     *
     * This will become less relevant once the IFrame system is in place - each token appearance will be defined explicitly.
     * However it may be necessary for a default display of token attributes for ease of use while potential
     * users become acquainted with the system.
     *
     * @param data
     * @return
     * @throws UnsupportedEncodingException
     */
    public String toString(BigInteger data) throws UnsupportedEncodingException
    {
        // TODO: in all cases other than UTF8, syntax should be checked
        switch (getAs())
        {
            case UTF8:
                return new String(data.toByteArray(), "UTF8");

            case Unsigned:
                return data.toString();

            case Mapping:
                // members might be null, but it is better to throw up ( NullPointerException )
                // than silently ignore
                // JB: Existing contracts and tokens throw this error. The wallet 'crashes' each time existing tokens are opened
                // due to assumptions made with extra tickets (ie null member is assumed to return null and not display that element).
                if (members != null && members.containsKey(data))
                {
                    return members.get(data);
                }
                else if (syntax == TokenDefinition.Syntax.GeneralizedTime)
                {
                    //This is a time entry but without a localised mapped entry. Return the EPOCH time.
                    Date date = new Date(data.multiply(BigInteger.valueOf(1000)).longValue());
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssZ");
                    return sdf.format(date);
                }
                else
                {
                    return null; // have to revert to this behaviour due to values being zero when tokens are created
                    //refer to 'AlphaWallet meetup indices' where 'Match' mapping is null but for FIFA is not.
                    //throw new NullPointerException("Key " + data.toString() + " can't be mapped.");
                }
            default:
                throw new NullPointerException("Missing valid 'as' attribute");
        }
    }

    public As getAs()
    {
        return as;
    }

    public void setAs(As as)
    {
        this.as = as;
    }
}