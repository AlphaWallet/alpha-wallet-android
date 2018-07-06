package io.stormbird.token.tools;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import io.stormbird.token.entity.NonFungibleToken;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenDefinition {
    protected Document xml;
    public Map<String, AttributeType> attributes = new ConcurrentHashMap<>();
    protected Locale locale;
    public Map<Integer, String> addresses = new HashMap<>();

    /* the following are incorrect, waiting to be further improved
     with suitable XML, because none of these String typed class variables
     are going to be one-per-XML-file:

     - each contract <feature> normally should invoke new code modules
       e.g. when a new decentralised protocol is introduced, a new
       class to handle the protocol needs to be introduced, which owns
       it own way of specifying implementation, like marketeQueueAPI.

     - tokenName is going to be selectable through filters -
       that is, it's allowed that token names are different in the
       same asset class. There are use-cases for this.

     - each token definition XML file can incorporate multiple
       contracts, each with different network IDs.

     - each XML file can be signed mulitple times, with multiple
       <KeyName>.
    */
    protected String marketQueueAPI = null;
    protected String feemasterAPI = null;
    protected String tokenName = null;
    protected String keyName = null;
    protected int networkId = 1; //default to main net unless otherwise specified

    public enum Syntax {
        DirectoryString, IA5String, Integer, GeneralizedTime,
        Boolean, BitString, CountryString, JPEG, NumericString
    }

    public enum As {  // always assume big endian
        UTF8, Unsigned, Signed, Mapping
    }

    protected class AttributeType {
        public BigInteger bitmask;    // TODO: BigInteger !== BitInt. Test edge conditions.
        public String name;  // TODO: should be polyglot because user change change language in the run
        public String id;
        public int bitshift = 0;
        public Syntax syntax;
        public As as;
        public Map<BigInteger, String> members;

        public AttributeType(Element attr) {
            name = getLocalisedName(attr,"name");
            id = attr.getAttribute("id");

            try {
                switch (attr.getAttribute("syntax")) { // We don't validate syntax here; schema does it.
                    case "1.3.6.1.4.1.1466.115.121.1.6":
                        syntax = Syntax.BitString;
                        break;
                    case "1.3.6.1.4.1.1466.115.121.1.7":
                        syntax = Syntax.Boolean;
                        break;
                    case "1.3.6.1.4.1.1466.115.121.1.11":
                        syntax = Syntax.CountryString;
                        break;
                    case "1.3.6.1.4.1.1466.115.121.1.28":
                        syntax = Syntax.JPEG;
                        break;
                    case "1.3.6.1.4.1.1466.115.121.1.36":
                        syntax = Syntax.NumericString;
                        break;
                    case "1.3.6.1.4.1.1466.115.121.1.24":
                        syntax = Syntax.GeneralizedTime;
                        break;
                    case "1.3.6.1.4.1.1466.115.121.1.26":
                        syntax = Syntax.IA5String;
                        break;
                    case "1.3.6.1.4.1.1466.115.121.1.27":
                        syntax = Syntax.Integer;
                        break;
                    default: // unknown syntax treat as Directory String
                        syntax = Syntax.DirectoryString;
                }
            } catch (NullPointerException e) { // missing <syntax>
                syntax = Syntax.DirectoryString; // 1.3.6.1.4.1.1466.115.121.1.15
            }
            bitmask = null;
            for(Node node=attr.getFirstChild();
                node!=null; node=node.getNextSibling()){
                if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals("origin")) {
                    // System.out.println("\nFound a name field: " + node.getNodeName());
                    Element origin = (Element) node;
                    switch(origin.getAttribute("as").toLowerCase()) {
                        case "signed":
                            as = As.Signed;
                            break;
                        case "utf8":
                            as = As.UTF8;
                            break;
                        case "mapping":
                            as = As.Mapping;
                            // TODO: Syntax is not checked
                            members = new ConcurrentHashMap<>();
                            populate(origin);
                            break;
                        default: // "unsigned"
                            as = As.Unsigned;
                    }
                    if (origin.hasAttribute("bitmask")) {
                        bitmask = new BigInteger(origin.getAttribute("bitmask"), 16);
                    }
                }
            }
            if (bitmask != null ) {
                while (bitmask.mod(BigInteger.ONE.shiftLeft(++bitshift)).equals(BigInteger.ZERO)) ; // !!
                bitshift--;
            }
            // System.out.println("New FieldDefinition :" + name);
        }

        private void populate(Element mapping) {
            Element option;
            for(Node child=mapping.getFirstChild(); child!=null; child=child.getNextSibling()){
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    option = (Element) child;
                    members.put(new BigInteger(option.getAttribute("key")), getLocalisedName(option,"value"));
                }
            }
        }

        public String toString(BigInteger data) {
            try {
                if (as == As.UTF8) {
                    return new String(data.toByteArray(), "UTF8");
                } else if(as == As.Unsigned){
                    return data.toString();
                } else if(as == As.Mapping){
                    // members might be null, but it is better to throw up ( NullPointerException )
                    // than silently ignore
                    System.out.println(data);
                    System.out.println(members.toString());
                    return members.get(data);
                }
                throw new NullPointerException("Missing valid 'as' attribute");
            } catch(UnsupportedEncodingException e){
                return null;
            }
        }
    }

    String getLocalisedName(Element nameContainer,String targetName) {
        Element name = null;
        Locale currentNodeLang;
        for(Node node=nameContainer.getLastChild();
            node!=null; node=node.getPreviousSibling()){
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(targetName)) {
                // System.out.println("\nFound a name field: " + node.getNodeName());
                name = (Element) node;
                currentNodeLang = new Locale(name.getAttribute("lang"));
                if (currentNodeLang.getLanguage().equals(locale.getLanguage())) {
                    return name.getTextContent();
                }
            }
        }
        return name != null ? name.getTextContent() : " "; /* Should be the first occurrence of <name> */
    }

    public TokenDefinition(InputStream xmlAsset, Locale locale) throws IOException, SAXException{
        this.locale = locale;
        /* guard input from bad programs which creates Locale not following ISO 639 */
        if (locale.getLanguage().length() < 2 || locale.getLanguage().length() > 3) {
            throw new SAXException("Locale object wasn't created following ISO 639");
        }
        DocumentBuilder dBuilder;
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            // TODO: if schema has problems (e.g. defined twice). Now, no schema, no exception.
            e.printStackTrace();
            return;
        }
        Document xml = dBuilder.parse(xmlAsset);
        xml.getDocumentElement().normalize(); // also good for parcel
        NodeList nList = xml.getElementsByTagName("attribute-type");
        for (int i = 0; i < nList.getLength(); i++) {
            Node nNode = nList.item(i);
            AttributeType attr = new AttributeType((Element) nList.item(i));
            if (attr.bitmask != null) {// has <origin> which is from bitmask
                attributes.put(attr.id, attr);
            } // TODO: take care of attributes whose value does not originate from bitmask!
        }
        extractFeatureTag(xml);
        extractContractTag(xml);
        extractSignedInfo(xml);
    }

    private void extractSignedInfo(Document xml) {
        NodeList nList;
        nList = xml.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "KeyName");
        nList = xml.getElementsByTagName("ds:KeyName"); // previous statement returns empty list, strange...
        if (nList.getLength() > 0) {
            this.keyName = ((Element) nList.item(0)).getTextContent();
        }
        return; // even if the document is signed, often it doesn't have KeyName
    }

    public String getKeyName() {
        return this.keyName;
    }

    public String getFeemasterAPI()
    {
        return feemasterAPI;
    }

    public int getNetworkId() { return networkId; }

    public String getTokenName() { return tokenName; }

    public String getContractAddress(int networkID) {
        return addresses.get(networkID);
    }

    public int getNetworkFromContract(String contractAddress)
    {
        for (Map.Entry e : addresses.entrySet())
        {
            if (((String)e.getValue()).equalsIgnoreCase(contractAddress))
            {
                return (Integer)e.getKey();
            }
        }

        return -1;
    }

    public Map<BigInteger, String> getMappingMembersByKey(String key){
        if(attributes.containsKey(key)) {
            AttributeType attr = attributes.get(key);
            return attr.members;
        }
        return null;
    }

    private String getContentByTagName(Node node, String tagname) {
        /* I hope stream() -like pattern is supported in DOM but they don't want to evolve */
        for (Node nNode = node.getFirstChild(); nNode != null; nNode = nNode.getNextSibling()) {
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;
                if (eElement.getTagName().equals(tagname)) {
                    return eElement.getTextContent();
                }
            }
        }
        return null;
    }

    private void extractFeatureTag(Document xml)
    {
        NodeList nList = xml.getElementsByTagName("feature");
        for (int i = 0; i < nList.getLength(); i++) {
            Element feature = (Element) nList.item(i);
            switch (feature.getAttribute("type")) {
                case "feemaster":
                    feemasterAPI = getContentByTagName(feature, "feemaster");
                    break;
                case "market-queue":
                    marketQueueAPI = getContentByTagName(feature, "gateway");
                    break;
                default:
                    break;
            }
        }
    }

    private void extractContractTag(Document xml) {
        String nameDefault = null;
        String nameEnglish = null;
        NodeList nList = xml.getElementsByTagName("contract");
        /* we allow multiple contracts, e.g. for issuing asset and for
         * proxy usage. but for now we only deal with the first */
        Element contract = (Element) nList.item(0);

        /* if there is no token name in <contract> this breaks;
         * token name shouldn't be in <contract> anyway, re-design pending */
        tokenName = getLocalisedName(contract,"name");

         /*if hit NullPointerException in the next statement, then XML file
         * must be missing <contract> elements */
         /* TODO: select the contract of type "holding_contract" */
        for(Node nNode = nList.item(0).getFirstChild(); nNode!=null; nNode = nNode.getNextSibling()){
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = ((Element) nNode);
                if (eElement.getTagName().equals("address")) {
                    Integer networkId = Integer.parseInt(eElement.getAttribute("network"));
                    addresses.put(networkId, nNode.getTextContent());
                }
                /* if there is no token name in <contract> this breaks;
                 * token name shouldn't be in <contract> anyway, re-design pending */
                if (eElement.getTagName().equals("name")) {
                    if (eElement.getAttribute("lang").equals(locale.getLanguage())) {
                        tokenName = eElement.getTextContent();
                    }
                }
            }
        }
    }

    /* take a token ID in byte-32, find all the fields in it and call back
     * token.setField(fieldID, fieldName, text-value). This is abandoned
     * temporarily for the need to retrofit the class with J.B.'s design */

    public void parseField(BigInteger tokenId, NonFungibleToken token) {
        for (String key : attributes.keySet()) {
            AttributeType attr = attributes.get(key);
            BigInteger val = tokenId.and(attr.bitmask).shiftRight(attr.bitshift);
            token.setAttribute(attr.id,
                    new NonFungibleToken.Attribute(attr.id, attr.name, val, attr.toString(val)));
        }
    }
}
