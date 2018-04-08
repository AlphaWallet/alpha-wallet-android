package io.awallet.crypto.alphawallet.repository;
import io.awallet.crypto.alphawallet.repository.entity.NonFungibleToken;

import android.content.res.Resources;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
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
import java.util.Date;
import java.util.Map;

public class AssetDefinition {
    protected Document xml;
    public Map<String, FieldDefinition> fields = new HashMap<>();
    public String locale;

    class FieldDefinition {
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
        private Map<BigInteger, String> members = new HashMap<>();
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
        for(Node node=nameContainer.getLastChild();
            node!=null; node=node.getPreviousSibling()){
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals("name")) {
                // System.out.println("\nFound a name field: " + node.getNodeName());
                name = (Element) node;
                if (name.getAttribute("lang").equals(locale)) {
                    return name.getTextContent();
                }
            }
        }
        return name.getTextContent(); /* Should be the first occurrence of <name> */
    }

    public AssetDefinition(InputStream xmlAsset, String locale) throws IOException, SAXException{
        this.locale = locale;
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
    }

    public AssetDefinition(String filename, Resources res) throws IOException, SAXException{
        this(res.getAssets().open(filename), res.getConfiguration().locale.getLanguage());
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

    /* retrofitting the ticketDecode written by J.B. */
    public String ticketDecode(BigInteger tokenId, String fieldId) {
        FieldDefinition field = fields.get(fieldId);
        BigInteger value = tokenId.and(field.bitmask).shiftRight(field.bitshift);
        return field.applyToFieldValue(value);
    }
}
