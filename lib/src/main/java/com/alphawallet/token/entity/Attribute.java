package com.alphawallet.token.entity;

import com.alphawallet.token.tools.Numeric;
import com.alphawallet.token.tools.TokenDefinition;
import com.alphawallet.token.util.DateTime;
import com.alphawallet.token.util.DateTimeFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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

public class Attribute {
    private static final int ADDRESS_SIZE = 160;
    private static final int ADDRESS_LENGTH_IN_HEX = ADDRESS_SIZE >> 2;
    private static final int ADDRESS_LENGTH_IN_BYTES = ADDRESS_SIZE >> 3;

    //default the bitmask to 32 bytes represented
    public BigInteger bitmask = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16);    // TODO: BigInteger !== BitInt. Test edge conditions.
    public String label;  // TODO: should be polyglot because user change change language in the run
    public String name;
    public int bitshift = 0;
    public TokenDefinition.Syntax syntax;
    public As as;
    public Map<BigInteger, String> members;
    public ContractInfo originContract;
    public FunctionDefinition function = null;
    public EventDefinition event = null;
    public boolean userInput = false;

    public Attribute(Element attr, TokenDefinition def) throws SAXException
    {
        originContract = def.contracts.get(def.holdingToken);
        //schema 2020/06 id is now name; name is now label
        name = attr.getAttribute("name");
        label = name; //set label to name if not specified
        as = As.Unsigned; //default value
        //TODO xpath would be better
        syntax = TokenDefinition.Syntax.DirectoryString; //default value

        for(Node node = attr.getFirstChild();
            node!=null; node=node.getNextSibling()) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                switch (node.getLocalName())
                {
                    case "type":
                        syntax = handleType(element);
                        break;
                    case "origins":
                        handleOrigins(element, def);
                        break;
                    case "label":
                        label = def.getLocalisedString(element);
                    case "mapping":
                        populate(element, def);
                        break;
                }
                switch(element.getAttribute("contract").toLowerCase()) {
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

    private TokenDefinition.Syntax handleType(Element syntax)
    {
        TokenDefinition.Syntax as = TokenDefinition.Syntax.DirectoryString;

        for(Node node = syntax.getFirstChild();
                node!=null; node=node.getNextSibling())
        {
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                Element element = (Element) node;
                switch (element.getLocalName())
                {
                    case "syntax":
                        as = getSyntax(element.getTextContent());
                        break;
                    default:
                        System.out.println("Possible fail: " + element.getLocalName() + " in attribute '" + name + "'");
                        break;
                }
            }
        }

        return as;
    }

    private TokenDefinition.Syntax getSyntax(String ISO) {
        switch (ISO) {
            case "1.3.6.1.4.1.1466.115.121.1.6":
                return TokenDefinition.Syntax.BitString;
            case "1.3.6.1.4.1.1466.115.121.1.7":
                return TokenDefinition.Syntax.Boolean;
            case "1.3.6.1.4.1.1466.115.121.1.11":
                return TokenDefinition.Syntax.CountryString;
            case "1.3.6.1.4.1.1466.115.121.1.28":
                return TokenDefinition.Syntax.JPEG;
            case "1.3.6.1.4.1.1466.115.121.1.36":
                return TokenDefinition.Syntax.NumericString;
            case "1.3.6.1.4.1.1466.115.121.1.24":
                return TokenDefinition.Syntax.GeneralizedTime;
            case "1.3.6.1.4.1.1466.115.121.1.26":
                return TokenDefinition.Syntax.IA5String;
            case "1.3.6.1.4.1.1466.115.121.1.27":
                return TokenDefinition.Syntax.Integer;
            case "1.3.6.1.4.1.1466.115.121.1.15":
                return TokenDefinition.Syntax.DirectoryString;
        }
        return null;
    }

    private void handleOrigins(Element origin, TokenDefinition def) throws SAXException
    {
        for(Node node = origin.getFirstChild();
            node!=null; node=node.getNextSibling())
        {
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                Element resolve = (Element) node;
                setAs(def.parseAs(resolve));
                if (resolve.getPrefix().equals("ethereum")) //handle ethereum namespace
                {
                    switch (node.getLocalName())
                    {
                        case "transaction":
                        case "call":
                            function = def.parseFunction(resolve, syntax);
                            break;
                        case "event":
                            event = def.parseEvent(resolve);
                            event.attributeName = name;
                            event.parentAttribute = this;
                            break;
                        default:
                            //throw parse error
                            break;
                    }
                }
                else
                {
                    switch (node.getLocalName())
                    {
                        case "token-id":
                            //this value is obtained from the token name
                            setAs(def.parseAs(resolve));
                            populate(resolve, def); //check for mappings
                            if (function != null)
                                function.as = def.parseAs(resolve);
                            if (resolve.hasAttribute("bitmask"))
                            {
                                bitmask = new BigInteger(resolve.getAttribute("bitmask"), 16);
                            }
                            break;
                        case "user-entry":
                            userInput = true;
                            setAs(def.parseAs(resolve));
                            if (resolve.hasAttribute("bitmask"))
                            {
                                bitmask = new BigInteger(resolve.getAttribute("bitmask"), 16);
                            }
                            break;
                    }
                }
            }
        }
    }

    private void populate(Element origin, TokenDefinition def) {
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

                    NodeList nList = origin.getElementsByTagNameNS(def.nameSpace, "option");
                    for (int i = 0; i < nList.getLength(); i++) {
                        option = (Element) nList.item(i);
                        members.put(new BigInteger(option.getAttribute("key")), def.getLocalisedString(option, "value"));
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
                if (data.length() == 0)
                {
                    return "0";
                }
                else if (Character.isDigit(data.charAt(0)))
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
                else if (data.startsWith("0x") && this.as != As.Address) //Address is a special case where we want the leading 0x
                {
                    data = data.substring(2);
                }
                return data;
            default:
                return data;
        }
    }

    //Sometimes value needs to be processed from the raw input.
    //Currently only time
    public BigInteger processValue(BigInteger val)
    {
        switch (syntax)
        {
            case GeneralizedTime:
                return parseGeneralizedTime(val);
            case DirectoryString:
            case IA5String:
            case Integer:
            case Boolean:
            case BitString:
            case CountryString:
            case JPEG:
            case NumericString:
                break;
        }
        return val;
    }

    private BigInteger parseGeneralizedTime(BigInteger value) {
        try
        {
            DateTime dt = DateTimeFactory.getDateTime(toString(value));
            return BigInteger.valueOf(dt.toEpoch());
        }
        catch (ParseException|UnsupportedEncodingException p)
        {
            p.printStackTrace();
            return value;
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

            case Boolean:
                if (data.equals(BigInteger.ZERO)) return "FALSE";
                else return "TRUE";

            case UnsignedInput: //convert to unsigned
                BigInteger conv = new BigInteger(1, data.toByteArray());
                return conv.toString();

            case TokenId:
                return data.toString();

            case Bytes:
                return Numeric.toHexString(data.toByteArray());

            case Address:
                return parseEthereumAddress(data);

            //e18, e8, e4, e2
            //return resized data value?

            default:
                throw new NullPointerException("Missing valid 'as' attribute");
        }
    }

    private String parseEthereumAddress(BigInteger data)
    {
        byte[] padded = Numeric.toBytesPadded(data, ADDRESS_LENGTH_IN_BYTES);
        String addr = Numeric.toHexString(padded);

        if (Numeric.cleanHexPrefix(addr).length() == ADDRESS_LENGTH_IN_HEX)
        {
            return addr;
        }
        else
        {
            return "<Invalid Address: addr>";
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

    /**
     * Detects a function call with more than one tokenId present - this means we shouldn't cache the result.
     *
     * @return does the function rely on more than one tokenId input?
     */
    public boolean isMultiTokenCall()
    {
        int tokenIdCount = 0;
        if (function != null && function.parameters != null && function.parameters.size() > 1)
        {
            for (MethodArg arg : function.parameters)
            {
                if (arg.isTokenId()) tokenIdCount++;
            }
        }

        return tokenIdCount > 1;
    }

    /**
     * Any property of the function that makes it volatile should go in here. Recommend we add a 'volatile' keyword.
     *
     * @return
     */
    public boolean isVolatile()
    {
        return isMultiTokenCall();
    }
}