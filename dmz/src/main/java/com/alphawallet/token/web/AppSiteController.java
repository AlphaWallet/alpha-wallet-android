package com.alphawallet.token.web;

import com.alphawallet.token.entity.Attribute;
import com.github.cliftonlabs.json_simple.JsonObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;

import com.alphawallet.token.entity.AttributeInterface;
import com.alphawallet.token.entity.ContractAddress;
import com.alphawallet.token.entity.ContractInfo;
import com.alphawallet.token.entity.MagicLinkData;
import com.alphawallet.token.entity.MagicLinkInfo;
import com.alphawallet.token.entity.NonFungibleToken;
import com.alphawallet.token.entity.SalesOrderMalformed;
import com.alphawallet.token.entity.XMLDsigVerificationResult;
import com.alphawallet.token.entity.TokenScriptResult;
import com.alphawallet.token.entity.TransactionResult;
import com.alphawallet.token.tools.ParseMagicLink;
import com.alphawallet.token.tools.TokenDefinition;
import com.alphawallet.token.tools.XMLDSigVerifier;
import com.alphawallet.token.web.Ethereum.TokenscriptFunction;
import com.alphawallet.token.web.Ethereum.TransactionHandler;
import com.alphawallet.token.web.Service.CryptoFunctions;
import static com.alphawallet.token.tools.Convert.getEthString;
import static com.alphawallet.token.tools.ParseMagicLink.normal;
import static com.alphawallet.token.web.Ethereum.TokenscriptFunction.ZERO_ADDRESS;

@Controller
@SpringBootApplication
@RequestMapping("/")
public class AppSiteController implements AttributeInterface
{
    private static CryptoFunctions cryptoFunctions = new CryptoFunctions();
    private static Map<Long, Map<String, File>> addresses;
    private static Map<Long, Map<String, Map<BigInteger, CachedResult>>> transactionResults = new ConcurrentHashMap<>();  //optimisation results
    private static final String appleAssociationConfig = "{\n" +
            "  \"applinks\": {\n" +
            "    \"apps\": [],\n" +
            "    \"details\": [\n" +
            "      {\n" +
            "        \"appID\": \"LRAW5PL536.com.stormbird.alphawallet\",\n" +
            "        \"paths\": [ \"*\" ]\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";
    private final MagicLinkData magicLinkData = new MagicLinkData();
    private final TokenscriptFunction tokenscriptFunction = new TokenscriptFunction() { };
    private static Path repoDir;
    private static String infuraKey = "da3717f25f824cc1baa32d812386d93f";

    @GetMapping(value = "/apple-app-site-association", produces = "application/json")
    @ResponseBody
    public String getAppleDeepLinkConfigure() {
        return appleAssociationConfig;
    }

    @GetMapping("/")
    public RedirectView home(RedirectAttributes attributes){
        return new RedirectView("http://alphawallet.com");
    }

    @GetMapping(value = "/{UniversalLink}")
    public @ResponseBody String handleUniversalLink(
            @PathVariable("UniversalLink") String universalLink,
            Model model,
            HttpServletRequest request
    )
            throws IOException, SAXException, NoHandlerFoundException
    {
        String domain = request.getServerName();
        ParseMagicLink parser = new ParseMagicLink(cryptoFunctions, null);
        MagicLinkData data;
        model.addAttribute("base64", universalLink);

        try
        {
            data = parser.parseUniversalLink(universalLink);
            data.chainId = MagicLinkInfo.getNetworkIdFromDomain(domain);
            model.addAttribute("domain", MagicLinkInfo.getMagicLinkDomainFromNetworkId(data.chainId));
        }
        catch (SalesOrderMalformed e)
        {
            return "error: " + e;
        }
        parser.getOwnerKey(data);
        return handleTokenLink(data, universalLink);
    }

