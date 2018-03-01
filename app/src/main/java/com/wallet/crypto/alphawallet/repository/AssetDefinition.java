package com.wallet.crypto.alphawallet.repository;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class AssetDefinition {
    protected Document xml;
    public Set<field> fields = new HashSet<>();
    public String locale = "en";

    class field {
        public BigInteger bitmask;   // TODO: BigInteger !== BitInt. Test edge conditions.
        public String name;
        public String id;
        public int bitshift = 0;
        public field(Element field) {
            name = getLocalisedName(field);
            id = field.getAttribute("id");
            bitmask = new BigInteger(field.getAttribute("bitmask"), 16);

            while(bitmask.mod(BigInteger.ONE.shiftLeft(++bitshift)).equals(BigInteger.ZERO)); // !!
            bitshift--;
        }
        public String getTextValue(BigInteger data) {
            return data.toString(16);
        }
    }

    class IA5String extends field {
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

    class Integer extends field {
        public Integer(Element field) {
            super(field);
        }
        public String getTextValue(BigInteger data) {
            return data.toString();
        }
    }

    class Enumeration extends field {
        private Map<BigInteger, String> members = new HashMap<>();
        public Enumeration(Element field) {
            super(field);
            for(Node child=field.getFirstChild(); child!=null; child=child.getNextSibling()){
                if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals("mapping") ) {
                    populate((Element) child);
                }
            }
        }
        public void populate(Element mapping) {
            Element entity;
            for(Node child=mapping.getFirstChild(); child!=null; child=child.getNextSibling()){
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    entity = (Element) child;
                    members.put(new BigInteger(entity.getAttribute("key")), getLocalisedName(entity));
                }
            }
            // System.out.println("\nPopulated Element :" + mapping.getTextContent());
        }
        public String getTextValue(BigInteger data) {
            return members.get(data);
        }
    }

    protected String getLocalisedName(Element nameContainer) {
        for(Node name=nameContainer.getFirstChild();
            name!=null; name=name.getNextSibling()){
            if (name.getNodeType() == Node.ELEMENT_NODE && name.getNodeName().equals("name")
                    && ((Element) name).getAttribute("lang") == locale) {
                return name.getTextContent();
            }
        }
        return null;
    }

    public AssetDefinition(File fXmlFile) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document xml = dBuilder.parse(fXmlFile);
            xml.getDocumentElement().normalize(); // also good for parcel
            NodeList nList = xml.getElementsByTagName("field");
            for (int i = 0; i < nList.getLength(); i++) {
                Node nNode = nList.item(i);
                System.out.println("\nCurrent Element :" + nNode.getNodeName());
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    switch (eElement.getAttribute("type")) {
                        case "Enumeration":
                            fields.add(new Enumeration(eElement));
                            break;
                        case "Integer":
                            fields.add(new Integer(eElement));
                            break;
                        case "IA5String":
                            fields.add(new IA5String(eElement));
                            break;
                        default:
                            fields.add(new field(eElement));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void parseField(BigInteger id, NonFungibleToken token) {
        for(field f: fields) {
            BigInteger value = id.and(f.bitmask).shiftRight(f.bitshift);
            token.setField(f.id, f.name, f.getTextValue(value));
            //System.out.println("name :" + f.name + "\n");
            //System.out.println("value :" + value + "\n");
        }
    }

}