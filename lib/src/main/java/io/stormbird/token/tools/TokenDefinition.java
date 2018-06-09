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
    public Map<String, FieldDefinition> fields = new ConcurrentHashMap<>();
    protected Locale locale;
    public Map<Integer, String> addresses = new HashMap<>();

    /* the following are incorrect, waiting to be further improved
     with suitable XML, because none of these String values are going
     to be one-per-XML-file:

     - each contract <feature> normally should invoke new code modules
       e.g. when a new decentralised protocol is introduced, a new
       class to handle the protocol needs to be introduced, which owns
       it own way of specifying implementation, like marketeQueueAPI.

     - tokenName is going to be refactored into a field attribute -
       that is, it's allowed that token names are different in the
       same asset class. There are use-cases for this.

     - each token definition XML file can incorporate multiple
       contracts, eachwith different network IDs.

     - each XML file can be signed mulitple times, with multiple
       <KeyName>.
    */
    protected String marketQueueAPI = null;
    protected String feemasterAPI = null;
    protected String tokenName = null;
    protected String keyName = null;
    protected int networkId = 1; //default to main net unless otherwise specified

    protected class FieldDefinition {
        public BigInteger bitmask;   // TODO: BigInteger !== BitInt. Test edge conditions.
        public String name;
        public String id;
        public int bitshift = 0;
        public FieldDefinition(Element field) {
            name = getLocalisedName(field);
            id = field.getAttribute("id");
            bitmask = new BigInteger(field.getAttribute("bitmask"), 16);
            while(bitmask.mod(BigInteger.ONE.shiftLeft(++bitshift)).equals(BigInteger.ZERO)); // !!
            bitshift--;
            // System.out.println("New FieldDefinition :" + name);
        }
        public String applyToFieldValue(BigInteger data) {
            return data.toString();
        }
    }

    class IA5String extends FieldDefinition {
        public IA5String(Element field) {
            super(field);
        }
        public String applyToFieldValue(BigInteger data) {
            try {
                return new String(data.toByteArray(), "ASCII");
            } catch(UnsupportedEncodingException e){
                return null;
            }
        }
    }

    class BinaryTime extends FieldDefinition {
        public BinaryTime(Element field) {
            super(field);
        }
        public String applyToFieldValue(BigInteger data) {
            Date date=new Date(data.longValue());
            return date.toString();
        }
    }

    class Enumeration extends FieldDefinition {
        private Map<BigInteger, String> members = new ConcurrentHashMap<>();
        public Enumeration(Element field) {
            super(field);
            for(Node child=field.getFirstChild(); child!=null; child=child.getNextSibling()){
                if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals("mapping") ) {
                    populate((Element) child);
                }
            }
        }
        void populate(Element mapping) {
            Element entity;
            for(Node child=mapping.getFirstChild(); child!=null; child=child.getNextSibling()){
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    entity = (Element) child;
                    members.put(new BigInteger(entity.getAttribute("key")), getLocalisedName(entity));
                }
            }
        }
        public String applyToFieldValue(BigInteger data) {
            return members.get(data);
        }
    }

    String getLocalisedName(Element nameContainer) {
        Element name = null;
        Locale currentNodeLang;
        for(Node node=nameContainer.getLastChild();
            node!=null; node=node.getPreviousSibling()){
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals("name")) {
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
        NodeList nList = xml.getElementsByTagName("field");
        for (int i = 0; i < nList.getLength(); i++) {
            Node nNode = nList.item(i);
            // System.out.println("\nParsing Element :" + nNode.getNodeName());
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;
                String id = ((Element) nNode).getAttribute("id");
                switch (eElement.getAttribute("type")) {
                    case "Enumeration":
                        fields.put(id, new Enumeration(eElement));
                        break;
                    case "BinaryTime":
                        fields.put(id, new BinaryTime(eElement));
                        break;
                    case "IA5String":
                        fields.put(id, new IA5String(eElement));
                        break;
                    default: /* Integer */
                        fields.put(id, new FieldDefinition(eElement));
                }
            }
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

    public String getFeemasterAPI(){
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

    private void extractFeatureTag(Document xml)
    {
        NodeList nList = xml.getElementsByTagName("feature");
        for (Node nNode = nList.item(0).getFirstChild(); nNode != null; nNode = nNode.getNextSibling())
        {
            if (nNode.getNodeType() == Node.ELEMENT_NODE)
            {
                Element eElement = (Element) nNode;
                switch (eElement.getTagName())
                {
                    case "feemaster":
                        feemasterAPI = eElement.getTextContent();
                        break;
                    case "trade":
                        if (eElement.getAttribute("method").equals("market-queue"))
                        {
                            marketQueueAPI = eElement.getTextContent().trim();
                        }
                        break;
                    case "network":
                        String networkIdStr = eElement.getTextContent();
                        if (networkIdStr != null)
                        {
                            try
                            {
                                networkId = Integer.parseInt(networkIdStr);
                            }
                            catch (NumberFormatException e)
                            {
                                networkId = 1; //default to main net
                            }
                        }
                        break;

                }
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
        tokenName = getLocalisedName(contract);

         /*if hit NullPointerException in the next statement, then XML file
         * must be missing <contract> elements */
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

    private String getNode(NodeList nList, String field)
    {
        String value = "";
        for (int i = 0; i < nList.getLength(); i++)
        {
            Node nNode = nList.item(i);
            if (nNode.getNodeName().equals("#text")) return nNode.getNodeValue();
            NodeList childNodes = nNode.getChildNodes();
            if (childNodes.getLength() > 0)
            {
                return getNode(childNodes, field);
            }
        }

        return value;
    }

    /* take a token ID in byte-32, find all the fields in it and call back
     * token.setField(fieldID, fieldName, text-value). This is abandoned
     * temporarily for the need to retrofit the class with J.B.'s design */

    public void parseField(BigInteger tokenId, NonFungibleToken token) {
        for (String key : fields.keySet()) {
            FieldDefinition f = fields.get(key);
            BigInteger val = tokenId.and(f.bitmask).shiftRight(f.bitshift);
            token.setAttribute(f.id,
                    new NonFungibleToken.Attribute(f.id, f.name, val, f.applyToFieldValue(val)));
        }
    }
}