    private String handleTokenLink(MagicLinkData data, String universalLink
    ) throws IOException, SAXException, NoHandlerFoundException
    {
        TokenDefinition definition = getTokenDefinition(data.chainId, data.contractAddress);

        if (definition == null)
        {
            return renderTokenWithoutTokenScript(data, universalLink);
        }
        String available = "available";
        try
        {
            if(data.contractType == normal)
            {
                checkTokensOwnedByMagicLinkCreator(data, definition);
            }
            else
            {
                checkTokensClaimableSpawnable(data);
            }
        }
        catch (Exception e)
        {
            //if the tokens are not available, an exception will be thrown and therefore the tokens are not available
            available = "unavailable";
        }

        //get attributes
        BigInteger firstTokenId = BigInteger.ZERO;

        if (data.tokenIds != null && data.tokenIds.size() > 0)
        {
            firstTokenId = data.tokenIds.get(0);
        }
        System.out.println(firstTokenId.toString(16));
        ContractAddress cAddr = new ContractAddress(data.chainId, data.contractAddress);
        StringBuilder tokenData = new StringBuilder();
        TransactionHandler txHandler = new TransactionHandler(data.chainId);

        String tokenName = txHandler.getNameOnly(data.contractAddress);
        String symbol = txHandler.getSymbolOnly(data.contractAddress);

        try
        {
            TokenScriptResult.addPair(tokenData, "name", tokenName);
            TokenScriptResult.addPair(tokenData, "symbol", symbol);
            TokenScriptResult.addPair(tokenData, "_count", String.valueOf(data.ticketCount));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        tokenscriptFunction.resolveAttributes(ZERO_ADDRESS, firstTokenId, this, cAddr, definition)
                .forEach(attr -> TokenScriptResult.addPair(tokenData, attr.id, attr.text))
                .isDisposed();

        if (Calendar.getInstance().getTime().after(new Date(data.expiry*1000)))
        {
            available = "expired";
        }

        String view = definition.getCardData("view");
        String style = definition.getCardData("style");

        String scriptData = loadFile("templates/token_inject.js.tokenscript");
        String tokenView = String.format(scriptData, tokenData.toString(), view);

        return formWebPage(txHandler, data, universalLink, available, style, tokenView);
    }

    private String renderTokenWithoutTokenScript(MagicLinkData data, String universalLink)
    {
        TransactionHandler txHandler = new TransactionHandler(data.chainId);
        String available = "available";

        if (Calendar.getInstance().getTime().after(new Date(data.expiry*1000)))
        {
            available = "expired";
        }
        try
        {
            if(data.contractType == normal)
            {
                checkTokensOwnedByMagicLinkCreator(data);
            }
            else
            {
                checkTokensClaimableSpawnable(data);
            }
        }
        catch (Exception e)
        {
            //if exception is thrown, we assume it is not available due to balance call failing to match
            available = "unavailable";
        }

        return formWebPage(txHandler, data, universalLink, available, "", "");
    }

    private String formWebPage(
            TransactionHandler txHandler,
            MagicLinkData data,
            String universalLink,
            String available,
            String style,
            String tokenView
    )
    {
        String tokenName = txHandler.getName(data.contractAddress);
        String symbol = txHandler.getSymbolOnly(data.contractAddress);
        String nameWithSymbol = tokenName + "(" + symbol + ")";

        String price = getEthString(data.price) + " " + MagicLinkInfo.getNetworkNameById(data.chainId);

        String title = data.ticketCount + " Tokens " + available;

        String initHTML = loadFile("templates/tokenscriptTemplate.html");

        String expiry = new java.util.Date(data.expiry * 1000).toString();

        String availableUntil = "<span title=\"Unix Time is " + data.expiry + "\">" + expiry + "</span>";
        String action = "\"" + universalLink + "\"";
        String originalLink = "\"https://" + MagicLinkInfo.getMagicLinkDomainFromNetworkId(data.chainId) + "/" + universalLink + "\"";

        String etherscanAccountLink = MagicLinkInfo.getEtherscanURLbyNetwork(data.chainId) + "address/" + data.ownerAddress;
        String etherscanTokenLink = MagicLinkInfo.getEtherscanURLbyNetwork(data.chainId) + "address/" + data.contractAddress;

        return String.format(
                initHTML,
                title,
                style,
                String.valueOf(data.ticketCount),
                nameWithSymbol,
                "",
                price,
                available,
                data.ticketCount,
                tokenName,
                tokenView,
                availableUntil,
                action,
                originalLink,
                etherscanAccountLink,
                data.ownerAddress,
                etherscanTokenLink,
                data.contractAddress
        );
    }

    private TokenDefinition getTokenDefinition(long chainId, String contractAddress) throws IOException, SAXException, NoHandlerFoundException
    {
        File xml = null;
        TokenDefinition definition = null;
        if (addresses.containsKey(chainId) && addresses.get(chainId).containsKey(contractAddress))
        {
            xml = addresses.get(chainId).get(contractAddress);
            if (xml == null) {
                /* this is impossible to happen, because at least 1 xml should present or main() bails out */
                throw new NoHandlerFoundException("GET", "/" + contractAddress, new HttpHeaders());
            }
            try(FileInputStream in = new FileInputStream(xml)) {
                // TODO: give more detail in the error
                // TODO: reflect on this: should the page bail out for contracts with completely no matching XML?
                definition = new TokenDefinition(in, new Locale("en"), null);
            }
        }
        return definition;
    }

    private void checkTokensClaimableSpawnable(MagicLinkData data) throws Exception {
        TransactionHandler txHandler = new TransactionHandler(data.chainId);
        //TODO replace with real admin(s) addresses in production
        if(data.ownerAddress.equalsIgnoreCase("0xEdd6D7ba0FF9f4bC501a12529cb736CA76A4fe7e") ||
                data.ownerAddress.equalsIgnoreCase("0x453aABe984b62eE28382c99A6d20447f7776b1fa"))
        {
            //check that token ids are not owned by someone
            for(BigInteger token: data.tokenIds)
            {
                if(!txHandler.getOwnerOf721(data.contractAddress, token).equals(""))
                {
                    throw new Exception("Token(s) already owned");
                }
            }
        }
        else
        {
            List<BigInteger> balance = txHandler.getBalanceArray721Tickets(data.ownerAddress, data.contractAddress);
            if(!balance.containsAll(data.tokenIds))
            {
                throw new Exception("Token(s) not owned by magic link creator");
            }
        }
    }

    /**
     * Check ownership of tokens: Ensure that all tokens are still owned by the party selling the tokens
     * @param data
     * @throws Exception
     */
    private void checkTokensOwnedByMagicLinkCreator(MagicLinkData data, TokenDefinition definition) throws Exception
    {
        TransactionHandler txHandler = new TransactionHandler(data.chainId);
        List<BigInteger> balanceArray = txHandler.getBalanceArray(data.ownerAddress, data.contractAddress);
        data.tokenIds = new ArrayList<>();

        List<NonFungibleToken> selection = Arrays.stream(data.indices)
                .mapToObj(i -> balanceArray.get(i))
                .filter(tokenId -> !tokenId.equals(BigInteger.ZERO))
                .map(tokenId -> {
                    data.tokenIds.add(tokenId);
                    return new NonFungibleToken(tokenId, definition);
                })
                .collect(Collectors.toList());

        if (selection.size() != data.indices.length)
        {
            throw new Exception("Some or all non-fungible tokens are not owned by the claimed owner");
        }
    }

    //For if there is no TokenScript
    private void checkTokensOwnedByMagicLinkCreator(MagicLinkData data) throws Exception
    {
        TransactionHandler txHandler = new TransactionHandler(data.chainId);
        List<BigInteger> balanceArray = txHandler.getBalanceArray(data.ownerAddress, data.contractAddress);
        data.tokenIds = new ArrayList<>();

        List<BigInteger> selection = Arrays.stream(data.indices)
                .mapToObj(i -> balanceArray.get(i))
                .filter(tokenId -> !tokenId.equals(BigInteger.ZERO))
                .collect(Collectors.toList());

        if (selection.size() != data.indices.length)
        {
            throw new Exception("Some or all non-fungible tokens are not owned by the claimed owner");
        }
    }

    @Value("${repository.dir}")
    public void setRepoDir(String value) {
        repoDir = Paths.get(value);
    }

    public static void main(String[] args) throws IOException { // TODO: should run System.exit() if IOException
        addresses = new HashMap<Long, Map<String, File>>();
        SpringApplication.run(AppSiteController.class, args);
        try (Stream<Path> dirStream = Files.walk(repoDir)) {
            dirStream.filter(path -> path.toString().toLowerCase().endsWith(".tsml"))
                    .filter(Files::isRegularFile)
                    .filter(Files::isReadable)
                    .forEach(AppSiteController::addContractAddresses);

            assert addresses != null : "Can't read all XML files";
        } catch (NoSuchFileException e) {
            System.err.println("repository.dir property is defined with a non-existing dir: " + repoDir.toString());
            System.err.println("Please edit your local copy of application.properties, or");
            System.err.println("try run with --repository.dir=/dir/to/repo");
            System.exit(255);
        } catch (AssertionError e) {
            System.err.println("Can't read all the XML files in repository.dir: " + repoDir.toString());
            System.exit(254);
        }

        if (addresses.size() == 0) { // if no XML file is found
            // the server still can run and wait for someone to dump an XML, but let's assume it's a mistake
            System.err.println("No valid contract XML found in " + repoDir.toString() + ", cowardly not continuing.");
            System.exit(253);
        } else {
            // the list should be reprinted whenever a new file is added.
            System.out.println("Serving an XML repo with the following contracts:");
            addresses.forEach((chainId, addrMap) -> {
                System.out.println("Network ID: " + MagicLinkInfo.getNetworkNameById(chainId) + "(" + chainId + ")");
                addrMap.forEach((addr, xml) -> {
                    System.out.println(addr + ":" + xml.getPath());
                });
                System.out.println(" ------------");
            });
        }

        loadInfuraKey();
	}

    private static void addContractAddresses(Path path) {
        try (InputStream input = Files.newInputStream(path)) {
            TokenDefinition token = new TokenDefinition(input, new Locale("en"), null);
            ContractInfo holdingContracts = token.contracts.get(token.holdingToken);
            if (holdingContracts != null)
                holdingContracts.addresses.keySet().stream().forEach(network -> addContractsToNetwork(network, networkAddresses(holdingContracts.addresses.get(network), path.toString())));
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e); // make it safe to use in stream
        }
    }

