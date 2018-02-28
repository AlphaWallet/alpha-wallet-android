package com.wallet.crypto.alphawallet.repository;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AssetDefinition {
    protected Document xml;
    protected Set<field> fields = new HashSet<>();
    public String locale = "en";

    class field {
        public BigInteger bitmask;
        public String name;
        public String id;
        public field(String id, String name) {
            this.name = name;
            this.id = id;
        }
    }

    class Enumeration extends field {
        private Map<Integer, String> members = new HashMap<>();
        public Enumeration(Element field) {
            super(field.getAttribute("id"), getLocalisedName(field));
            for(Node child=field.getFirstChild(); child!=null; child=child.getNextSibling()){
                if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals("mapping") ) {
                    populate((Element) child);
                }
            }
        }
        public void populate(Element mapping) {
            Element entity;
            Integer key;
            for(Node child=mapping.getFirstChild(); child!=null; child=child.getNextSibling()){
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    entity = (Element) child;
                    key = Integer.parseInt(entity.getAttribute("key"));
                    members.put(key, getLocalisedName(entity));
                    System.out.println("\nCurrent Element :" + key);
                }
            }
            System.out.println("\nPopulated Element :" + mapping.getTextContent());
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
                        default:
                            break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}