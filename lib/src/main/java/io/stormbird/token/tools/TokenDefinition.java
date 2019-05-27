package io.stormbird.token.tools;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.internal.operators.observable.ObservableError;
import io.stormbird.token.entity.*;
import org.w3c.dom.*;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
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

    public void populateNetworks(Map<Integer, Map<String, TokenDefinition>> assets, Map<Integer, List<String>> devOverrideContracts)
    {
        for (String name : contracts.keySet())
        {
            ContractInfo info = contracts.get(name);
            for (int network : info.addresses.keySet())
            {
                Map<String, TokenDefinition> definitionMapping = assets.get(network);
                if (definitionMapping == null) //there is a cool shortcut when at API24+
                {
                    definitionMapping = new HashMap<>();
                    assets.put(network, definitionMapping);
                }

                List<String> addresses = info.addresses.get(network);
                for (String address : addresses)
                {
                    if (devOverrideContracts == null || !devOverrideContracts.containsKey(network) || !devOverrideContracts.get(network).contains(address))
                    {
                        definitionMapping.put(address, this);
                    }
                }
            }
        }
    }

    public boolean hasNetwork(int networkId)
    {
        for (String name : contracts.keySet())
        {
            ContractInfo info = contracts.get(name);
            if (info.addresses.containsKey(networkId)) return true;
        }

        return false;
    }

    public boolean hasContracts()
    {
        return contracts.size() > 0;
    }

    public void addToOverrides(Map<Integer, List<String>> devOverrideContracts)
    {
        for (String name : contracts.keySet())
        {
            ContractInfo info = contracts.get(name);
            for (int network : info.addresses.keySet())
            {
                for (String address : info.addresses.get(network))
                {
                    List<String> contracts = devOverrideContracts.get(network);
                    if (contracts == null)
                    {
                        contracts = new ArrayList<>();
                        devOverrideContracts.put(network, contracts);
                    }

                    if (!contracts.contains(address))
                        contracts.add(address);
                }
            }
        }
    }

    private static final String ATTESTATION = "http://attestation.id/ns/tbml";
    private static final String TOKENSCRIPT = "http://tokenscript.org/2019/05/tokenscript";
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

    public boolean hasEthFunctions()
    {
        for (AttributeType attr : attributeTypes.values())
        {
            if (attr.function != null) return true;
        }

        return false;
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
                return TokenDefinition.As.Signed;
            case "utf8":
            case "": //no type specified, return string
                return TokenDefinition.As.UTF8;
            case "bool":
                return TokenDefinition.As.Boolean;
            case "mapping":
                return TokenDefinition.As.Mapping;
            default: // "unsigned"
                return TokenDefinition.As.Unsigned;
        }
    }

    public enum Syntax {
        DirectoryString, IA5String, Integer, GeneralizedTime,
        Boolean, BitString, CountryString, JPEG, NumericString
    }

    public enum As {  // always assume big endian
        UTF8, Unsigned, Signed, Mapping, Boolean, UnsignedInput
    }

    /* for many occurance of the same tag, return the text content of the one in user's current language */
    // FIXME: this function will break if there are nested <tagName> in the nameContainer
    public String getLocalisedString(Element nameContainer, String tagName) {
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
        if (nList.getLength() == 0) nList = nameContainer.getElementsByTagName(tagName);
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
                else if (langAttr.equals(""))
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
                case ELEMENT_NODE:
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

        if (nList.getLength() == 0 || !nameSpace.equals(TOKENSCRIPT))
        {
            System.out.println("Legacy XML format - no longer supported");
            return;
        }

        try
        {
            extractContracts(xml);
            parseTags(xml);
            extractNameTag(xml);
            checkGlobalAttributes(xml);
            extractFeatureTag(xml);
            extractSignedInfo(xml);
            extractCards(xml);
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

    private void extractCards(Document xml) throws Exception
    {
        NodeList nList = xml.getElementsByTagNameNS(nameSpace, "cards");
        if (nList.getLength() == 0) return;
        Element cards = (Element) nList.item(0);
        nList = cards.getElementsByTagNameNS(nameSpace, "token-card");
        if (nList.getLength() == 0) return;
        Element viewRoot = (Element)nList.item(0);
        Map<String, String> attributeSet = new HashMap<>();
        addToHTMLSet(attributeSet, viewRoot, "style");
        addToHTMLSet(attributeSet, viewRoot,"view-iconified");
        addToHTMLSet(attributeSet, viewRoot,"view");

        if (attributeSet.size() > 0)
        {
            attributeSets.put("cards", attributeSet);
        }

        extractActions(cards);
    }

    private void extractActions(Element cards) throws Exception
    {
        NodeList nList = cards.getElementsByTagNameNS(nameSpace, "action");
        if (nList.getLength() == 0) return;
        String name = null;
        for (int i = 0; i < nList.getLength(); i++)
        {
            Element action = (Element) nList.item(i);
            NodeList ll = action.getChildNodes();
            TSAction tsAction = new TSAction();
            tsAction.type = action.getAttribute("type");
            tsAction.exclude = "";
            tsAction.style = null;
            for (int j = 0; j < ll.getLength(); j++)
            {
                Node node = ll.item(j);
                switch (node.getNodeType())
                {
                    case ELEMENT_NODE:
                        Element element = (Element)node;
                        switch (node.getLocalName())
                        {
                            case "name":
                                System.out.println(node.getLocalName());
                                name = getLocalisedString(element);
                                break;
                            case "attribute-type":
                                AttributeType attr = new AttributeType(element, this);
                                if (tsAction.attributeTypes == null) tsAction.attributeTypes = new HashMap<>();
                                tsAction.attributeTypes.put(attr.id, attr);
                                break;
                            case "transaction":
                                System.out.println(node.getLocalName());
                                handleTransaction(tsAction, element);
                                break;
                            case "exclude":
                                System.out.println(node.getLocalName());
                                tsAction.exclude = element.getAttribute("selection");
                                break;
                            case "view":
                                System.out.println(node.getLocalName());
                                tsAction.view = getHTMLContent(element);
                                break;
                            case "style":
                                System.out.println(node.getLocalName());
                                tsAction.style = getHTMLContent(element);
                                break;
                        }
                        break;
                    default:
                        break;
                }
            }

            actions.put(name, tsAction);
        }
    }

    private void handleTransaction(TSAction tsAction, Element element)
    {
        for(Node node=element.getFirstChild(); node!=null; node=node.getNextSibling())
        {
            if (node.getNodeType() == ELEMENT_NODE)
            {
                Element tx = (Element) node;
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
        }
    }

    private void addToHTMLSet(Map<String, String> attributeSet, Element root, String tagName)
    {
        Node view = getLocalisedNode(root, tagName);
        if (view != null)
        {
            String iconifiedContent = getHTMLContent(view);
            attributeSet.put(tagName, iconifiedContent);
        }
    }

    private void checkGlobalAttributes(Document xml)
    {
        NodeList nList = xml.getElementsByTagNameNS(nameSpace, "attribute-types");
        if (nList.getLength() == 0) return;
        Node attributeTypes = nList.item(0);
        for (int j = 0; j < attributeTypes.getChildNodes().getLength(); j++)
        {
            Node n = attributeTypes.getChildNodes().item(j);
            if (n.getNodeType() == ELEMENT_NODE && n.getLocalName().equals("attribute-type"))
            {
                processAttrs(n);
            }
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

    public String getFeemasterAPI()
    {
        return feemasterAPI;
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

        if (value == null)
        {
            for (String v : names.values()) //pick first value
            {
                value = v;
                break;
            }
        }

        return value;
    }

    /**
     * This is only for legacy lookup, remove once safe to do so (see importTokenViewModel)
     * Safe to do so = no more handling of legacy magiclinks.
     *
     * @param contractAddress
     * @return
     */
    public int getNetworkFromContract(String contractAddress)
    {
        for (String contractName : contracts.keySet())
        {
            ContractInfo info = contracts.get(contractName);
            for (int network : info.addresses.keySet())
            {
                if (info.addresses.get(network).equals(contractAddress))
                {
                    return network;
                }
            }
        }

        return -1;
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

    private void extractNameTag(Document xml)
    {
        NodeList nList = xml.getElementsByTagNameNS(nameSpace, "name");
        if (nList.getLength() == 0) return;

        Element contract = (Element) nList.item(0);

        //deal with plurals
        Node nameNode = getLocalisedNode(contract, "plurals");
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
            nameNode = getLocalisedNode(contract, "string");
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

    private void extractContracts(Document xml)
    {
        NodeList nList = xml.getElementsByTagNameNS(nameSpace, "contract");
        for (int i = 0; i < nList.getLength(); i++)
        {
            Node n = nList.item(i);
            if (n.getNodeType() == ELEMENT_NODE)
            {
                handleAddresses((Element) n);
            }
        }
    }

    private void parseTags(Document xml)
    {
        for (Node n = xml.getFirstChild(); n != null; n = n.getNextSibling())
        {
            switch (n.getNodeType())
            {
                case ELEMENT_NODE:
                    extractTags((Element)n);
                    break;
                default:
                    break;
            }
        }
    }

    private void extractTags(Element token)
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
                    default:
                        break;
                }
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
        NodeList nList = eth.getElementsByTagNameNS(nameSpace, "data");
        if (nList.getLength() == 0) return;
        Element inputs = (Element) nList.item(0);

        for(Node input=inputs.getFirstChild(); input!=null; input=input.getNextSibling())
        {
            if (input.getNodeType() == ELEMENT_NODE)
            {
                Element inputElement = (Element) input;
                MethodArg arg = new MethodArg();
                arg.parameterType = input.getLocalName();
                arg.ref = inputElement.getAttribute("ref");
                arg.value = inputElement.getTextContent();
                fd.parameters.add(arg);
            }
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


    //Generic methods for resolving attributes
    //need a method to simply 'fetch' an attribute
//    public Single<TokenScriptResult.Attribute> getAttribute(String attribute, BigInteger tokenId, ContractAddress cAddr, AttributeInterface attrIf)
//    {
//
//    }


    public Observable<TokenScriptResult.Attribute> fetchAttrResult(String attribute, BigInteger tokenId, ContractAddress cAddr, AttributeInterface attrIf)
    {
        AttributeType attr = attributeTypes.get(attribute);
        if (attr == null) return Observable.fromCallable(() -> null);
        if (attr.function == null)  // static attribute from tokenId (eg city mapping from tokenId)
        {
            return staticAttribute(attr, tokenId);
        }
        else
        {
            TransactionResult transactionResult = attrIf.getFunctionResult(cAddr, attr, tokenId);
            if (attrIf.resolveOptimisedAttr(cAddr, attr, transactionResult) || !transactionResult.needsUpdating()) //can we use wallet's known data or cached value?
            {
                return resultFromDatabase(transactionResult, attr);
            }
            else  //if value is old or there wasn't any previous value
            {
                //for function query, never need wallet address
                return attr.function.fetchResultFromEthereum(cAddr, attr, tokenId, this)          // Fetch function result from blockchain
                        .map(result -> restoreFromDBIfRequired(result, transactionResult))  // If network unavailable restore value from cache
                        .map(attrIf::storeAuxData)                                          // store new data
                        .map(result -> attr.function.parseFunctionResult(result, attr));    // write returned data into attribute
            }
        }
    }

    public Observable<TokenScriptResult.Attribute> resolveAttributes(BigInteger tokenId, AttributeInterface attrIf, ContractAddress cAddr)
    {
        context = new TokenscriptContext();
        context.cAddr = cAddr;
        context.attrInterface = attrIf;

        return Observable.fromIterable(new ArrayList<>(attributeTypes.values()))
                .flatMap(attr -> fetchAttrResult(attr.id, tokenId, cAddr, attrIf));
    }

    private Observable<TokenScriptResult.Attribute> staticAttribute(AttributeType attr, BigInteger tokenId)
    {
        return Observable.fromCallable(() -> {
            try
            {
                BigInteger val = tokenId.and(attr.bitmask).shiftRight(attr.bitshift);
                return new TokenScriptResult.Attribute(attr.id, attr.name, val, attr.toString(val));
            }
            catch (Exception e)
            {
                return new TokenScriptResult.Attribute(attr.id, attr.name, tokenId, "unsupported encoding");
            }
        });
    }

    private Observable<TokenScriptResult.Attribute> resultFromDatabase(TransactionResult transactionResult, AttributeType attr)
    {
        return Observable.fromCallable(() -> attr.function.parseFunctionResult(transactionResult, attr));
    }

    /**
     * Restore result from Database if required (eg connection failure), and if there was a database value to restore
     * @param result
     * @param transactionResult
     * @return
     */
    private TransactionResult restoreFromDBIfRequired(TransactionResult result, TransactionResult transactionResult)
    {
        if (result.resultTime == 0 && transactionResult != null)
        {
            result.result = transactionResult.result;
            result.resultTime = transactionResult.resultTime;
        }

        return result;
    }
}
