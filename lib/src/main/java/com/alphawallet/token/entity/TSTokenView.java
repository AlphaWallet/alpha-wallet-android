package com.alphawallet.token.entity;

import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;

import static org.w3c.dom.Node.ELEMENT_NODE;
import static org.w3c.dom.Node.TEXT_NODE;

/**
 * Holds an individual Token View which consists of style and HTML view code
 *
 * Created by JB on 8/05/2020.
 */
public class TSTokenView
{
    public String tokenView = "";
    public String style = "";

    public TSTokenView(Element element)
    {
        for (int i = 0; i < element.getChildNodes().getLength(); i++)
        {
            Node child = element.getChildNodes().item(i);

            switch (child.getNodeType())
            {
                case ELEMENT_NODE:
                    switch (child.getLocalName())
                    {
                        case "style":
                            //record the style for this
                            style += getHTMLContent(child);
                            break;
                        default:
                            tokenView += getElementHTML(child);
                            break;
                    }
                    break;
                case TEXT_NODE:
                    if (element.getChildNodes().getLength() == 1)
                    {
                        //handle text item-view
                        tokenView = child.getTextContent().replace("\u2019", "&#x2019;");
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private String getElementHTML(Node content)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<");
        sb.append(content.getLocalName());
        sb.append(htmlAttributes(content));
        sb.append(">");
        sb.append(getHTMLContent(content));
        sb.append("</");
        sb.append(content.getLocalName());
        sb.append(">");

        return sb.toString();
    }

    private String getHTMLContent(Node content)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < content.getChildNodes().getLength(); i++)
        {
            Node child = content.getChildNodes().item(i);
            switch (child.getNodeType())
            {
                case ELEMENT_NODE:
                    sb.append("<");
                    sb.append(child.getLocalName());
                    sb.append(htmlAttributes(child));
                    sb.append(">");
                    sb.append(getHTMLContent(child));
                    sb.append("</");
                    sb.append(child.getLocalName());
                    sb.append(">");
                    break;
                case Node.COMMENT_NODE: //no need to record comment nodes
                    break;
                case Node.ENTITY_REFERENCE_NODE:
                    //load in external content
                    String entityRef = child.getTextContent();
                    EntityReference ref = (EntityReference) child;

                    System.out.println(entityRef);
                    break;
                default:
                    if (child != null && child.getTextContent() != null)
                    {
                        String parsed = child.getTextContent().replace("\u2019", "&#x2019;");
                        sb.append(parsed);
                    }
                    break;
            }
        }

        return sb.toString();
    }

    private String htmlAttributes(Node attribute)
    {
        StringBuilder sb = new StringBuilder();
        if (attribute.hasAttributes())
        {
            for (int i = 0; i < attribute.getAttributes().getLength(); i++)
            {
                Node node = attribute.getAttributes().item(i);
                sb.append(" ");
                sb.append(node.getLocalName());
                sb.append("=\"");
                sb.append(node.getTextContent());
                sb.append("\"");
            }
        }

        return sb.toString();
    }
}
