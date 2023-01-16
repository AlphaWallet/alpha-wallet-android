package com.alphawallet.attestation;

import static org.w3c.dom.Node.ELEMENT_NODE;

import com.alphawallet.token.entity.ParseResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by JB on 23/01/2023.
 */
public class AttestationParser
{
    protected Locale locale;

    public AttestationParser(InputStream xmlAsset, Locale locale, ParseResult result) throws IllegalArgumentException, IOException, SAXException
    {
        this.locale = locale;
        /* guard input from bad programs which creates Locale not following ISO 639 */
        if (locale.getLanguage().length() < 2 || locale.getLanguage().length() > 3)
        {
            throw new SAXException("Locale object wasn't created following ISO 639");
        }

        DocumentBuilder dBuilder;

        try
        {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(true);
            dbFactory.setExpandEntityReferences(true);
            dbFactory.setCoalescing(true);
            dBuilder = dbFactory.newDocumentBuilder();
        }
        catch (ParserConfigurationException e)
        {
            // TODO: if schema has problems (e.g. defined twice). Now, no schema, no exception.
            e.printStackTrace();
            return;
        }
        Document xml = dBuilder.parse(xmlAsset);
        xml.getDocumentElement().normalize();

        NodeList nList = xml.getElementsByTagNameNS("ts", "attestation");

        try
        {
            for (Node n = xml.getFirstChild(); n != null; n = n.getNextSibling())
            {
                if (n.getNodeType() != ELEMENT_NODE) continue;
                switch (n.getLocalName())
                {
                    case "card": //action only script
                        //TSAction action = handleAction((Element)n);
                        //actions.put(action.name, action);
                        break;
                    default:
                        //extractTags((Element)n);
                        break;
                }
            }
        }
        /*catch (IOException | SAXException e)
        {
            throw e;
        }*/
        catch (Exception e)
        {
            e.printStackTrace(); //catch other type of exception not thrown by this function.
            result.parseMessage(ParseResult.ParseResultId.PARSE_FAILED);
        }

        //NodeList nList = xml.getElementsByTagNameNS(nameSpace, "token");
        //actionCount = 0;

        /*if (nList.getLength() == 0 || nameSpace == null)
        {
            System.out.println("Legacy XML format - no longer supported");
            return;
        }

        try
        {
            parseTags(xml);
            extractSignedInfo(xml);
        }
        catch (IOException | SAXException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            e.printStackTrace(); //catch other type of exception not thrown by this function.
            result.parseMessage(ParseResult.ParseResultId.PARSE_FAILED);
        }*/
    }
}
