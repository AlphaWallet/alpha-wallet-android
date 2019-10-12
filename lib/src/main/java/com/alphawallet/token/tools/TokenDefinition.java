package com.alphawallet.token.tools;

import com.alphawallet.token.entity.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.w3c.dom.Node.ELEMENT_NODE;

public class TokenDefinition {
    protected Document xml;
    public Map<String, AttributeType> attributeTypes = new HashMap<>();
    protected Locale locale;

    public Map<String, ContractInfo> contracts = new HashMap<>();
    public Map<String, Map<String, String>> attributeSets = new HashMap<>(); //TODO: add language, in case user changes language during operation - see Weiwu's comment further down
    public Map<String, TSAction> actions = new HashMap<>();
    private Map<String, String> names = new HashMap<>(); // store plural etc for token name

    public String nameSpace;
    public TokenscriptContext context;
    public String holdingToken;

    public static final String TOKENSCRIPT_CURRENT_SCHEMA = "2019/10";
    public static final String TOKENSCRIPT_REPO_SERVER = "https://repo.tokenscript.org/";

    private static final String ATTESTATION = "http://attestation.id/ns/tbml";
    private static final String TOKENSCRIPT_NAMESPACE = "http://tokenscript.org/" + TOKENSCRIPT_CURRENT_SCHEMA + "/tokenscript";
    private static final String TOKENSCRIPT_BASE_URL = "http://tokenscript.org/";

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
    protected String keyName = null;

    public List<FunctionDefinition> getFunctionData()
    {
        List<FunctionDefinition> defs = new ArrayList<>();
        for (AttributeType attr : attributeTypes.values())
        {
            if (attr.function != null)
            {
                defs.add(attr.function);
            }
        }

        return defs;
    }

    public FunctionDefinition parseFunction(Element resolve, Syntax syntax)
    {
        FunctionDefinition function = new FunctionDefinition();
        //this value is obtained from a contract call
        String contract = resolve.getAttribute("contract");
        function.contract = contracts.get(contract);
        function.method = resolve.getAttribute("function");
        addFunctionInputs(function, resolve);
        function.syntax = syntax;
        return function;
    }

    public As parseAs(Element resolve)
    {
        switch(resolve.getAttribute("as").toLowerCase()) {
            case "signed":
                return As.Signed;
            case "utf8":
            case "": //no type specified, return string
                return As.UTF8;
            case "bytes":
                return As.Bytes;
            case "e18":
                return As.e18;
            case "e8":
                return As.e8;
            case "e4":
                return As.e4;
            case "e2":
                return As.e2;
            case "bool":
                return As.Boolean;
            case "mapping":
                return As.Mapping;
            default: // "unsigned"
                return As.Unsigned;
        }
    }

    public enum Syntax {
        DirectoryString, IA5String, Integer, GeneralizedTime,
        Boolean, BitString, CountryString, JPEG, NumericString
    }

    /* for many occurance of the same tag, return the text content of the one in user's current language */
    // FIXME: this function will break if there are nested <tagName> in the nameContainer
    public String getLocalisedString(Element nameContainer, String tagName) {
        NodeList nList = nameContainer.getElementsByTagNameNS(nameSpace, tagName);
        Element name;
        String nonLocalised = null;
        for (int i = 0; i < nList.getLength(); i++) {
            name = (Element) nList.item(i);
            String langAttr = getLocalisationLang(name);
            if (langAttr.equals(locale.getLanguage())) {
                return name.getTextContent();
            }
            else if (langAttr.equals("en")) nonLocalised = name.getTextContent();
        }

        if (nonLocalised != null) return nonLocalised;
        else
        {
            name = (Element) nList.item(0);
            // TODO: catch the indice out of bound exception and throw it again suggesting dev to check schema
            if (name != null) return name.getTextContent();
            else return null;
        }
    }

    Node getLocalisedNode(Element nameContainer, String tagName) {
        NodeList nList = nameContainer.getElementsByTagNameNS(nameSpace, tagName);
        if (nList.getLength() == 0) nList = nameContainer.getElementsByTagName(tagName);
        Element name;
        Element nonLocalised = null;
        for (int i = 0; i < nList.getLength(); i++) {
            name = (Element) nList.item(i);
            String langAttr = getLocalisationLang(name);
            if (langAttr.equals(locale.getLanguage())) {
                return name;
            }
            else if (nonLocalised == null && (langAttr.equals("") || langAttr.equals("en")))
            {
                nonLocalised = name;
            }
        }

        return nonLocalised;
    }

