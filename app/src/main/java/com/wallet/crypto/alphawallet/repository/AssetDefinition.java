package com.wallet.crypto.alphawallet.repository;
import com.wallet.crypto.alphawallet.repository.entity.NonFungibleToken;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public class AssetDefinition {
    protected Document xml;
    public Set<Field> fields = new HashSet<>();
    public String locale;

    class Field {
        public BigInteger bitmask;   // TODO: BigInteger !== BitInt. Test edge conditions.
        public String name;
        public String id;
        public int bitshift = 0;
        public Field(Element field) {
            name = getLocalisedName(field);
            id = field.getAttribute("id");
            bitmask = new BigInteger(field.getAttribute("bitmask"), 16);
            while(bitmask.mod(BigInteger.ONE.shiftLeft(++bitshift)).equals(BigInteger.ZERO)); // !!
            bitshift--;
            // System.out.println("New Field :" + name);
        }
        public String getTextValue(BigInteger data) {
            return data.toString();
        }
    }

    class IA5String extends Field {
        public IA5String(Element field) {
            super(field);
        }
        public String getTextValue(BigInteger data) {
            try {
                return new String(data.toByteArray(), "ASCII");
            } catch(UnsupportedEncodingException e){
                return null;
            }
        }
    }

    class BinaryTime extends Field {
        public BinaryTime(Element field) {
            super(field);
        }
        public String getTextValue(BigInteger data) {
            Date date=new Date(data.longValue());
            return date.toString();

        }
    }

    class Enumeration extends Field {
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
        public String getTextValue(BigInteger data) {
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

    public AssetDefinition(File fXmlFile, String locale) {
        this.locale = locale;
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document xml = dBuilder.parse(fXmlFile);
            xml.getDocumentElement().normalize(); // also good for parcel
            NodeList nList = xml.getElementsByTagName("field");
            for (int i = 0; i < nList.getLength(); i++) {
                Node nNode = nList.item(i);
                // System.out.println("\nParsing Element :" + nNode.getNodeName());
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    switch (eElement.getAttribute("type")) {
                        case "Enumeration":
                            fields.add(new Enumeration(eElement));
                            break;
                        case "BinaryTime":
                            fields.add(new BinaryTime(eElement));
                            break;
                        case "IA5String":
                            fields.add(new IA5String(eElement));
                            break;
                        default: /* Integer */
                            fields.add(new Field(eElement));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void parseField(BigInteger id, NonFungibleToken token) {
        for(Field f: fields) {
            BigInteger value = id.and(f.bitmask).shiftRight(f.bitshift);
            token.setField(f.id, f.name, f.getTextValue(value));
            //System.out.println("name :" + f.name + "\n");
            //System.out.println("value :" + value + "\n");
        }
    }

}