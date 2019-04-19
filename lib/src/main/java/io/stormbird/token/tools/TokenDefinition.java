package io.stormbird.token.tools;

import io.stormbird.token.entity.FunctionDefinition;
import io.stormbird.token.entity.NonFungibleToken;
import io.stormbird.token.entity.ParseResult;
import io.stormbird.token.entity.TSAction;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenDefinition {
    protected Document xml;
    public Map<String, AttributeType> attributeTypes = new ConcurrentHashMap<>();
    protected Locale locale;
    public Map<String, Integer> addresses = new HashMap<>();
    public Map<String, FunctionDefinition> functions = new ConcurrentHashMap<>();
    public Map<String, Map<String, String>> attributeSets = new ConcurrentHashMap<>(); //TODO: add language, in case user changes language during operation - see Weiwu's comment further down
    public Map<String, TSAction> actions = new ConcurrentHashMap<>();

    private String nameSpace;

    private static final String ATTESTATION = "http://attestation.id/ns/tbml";
    private static final String TOKENSCRIPT = "http://tokenscript.org/2019/04/tokenscript";
    private static final String TOKENSCRIPTBASE = "http://tokenscript.org/";

    /* the following are incorrect, waiting to be further improved
     with suitable XML, because none of these String typed class variables
     are going to be one-per-XML-file:

     - each contract <feature> normally should invoke new code modules
       e.g. when a new decentralised protocol is introduced, a new
       class to handle the protocol needs to be introduced, which owns
       it own way of specifying implementation, like marketeQueueAPI.

     - tokenName is going to be selectable through filters -
       that is, it's allowed that token names are different in the
       same asset class. There are use-cases for this.

     - each token definition XML file can incorporate multiple
       contracts, each with different network IDs.

     - each XML file can be signed mulitple times, with multiple
       <KeyName>.
    */
    protected String marketQueueAPI = null;
    protected String feemasterAPI = null;
    protected String tokenName = null;
    protected String tokenNameCollective = null;
    protected String keyName = null;
    protected String contractType = null;
    protected int networkId = 1; //default to main net unless otherwise specified

    public enum Syntax {
        DirectoryString, IA5String, Integer, GeneralizedTime,
        Boolean, BitString, CountryString, JPEG, NumericString
    }

    public enum As {  // always assume big endian
        UTF8, Unsigned, Signed, Mapping
    }

    protected class AttributeType {
        public BigInteger bitmask;    // TODO: BigInteger !== BitInt. Test edge conditions.
        public String name;  // TODO: should be polyglot because user change change language in the run
        public String id;
        public int bitshift = 0;
        public Syntax syntax;
        public As as;
        public Map<BigInteger, String> members;
        public String function = null;

        public AttributeType(Element attr) {
            name = getLocalisedString(attr,"name");
            if (name == null) return;
            id = attr.getAttribute("id");
            try {
                switch (attr.getAttribute("syntax")) { // We don't validate syntax here; schema does it.
                    case "1.3.6.1.4.1.1466.115.121.1.6":
                        syntax = Syntax.BitString;
                        break;
                    case "1.3.6.1.4.1.1466.115.121.1.7":
                        syntax = Syntax.Boolean;
                        break;
                    case "1.3.6.1.4.1.1466.115.121.1.11":
                        syntax = Syntax.CountryString;
                        break;
                    case "1.3.6.1.4.1.1466.115.121.1.28":
                        syntax = Syntax.JPEG;
                        break;
                    case "1.3.6.1.4.1.1466.115.121.1.36":
                        syntax = Syntax.NumericString;
                        break;
                    case "1.3.6.1.4.1.1466.115.121.1.24":
                        syntax = Syntax.GeneralizedTime;
                        break;
                    case "1.3.6.1.4.1.1466.115.121.1.26":
                        syntax = Syntax.IA5String;
                        break;
                    case "1.3.6.1.4.1.1466.115.121.1.27":
                        syntax = Syntax.Integer;
                        break;
                    default: // unknown syntax treat as Directory String
                        syntax = Syntax.DirectoryString;
                }
            } catch (NullPointerException e) { // missing <syntax>
                syntax = Syntax.DirectoryString; // 1.3.6.1.4.1.1466.115.121.1.15
            }
            bitmask = null;
            for(Node node=attr.getFirstChild();
                node!=null; node=node.getNextSibling()){
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element origin = (Element) node;
                    String label = node.getLocalName();
                    switch (label)
                    {
                        case "features":
                            //look for function
                            break;
                        case "contracts":
                            //System.out.println(node.getAttributes().toString());
                            break;
                        case "appearance":

                            break;
                    }
                    switch(origin.getAttribute("contract").toLowerCase()) {
                        case "holding-contract":
                            as = As.Mapping;
                            // TODO: Syntax is not checked
                            //getFunctions(origin);
                            break;
                        default:
                            break;
                    }
                    switch(origin.getAttribute("as").toLowerCase()) {
                        case "signed":
                            as = As.Signed;
                            break;
                        case "utf8":
                            as = As.UTF8;
                            break;
                        case "mapping":
                            as = As.Mapping;
                            // TODO: Syntax is not checked
                            members = new ConcurrentHashMap<>();
                            populate(origin);
                            break;
                        default: // "unsigned"
                            as = As.Unsigned;
                    }
                    if (origin.hasAttribute("bitmask")) {
                        bitmask = new BigInteger(origin.getAttribute("bitmask"), 16);
                    }
                }
            }
            if (bitmask != null ) {
                while (bitmask.mod(BigInteger.ONE.shiftLeft(++bitshift)).equals(BigInteger.ZERO)) ; // !!
                bitshift--;
            }
            // System.out.println("New FieldDefinition :" + name);
        }

        private void populate(Element mapping) {
            Element option;
            NodeList nList = mapping.getElementsByTagNameNS(nameSpace, "option");
            for (int i = 0; i < nList.getLength(); i++) {
                option = (Element) nList.item(i);
                members.put(new BigInteger(option.getAttribute("key")), getLocalisedString(option, "value"));
            }
        }

        /**
         * Converts bitshifted/masked token numeric data into corresponding string.
         * eg. Attr is 'venue'; choices are "1" -> "Kaliningrad Stadium", "2" -> "Volgograd Arena" etc.
         * NB 'time' is Unix EPOCH, which is also a mapping.
         * Since the value may not have a corresponding mapping, but is a valid time we should still return the time value
         * and interpret it as a local time
         *
         * Also - some NF tokens which share a contract with others (eg World Cup, Meetup invites) will have mappings
         * which intentionally have zero value - eg 'Match' has no lookup value for a meeting. Returning null is a guide for the
         * token layout not to show the value.
         *
         * This will become less relevant once the IFrame system is in place - each token appearance will be defined explicitly.
         * However it may be necessary for a default display of token attributes for ease of use while potential
         * users become acquainted with the system.
         *
         * @param data
         * @return
         * @throws UnsupportedEncodingException
         */
        public String toString(BigInteger data) throws UnsupportedEncodingException
        {
            // TODO: in all cases other than UTF8, syntax should be checked
            switch (as)
            {
                case UTF8:
                    return new String(data.toByteArray(), "UTF8");

                case Unsigned:
                    return data.toString();

                case Mapping:
                    // members might be null, but it is better to throw up ( NullPointerException )
                    // than silently ignore
                    // JB: Existing contracts and tokens throw this error. The wallet 'crashes' each time existing tokens are opened
                    // due to assumptions made with extra tickets (ie null member is assumed to return null and not display that element).
                    if (members.containsKey(data))
                    {
                        return members.get(data);
                    }
                    else if (syntax == Syntax.GeneralizedTime)
                    {
                        //This is a time entry but without a localised mapped entry. Return the EPOCH time.
                        return data.toString(10);
                    }
                    else
                    {
                        return null; // have to revert to this behaviour due to values being zero when tokens are created
                        //refer to 'AlphaWallet meetup tickets' where 'Match' mapping is null but for FIFA is not.
                        //throw new NullPointerException("Key " + data.toString() + " can't be mapped.");
                    }
                default:
                    throw new NullPointerException("Missing valid 'as' attribute");
            }
        }
    }

    private FunctionDefinition getFunction(Node mapping) {
        Element option;
        FunctionDefinition fd = new FunctionDefinition();
        if (mapping.getAttributes().getLength() > 0)
        {
            Node attr = mapping.getAttributes().getNamedItem("name");
            if (attr != null)
            {
                fd.method = attr.getTextContent();
                fd.syntax = Syntax.Integer;
            }
        }

        for(Node child=mapping.getFirstChild(); child!=null; child=child.getNextSibling())
        {
            if (child.getNodeType() == Node.ELEMENT_NODE)
            {
                option = (Element) child;
                String type = child.getLocalName();
                String functionName = option.getAttribute("name");
                //TODO: Get child elements; inputs and input param keys

                switch (type)
                {
                    case "inputs":
                        //TODO Read inputs from child node
                        //String inputSpec = getChildElement(child, );
                        break;
                    default:
                        break;
                }
            }
        }
        return fd;
    }

    /* for many occurance of the same tag, return the text content of the one in user's current language */
    // FIXME: this function will break if there are nested <tagName> in the nameContainer
    String getLocalisedString(Element nameContainer, String tagName) {
        NodeList nList = nameContainer.getElementsByTagNameNS(nameSpace, tagName);
        Element name;
        for (int i = 0; i < nList.getLength(); i++) {
            name = (Element) nList.item(i);
            String langAttr = getLocalisationLang(name);
            if (langAttr.equals(locale.getLanguage())) {
                return name.getTextContent();
            }
        }
        /* no matching language found. return the first tag's content */
        name = (Element) nList.item(0);
        // TODO: catch the indice out of bound exception and throw it again suggesting dev to check schema
        if (name != null) return name.getTextContent();
        else return null;
    }

    Node getLocalisedNode(Element nameContainer, String tagName) {
        NodeList nList = nameContainer.getElementsByTagNameNS(nameSpace, tagName);
        Element name;
        for (int i = 0; i < nList.getLength(); i++) {
            name = (Element) nList.item(i);
            String langAttr = getLocalisationLang(name);
            if (langAttr.equals(locale.getLanguage())) {
                return name;
            }
        }

        return null;
    }

    Node getNode(Element nameContainer, String tagName) {
        NodeList nList = nameContainer.getElementsByTagName(tagName);
        Element name;
        Element nonLocalised = null;
        for (int i = 0; i < nList.getLength(); i++) {
            name = (Element) nList.item(i);
            String langAttr = getLocalisationLang(name);
            if (langAttr.equals(locale.getLanguage())) {
                return name;
            }
            else if (langAttr.length() == 0)
            {
                nonLocalised = name;
            }
        }

        return nonLocalised;
    }

    String getLocalisedString(Element nameContainer, String tagName, String typeAttr) {
        NodeList nList = nameContainer.getElementsByTagNameNS(nameSpace, tagName);
        Element name;
        for (int i = 0; i < nList.getLength(); i++) {
            name = (Element) nList.item(i);
            String langAttr = getLocalisationLang(name);
            if (langAttr.equals(locale.getLanguage()) && hasAttribute(name, typeAttr)) {
                return name.getTextContent();
            }
        }

        return null;
    }

    private boolean hasAttribute(Element name, String typeAttr)
    {
        if (name.hasAttributes())
        {
            for (int i = 0; i < name.getAttributes().getLength(); i++)
            {
                Node thisAttr = name.getAttributes().item(i);
                if (thisAttr.getTextContent() != null && thisAttr.getTextContent().equals(typeAttr))
                {
                    return true;
                }
            }
        }

        return false;
    }

    private String getLocalisationLang(Element name)
    {
        if (name.hasAttributes())
        {
            for (int i = 0; i < name.getAttributes().getLength(); i++)
            {
                Node thisAttr = name.getAttributes().item(i);
                if (thisAttr.getLocalName().equals("lang"))
                {
                    return thisAttr.getTextContent();
                }
            }
        }

        return "";
    }

    Node getLocalisedContent(Node container, String tagName)
    {
        NodeList nList = container.getChildNodes();
        Node node;
        Node fallback = null;

        for (int i = 0; i < nList.getLength(); i++)
        {
            node = nList.item(i);
            switch (node.getNodeType())
            {
                case Node.TEXT_NODE:
                    break;
                case Node.ELEMENT_NODE:
                    fallback = node;
                    if (node.getLocalName().equals(tagName))
                    {
                        Element element = (Element)node;
                        String currentNodeLang = null;
                        if (node.hasAttributes())
                        {
                            Node attr = node.getAttributes().item(0);
                            if (attr.getLocalName().equals("lang"))
                            {
                                currentNodeLang = attr.getTextContent();
                            }
                        }
                        if (currentNodeLang == null || currentNodeLang.length() == 0) currentNodeLang = (new Locale(element.getAttribute("lang"))).getLanguage();
                        if (currentNodeLang.equals(locale.getLanguage()))
                        {
                            return node;
                        }
                    }
                    break;
            }
        }

        return fallback;
    }

    private String getTextContent(Element element)
    {
        if (element.getChildNodes().getLength() > 0 && element.getChildNodes().item(0).getNodeType() == Node.TEXT_NODE)
        {
            return element.getChildNodes().item(0).getTextContent();
        }
        else
        {
            return null;
        }
    }

    public TokenDefinition(InputStream xmlAsset, Locale locale, ParseResult result) throws IOException, SAXException{
        this.locale = locale;
        /* guard input from bad programs which creates Locale not following ISO 639 */
        if (locale.getLanguage().length() < 2 || locale.getLanguage().length() > 3) {
            throw new SAXException("Locale object wasn't created following ISO 639");
        }
        DocumentBuilder dBuilder;
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(true);
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            // TODO: if schema has problems (e.g. defined twice). Now, no schema, no exception.
            e.printStackTrace();
            return;
        }
        Document xml = dBuilder.parse(xmlAsset);
        xml.getDocumentElement().normalize();
        determineNamespace(xml, result);
        //TSValidator.check(xml, nameSpace);

        NodeList nList = xml.getElementsByTagNameNS(nameSpace, "token");

        try
        {
            TSValidator.check(xml);
            //extractSignatureTag(nList, xml);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


        if (nList.getLength() == 0)
        {
            System.out.println("Legacy XML format - no longer supported");
            return;
        }

        //TODO: Needs to be namespace aware
        CrawlAttrs(nList);

        extractFeatureTag(xml);
        extractContractTag(xml);
        extractNameTag(xml);
        extractSignedInfo(xml);
        extractCards(xml);

        //TODO: 'appearance' in XML needs to have an HTML attribute
        extractTags(xml, "appearance", true);
        extractTags(xml, "features", false);
    }

    private void determineNamespace(Document xml, ParseResult result)
    {
        nameSpace = ATTESTATION;

        NodeList check = xml.getChildNodes();
        for (int i = 0; i < check.getLength(); i++)
        {
            Node n = check.item(i);
            if (!n.hasAttributes()) continue;
            //check attributes
            for (int j = 0; j < n.getAttributes().getLength(); j++)
            {
                Node thisAttr = n.getAttributes().item(j);
                if (thisAttr.getNodeValue().contains(TOKENSCRIPTBASE))
                {
                    nameSpace = TOKENSCRIPT;
                    if (result != null && !thisAttr.getNodeValue().equals(TOKENSCRIPT))
                    {
                        result.parseMessage(ParseResult.ParseResultId.OK);
                    }
                    return;
                }
            }
        }
    }

    //TODO: switch from hard-code to token parser
    private void extractCards(Document xml)
    {
        NodeList nList = xml.getElementsByTagNameNS(nameSpace, "cards");
        if (nList.getLength() == 0) return;
        Element cards = (Element) nList.item(0);
        nList = cards.getElementsByTagNameNS(nameSpace, "token-card");
        if (nList.getLength() == 0) return;
        Map<String, String> attributeSet = new ConcurrentHashMap<>();
        addToHTMLSet(attributeSet, cards, "style");
        addToHTMLSet(attributeSet, cards,"view-iconified");
        addToHTMLSet(attributeSet, cards,"view");

        if (attributeSet.size() > 0)
        {
            attributeSets.put("cards", attributeSet);
        }

        nList = cards.getElementsByTagNameNS(nameSpace, "action");
        if (nList.getLength() == 0) return;
        Element action = (Element) nList.item(0);
        TSAction tsAction = new TSAction();
        //name (localised)
        String name = getLocalisedString(action, "name");
        tsAction.type = action.getAttribute("type");
        //exclude
        tsAction.exclude = "";
        tsAction.view = "none";
        nList = action.getElementsByTagNameNS(nameSpace, "exclude");
        if (nList.getLength() > 0)
        {
            Element excludeNode = (Element) nList.item(0);
            tsAction.exclude = excludeNode.getAttribute("selection");
        }
        Node viewNode = getLocalisedNode(action, "view");
        if (viewNode != null)
        {
            tsAction.view = getHTMLContent(viewNode);
        }

        actions.put(name, tsAction);
    }

    private void addToHTMLSet(Map<String, String> attributeSet, Element root, String tagName)
    {
        Node view = getLocalisedNode(root, tagName);
        if (view == null) view = getNode(root, tagName);
        if (view != null)
        {
            String iconifiedContent = getHTMLContent(view);
            attributeSet.put(tagName, iconifiedContent);
        }
    }

    private void CrawlAttrs(NodeList nList)
    {
        //Node impl = nList.item(i);
        //NodeList cNodes = impl.getChildNodes();
        for (int j = 0; j < nList.getLength(); j++)
        {
            Node n = nList.item(j);
            if (n.getPrefix() != null)
            {
                processAttrs(n);
            }

            if (n.hasChildNodes())
            {
                CrawlAttrs(n.getChildNodes());
            }
        }
    }

    private void processAttrs(Node n)
    {
        AttributeType attr = new AttributeType((Element) n);
        if (attr.bitmask != null)
        {// has <origin> which is from bitmask
            attributeTypes.put(attr.id, attr);
        } // TODO: take care of attributeTypes whose value does not originate from bitmask!
        else if (attr.function != null)
        {
            FunctionDefinition fd = new FunctionDefinition();
            fd.method = attr.function;
            fd.syntax = attr.syntax;
            functions.put(attr.id, fd);
        }
    }

    private void extractSignedInfo(Document xml) {
        NodeList nList;
        nList = xml.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "KeyName");
        if (nList.getLength() > 0) {
            this.keyName = nList.item(0).getTextContent();
        }
        return; // even if the document is signed, often it doesn't have KeyName
    }

    public String getKeyName() {
        return this.keyName;
    }

    public String getFeemasterAPI()
    {
        return feemasterAPI;
    }

    public String getTokenName() { return tokenName; }
    public String getTokenNameCollective() { return tokenNameCollective; }
    public String getContractType() { return contractType; }

    public int getNetworkFromContract(String contractAddress)
    {
        return (addresses.get(contractAddress) == null ? -1 : addresses.get(contractAddress));
    }

    public Map<BigInteger, String> getMappingMembersByKey(String key){
        if(attributeTypes.containsKey(key)) {
            AttributeType attr = attributeTypes.get(key);
            return attr.members;
        }
        return null;
    }
    public Map<BigInteger, String> getConvertedMappingMembersByKey(String key){
        if(attributeTypes.containsKey(key)) {
            Map<BigInteger,String> convertedMembers=new HashMap<>();
            AttributeType attr = attributeTypes.get(key);
            for(BigInteger actualValue:attr.members.keySet()){
                convertedMembers.put(actualValue.shiftLeft(attr.bitshift).and(attr.bitmask),attr.members.get(actualValue));
            }
            return convertedMembers;
        }
        return null;
    }

    private void extractFeatureTag(Document xml)
    {
        NodeList l;
        NodeList nList = xml.getElementsByTagNameNS(nameSpace, "feature");
        for (int i = 0; i < nList.getLength(); i++) {
            Element feature = (Element) nList.item(i);
            switch (feature.getAttribute("type")) {
                case "feemaster":
                    l = feature.getElementsByTagNameNS(nameSpace, "feemaster");
                    for (int j = 0; j < l.getLength(); j++)
                        feemasterAPI = l.item(j).getTextContent();
                    break;
                case "market-queue":
                    l = feature.getElementsByTagNameNS(nameSpace, "gateway");
                    for (int j = 0; j < l.getLength(); j++)
                        marketQueueAPI = l.item(j).getTextContent();
                    break;
                default:
                    break;
            }
        }
    }

    private void extractSignatureTag(NodeList nList, Document xml) throws Exception
    {
        Element token = (Element)nList.item(0);
        for (int i = 0; i < token.getChildNodes().getLength(); i++)
        {
            Node child = token.getChildNodes().item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE)
            {
                if (child.getLocalName().equals("Signature"))
                {
                    TSValidator.check(xml);
                    return;
                }
            }
        }

        //NodeList nList = xml.getElementsByTagNameNS(nameSpace, "Signature");
        /* we allow multiple contracts, e.g. for issuing asset and for
         * proxy usage. but for now we only deal with the first */
        //Element contract = (Element) nList.item(0);

        String tokenSig = getLocalisedString(token, "SignedInfo");
    }

    private void extractNameTag(Document xml)
    {
        NodeList nList = xml.getElementsByTagNameNS(nameSpace, "token");
        if (nList.getLength() == 0) return;

        Element contract = (Element) nList.item(0);

        tokenName = getLocalisedString(contract, "name");
        tokenNameCollective = getLocalisedString(contract, "name", "collective");
    }

    private void extractContractTag(Document xml)
    {
        NodeList nList = xml.getElementsByTagNameNS(nameSpace, "contract");
        /* we allow multiple contracts, e.g. for issuing asset and for
         * proxy usage. but for now we only deal with the first */
        Element contract = (Element) nList.item(0);

        tokenName = getLocalisedString(contract, "name");

        /*if hit NullPointerException in the next statement, then XML file
         * must be missing <contract> elements */
        /* TODO: select the contract of type "holding_contract" */
        nList = contract.getElementsByTagNameNS(nameSpace, "address");
        contractType = contract.getAttribute("id");
        for (int addrIndex = 0; addrIndex < nList.getLength(); addrIndex++)
        {
            Node node = nList.item(addrIndex);
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                int network = networkId;
                if (node.hasAttributes() && node.getAttributes().item(0).getLocalName().equals("network"))
                {
                    String networkStr = node.getAttributes().item(0).getNodeValue();
                    network = Integer.parseInt(networkStr);
                }

                for (int i = 0; i < node.getChildNodes().getLength(); i++)
                {
                    Node address = node.getChildNodes().item(i);
                    if (address.getNodeType() == Node.TEXT_NODE)
                    {
                        addresses.put(address.getTextContent().toLowerCase(), network);
                    }
                }
            }
        }
    }

    private void extractTags(Document xml, String localName, boolean isHTML)
    {
        NodeList nList = xml.getElementsByTagNameNS(nameSpace, localName);
        Element element = (Element) nList.item(0);

        if (element != null)
        {
            Map<String, String> attributeSet = new ConcurrentHashMap<>();
            attributeSets.put(localName, attributeSet);

            //go through each child in this element looking for tags
            for (int i = 0; i < element.getChildNodes().getLength(); i++)
            {
                Node node = element.getChildNodes().item(i);
                switch (node.getNodeType())
                {
                    case Node.ATTRIBUTE_NODE:
                        //System.out.println(node.getAttributes().toString());
                        break;
                    case Node.ELEMENT_NODE:
                        String nodeName = node.getLocalName();
                        if (attributeSet.containsKey(nodeName))
                            continue;
                        Node content = getLocalisedContent(element, nodeName);
                        if (content != null)
                        {
                            String contentString;
                            if (isHTML)
                            {
                                contentString = getHTMLContent(content);
                                attributeSet.put(nodeName, contentString);
                            }
                            else
                                getContent(content);
                        }
                        break;
                    case Node.TEXT_NODE:
                        //System.out.println(node.getTextContent());
                        break;
                }
            }
        }
    }

    private String getHTMLContent(Node content)
    {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < content.getChildNodes().getLength(); i++)
        {
            Node child = content.getChildNodes().item(i);
            switch (child.getNodeType())
            {
                case Node.ELEMENT_NODE:
                    if (child.getLocalName().equals("iframe")) continue;
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
                default:
                    String parsed = child.getTextContent().replace("\u2019", "&#x2019;");
                    sb.append(parsed);
                    break;
            }
        }

        return sb.toString();
    }

    private void getContent(Node content)
    {
        switch (content.getLocalName())
        {
            case "action":
                handleFunction(content);
                break;
            case "contract":
                //handleContract
                break;
        }
    }

    private void handleFunction(Node content)
    {
        String functionName = "";
        for (int i = 0; i < content.getChildNodes().getLength(); i++)
        {
            Node child = content.getChildNodes().item(i);
            switch (child.getNodeType())
            {
                case Node.TEXT_NODE:
                    break;
                case Node.ELEMENT_NODE:
                    switch (child.getLocalName())
                    {
                        case "name":
                            functionName = getHTMLContent(child);
                            break;
                        case "function":
                            FunctionDefinition fd = getFunction(child);
                            if (fd != null)
                            {
                                functions.put(functionName, fd);
                            }
                        default:
                            break;
                    }
                    break;
                default:
                    break;
            }
        }
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

    /* take a token ID in byte-32, find all the fields in it and call back
     * token.setField(fieldID, fieldName, text-value). This is abandoned
     * temporarily for the need to retrofit the class with J.B.'s design */

    public void parseField(BigInteger tokenId, NonFungibleToken token) {
        for (String key : attributeTypes.keySet()) {
            AttributeType attrtype = attributeTypes.get(key);
            BigInteger val = tokenId.and(attrtype.bitmask).shiftRight(attrtype.bitshift);
            try {
                token.setAttribute(attrtype.id,
                        new NonFungibleToken.Attribute(attrtype.id, attrtype.name, val, attrtype.toString(val)));
            } catch (UnsupportedEncodingException e) {
                token.setAttribute(attrtype.id,
                        new NonFungibleToken.Attribute(attrtype.id, attrtype.name, val, "unsupported encoding"));
            }
        }
    }

    /**
     * Check for 'appearance' attribute set
     * @param tag
     * @return
     */
    public String getAppearance(String tag)
    {
        Map<String, String> appearanceSet = attributeSets.get("appearance");
        if (appearanceSet != null)
        {
            return appearanceSet.get(tag);
        }
        else
        {
            return "";
        }
    }

    /**
     * Check for 'cards' attribute set
     * @param tag
     * @return
     */
    public String getCardData(String tag)
    {
        Map<String, String> appearanceSet = attributeSets.get("cards");
        if (appearanceSet != null)
        {
            return appearanceSet.get(tag);
        }
        else
        {
            return "";
        }
    }

    public Map<String, TSAction> getActions()
    {
        return actions;
    }
}