    String getLocalisedString(Element container) {
        NodeList nList = container.getChildNodes();

        String nonLocalised = null;
        for (int i = 0; i < nList.getLength(); i++) {
            Node n = nList.item(i);
            if (n.getNodeType() == ELEMENT_NODE)
            {
                String langAttr = getLocalisationLang((Element)n);
                if (langAttr.equals(locale.getLanguage()))
                {
                    return n.getTextContent();
                }
                else if (nonLocalised == null && (langAttr.equals("") || langAttr.equals("en")))
                {
                    nonLocalised = n.getTextContent();
                }
            }
        }

        return nonLocalised;
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

    //Empty definition
    public TokenDefinition()
    {
        holdingToken = null;
    }

    public TokenDefinition(InputStream xmlAsset, Locale locale, ParseResult result) throws IOException, SAXException {
        this.locale = locale;
        /* guard input from bad programs which creates Locale not following ISO 639 */
        if (locale.getLanguage().length() < 2 || locale.getLanguage().length() > 3) {
            throw new SAXException("Locale object wasn't created following ISO 639");
        }

        DocumentBuilder dBuilder;

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(true);
            dbFactory.setExpandEntityReferences(true);
            dbFactory.setCoalescing(true);
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            // TODO: if schema has problems (e.g. defined twice). Now, no schema, no exception.
            e.printStackTrace();
            return;
        }
        Document xml = dBuilder.parse(xmlAsset);
        xml.getDocumentElement().normalize();
        determineNamespace(xml, result);

        NodeList nList = xml.getElementsByTagNameNS(nameSpace, "token");

        if (nList.getLength() == 0 || nameSpace == null)
        {
            System.out.println("Legacy XML format - no longer supported");
            return;
        }

        try
        {
            parseTags(xml);
            extractSignedInfo(xml);
        }
        catch (IOException|SAXException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            e.printStackTrace(); //catch other type of exception not thrown by this function.
            result.parseMessage(ParseResult.ParseResultId.PARSE_FAILED);
        }
    }

