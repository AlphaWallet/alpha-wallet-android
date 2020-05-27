package com.alphawallet.token.entity;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by JB on 20/03/2020.
 */
public class Module
{
    public final ContractInfo contractInfo;
    public List<SequenceElement> sequence = new ArrayList<>();

    public Module(ContractInfo info)
    {
        contractInfo = info;
    }

    public void addSequenceElement(Element element, String sequenceName) throws SAXException
    {
        SequenceElement se = new SequenceElement();
        String indexed = element.getAttribute("ethereum:indexed");
        se.indexed = indexed != null && indexed.equalsIgnoreCase("true");
        se.type = element.getAttribute("ethereum:type");
        se.name = element.getAttribute("name");
        sequence.add(se);

        if (se.type == null)
        {
            throw new SAXException("Malformed sequence element in: " + sequenceName + " name: " + se.name);
        }
        else if (se.name == null)
        {
            throw new SAXException("Malformed sequence element in: " + sequenceName + " type: " + se.type);
        }
    }


    public List<SequenceElement> getSequenceArgs()
    {
        return sequence;
    }

    public List<String> getArgNames(boolean indexed)
    {
        List<String> argNameIndexedList = new ArrayList<>();
        for (SequenceElement se : sequence)
        {
            if (se.indexed == indexed)
            {
                argNameIndexedList.add(se.name);
            }
        }

        return argNameIndexedList;
    }

    int getTopicIndex(String filterTopic)
    {
        int topicIndex = -1;
        int currentIndex = 0;
        for (SequenceElement se : sequence)
        {
            if (se.indexed)
            {
                if (se.name.equals(filterTopic))
                {
                    topicIndex = currentIndex;
                    break;
                }
                else
                {
                    currentIndex++;
                }
            }
        }

        return topicIndex;
    }

    public class SequenceElement
    {
        public String name;
        public String type;
        public boolean indexed;
    }
}
