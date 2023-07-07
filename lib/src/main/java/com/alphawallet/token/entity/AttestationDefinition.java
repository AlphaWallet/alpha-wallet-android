package com.alphawallet.token.entity;

import static org.w3c.dom.Node.ELEMENT_NODE;

import com.alphawallet.token.entity.ContractInfo;
import com.alphawallet.token.entity.FunctionDefinition;
import com.alphawallet.token.tools.Numeric;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by JB on 19/01/2023.
 */

public class AttestationDefinition
{
    //public TSOrigins origin; //single value for validation
    public FunctionDefinition function = null;
    //public final List<AttnElement> members;
    public Map<String, String> metadata;
    public Map<String, String> attributes;
    public final String name;
    public long chainId;
    public byte[] issuerKey; //also used to generate collectionId
    public String terminationId; //used to generate collectionId
    public String replacementFieldId; //used to check if new attestation should replace the old

    public AttestationDefinition(String name)
    {
        this.name = name;
        metadata = null;
        attributes = null;
    }

    public void handleEventId(Element element)
    {
        terminationId = element.getTextContent();
    }

    public void handleReplacementField(Element element)
    {
        replacementFieldId = element.getTextContent();
    }

    public void handleKey(Element element)
    {
        //should be the key itself
        String key = Numeric.cleanHexPrefix(element.getTextContent());
        if (key.length() == 130 && key.startsWith("04"))
        {
            key = key.substring(2);
        }

        issuerKey = Numeric.hexStringToByteArray(key);
    }

    public ContractInfo addAttributes(Element element)
    {
        //get schemaUID attribute
        String schemaUID = element.getAttribute("schemaUID");
        String networkStr = element.getAttribute("network");
        //this is the backlink to the attestation
        ContractInfo info = new ContractInfo("Attestation");
        if (networkStr.length() > 0)
        {
            this.chainId = Long.parseLong(networkStr);
        }
        else
        {
            this.chainId = 1;
        }
        info.addresses.put(this.chainId, Collections.singletonList(schemaUID));
        //contracts.put(this.name, info);
        for (Node n = element.getFirstChild(); n != null; n = n.getNextSibling())
        {
            if (n.getNodeType() != ELEMENT_NODE) continue;
            Element attnElement = (Element) n;

            String name = attnElement.getAttribute("name");
            String text = attnElement.getTextContent();

            attributes.put(name, text);
        }

        return info;
    }

    public void addMetaData(Element element)
    {
        metadata = new HashMap<>();
        attributes = new HashMap<>();
        for (Node n = element.getFirstChild(); n != null; n = n.getNextSibling())
        {
            if (n.getNodeType() != ELEMENT_NODE) continue;
            Element attnElement = (Element) n;

            String metaText = attnElement.getTextContent();
            String metaName = attnElement.getLocalName();
            if (metaName.equals("attributeField"))
            {
                String attrName = attnElement.getAttribute("name");
                attributes.put(attrName, metaText);
            }
            else
            {
                metadata.put(metaName, metaText);
            }
        }
    }
}