    private static void addContractsToNetwork(Long network, Map<String, File> newTokenDescriptionAddresses)
    {
        Map<String, File> existingDefinitions = addresses.get(network);
        if (existingDefinitions == null) existingDefinitions = new HashMap<>();

        addresses.put(network, Stream.concat(existingDefinitions.entrySet().stream(), newTokenDescriptionAddresses.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (value1, value2) -> new File(value2.getAbsolutePath())
                         )
                ));
    }

    private static Map<String, File> networkAddresses(List<String> strings, String path)
    {
        Map<String, File> addrMap = new HashMap<>();
        strings.forEach(address -> addrMap.put(address, new File(path)));
        return addrMap;
    }

    private String loadFile(String fileName) {
        byte[] buffer = new byte[0];
        try {
            InputStream in = getClass()
                    .getClassLoader().getResourceAsStream(fileName);
            buffer = new byte[in.available()];
            int len = in.read(buffer);
            if (len < 1) {
                throw new IOException("Nothing is read.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new String(buffer);
    }

    //These functions are for caching and restoring results for optimsation.
    //TODO: rather than a simple time invalidation method, periodically scan transactions for token contracts which have entries in this mapping
    //      if any of those contracts has had a transaction written to it, then refresh all the cached entries
    //      once events are available we can selectively update entries.

    @Override
    public TransactionResult getFunctionResult(ContractAddress contract, Attribute attr, BigInteger tokenId)
    {
        String addressFunctionKey = contract.address + "-" + attr.name;
        TransactionResult tr = new TransactionResult(contract.chainId, contract.address, tokenId, attr);
        //existing entry in map?
        if (transactionResults.containsKey(contract.chainId))
        {
            Map<BigInteger, CachedResult> contractResult = transactionResults.get(contract.chainId).get(addressFunctionKey);
            if (contractResult != null && contractResult.containsKey(tokenId))
            {
                tr.resultTime = contractResult.get(tokenId).resultTime;
                tr.result = contractResult.get(tokenId).result;
            }
        }

        return tr;
    }

    @Override
    public TransactionResult storeAuxData(String wAddress, TransactionResult tResult)
    {
        String addressFunctionKey = tResult.contractAddress + "-" + tResult.attrId;
        if (!transactionResults.containsKey(tResult.contractChainId)) transactionResults.put(tResult.contractChainId, new HashMap<>());
        if (!transactionResults.get(tResult.contractChainId).containsKey(addressFunctionKey)) transactionResults.get(tResult.contractChainId).put(addressFunctionKey, new HashMap<>());
        Map<BigInteger, CachedResult> tokenResultMap = transactionResults.get(tResult.contractChainId).get(addressFunctionKey);
        tokenResultMap.put(tResult.tokenId, new CachedResult(tResult.resultTime, tResult.result));
        transactionResults.get(tResult.contractChainId).put(addressFunctionKey, tokenResultMap);

        return tResult;
    }

    //Not relevant for website - this function is to access wallet internal balance for tokens
    @Override
    public boolean resolveOptimisedAttr(ContractAddress contract, Attribute attr, TransactionResult transactionResult)
    {
        return false;
    }

    @Override
    public String getWalletAddr()
    {
        return ZERO_ADDRESS;
    }

    /**
     * Can ditch this class once we have the transaction optimisation working as detailed in the "TO-DO" above
     */
    private class CachedResult
    {
        long resultTime;
        String result;

        CachedResult(long time, String r)
        {
            resultTime = time;
            result = r;
        }
    }

    /* usage: (Weiwu documented after Sangalli's implementation)

    1) for a test file whose root certificate isn't in the trusted CA list:

    $ curl -F 'file=@lib/src/test/ts/EntryToken.tsml' localhost:8080/api/v1/verifyXMLDSig
    {"result":"fail","failureReason":"Path does not chain with any of the trust anchors"}

    2) for a test file which has valid certificates:

    $ curl -F 'file=@lib/src/test/ts/DAI.tsml' localhost:8080/api/v1/verifyXMLDSig
    {"result":"pass","subject":"CN=*.aw.app","keyName":"","keyType":"SHA256withRSA","issuer":"CN=Let's Encrypt Authority X3,O=Let's Encrypt,C=US"}

    Client be-aware! Please handle return code 404 gracefully. It's content look like this:

    {
    "timestamp": "2019-07-04T08:43:32.885+0000",
    "status": 404,
    "error": "Not Found",
    "message": "API v1 is no longer supported. Upgrade your software.",
    "path": "/api/v1/verifyXMLDSig"
    }

    This message likely signify that version 1 of the api is no longer supported, i.e. the client is too old.
    You can simply display the message to the end user (despite it's not multi-lingual).
     */
    @PostMapping("/api/v1/verifyXMLDSig")
    @ResponseBody
    @SuppressWarnings("unchecked")
    public ResponseEntity<String> validateSSLCertificate(@RequestParam("file") MultipartFile file) throws IOException {
        HttpStatus status = HttpStatus.ACCEPTED;
        JsonObject result = new JsonObject();
        XMLDsigVerificationResult XMLDsigVerificationResult = new XMLDSigVerifier().VerifyXMLDSig(file.getInputStream());
        if (XMLDsigVerificationResult.isValid)
        {
            result.put("result", "pass");
            result.put("issuer", XMLDsigVerificationResult.issuerPrincipal);
            result.put("subject", XMLDsigVerificationResult.subjectPrincipal);
            result.put("keyName", XMLDsigVerificationResult.keyName);
            result.put("keyType", XMLDsigVerificationResult.keyType);
        }
        else
        {
            result.put("result", "fail");
            result.put("failureReason", XMLDsigVerificationResult.failureReason);
            status = HttpStatus.BAD_REQUEST;
        }
        return new ResponseEntity<String>(result.toJson(), status);
    }

    private static void loadInfuraKey()
    {
        try (InputStream input = new FileInputStream("../gradle.properties")) {

            Properties prop = new Properties();

            if (input == null) {
                return;
            }

            //load a properties file from class path, inside static method
            prop.load(input);

            //get the property value and print it out
            infuraKey = prop.getProperty("infuraAPI").replaceAll("\"", "");

        } catch (IOException ex) {
            System.out.println("Locate gradle.properties");
            ex.printStackTrace();
        }
    }

    public static String getInfuraKey()
    {
        return infuraKey;
    }
}