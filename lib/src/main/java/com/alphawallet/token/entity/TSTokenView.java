package com.alphawallet.token.entity;

import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import static org.w3c.dom.Node.ELEMENT_NODE;
import static org.w3c.dom.Node.TEXT_NODE;

import com.alphawallet.token.tools.TokenDefinition;

import java.util.Objects;

/**
 * Holds an individual Token View which consists of style and HTML view code
 *
 * Created by JB on 8/05/2020.
 */
public class TSTokenView
{
    private String tokenView = "";
    private String style = "";
    private String label = "";
    private String url;
    private String urlFragment;

    public TSTokenView(Element element, TokenDefinition td) throws SAXException
    {
        //handle raw view card, only need to process the view element itself
        if (element.getLocalName().equals("view"))
        {
            handleView(element, td);
            return;
        }

        for (int j = 0; j < element.getAttributes().getLength(); j++)
        {
            Node node = element.getAttributes().item(j);
            switch (node.getLocalName())
            {
                case "name":
                    label = node.getTextContent();
                    break;
                default:
                    break;
            }
        }

        NodeList ll = element.getChildNodes();

        for (int j = 0; j < ll.getLength(); j++)
        {
            Node node = ll.item(j);
            if (node.getNodeType() != ELEMENT_NODE)
                continue;

            Element thisElement = (Element) node;
            switch (node.getLocalName())
            {
                case "attribute":
                    Attribute attr = new Attribute(thisElement, td);
                    td.addLocalAttr(attr); //TODO: This should be stored in this class
                    break;
                case "view": //TODO: Localisation
                case "item-view":
                    handleView(thisElement, td);
                    break;
                case "view-iconified":
                    throw new SAXException("Deprecated <view-iconified> used in <ts:token>. Replace with <item-view>");
                case "style":
                    td.addGlobalStyle(thisElement);
                    break;
                case "script":
                    //misplaced script tag
                    throw new SAXException("Misplaced <script> tag in <ts:token>");
                case "label":
                    label = td.getLocalisedString(thisElement);
                    break;
                default:
                    throw new SAXException("Unknown tag <" + node.getLocalName() + "> tag in tokens");
            }
        }
    }

    public String getLabel()
    {
        return label;
    }

    private void handleView(Element element, TokenDefinition td)
    {
        url = element.getAttribute("url");
        urlFragment = element.getAttribute("urlFragment");
        generateTokenView(element, td);
    }

    private void generateTokenView(Element element, TokenDefinition td)
    {
        if (!Objects.equals(this.getUrl(), ""))
        {
            return;
        }

        String lStyle = "";
        String lView = "";
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
                            lStyle += getHTMLContent(child);
                            break;
                        case "viewContent":
                            String name = child.getAttributes().getNamedItem("name").getTextContent();
                            Element content = td.getViewContent(name);
                            generateTokenView(content, td);
                            break;
                        default:
                            lView += getElementHTML(child);
                            break;
                    }
                    break;
                case TEXT_NODE:
                    if (element.getChildNodes().getLength() == 1)
                    {
                        //handle text item-view
                        lView = child.getTextContent().replace("\u2019", "&#x2019;");
                    }
                    break;
                default:
                    break;
            }
        }

        tokenView += lView;
        style += lStyle;
    }

    public String getTokenView()
    {
        return tokenView;
    }

    public String getStyle()
    {
        return style;
    }

    public String getUrl()
    {
        return url;
    }

    public String getUrlFragment()
    {
        return urlFragment;
    }

    public TSTokenView(String style, String view)
    {
        this.style = style;
        this.tokenView = view;
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