    private void extractTags(Element token) throws Exception
    {
        //trawl through the child nodes, interpret each in turn
        for (Node n = token.getFirstChild(); n != null; n = n.getNextSibling())
        {
            if (n.getNodeType() == ELEMENT_NODE)
            {
                Element element = (Element)n;
                switch (element.getLocalName())
                {
                    case "origins":
                        parseOrigins(element);
                        break;
                    case "contract":
                        handleAddresses(element);
                        break;
                    case "name":
                        extractNameTag(element);
                        break;
                    case "attribute-types":
                        handleGlobalAttributes(element);
                        break;
                    case "cards":
                        handleCards(element);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void handleCards(Element cards) throws Exception
    {
        for(Node node=cards.getFirstChild(); node!=null; node=node.getNextSibling())
        {
            if (node.getNodeType() == ELEMENT_NODE)
            {
                Element card = (Element) node;
                switch (card.getLocalName())
                {
                    case "token-card":
                        processTokenCardElements(card);
                        break;
                    case "action":
                        extractActions(card);
                        break;
                }
            }
        }
    }

    private void processTokenCardElements(Element card)
    {
        Map<String, Map<String, String>> attributeSet = new HashMap<>();
        for(Node node=card.getFirstChild(); node!=null; node=node.getNextSibling())
        {
            if (node.getNodeType() == ELEMENT_NODE)
            {
                String htmlContent = getHTMLContent(node);
                if (!attributeSet.containsKey(node.getLocalName())) attributeSet.put(node.getLocalName(), new HashMap<>());
                attributeSet.get(node.getLocalName()).put(getLocalisationLang((Element)node), htmlContent);
            }
        }

        if (attributeSet.size() > 0)
        {
            //create localised attribute set
            Map<String, String> localisedAttributes = new HashMap<>();
            for (String attr : attributeSet.keySet())
            {
                Map<String, String> attrEntry = attributeSet.get(attr);
                localisedAttributes.put(attr, getLocalisedEntry(attrEntry));
            }
            attributeSets.put("cards", localisedAttributes);
        }
    }

    private String getLocalisedEntry(Map<String, String> attrEntry)
    {
        //Picking order
        //1. actual locale
        //2. entry with no locale
        //3. first non-localised locale
        String bestGuess = null;
        for (String lang: attrEntry.keySet())
        {
            if (lang.equals(locale.getLanguage())) return attrEntry.get(lang);
            if (lang.equals("") || (lang.equals("en"))) bestGuess = attrEntry.get(lang);
        }

        if (bestGuess == null) bestGuess = attrEntry.values().iterator().next(); //first non-localised locale

        return bestGuess;
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
                try
                {
                    Node thisAttr = n.getAttributes().item(j);
                    if (thisAttr.getNodeValue().contains(TOKENSCRIPT_BASE_URL))
                    {
                        nameSpace = thisAttr.getNodeValue();

                        int dateIndex = nameSpace.indexOf(TOKENSCRIPT_BASE_URL) + TOKENSCRIPT_BASE_URL.length();
                        int lastSeparator = nameSpace.lastIndexOf("/");
                        if ((lastSeparator - dateIndex) == 7)
                        {
                            DateFormat format = new SimpleDateFormat("yyyy/MM", Locale.ENGLISH);
                            Date thisDate = format.parse(nameSpace.substring(dateIndex, lastSeparator));
                            Date schemaDate = format.parse(TOKENSCRIPT_CURRENT_SCHEMA);

                            if (thisDate.equals(schemaDate))
                            {
                                //all good
                                if (result != null) result.parseMessage(ParseResult.ParseResultId.OK);
                            }
                            else if (thisDate.before(schemaDate))
                            {
                                //still acceptable
                                if (result != null) result.parseMessage(ParseResult.ParseResultId.XML_OUT_OF_DATE);
                            }
                            else
                            {
                                //cannot parse future schema
                                if (result != null) result.parseMessage(ParseResult.ParseResultId.PARSER_OUT_OF_DATE);
                                nameSpace = null;
                            }
                        }
                        else
                        {
                            if (result != null) result.parseMessage(ParseResult.ParseResultId.PARSE_FAILED);
                            nameSpace = null;
                        }
                        return;
                    }
                }
                catch (Exception e)
                {
                    if (result != null) result.parseMessage(ParseResult.ParseResultId.PARSE_FAILED);
                    nameSpace = null;
                    e.printStackTrace();
                }
            }
        }
    }

    private void extractActions(Element action) throws Exception
    {
        String name = null;
        NodeList ll = action.getChildNodes();
        TSAction tsAction = new TSAction();
        tsAction.type = action.getAttribute("type");
        tsAction.exclude = "";
        tsAction.style = null;
        for (int j = 0; j < ll.getLength(); j++)
        {
            Node node = ll.item(j);
            if (node.getNodeType() != ELEMENT_NODE)
                continue;
            Element element = (Element) node;
            switch (node.getLocalName())
            {
                case "name":
                    name = getLocalisedString(element);
                    break;
                case "attribute-type":
                    AttributeType attr = new AttributeType(element, this);
                    if (tsAction.attributeTypes == null)
                        tsAction.attributeTypes = new HashMap<>();
                    tsAction.attributeTypes.put(attr.id, attr);
                    break;
                case "transaction":
                    handleTransaction(tsAction, element);
                    break;
                case "exclude":
                    tsAction.exclude = element.getAttribute("selection");
                    break;
                case "view": //localised?
                    tsAction.view = getHTMLContent(element);
                    break;
                case "style":
                    tsAction.style = getHTMLContent(element);
                    break;
                case "input": //required for action only scripts
                    handleInput(element);
                    holdingToken = contracts.keySet().iterator().next(); //first key value
                    break;
                default:
                    System.out.println("Unknown tag while processing Action: " + node.getLocalName());
                    break;
            }
        }

        actions.put(name, tsAction);
    }

    private Element getFirstChildElement(Element e)
    {
        for(Node n=e.getFirstChild(); n!=null; n=n.getNextSibling())
        {
            if (n.getNodeType() == ELEMENT_NODE) return (Element)n;
        }

        return null;
    }

    private void handleInput(Element element) throws Exception
    {
        ContractInfo ci = new ContractInfo();

        for(Node n=element.getFirstChild(); n!=null; n=n.getNextSibling())
        {
            if (n.getNodeType() != ELEMENT_NODE) continue;
            Element tokenType = (Element)n;
            String name = tokenType.getAttribute("name");
            switch (tokenType.getLocalName())
            {
                case "token":
                    Element tokenSpec = getFirstChildElement(tokenType);
                    if (tokenSpec != null)
                    {
                        ci.contractInterface = tokenSpec.getLocalName();
                        switch (tokenSpec.getLocalName())
                        {
                            case "ethereum":
                                String chainIdStr = tokenSpec.getAttribute("network");
                                int chainId = Integer.parseInt(chainIdStr);
                                ci.addresses.put(chainId, new ArrayList<>(Arrays.asList(ci.contractInterface)));
                                contracts.put(name, ci);
                                break;
                            case "contract":
                                handleAddresses(getFirstChildElement(element));
                                break;
                            default:
                                break;
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void handleTransaction(TSAction tsAction, Element element)
    {
        Element tx = getFirstChildElement(element);
        switch (tx.getLocalName())
        {
            case "ethereum":
                tsAction.function = parseFunction(tx, Syntax.IA5String);
                tsAction.function.as = parseAs(tx);
                break;
            default:
                break;
        }
    }

    private void processAttrs(Node n)
    {
        AttributeType attr = new AttributeType((Element) n, this);
        if (attr.bitmask != null || attr.function != null)
        {
            attributeTypes.put(attr.id, attr);
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

    public String getTokenName(int count)
    {
        String value = null;
        switch (count)
        {
            case 1:
                if (names.containsKey("one")) value = names.get("one");
                else value = names.get("");
                break;
            case 2:
                value = names.get("two");
                if (value != null) break; //drop through to 'other' if null.
            default:
                value = names.get("other");
                break;
        }

        if (value == null && names.values().size() > 0)
        {
            value = names.values().iterator().next();
        }

        return value;
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

    private void parseTags(Document xml) throws Exception
    {
        for (Node n = xml.getFirstChild(); n != null; n = n.getNextSibling())
        {
            if (n.getNodeType() != ELEMENT_NODE) continue;
            switch (n.getLocalName())
            {
                case "action": //action only script
                    extractActions((Element)n);
                    break;
                default:
                    extractTags((Element)n);
                    break;
            }
        }
    }

    private void handleGlobalAttributes(Element attributes)
    {
        for (Node n = attributes.getFirstChild(); n != null; n = n.getNextSibling())
        {
            if (n.getNodeType() == ELEMENT_NODE && n.getLocalName().equals("attribute-type"))
            {
                processAttrs(n);
            }
        }
    }

    private void extractNameTag(Element nameTag)
    {
        //deal with plurals
        Node nameNode = getLocalisedNode(nameTag, "plurals");
        if (nameNode != null)
        {
            for (int i = 0; i < nameNode.getChildNodes().getLength(); i++)
            {
                Node node = nameNode.getChildNodes().item(i);
                handleNameNode(node);
            }
        }
        else //no plural
        {
            nameNode = getLocalisedNode(nameTag, "string");
            handleNameNode(nameNode);
        }
    }

    private void handleNameNode(Node node)
    {
        if (node != null && node.getNodeType() == ELEMENT_NODE && node.getLocalName().equals("string"))
        {
            Element element = (Element) node;
            String quantity = element.getAttribute("quantity");
            String name = element.getTextContent();
            if (quantity != null && name != null)
            {
                names.put(quantity, name);
            }
        }
    }

    private void parseOrigins(Element origins)
    {
        for (Node n = origins.getFirstChild(); n != null; n = n.getNextSibling())
        {
            if (n.getNodeType() != ELEMENT_NODE)
                continue;

            Element element = (Element) n;

            switch (element.getLocalName())
            {
                case "ethereum":
                    holdingToken = element.getAttribute("contract");
                    break;
                default:
                    break;
            }
        }
    }

    private void handleAddresses(Element contract)
    {
        NodeList nList = contract.getElementsByTagNameNS(nameSpace, "address");
        ContractInfo info = new ContractInfo();
        String name = contract.getAttribute("name");
        info.contractInterface = contract.getAttribute("interface");
        contracts.put(name, info);

        for (int addrIndex = 0; addrIndex < nList.getLength(); addrIndex++)
        {
            Node node = nList.item(addrIndex);
            if (node.getNodeType() == ELEMENT_NODE)
            {
                Element addressElement = (Element) node;
                String networkStr = addressElement.getAttribute("network");
                int network = 1;
                if (networkStr != null) network = Integer.parseInt(networkStr);
                String address = addressElement.getTextContent().toLowerCase();
                List<String> addresses = info.addresses.get(network);
                if (addresses == null)
                {
                    addresses = new ArrayList<>();
                    info.addresses.put(network, addresses);
                }

                if (!addresses.contains(address))
                {
                    addresses.add(address);
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
                case ELEMENT_NODE:
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

    public void parseField(BigInteger tokenId, NonFungibleToken token, Map<String, FunctionDefinition> functionMappings) {
        for (String key : attributeTypes.keySet()) {
            AttributeType attrtype = attributeTypes.get(key);
            BigInteger val = BigInteger.ZERO;
            try
            {
                if (attrtype.function != null && functionMappings != null)
                {
                    //obtain this value from the token function mappings
                    FunctionDefinition functionDef = functionMappings.get(attrtype.function.method);
                    String result = functionDef.result;
                    System.out.println("Result: " + result);
                    if (attrtype.syntax == Syntax.NumericString)
                    {
                        if (result.startsWith("0x")) result = result.substring(2);
                        val = new BigInteger(result, 16);
                    }
                    token.setAttribute(attrtype.id,
                                       new NonFungibleToken.Attribute(attrtype.id, attrtype.name, val, result));
                }
                else
                {
                    val = tokenId.and(attrtype.bitmask).shiftRight(attrtype.bitshift);
                    token.setAttribute(attrtype.id,
                                       new NonFungibleToken.Attribute(attrtype.id, attrtype.name, val, attrtype.toString(val)));
                }
            }
            catch (Exception e)
            {
                token.setAttribute(attrtype.id,
                                   new NonFungibleToken.Attribute(attrtype.id, attrtype.name, val, "unsupported encoding"));
            }
        }
    }

    private void addFunctionInputs(FunctionDefinition fd, Element eth)
    {
        for(Node n=eth.getFirstChild(); n!=null; n=n.getNextSibling())
        {
            if (n.getNodeType() != ELEMENT_NODE) continue;
            Element input = (Element)n;
            switch (input.getLocalName())
            {
                case "data":
                    processDataInputs(fd, input);
                    break;
                case "to":
                case "value":
                    if (fd.tx == null) fd.tx = new EthereumTransaction();
                    fd.tx.args.put(input.getLocalName(), parseTxTag(input));
                    break;
                default:
                    //future elements
                    break;
            }
        }
    }

    private TokenscriptElement parseTxTag(Element input)
    {
        TokenscriptElement tse = new TokenscriptElement();
        tse.ref = input.getAttribute("ref");
        tse.value = input.getTextContent();

        return tse;
    }

    private void processDataInputs(FunctionDefinition fd, Element input)
    {
        for(Node n=input.getFirstChild(); n!=null; n=n.getNextSibling())
        {
            if (n.getNodeType() != ELEMENT_NODE)
                continue;

            Element inputElement = (Element) n;
            MethodArg arg = new MethodArg();
            arg.parameterType = inputElement.getLocalName();
            arg.element = parseTxTag(inputElement);
            fd.parameters.add(arg);
        }
    }

    public void parseField(BigInteger tokenId, NonFungibleToken token) {
        for (String key : attributeTypes.keySet()) {
            AttributeType attrtype = attributeTypes.get(key);
            BigInteger val = BigInteger.ZERO;
            try
            {
                if (attrtype.function != null)
                {
                    //obtain this from the function return, can't get it here
                    token.setAttribute(attrtype.id,
                                       new NonFungibleToken.Attribute(attrtype.id, attrtype.name, val, "unsupported encoding"));
                }
                else
                {
                    val = tokenId.and(attrtype.bitmask).shiftRight(attrtype.bitshift);
                    token.setAttribute(attrtype.id,
                                       new NonFungibleToken.Attribute(attrtype.id, attrtype.name, val, attrtype.toString(val)));
                }
            }
            catch (UnsupportedEncodingException e)
            {
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
