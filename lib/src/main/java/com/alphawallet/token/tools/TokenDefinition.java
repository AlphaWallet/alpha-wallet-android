package com.alphawallet.token.tools;

import static org.w3c.dom.Node.ELEMENT_NODE;

import com.alphawallet.token.entity.As;
import com.alphawallet.token.entity.Attribute;
import com.alphawallet.token.entity.ContractInfo;
import com.alphawallet.token.entity.EthereumTransaction;
import com.alphawallet.token.entity.EventDefinition;
import com.alphawallet.token.entity.FunctionDefinition;
import com.alphawallet.token.entity.MethodArg;
import com.alphawallet.token.entity.NamedType;
import com.alphawallet.token.entity.NonFungibleToken;
import com.alphawallet.token.entity.ParseResult;
import com.alphawallet.token.entity.TSAction;
import com.alphawallet.token.entity.TSActivityView;
import com.alphawallet.token.entity.TSOriginType;
import com.alphawallet.token.entity.TSOrigins;
import com.alphawallet.token.entity.TSSelection;
import com.alphawallet.token.entity.TSTokenView;
import com.alphawallet.token.entity.TSTokenViewHolder;
import com.alphawallet.token.entity.TokenscriptContext;
import com.alphawallet.token.entity.TokenscriptElement;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class TokenDefinition {
    protected Document xml;
    public final Map<String, Attribute> attributes = new HashMap<>();
    protected Locale locale;

    public final Map<String, ContractInfo> contracts = new HashMap<>();
    public final Map<String, TSAction> actions = new HashMap<>();
    private Map<String, String> labels = new HashMap<>(); // store plural etc for token name
    private final Map<String, NamedType> namedTypeLookup = new HashMap<>(); //used to protect against name collision
    private final TSTokenViewHolder tokenViews = new TSTokenViewHolder();
    private final Map<String, TSSelection> selections = new HashMap<>();
    private final Map<String, TSActivityView> activityCards = new HashMap<>();

    public String nameSpace;
    public TokenscriptContext context;
    public String holdingToken = null;
    private int actionCount;

    public static final String TOKENSCRIPT_CURRENT_SCHEMA = "2020/06";
    public static final String TOKENSCRIPT_REPO_SERVER = "https://repo.tokenscript.org/";
    public static final String TOKENSCRIPT_NAMESPACE = "http://tokenscript.org/" + TOKENSCRIPT_CURRENT_SCHEMA + "/tokenscript";

    private static final String ATTESTATION = "http://attestation.id/ns/tbml";
    private static final String TOKENSCRIPT_BASE_URL = "http://tokenscript.org/";

    public static final String TOKENSCRIPT_ERROR = "<h2 style=\"color:rgba(207, 0, 15, 1);\">TokenScript Error</h2>";
    private static final String LEGACY_WARNING_TEMPLATE = "<html>" + TOKENSCRIPT_ERROR + "<h3>ts:${ERR1} is deprecated.<br/>Use ts:${ERR2}</h3>";

    /* the following are incorrect, waiting to be further improved
     with suitable XML, because none of these String typed class variables
     are going to be one-per-XML-file:

     - each contract <feature> normally should invoke new code modules
       e.g. when a new decentralised protocol is introduced, a new
       class to handle the protocol needs to be introduced, which owns
       it own way of specifying implementation, like marketQueueAPI.

     - tokenName is going to be selectable through filters -
       that is, it's allowed that token labels are different in the
       same asset class. There are use-cases for this.

     - each token definition XML file can incorporate multiple
       contracts, each with different network IDs.

     - each XML file can be signed multiple times, with multiple
       <KeyName>.
    */
    protected String keyName = null;

    public List<FunctionDefinition> getFunctionData()
    {
        List<FunctionDefinition> defs = new ArrayList<>();
        for (Attribute attr : attributes.values())
        {
            if (attr.function != null)
            {
                defs.add(attr.function);
            }
        }

        return defs;
    }

    public Map<String, TSActivityView> getActivityCards() { return activityCards; }

    public EventDefinition parseEvent(Element resolve) throws SAXException
    {
        EventDefinition ev = new EventDefinition();

        for (int i = 0; i < resolve.getAttributes().getLength(); i++)
        {
            Node thisAttr = resolve.getAttributes().item(i);
            String attrValue = thisAttr.getNodeValue();
            switch (thisAttr.getNodeName())
            {
                case "contract":
                    ev.contract = contracts.get(attrValue);
                    break;
                case "type":
                    ev.type = namedTypeLookup.get(attrValue);
                    if (ev.type == null)
                    {
                        throw new SAXException("Event module not found: " + attrValue);
                    }
                    break;
                case "filter":
                    ev.filter = attrValue;
                    break;
                case "select":
                    ev.select = attrValue;
                    break;
            }
        }

        return ev;
    }

    public FunctionDefinition parseFunction(Element resolve, Syntax syntax)
    {
        FunctionDefinition function = new FunctionDefinition();
        String contract = resolve.getAttribute("contract");
        function.contract = contracts.get(contract);
        if (function.contract == null)
        {
            function.contract = contracts.get(holdingToken);
        }
        function.method = resolve.getAttribute("function");
        function.as = parseAs(resolve);
        addFunctionInputs(function, resolve);
        function.syntax = syntax;
        return function;
    }

    public As parseAs(Element resolve)
    {
        switch(resolve.getAttribute("as").toLowerCase()) {
            case "signed":
                return As.Signed;
            case "string":
            case "utf8":
            case "": //no type specified, return string
                return As.UTF8;
            case "bytes":
                return As.Bytes;
            case "e18":
                return As.e18;
            case "e8":
                return As.e8;
            case "e6":
                return As.e6;
            case "e4":
                return As.e4;
            case "e3":
                return As.e3;
            case "e2":
                return As.e2;
            case "bool":
                return As.Boolean;
            case "mapping":
                return As.Mapping;
            case "address":
                return As.Address;
            default: // "unsigned"
                return As.Unsigned;
        }
    }

    public EventDefinition getEventDefinition(String activityName)
    {
        if (getActivityCards().size() > 0)
        {
            TSActivityView v = getActivityCards().get(activityName);
            if (v != null)
            {
                return getActivityEvent(activityName);
            }
        }

        return null;
    }

    public EventDefinition getActivityEvent(String activityCardName)
    {
        TSActivityView av = activityCards.get(activityCardName);
        EventDefinition ev = new EventDefinition();
        ev.contract = contracts.get(holdingToken);
        ev.filter = av.getActivityFilter();
        ev.type = namedTypeLookup.get(av.getEventName());
        ev.activityName = activityCardName;
        ev.parentAttribute = null;
        ev.select = null;
        return ev;
    }

    public boolean hasEvents()
    {
        for (String attrName : attributes.keySet())
        {
            Attribute attr = attributes.get(attrName);
            if (attr.event != null && attr.event.contract != null)
            {
                return true;
            }
        }

        if (getActivityCards().size() > 0)
        {
            return true;
        }

        return false;
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

    public String getLocalisedString(Element container) {
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

    public TokenDefinition(InputStream xmlAsset, Locale locale, ParseResult result) throws IllegalArgumentException, IOException, SAXException {
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
        actionCount = 0;

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
                        TSOrigins origin = parseOrigins(element); //parseOrigins(element);
                        if (origin.isType(TSOriginType.Contract)) holdingToken = origin.getOriginName();
                        break;
                    case "contract":
                        handleAddresses(element);
                        break;
                    case "label":
                        labels = extractLabelTag(element);
                        break;
                    case "selection":
                        TSSelection selection = parseSelection(element);
                        if (selection != null && selection.checkParse())
                            selections.put(selection.name, selection);
                        break;
                    case "module":
                        handleModule(element, null);
                        break;
                    case "cards":
                        handleCards(element);
                        break;
                    case "attribute":
                        Attribute attr = new Attribute((Element) n, this);
                        if (attr.bitmask != null || attr.function != null)
                        {
                            attributes.put(attr.name, attr);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private TSSelection parseSelection(Element node) throws SAXException
    {
        String name = "";
        TSSelection selection = null;
        for (int i = 0; i < node.getAttributes().getLength(); i++)
        {
            Node thisAttr = node.getAttributes().item(i);
            switch (thisAttr.getLocalName())
            {
                case "name":
                case "id":
                    name = thisAttr.getNodeValue();
                    break;
                case "filter":
                    selection = new TSSelection(thisAttr.getNodeValue());
                    break;
            }
        }

        if (selection != null)
        {
            selection.name = name;
            for (Node n = node.getFirstChild(); n != null; n = n.getNextSibling())
            {
                if (n.getNodeType() == ELEMENT_NODE)
                {
                    Element element = (Element) n;
                    switch (element.getLocalName())
                    {
                        case "name":
                            selection.names = extractLabelTag(element);
                            break;
                        case "denial":
                            Node denialNode = getLocalisedNode(element, "string");
                            selection.denialMessage = (denialNode != null) ? denialNode.getTextContent() : null;
                            break;
                    }
                }
            }
        }

        return selection;
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
                    case "token":
                        processTokenCardElements(card);
                        break;
                    case "card":
                        extractCard(card);
                        break;
                }
            }
        }
    }

    private TSActivityView processActivityView(Element card) throws Exception
    {
        NodeList ll = card.getChildNodes();
        TSActivityView activityView = null;

        for (int j = 0; j < ll.getLength(); j++)
        {
            Node node = ll.item(j);
            if (node.getNodeType() != ELEMENT_NODE)
                continue;

            Element element = (Element) node;
            switch (node.getLocalName())
            {
                case "origins":
                    TSOrigins origins = parseOrigins(element);
                    if (origins.isType(TSOriginType.Event)) activityView = new TSActivityView(origins);
                    break;
                case "view": //TODO: Localisation
                case "item-view":
                    if (activityView == null) throw new SAXException("Activity card declared without origins tag");
                    activityView.addView(node.getLocalName(), new TSTokenView(element));
                    break;
                default:
                    throw new SAXException("Unknown tag <" + node.getLocalName() + "> tag in tokens");
            }
        }

        return activityView;
    }

    private void processTokenCardElements(Element card) throws Exception
    {
        NodeList ll = card.getChildNodes();

        for (int j = 0; j < ll.getLength(); j++)
        {
            Node node = ll.item(j);
            if (node.getNodeType() != ELEMENT_NODE)
                continue;

            Element element = (Element) node;
            switch (node.getLocalName())
            {
                case "attribute":
                    Attribute attr = new Attribute(element, this);
                    tokenViews.localAttributeTypes.put(attr.name, attr);
                    break;
                case "view": //TODO: Localisation
                case "item-view":
                    TSTokenView v = new TSTokenView(element);
                    tokenViews.views.put(node.getLocalName(), v);
                    break;
                case "view-iconified":
                    throw new SAXException("Deprecated <view-iconified> used in <ts:token>. Replace with <item-view>");
                case "style":
                    tokenViews.globalStyle = getHTMLContent(element);
                    break;
                case "script":
                    //misplaced script tag
                    throw new SAXException("Misplaced <script> tag in <ts:token>");
                default:
                    throw new SAXException("Unknown tag <" + node.getLocalName() + "> tag in tokens");
            }
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

    private void extractCard(Element card) throws Exception
    {
        String type = card.getAttribute("type");
        switch (type)
        {
            case "token":
                processTokenCardElements(card);
                break;
            case "action":
                TSAction action = handleAction(card);
                actions.put(action.name, action);
                break;
            case "activity":
                TSActivityView activity = processActivityView(card);
                activityCards.put(card.getAttribute("name"), activity);
                break;
            default:
                throw new SAXException("Unexpected card type found: " + type);
        }
    }

    private TSAction handleAction(Element action) throws Exception
    {
        NodeList ll = action.getChildNodes();
        TSAction tsAction = new TSAction();
        tsAction.order = actionCount;
        tsAction.exclude = action.getAttribute("exclude");
        actionCount++;
        for (int j = 0; j < ll.getLength(); j++)
        {
            Node node = ll.item(j);
            if (node.getNodeType() != ELEMENT_NODE)
                continue;

            if (node.getPrefix() != null && node.getPrefix().equalsIgnoreCase("ds"))
                continue;

            Element element = (Element) node;
            switch (node.getLocalName())
            {
                case "label":
                    tsAction.name = getLocalisedString(element);
                    break;
                case "attribute":
                    Attribute attr = new Attribute(element, this);
                    if (tsAction.attributes == null) {
                        tsAction.attributes = new HashMap<>();
                    }
                    tsAction.attributes.put(attr.name, attr);
                    break;
                case "transaction":
                    handleTransaction(tsAction, element);
                    break;
                case "exclude":
                    tsAction.exclude = element.getAttribute("selection");
                    break;
                case "selection":
                    throw new SAXException("<ts:selection> tag must be in main scope (eg same as <ts:origins>)");
                case "view": //localised?
                    tsAction.view = new TSTokenView(element);
                    break;
                case "style":
                    tsAction.style = getHTMLContent(element);
                    break;
                case "input": //required for action only scripts
                    handleInput(element);
                    holdingToken = contracts.keySet().iterator().next(); //first key value
                    break;
                case "output":
                    //TODO: Not yet handled.
                    break;
                case "script":
                    //misplaced script tag
                    throw new SAXException("Misplaced <script> tag in Action '" + tsAction.name + "'");
                default:
                    throw new SAXException("Unknown tag <" + node.getLocalName() + "> tag in Action '" + tsAction.name + "'");
            }
        }

        return tsAction;
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
        for(Node n=element.getFirstChild(); n!=null; n=n.getNextSibling())
        {
            if (n.getNodeType() != ELEMENT_NODE) continue;
            Element tokenType = (Element)n;
            String label = tokenType.getAttribute("label");
            switch (tokenType.getLocalName())
            {
                case "token":
                    Element tokenSpec = getFirstChildElement(tokenType);
                    if (tokenSpec != null)
                    {
                        switch (tokenSpec.getLocalName())
                        {
                            case "ethereum":
                                String chainIdStr = tokenSpec.getAttribute("network");
                                long chainId = Long.parseLong(chainIdStr);
                                ContractInfo ci = new ContractInfo(tokenSpec.getLocalName());
                                ci.addresses.put(chainId, new ArrayList<>(Arrays.asList(ci.contractInterface)));
                                contracts.put(label, ci);
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
            case "transaction":
                if (tx.getPrefix().equals("ethereum"))
                {
                    tsAction.function = parseFunction(tx, Syntax.IA5String);
                    tsAction.function.as = parseAs(tx);
                }
                break;
            default:
                break;
        }
    }

    private void processAttrs(Node n) throws SAXException
    {
        Attribute attr = new Attribute((Element) n, this);
        if (attr.bitmask != null || attr.function != null)
        {
            attributes.put(attr.name, attr);
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

    public String getTokenNameList()
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String labelKey : labels.keySet())
        {
            if (!first) sb.append(",");
            sb.append(labelKey).append(",").append(labels.get(labelKey));
            first = false;
        }

        return sb.toString();
    }

    public String getTokenName(int count)
    {
        String value = null;
        switch (count)
        {
            case 1:
                if (labels.containsKey("one")) value = labels.get("one");
                else value = labels.get("");
                break;
            case 2:
                value = labels.get("two");
                if (value != null) break; //drop through to 'other' if null.
            default:
                value = labels.get("other");
                break;
        }

        if (value == null && labels.values().size() > 0)
        {
            value = labels.values().iterator().next();
        }

        return value;
    }

    public Map<BigInteger, String> getMappingMembersByKey(String key){
        if(attributes.containsKey(key)) {
            Attribute attr = attributes.get(key);
            return attr.members;
        }
        return null;
    }
    public Map<BigInteger, String> getConvertedMappingMembersByKey(String key){
        if(attributes.containsKey(key)) {
            Map<BigInteger,String> convertedMembers=new HashMap<>();
            Attribute attr = attributes.get(key);
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
                case "card": //action only script
                    TSAction action = handleAction((Element)n);
                    actions.put(action.name, action);
                    break;
                default:
                    extractTags((Element)n);
                    break;
            }
        }
    }

    private Map<String, String> extractLabelTag(Element labelTag)
    {
        Map<String, String> localNames = new HashMap<>();
        //deal with plurals
        Node nameNode = getLocalisedNode(labelTag, "plurals");
        if (nameNode != null)
        {
            for (int i = 0; i < nameNode.getChildNodes().getLength(); i++)
            {
                Node node = nameNode.getChildNodes().item(i);
                handleNameNode(localNames, node);
            }
        }
        else //no plural
        {
            nameNode = getLocalisedNode(labelTag, "string");
            handleNameNode(localNames, nameNode);
        }

        return localNames;
    }

    private void handleNameNode(Map<String, String> localNames, Node node)
    {
        if (node != null && node.getNodeType() == ELEMENT_NODE && node.getLocalName().equals("string"))
        {
            Element element = (Element) node;
            String quantity = element.getAttribute("quantity");
            String name = element.getTextContent();
            if (quantity != null && name != null)
            {
                localNames.put(quantity, name);
            }
        }
    }

    private TSOrigins parseOrigins(Element origins) throws SAXException
    {
        TSOrigins tsOrigins = null;
        for (Node n = origins.getFirstChild(); n != null; n = n.getNextSibling())
        {
            if (n.getNodeType() != ELEMENT_NODE)
                continue;

            Element element = (Element) n;

            switch (element.getLocalName())
            {
                case "ethereum":
                    String contract = element.getAttribute("contract");
                    tsOrigins = new TSOrigins.Builder(TSOriginType.Contract)
                                    .name(contract).build();
                    break;
                case "event":
                    EventDefinition ev = parseEvent(element);
                    ev.contract = contracts.get(holdingToken);
                    tsOrigins = new TSOrigins.Builder(TSOriginType.Event)
                                    .name(ev.type.name)
                                    .event(ev).build();
                    break;
                default:
                    throw new SAXException("Unknown Origin Type: '" + element.getLocalName() + "'" );
            }
        }

        return tsOrigins;
    }

    private void handleAddresses(Element contract) throws Exception
    {
        ContractInfo info = new ContractInfo(contract.getAttribute("interface"));
        String name = contract.getAttribute("name");
        contracts.put(name, info);

        for (Node n = contract.getFirstChild(); n != null; n = n.getNextSibling())
        {
            if (n.getNodeType() == ELEMENT_NODE)
            {
                Element element = (Element) n;
                switch (element.getLocalName())
                {
                    case "address":
                        handleAddress(element, info);
                        break;
                    case "module":
                        handleModule(element, null);
                        break;
                }
            }
        }
    }

    private void handleModule(Node module, String namedType) throws SAXException
    {
        for (Node n = module.getFirstChild(); n != null; n = n.getNextSibling())
        {
            if (n.getNodeType() == ELEMENT_NODE)
            {
                Element element = (Element)n;
                switch (n.getNodeName())
                {
                    case "namedType":
                        namedType = element.getAttribute("name");
                        if (namedType.length() == 0)
                        {
                            throw new SAXException("namedType must have name attribute.");
                        }
                        else if (namedTypeLookup.containsKey(namedType))
                        {
                            throw new SAXException("Duplicate Module label: " + namedType);
                        }
                        handleModule(element, namedType);
                        break;
                    case "type":
                        if (namedType == null) throw new SAXException("type sequence must have name attribute.");
                        handleModule(element, namedType);
                        break;
                    case "sequence":
                        if (namedType == null) {
                            throw new SAXException("Sequence must be enclosed within <namedType name=... />");
                        }
                        NamedType eventDataType = handleElementSequence(element, namedType);
                        namedTypeLookup.put(namedType, eventDataType);
                        namedType = null;
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private NamedType handleElementSequence(Node c, String moduleName) throws SAXException
    {
        NamedType module = new NamedType(moduleName);
        for (Node n = c.getFirstChild(); n != null; n = n.getNextSibling())
        {
            if (n.getNodeType() == ELEMENT_NODE)
            {
                Element element = (Element) n;
                module.addSequenceElement(element, moduleName);
            }
        }

        return module;
    }

    private void handleAddress(Element addressElement, ContractInfo info)
    {
        String networkStr = addressElement.getAttribute("network");
        long network = 1;
        if (networkStr != null) network = Long.parseLong(networkStr);
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
        for (String key : attributes.keySet()) {
            Attribute attrtype = attributes.get(key);
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
                    token.setAttribute(attrtype.name,
                                       new NonFungibleToken.Attribute(attrtype.name, attrtype.label, val, result));
                }
                else
                {
                    val = tokenId.and(attrtype.bitmask).shiftRight(attrtype.bitshift);
                    token.setAttribute(attrtype.name,
                                       new NonFungibleToken.Attribute(attrtype.name, attrtype.label, val, attrtype.toString(val)));
                }
            }
            catch (Exception e)
            {
                token.setAttribute(attrtype.name,
                                   new NonFungibleToken.Attribute(attrtype.name, attrtype.label, val, "unsupported encoding"));
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
        tse.localRef = input.getAttribute("local-ref");
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
        for (String key : attributes.keySet()) {
            Attribute attrtype = attributes.get(key);
            BigInteger val = BigInteger.ZERO;
            try
            {
                if (attrtype.function != null)
                {
                    //obtain this from the function return, can't get it here
                    token.setAttribute(attrtype.name,
                                       new NonFungibleToken.Attribute(attrtype.name, attrtype.label, val, "unsupported encoding"));
                }
                else
                {
                    val = tokenId.and(attrtype.bitmask).shiftRight(attrtype.bitshift);
                    token.setAttribute(attrtype.name,
                                       new NonFungibleToken.Attribute(attrtype.name, attrtype.label, val, attrtype.toString(val)));
                }
            }
            catch (UnsupportedEncodingException e)
            {
                token.setAttribute(attrtype.name,
                                   new NonFungibleToken.Attribute(attrtype.name, attrtype.label, val, "unsupported encoding"));
            }
        }
    }

    /**
     * Legacy interface for AppSiteController
     * Check for 'cards' attribute set
     * @param tag
     * @return
     */
    public String getCardData(String tag)
    {
        TSTokenView view = tokenViews.views.get("view");

        if (tag.equals("view")) return view.tokenView;
        else if (tag.equals("style")) return view.style;
        else return null;
    }

    public boolean hasTokenView()
    {
        return tokenViews.views.size() > 0;
    }

    public String getViews()
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String s : tokenViews.views.keySet())
        {
            if (!first) sb.append(",");
            sb.append(s);
            first = false;
        }

        return sb.toString();
    }

    public String getTokenView(String viewTag)
    {
        return tokenViews.getView(viewTag);
    }

    public String getTokenViewStyle(String viewTag)
    {
        return tokenViews.getViewStyle(viewTag);
    }

    public Map<String, Attribute> getTokenViewLocalAttributes()
    {
        return tokenViews.localAttributeTypes;
    }

    public Map<String, TSAction> getActions()
    {
        return actions;
    }

    public TSSelection getSelection(String id)
    {
        return selections.get(id);
    }
}
