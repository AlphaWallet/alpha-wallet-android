package com.alphawallet.token.tools;

import static org.w3c.dom.Node.ELEMENT_NODE;

import com.alphawallet.token.entity.ActionModifier;
import com.alphawallet.token.entity.As;
import com.alphawallet.token.entity.AttestationDefinition;
import com.alphawallet.token.entity.AttestationValidation;
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

import org.bouncycastle.jce.interfaces.ECKey;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
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

import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class TokenDefinition
{
    public final Map<String, Attribute> attributes = new HashMap<>();
    protected Locale locale;

    public final Map<String, ContractInfo> contracts = new HashMap<>();
    public final Map<String, AttestationDefinition> attestations = new HashMap<>();
    public final Map<String, TSAction> actions = new HashMap<>();
    private final Map<String, String> labels = new HashMap<>(); // store plural etc for token name
    private final Map<String, NamedType> namedTypeLookup = new HashMap<>(); //used to protect against name collision
    private final TSTokenViewHolder tokenViews = new TSTokenViewHolder();
    private final Map<String, TSSelection> selections = new HashMap<>();
    private final Map<String, TSActivityView> activityCards = new HashMap<>();
    private final Map<String, Element> viewContent = new HashMap<>();

    public String nameSpace;
    public TokenscriptContext context;
    public String holdingToken = null;
    private int actionCount;
    private TSOrigins defaultOrigin = null;

    public static final String TOKENSCRIPT_MINIMUM_SCHEMA = "2020/06";
    public static final String TOKENSCRIPT_CURRENT_SCHEMA = "2024/01";
    public static final String TOKENSCRIPT_ADDRESS = "{TS_ADDRESS}";
    public static final String TOKENSCRIPT_CHAIN = "{TS_CHAIN}";
    public static final String TOKENSCRIPT_REPO_SERVER = "https://repo.tokenscript.org/";
    public static final String TOKENSCRIPT_STORE_SERVER = "https://store-backend.smartlayer.network/tokenscript/" + TOKENSCRIPT_ADDRESS + "/chain/" + TOKENSCRIPT_CHAIN + "/script-uri";
    public static final String TOKENSCRIPT_NAMESPACE = "http://tokenscript.org/" + TOKENSCRIPT_CURRENT_SCHEMA + "/tokenscript";

    private static final String ATTESTATION = "http://attestation.id/ns/tbml";
    private static final String TOKENSCRIPT_BASE_URL = "http://tokenscript.org/";

    public static final String TOKENSCRIPT_ERROR = "<h2 style=\"color:rgba(207, 0, 15, 1);\">TokenScript Error</h2>";
    private static final String LEGACY_WARNING_TEMPLATE = "<html>" + TOKENSCRIPT_ERROR + "<h3>ts:${ERR1} is deprecated.<br/>Use ts:${ERR2}</h3>";

    public static final String UNCHANGED_SCRIPT = "<unchanged>";
    public static final String NO_SCRIPT = "<blank_script>";

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

    public AttestationDefinition getAttestation()
    {
        return attestations.get(holdingToken);
    }

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
        if (function.as == As.Unknown)
        {
            function.namedTypeReturn = resolve.getAttribute("as");
        }
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
            case "uint":
                return As.Unsigned;
            default: // "unsigned"
                return As.Unknown;
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

    //If there's no tokenId input in the call use tokenId 0
    public BigInteger useZeroForTokenIdAgnostic(String attributeName, BigInteger tokenId)
    {
        Attribute attr = attributes.get(attributeName);

        if (!attr.usesTokenId())
        {
            return BigInteger.ZERO;
        }
        else
        {
            return tokenId;
        }
    }

    public List<String> getAttestationIdFields()
    {
        if (attestations.size() > 0)
        {
            return getAttestation().replacementFieldIds;
        }
        else
        {
            return null;
        }
    }

    public List<String> getAttestationCollectionKeys()
    {
        if (attestations.size() > 0)
        {
            return getAttestation().collectionKeys;
        }
        else
        {
            return null;
        }
    }

    public String getAttestationSchemaUID()
    {
        if (getAttestation() != null)
        {
            return getAttestation().schemaUID;
        }
        else
        {
            return "";
        }
    }

    public byte[] getAttestationCollectionPreHash()
    {
        if (getAttestation() != null)
        {
            return getAttestation().getCollectionIdPreHash();
        }
        else
        {
            return null;
        }
    }

    public boolean matchCollection(String attestationCollectionId)
    {
        if (getAttestation() != null)
        {
            return getAttestation().matchCollection(attestationCollectionId);
        }
        else
        {
            return false;
        }
    }

    public void addLocalAttr(Attribute attr)
    {
        tokenViews.localAttributeTypes.put(attr.name, attr); //TODO: Refactor as it appears this doesn't respect scope
    }

    public void addGlobalStyle(Element element)
    {
        tokenViews.globalStyle = getHTMLContent(element); //TODO: Refactor this as it appears global style is located elsewhere. This may have been deprecated
    }

    public boolean isChanged()
    {
        return (nameSpace != null && !nameSpace.equals(UNCHANGED_SCRIPT) && !nameSpace.equals(NO_SCRIPT));
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
                return name.getTextContent().strip();
            }
            else if (langAttr.equals("en")) nonLocalised = name.getTextContent().strip();
        }

        if (nonLocalised != null) return nonLocalised;
        else
        {
            name = (Element) nList.item(0);
            // TODO: catch the indice out of bound exception and throw it again suggesting dev to check schema
            if (name != null) return name.getTextContent().strip();
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
                    return processText(n.getTextContent()).strip();
                }
                else if (nonLocalised == null && (langAttr.equals("") || langAttr.equals("en")))
                {
                    nonLocalised = n.getTextContent().strip();
                }
            }
        }

        return nonLocalised;
    }

    private String processText(String text)
    {
        //strip out whitespace and cr/lf
        return text.strip();
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
                    return thisAttr.getTextContent().strip();
                }
            }
        }

        return "";
    }

    //Empty definition
    public TokenDefinition()
    {
        holdingToken = null;
        nameSpace = NO_SCRIPT;
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
            //DOMSignContext signContext = new DOMSignContext(privateKey, document.getDocumentElement());
            //XMLSignatureFactory ssig;
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
            //scanAttestation(xml);
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
                        TSOrigins origin = parseOrigins(element);
                        if (origin.isType(TSOriginType.Contract) || origin.isType(TSOriginType.Attestation)) holdingToken = origin.getOriginName();
                        defaultOrigin = origin;
                        break;
                    case "contract":
                        handleAddresses(element);
                        break;
                    case "label":
                        labels.putAll(extractLabelTag(element));
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
                    case "attestation":
                        AttestationDefinition attestation = scanAttestation(element);
                        attestations.put(attestation.name, attestation);
                        break;
                    case "Signature":
                        //pull signature info

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
                            selection.denialMessage = (denialNode != null) ? denialNode.getTextContent().strip() : null;
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
                    case "token-card":
                        TSTokenView tv = new TSTokenView(card, this);
                        tokenViews.views.put(tv.getLabel(), tv);
                        break;
                    case "card":
                        extractCard(card);
                        break;
                    case "viewContent":
                        this.viewContent.put(card.getAttribute("name"), card);
                        break;
                }
            }
        }
    }

    public Element getViewContent(String name)
    {
        return this.viewContent.get(name);
    }

    private TSActivityView processActivityView(Element card) throws Exception
    {
        NodeList ll = card.getChildNodes();
        TSActivityView activityView = null;
        String useName = "";

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
                    if (origins.isType(TSOriginType.Event))
                    {
                        activityView = new TSActivityView(origins);
                    }
                    break;
                case "view": //TODO: Localisation
                case "item-view":
                    if (activityView == null)
                    {
                        activityView = new TSActivityView(defaultOrigin);
                    }
                    if (useName.isEmpty())
                    {
                        useName = node.getLocalName();
                    }
                    activityView.addView(useName, new TSTokenView(element, this));
                    break;
                case "label":
                    useName = getLocalisedString(element);
                    break;
                default:
                    throw new SAXException("Unknown tag <" + node.getLocalName() + "> tag in tokens");
            }
        }

        return activityView;
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

    public boolean isSchemaLessThanMinimum()
    {
        if (nameSpace == null)
        {
            return true;
        }

        int dateIndex = nameSpace.indexOf(TOKENSCRIPT_BASE_URL) + TOKENSCRIPT_BASE_URL.length();
        int lastSeparator = nameSpace.lastIndexOf("/");
        if ((lastSeparator - dateIndex) == 7)
        {
            try
            {
                DateFormat format = new SimpleDateFormat("yyyy/MM", Locale.ENGLISH);
                Date thisDate = format.parse(nameSpace.substring(dateIndex, lastSeparator));
                Date schemaDate = format.parse(TOKENSCRIPT_MINIMUM_SCHEMA);

                return thisDate.before(schemaDate);
            }
            catch (Exception e)
            {
                return true;
            }
        }

        return true;
    }

    private void extractCard(Element card) throws Exception
    {
        TSAction action;
        TSActivityView activity;
        String type = card.getAttribute("type");
        switch (type)
        {
            case "token":
                TSTokenView tv = new TSTokenView(card, this);
                tokenViews.views.put(tv.getLabel(), tv);
                break;
            case "action":
            case "activity":
                action = handleAction(card);
                actions.put(action.name, action);
                setModifier(action, card);
                break;
            case "onboarding":
                // do not parse onboarding cards
                break;
            default:
                throw new SAXException("Unexpected card type found: " + type);
        }


    }

    private void setModifier(TSAction action, Element card) throws Exception
    {
        String modifier = card.getAttribute("modifier");
        System.out.println("YOLESS MOD: " + modifier);
        switch (modifier.toLowerCase())
        {
            case "attestation":
                action.modifier = ActionModifier.ATTESTATION;
                break;
            case "none":
            case "":
                action.modifier = ActionModifier.NONE;
                break;
            default:
                throw new SAXException("Unexpected modifier found: " + modifier);
        }

        String type = card.getAttribute("type");
        System.out.println("YOLESS Type: " + type);
        switch (type)
        {
            case "activity":
                action.modifier = ActionModifier.ACTIVITY;
                break;
            case "action":
                action.modifier = ActionModifier.NONE;
                break;
            default:
                if (action.modifier == null)
                {
                    action.modifier = ActionModifier.NONE;
                }
                break;
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
                    tsAction.view = new TSTokenView(element, this);
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
        nList = xml.getElementsByTagName("http://www.w3.org/2000/09/xmldsig#");
        nList = xml.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "KeyName");
        if (nList.getLength() > 0) {
            this.keyName = nList.item(0).getTextContent().strip();
        }
        return; // even if the document is signed, often it doesn't have KeyName
    }

    private AttestationDefinition scanAttestation(Node attestationNode) throws SAXException
    {
        Element element = (Element) attestationNode;
        String name = element.getAttribute("name");
        AttestationDefinition attn = new AttestationDefinition(name);

        for (Node n = attestationNode.getFirstChild(); n != null; n = n.getNextSibling())
        {
            if (n.getNodeType() != ELEMENT_NODE) continue;
            Element attnElement = (Element) n;

            switch (attnElement.getLocalName())
            {
                case "meta":
                    //read elements of the metadata
                    attn.addMetaData(attnElement);
                    break;
                case "display":
                    handleAttestationDisplay(attnElement);
                    break;
                case "eas":
                    ContractInfo info = attn.addAttributes(attnElement);
                    if (info != null)
                    {
                        contracts.put(attn.name, info);
                    }
                    break;
                case "key":
                    attn.handleKey(attnElement);
                    break;
                case "collectionFields":
                    attn.handleCollectionFields(attnElement);
                    break;
                case "idFields":
                    attn.handleReplacementField(attnElement);
                    break;
                case "struct":
                case "ProofOfKnowledge":
                    //attn.members.add(parseAttestationStruct(attnElement));
                    //attestation.add(parseAttestationStruct(attnElement));
                    break;
                case "origins": //TODO: Recode this
                    //attn.origin = parseOrigins(attnElement);
                    //advance to function
                    Element functionElement = getFirstChildElement(attnElement);
                    attn.function = parseFunction(functionElement, Syntax.IA5String);
                    attn.function.as = parseAs(functionElement);
                    break;
            }
        }

        return attn;
    }

    private void handleAttestationDisplay(Element attnElement)
    {

    }

    private List<AttnElement> parseAttestationStructMembers(Node attnStruct)
    {
        //get struct list
        List<AttnElement> attnList = new ArrayList<>();

        for(Node n = attnStruct.getFirstChild(); n!=null; n=n.getNextSibling())
        {
            if (n.getNodeType() != ELEMENT_NODE) continue;
            Element e = (Element) n;
            AttnElement attnE = parseAttestationStruct(e);
            attnList.add(attnE);
        }

        return attnList;
    }

    private String getElementName(Node attribute)
    {
        String name = "";
        if (attribute.hasAttributes())
        {
            for (int i = 0; i < attribute.getAttributes().getLength(); i++)
            {
                Node node = attribute.getAttributes().item(i);
                switch (node.getLocalName())
                {
                    case "name":
                        name = node.getTextContent().strip();
                        break;
                    default:
                        break;
                }
            }
        }

        return name;
    }

    public AttestationValidation getValidation(List<Type> values)
    {
        //legacy attestations should only have one type
        AttestationDefinition attn = null;
        if (attestations.size() > 0)
        {
            attn = (AttestationDefinition)attestations.values().toArray()[0];
        }

        if (attn == null || !namedTypeLookup.containsKey(attn.function.namedTypeReturn))
        {
            return null;
        }

        //get namedType for return
        NamedType nType = namedTypeLookup.get(attn.function.namedTypeReturn);
        AttestationValidation.Builder builder = new AttestationValidation.Builder();

        //find issuerkey
        ContractInfo issuerKey = contracts.get("_IssuerKey");
        builder.issuerKey(issuerKey != null ? issuerKey.getFirstAddress() : null);

        int index = 0;

        for (NamedType.SequenceElement element : nType.sequence)
        {
            //handle magic values plus generic
            switch (element.name)
            {
                case "_issuerValid":
                    builder.issuerValid((Boolean)values.get(index++).getValue());
                    break;
                case "_issuerAddress":
                    builder.issuerAddress((String)values.get(index++).getValue());
                    break;
                case "_subjectAddress":
                    builder.subjectAddress((String)values.get(index++).getValue());
                    break;
                case "_attestationId":
                    builder.attestationId((BigInteger)values.get(index++).getValue());
                    break;
                case "isValid":
                    builder.isValid((Boolean)values.get(index++).getValue());
                    break;
                default:
                    builder.additional(element.name, (Type<?>)values.get(index++).getValue());
                    break;
            }
        }

        return builder.build();
    }

    public List<TypeReference<?>> getAttestationReturnTypes()
    {
        List<TypeReference<?>> returnTypes = new ArrayList<>();
        AttestationDefinition attn = null;
        if (attestations.size() > 0)
        {
            attn = (AttestationDefinition)attestations.values().toArray()[0];
        }

        if (attn == null || !namedTypeLookup.containsKey(attn.function.namedTypeReturn))
        {
            return returnTypes;
        }

        //get namedType for return
        NamedType nType = namedTypeLookup.get(attn.function.namedTypeReturn);

        //add output params
        for (NamedType.SequenceElement element : nType.sequence)
        {
            switch (element.type)
            {
                case "uint":
                case "uint256":
                    returnTypes.add(new TypeReference<Uint256>() {});
                    break;
                case "bytes32":
                    returnTypes.add(new TypeReference<Bytes32>() {});
                    break;
                case "bytes":
                    returnTypes.add(new TypeReference<DynamicBytes>() {});
                    break;
                case "string":
                    returnTypes.add(new TypeReference<Utf8String>() {});
                    break;
                case "address":
                    returnTypes.add(new TypeReference<Address>() {});
                    break;
                case "bool":
                    returnTypes.add(new TypeReference<Bool>() {});
                    break;
                default:
                    break;
            }
        }

        return returnTypes;
    }

    private AttnElement parseAttestationStruct(Node attnElement)
    {
        AttnElement thisElement = new AttnElement();
        thisElement.name = getElementName(attnElement);
        String type = processTypeName(attnElement.getLocalName());
        switch (type)
        {
            case "struct":
                thisElement.type = AttnStructType.STRUCT;
                thisElement.members = parseAttestationStructMembers(attnElement);
                break;
            case "UTF8-String":
                thisElement.type = AttnStructType.UTF8_STRING;
                break;
            case "ASN1-Integer":
                thisElement.type = AttnStructType.ASN1_INTEGER;
                break;
            case "Octet-String":
                thisElement.type = AttnStructType.OCTET_STRING;
                break;
            case "signature":
                thisElement.type = AttnStructType.SIGNATURE;
                break;
            case "DER-Enum":
                thisElement.type = AttnStructType.DER_ENUM;
                break;
            case "SubjectPublicKeyInfo":
                thisElement.type = AttnStructType.SUBJECT_PUBLIC_KEY;
                break;
            case "ProofOfKnowledge":
                thisElement.type = AttnStructType.PROOF_OF_KNOWLEDGE;
                thisElement.members = parseAttestationStructMembers(attnElement);
                break;
            case "timestamp":
                thisElement.type = AttnStructType.TIMESTAMP;
                break;
            case "bytes":
                thisElement.type = AttnStructType.BYTES;
                break;
            case "uint":
                thisElement.type = AttnStructType.UINT;
                break;
            case "string":
                thisElement.type = AttnStructType.STRING;
                break;
            case "address":
                thisElement.type = AttnStructType.ADDRESS;
                break;
            case "bool":
                thisElement.type = AttnStructType.BOOL;
                break;
            default:
                break;
        }

        return thisElement;
    }

    private String processTypeName(String tag)
    {
        if (tag.startsWith("uint") || tag.startsWith("int"))
        {
            return "uint";
        }
        else if (tag.startsWith("bytes"))
        {
            return "bytes";
        }
        else
        {
            return tag;
        }
    }

    private enum AttnStructType
    {
        STRUCT,
        SUBJECT_PUBLIC_KEY,
        PROOF_OF_KNOWLEDGE,
        SIGNATURE,
        UTF8_STRING,
        ASN1_INTEGER,
        OCTET_STRING,
        DER_ENUM,
        DER_OBJECT,
        TIMESTAMP,
        //Ethereum return types
        ADDRESS,
        BYTES,
        STRING,
        UINT,
        BOOL,
    }

    private static class AttnElement
    {
        public String name;
        public String data;
        public AttnStructType type;
        public List<AttnElement> members;
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
            String name = element.getTextContent().strip();
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
                case "attestation":
                    String attestationName = element.getAttribute("name");
                    tsOrigins = new TSOrigins.Builder(TSOriginType.Attestation)
                                    .name(attestationName).build();
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
                            String contractAddress = "";
                            if (contracts.size() > 0) { contractAddress = contracts.keySet().iterator().next(); }
                            throw new SAXException("[" + contractAddress + "] Sequence must be enclosed within <namedType name=... />");
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
        String address = addressElement.getTextContent().toLowerCase().strip();
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
                    String entityRef = child.getTextContent().strip();
                    EntityReference ref = (EntityReference) child;

                    System.out.println(entityRef);
                    break;
                default:
                    if (child != null && child.getTextContent() != null)
                    {
                        String parsed = child.getTextContent().replace("\u2019", "&#x2019;").strip();
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
                sb.append(node.getTextContent().strip());
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
        tse.value = input.getTextContent().strip();
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

        if (tag.equals("view")) return view.getTokenView();
        else if (tag.equals("style")) return view.getStyle();
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

    public TSTokenView getTSTokenView(String name)
    {
        return tokenViews.getTSView(name);
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
