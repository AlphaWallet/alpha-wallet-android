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
    public final String tokenView;
    public final String style;

    public TSTokenView(Element element)
    {
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

        tokenView = lView;
        style = lStyle;
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

    public static TSTokenView getDefaultView(String assetDetailViewName)
    {
        String style = "h3 { color: #111; font-family: 'Open Sans', sans-serif; font-size: 20px; font-weight: 300; line-height: 32px; }\n" +
                "\n" +
                "#inputBox {\n" +
                "  text-align: center;\n" +
                "}\n" +
                "\n" +
                "html,\n" +
                "body {\n" +
                "  height: 100%;\n" +
                "}\n" +
                "html {\n" +
                "  font-size: 14px;\n" +
                "}\n" +
                "body {\n" +
                "  margin: 0px;\n" +
                "  padding: 0px;\n" +
                "  overflow-x: hidden;\n" +
                "  min-width: 320px;\n" +
                "  background: #FFFFFF;\n" +
                "  font-family: 'Lato', 'Helvetica Neue', Arial, Helvetica, sans-serif;\n" +
                "  font-size: 14px;\n" +
                "  line-height: 1.4285em;\n" +
                "  color: rgba(0, 0, 0, 0.87);\n" +
                "  font-smoothing: antialiased;\n" +
                "}\n" +
                ".ui.container {\n" +
                "  display: block;\n" +
                "  max-width: 100% !important;\n" +
                "}\n" +
                "@media only screen and (max-width: 767px) {\n" +
                "  .ui.container {\n" +
                "    width: auto !important;\n" +
                "    margin-left: 1em !important;\n" +
                "    margin-right: 1em !important;\n" +
                "  }\n" +
                "}\n" +
                "@media only screen and (min-width: 768px) and (max-width: 991px) {\n" +
                "  .ui.container {\n" +
                "    width: 723px;\n" +
                "    margin-left: auto !important;\n" +
                "    margin-right: auto !important;\n" +
                "  }\n" +
                "}\n" +
                "@media only screen and (min-width: 992px) and (max-width: 1199px) {\n" +
                "  .ui.container {\n" +
                "    width: 933px;\n" +
                "    margin-left: auto !important;\n" +
                "    margin-right: auto !important;\n" +
                "  }\n" +
                "}\n" +
                "@media only screen and (min-width: 1200px) {\n" +
                "  .ui.container {\n" +
                "    width: 1127px;\n" +
                "    margin-left: auto !important;\n" +
                "    margin-right: auto !important;\n" +
                "  }\n" +
                "}\n" +
                ".ui.segment {\n" +
                "  position: relative;\n" +
                "  background: #FFFFFF;\n" +
                "  -webkit-box-shadow: 0px 1px 2px 0 rgba(34, 36, 38, 0.15);\n" +
                "  box-shadow: 0px 1px 2px 0 rgba(34, 36, 38, 0.15);\n" +
                "  margin: 0.5rem 0em;\n" +
                "  padding: 0.5em 0.5em;\n" +
                "  border-radius: 0.28571429rem;\n" +
                "  border: 1px solid rgba(34, 36, 38, 0.15);\n" +
                "  text-align: center;\n" +
                "}\n" +
                ".ui.segment:first-child {\n" +
                "  margin-top: 0em;\n" +
                "}\n" +
                ".ui.segment:last-child {\n" +
                "  margin-bottom: 0em;\n" +
                "}\n" +
                "input {\n" +
                "  position: relative;\n" +
                "  font-weight: normal;\n" +
                "  font-style: normal;\n" +
                "  font-size: 12px;\n" +
                "  display: -ms-inline-flexbox;\n" +
                "  display: inline-flex;\n" +
                "  color: rgba(0, 0, 0, 0.87);\n" +
                "  padding: 9.5px 14px;\n" +
                "  width: 300px;\n" +
                "  border-color: #D8D8D8;\n" +
                "}\n" +
                "input[type=text]:focus {\n" +
                "  border-color: #D8D8D8;\n" +
                "  background: #FAFAFA;\n" +
                "  color: rgba(0, 0, 0, 0.87);\n" +
                "  -webkit-box-shadow: none;\n" +
                "  box-shadow: none;\n" +
                "}\n" +
                "label {\n" +
                "  font-size: 12px;\n" +
                "  font-weight: 500;\n" +
                "  margin-top: 6px;\n" +
                "}";
        String view = "<script type=\"text/javascript\"> //\n" +
                "class Token {\n" +
                "\n" +
                "    constructor(tokenInstance) {\n" +
                "        this.props = tokenInstance;\n" +
                "    }\n" +
                "\n" +
                "    render() {\n" +
                "        let message = `Sent ${this.props.symbol}`;\n" +
                "        var amountFixed = (this.props.amount / 10**this.props.decimals).toFixed(2);\n" +
                "        return`\n" +
                "        &lt;div class=\"ui container\"&gt;\n" +
                "          &lt;div class=\"ui segment\"&gt;\n" +
                "            &lt;img src=\"data:image/png;base64,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\"&gt;\n" +
                "            &lt;span&gt;&lt;bold&gt;&lt;h3&gt;${message}&lt;/h3&gt;&lt;/bold&gt;&lt;/span&gt;\n" +
                "            &lt;p&gt;Sent ${amountFixed} ${this.props.symbol} to ${this.props.to}&lt;/p&gt;\n" +
                "          &lt;/div&gt;\n" +
                "        &lt;/div&gt;\n" +
                "`;\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "web3.tokens.dataChanged = (oldTokens, updatedTokens, tokenIdCard) =&gt; {\n" +
                "    const currentTokenInstance = web3.tokens.data.currentInstance;\n" +
                "    document.getElementById(tokenIdCard).innerHTML = new Token(currentTokenInstance).render();\n" +
                "};\n" +
                "//\n" +
                "</script>";

        return new TSTokenView(style, view);
    }
}
