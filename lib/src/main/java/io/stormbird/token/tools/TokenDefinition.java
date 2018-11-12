package io.stormbird.token.tools;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import io.stormbird.token.entity.FunctionDefinition;
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenDefinition {
    protected Document xml;
    public Map<String, AttributeType> attributes = new ConcurrentHashMap<>();
    protected Locale locale;
    public Map<String, Integer> addresses = new HashMap<>();
    public Map<String, FunctionDefinition> functions = new ConcurrentHashMap<>();

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
        public String function = null;

        public AttributeType(Element attr) {
            name = getLocalisedString(attr,"name");
            System.err.println(name);
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
            NodeList nList = attr.getElementsByTagNameNS("http://attestation.id/ns/tbml", "origin");
            for (int i = 0; i < nList.getLength(); i++) {
                    Element origin = (Element) nList.item(i);
                    switch(origin.getAttribute("contract").toLowerCase()) {
                        case "holding-contract":
                            as = As.Mapping;
                            // TODO: Syntax is not checked
                            getFunctions(origin);
                            break;
                        default:
                            break;
                    }
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
            if (bitmask != null ) {
                while (bitmask.mod(BigInteger.ONE.shiftLeft(++bitshift)).equals(BigInteger.ZERO)) ; // !!
                bitshift--;
            }
            // System.out.println("New FieldDefinition :" + name);
        }

        private void populate(Element mapping) {
            Element option;
            NodeList nList = mapping.getElementsByTagNameNS("http://attestation.id/ns/tbml", "option");
            for (int i = 0; i < nList.getLength(); i++) {
                option = (Element) nList.item(i);
                members.put(new BigInteger(option.getAttribute("key")), getLocalisedString(option, "value"));
            }
        }

        private void getFunctions(Element mapping) {
            Element option;
            Node functionDef;
            for(Node child=mapping.getFirstChild(); child!=null; child=child.getNextSibling()){
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    option = (Element) child;
                    String type = child.getNodeName(); //function
                    String functionName = child.getTextContent();  //isExpired

                    switch (type)
                    {
                        case "function":
                            function = functionName;
                            break;
                        default:
                            break;
                    }
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

    /* for many occurance of the same tag, return the text content of the one in user's current language */
    // FIXME: this function will break if there are nested <tagName> in the nameContainer
    String getLocalisedString(Element nameContainer, String tagName) {
        NodeList nList = nameContainer.getElementsByTagNameNS("http://attestation.id/ns/tbml", tagName);
        Element name;
        for (int i = 0; i < nList.getLength(); i++) {
            name = (Element) nList.item(i);
            String currentNodeLang = (new Locale(name.getAttribute("lang"))).getLanguage();
            if (currentNodeLang.equals(locale.getLanguage())) {
                return name.getTextContent();
            }
        }
        /* no matching language found. return the first tag's content */
        name = (Element) nList.item(0);
        // TODO: catch the indice out of bound exception and throw it again suggesting dev to check schema
        return name.getTextContent();
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
            dbFactory.setNamespaceAware(true);
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            // TODO: if schema has problems (e.g. defined twice). Now, no schema, no exception.
            e.printStackTrace();
            return;
        }
        Document xml = dBuilder.parse(xmlAsset);
        xml.getDocumentElement().normalize(); // good for parcel, bad for signature verification. JB likes it that way. -weiwu
        NodeList nList = xml.getElementsByTagNameNS("http://attestation.id/ns/tbml", "attribute-type");
        for (int i = 0; i < nList.getLength(); i++) {
            AttributeType attr = new AttributeType((Element) nList.item(i));
            if (attr.bitmask != null) {// has <origin> which is from bitmask
                attributes.put(attr.id, attr);
            } // TODO: take care of attributes whose value does not originate from bitmask!
            else if (attr.function != null) {
                FunctionDefinition fd = new FunctionDefinition();
                fd.method = attr.function;
                fd.syntax = attr.syntax;
                functions.put(attr.id, fd);
            }
        }
        extractFeatureTag(xml);
        extractContractTag(xml);
        extractSignedInfo(xml);
    }

    private void extractSignedInfo(Document xml) {
        NodeList nList;
        nList = xml.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "KeyName");
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

    public String getTokenName() { return tokenName; }

    public int getNetworkFromContract(String contractAddress)
    {
        return (addresses.get(contractAddress) == null ? -1 : addresses.get(contractAddress));
    }

    public Map<BigInteger, String> getMappingMembersByKey(String key){
        if(attributes.containsKey(key)) {
            AttributeType attr = attributes.get(key);
            return attr.members;
        }
        return null;
    }
    public Map<BigInteger, String> getConvertedMappingMembersByKey(String key){
        if(attributes.containsKey(key)) {
            Map<BigInteger,String> convertedMembers=new HashMap<>();
            AttributeType attr = attributes.get(key);
            for(BigInteger actualValue:attr.members.keySet()){
                convertedMembers.put(actualValue.shiftLeft(attr.bitshift).and(attr.bitmask),attr.members.get(actualValue));
            }
            return convertedMembers;
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
        NodeList l;
        NodeList nList = xml.getElementsByTagNameNS("http://attestation.id/ns/tbml", "feature");
        for (int i = 0; i < nList.getLength(); i++) {
            Element feature = (Element) nList.item(i);
            switch (feature.getAttribute("type")) {
                case "feemaster":
                    l = feature.getElementsByTagNameNS("http://attestation.id/ns/tbml", "feemaster");
                    for (int j = 0; j < l.getLength(); j++)
                        feemasterAPI = l.item(j).getTextContent();
                    break;
                case "market-queue":
                    l = feature.getElementsByTagNameNS("http://attestation.id/ns/tbml", "gateway");
                    for (int j = 0; j < l.getLength(); j++)
                        marketQueueAPI = l.item(j).getTextContent();
                    break;
                default:
                    break;
            }
        }
    }

    private void extractContractTag(Document xml) {
        String nameDefault = null;
        String nameEnglish = null;
        NodeList nList = xml.getElementsByTagNameNS("http://attestation.id/ns/tbml", "contract");
        /* we allow multiple contracts, e.g. for issuing asset and for
         * proxy usage. but for now we only deal with the first */
        Element contract = (Element) nList.item(0);

        /* if there is no token name in <contract> this breaks;
         * token name shouldn't be in <contract> anyway, re-design pending */
        tokenName = getLocalisedString(contract,"name");

         /*if hit NullPointerException in the next statement, then XML file
         * must be missing <contract> elements */
         /* TODO: select the contract of type "holding_contract" */
        nList = contract.getElementsByTagNameNS("http://attestation.id/ns/tbml", "address");
        for(int i=  0; i < nList.getLength(); i++){
            Element address = (Element) nList.item(i);
            String networkElement = address.getAttribute("network");
            if (networkElement.length() < 1) networkElement = "1"; //default to mainnet
            Integer networkId = Integer.parseInt(networkElement);
            addresses.put(address.getTextContent().toLowerCase(), networkId);
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
