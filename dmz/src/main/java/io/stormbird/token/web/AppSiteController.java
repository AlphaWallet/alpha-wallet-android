package io.stormbird.token.web;

import io.stormbird.token.entity.*;
import io.stormbird.token.tools.TokenDefinition;
import io.stormbird.token.web.Ethereum.TokenscriptFunction;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.stormbird.token.tools.ParseMagicLink;
import io.stormbird.token.web.Ethereum.TransactionHandler;
import io.stormbird.token.web.Service.CryptoFunctions;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import org.xml.sax.SAXException;
import javax.servlet.http.HttpServletRequest;
import static io.stormbird.token.tools.Convert.getEthString;
import static io.stormbird.token.tools.Convert.getEthStringSzabo;
import static io.stormbird.token.tools.ParseMagicLink.currencyLink;

@Controller
@SpringBootApplication
@RequestMapping("/")
public class AppSiteController implements AttributeInterface
{
    private static CryptoFunctions cryptoFunctions = new CryptoFunctions();
    private static Map<Integer, Map<String, File>> addresses;
    private static Map<Integer, Map<String, Map<BigInteger, CachedResult>>> transactionResults = new ConcurrentHashMap<>();  //optimisation results
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
            @RequestHeader("User-Agent") String agent,
            Model model,
            HttpServletRequest request
    )
            throws IOException, SAXException, NoHandlerFoundException
    {
        String domain = request.getServerName();
        ParseMagicLink parser = new ParseMagicLink(cryptoFunctions);
        MagicLinkData data;
        model.addAttribute("base64", universalLink);

        try
        {
            data = parser.parseUniversalLink(universalLink);
            data.chainId = MagicLinkInfo.getNetworkIdFromDomain(domain);

            if (domain.contains("duckdns.org") || domain.contains("192.168"))
            {
                data.chainId = 1;
            }
        }
        catch (SalesOrderMalformed e)
        {
            return "error: " + e;
        }
        parser.getOwnerKey(data);
        switch (data.contractType)
        {
            case currencyLink:
                return handleCurrencyLink(data, agent, model);
            default:
                return handleTokenLink(data, agent, model, universalLink);
        }
    }

    private String handleTokenLink(
            MagicLinkData data,
            String agent,
            Model model,
            String universalLink
    ) throws IOException, SAXException, NoHandlerFoundException
    {
        TokenDefinition definition = getTokenDefinition(data.chainId, data.contractAddress);

        //get attributes
        BigInteger firstTokenId = BigInteger.ZERO;

        try
        {
            updateTokenInfo(model, data, definition);
        }
        catch (Exception e)
        {
            /* although contract is okay, we can't getting
             * tokens. This could be caused by a wrong signature. The
             * case that the tokens are redeemd is handled inside, not
             * as an exception */
            model.addAttribute("tokenAvailable", "unavailable");
            return passThroughToken(data, universalLink);
            //return "index";
        }

        if (data.tokenIds != null && data.tokenIds.size() > 0)
            firstTokenId = data.tokenIds.get(0);
        System.out.println(firstTokenId.toString(16));
        ContractAddress cAddr = new ContractAddress(data.chainId, data.contractAddress);
        StringBuilder tokenData = new StringBuilder();
        TransactionHandler txHandler = new TransactionHandler(data.chainId);

        String tokenName = txHandler.getNameOnly(data.contractAddress);
        String symbol = txHandler.getSymbolOnly(data.contractAddress);
        String nameWithSymbol = tokenName + "(" + symbol + ")";

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

        tokenscriptFunction.resolveAttributes(firstTokenId, this, cAddr, definition)
                .forEach(attr -> TokenScriptResult.addPair(tokenData, attr.id, attr.text))
                .isDisposed();

        String available = "available";
        if (Calendar.getInstance().getTime().after(new Date(data.expiry*1000))) available = "expired";

        String price = getEthString(data.price) + " " + MagicLinkInfo.getNetworkNameById(data.chainId);

        String title = data.ticketCount + " " + definition.getTokenName(data.ticketCount) + " " + available;

        String view = definition.getCardData("view");
        String style = definition.getCardData("style");
        String initHTML = loadFile("templates/tokenscriptTemplate.html");

        String scriptData = loadFile("templates/token_inject.js.tokenscript");
        String tokenView = String.format(scriptData,
                                         tokenData.toString(), view);

        String expiry = new java.util.Date(data.expiry*1000).toString();

        String availableUntil = "<span title=\"Unix Time is " + data.expiry + "\">" + expiry + "</span>";
        String action = "\"" + universalLink + "\"";
        String originalLink = "\"https://" + MagicLinkInfo.getMagicLinkDomainFromNetworkId(data.chainId) + "/" + universalLink + "\"";

        String etherscanAccountLink = MagicLinkInfo.getEtherscanURLbyNetwork(data.chainId) + "address/" + data.ownerAddress;
        String etherscanTokenLink = MagicLinkInfo.getEtherscanURLbyNetwork(data.chainId) + "token/" + data.contractAddress;

        return String.format(initHTML,
                                        title, style, String.valueOf(data.ticketCount), nameWithSymbol, definition.getTokenName(data.ticketCount),
                                        price, available,
                                        data.ticketCount, definition.getTokenName(data.ticketCount),
                                        tokenView, availableUntil,
                                        action, originalLink,
                                        etherscanAccountLink, data.ownerAddress,
                                        etherscanTokenLink, data.contractAddress
                                        );
    }

    private String passThroughToken(MagicLinkData data, String universalLink)
    {
        //construct passthrough html
        dsfdsf
    }

    private String handleCurrencyLink(
            MagicLinkData data,
            String agent,
            Model model
    )
    {
        String networkName = MagicLinkInfo.getNetworkNameById(data.chainId);
        model.addAttribute("link", data);
        model.addAttribute("linkValue", getEthStringSzabo(data.amount));
        model.addAttribute("title", networkName + " Currency Drop");
        model.addAttribute("currency", networkName);
        model.addAttribute("domain", MagicLinkInfo.getMagicLinkDomainFromNetworkId(data.chainId));

        try {
            updateContractInfo(model, data);
        } catch (Exception e) {
            /* The link points to a non-existing contract - most
             * likely from a different chainID. Now, if Ethereum node
             * is offline, this may get triggered too. */
            model.addAttribute("tokenAvailable", "unattainable");
            return "currency";
        }

        if (Calendar.getInstance().getTime().after(new Date(data.expiry*1000))){
            model.addAttribute("tokenAvailable", "expired");
        } else {
            model.addAttribute("tokenAvailable", "available");
        }
        return "currency";
    }

    private TokenDefinition getTokenDefinition(int chainId, String contractAddress) throws IOException, SAXException, NoHandlerFoundException
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

    private void updateContractInfo(Model model, MagicLinkData data) {
        //find out the contract name, symbol and balance
        //have to use blocking gets here
        //TODO: we should be able to update components here instead of waiting
        TransactionHandler txHandler = new TransactionHandler(data.chainId);
        String contractName = txHandler.getName(data.contractAddress);
        model.addAttribute("contractName", contractName);
    }

    /**
     * Check ownership of tokens: Ensure that all tokens are still owned by the party selling the tokens
     * @param model
     * @param data
     * @param definition
     * @throws Exception
     */
    private void updateTokenInfo(
            Model model,
            MagicLinkData data,
            TokenDefinition definition
    ) throws Exception {
        model.addAttribute("domain", MagicLinkInfo.getMagicLinkDomainFromNetworkId(data.chainId));
        TransactionHandler txHandler = new TransactionHandler(data.chainId);
        List<BigInteger> balanceArray = txHandler.getBalanceArray(data.ownerAddress, data.contractAddress);
        data.tokenIds = new ArrayList<>();

        List<NonFungibleToken> selection = Arrays.stream(data.tickets)
                .mapToObj(i -> balanceArray.get(i))
                .filter(tokenId -> !tokenId.equals(BigInteger.ZERO))
                .map(tokenId -> {
                    data.tokenIds.add(tokenId);
                    return new NonFungibleToken(tokenId, definition);
                })
                .collect(Collectors.toList());

        if (selection.size() != data.tickets.length)
            throw new Exception("Some or all non-fungible tokens are not owned by the claimed owner");
    }

    private static Path repoDir;

    @Value("${repository.dir}")
    public void setRepoDir(String value) {
        repoDir = Paths.get(value);
    }

    public static void main(String[] args) throws IOException { // TODO: should run System.exit() if IOException
        addresses = new HashMap<Integer, Map<String, File>>();
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
	}

    private static void addContractAddresses(Path path) {
        try (InputStream input = Files.newInputStream(path)) {
            TokenDefinition token = new TokenDefinition(input, new Locale("en"), null);
            ContractInfo holdingContracts = token.contracts.get(token.holdingToken);
            if (holdingContracts != null)
                holdingContracts.addresses.keySet().stream().forEach(network -> addContractsToNetwork(network, networkAddresses(holdingContracts.addresses.get(network), path.toString()))); //map.put(network, networkAddresses(holdingContracts.addresses.get(network), path.toString())));
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e); // make it safe to use in stream
        }
    }

    private static void addContractsToNetwork(Integer network, Map<String, File> newTokenDescriptionAddresses)
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

    @GetMapping(value = "/0x{address}", produces = MediaType.TEXT_XML_VALUE) // TODO: use regexp 0x[0-9a-fA-F]{20}
    public @ResponseBody String getContractBehaviour(@PathVariable("address") String address)
            throws IOException, NoHandlerFoundException
    {
        StringBuilder tokenDefinitionList = new StringBuilder();
        /* TODO: should parse the address, do checksum, store in a byte160 */
        address = "0x" + address.toLowerCase();
        //find all potential hits
        for (int networkId : addresses.keySet())
        {
            if (addresses.get(networkId).containsKey(address))
            {
                File file = addresses.get(networkId).get(address);
                try (FileInputStream in = new FileInputStream(file)) {
                    /* TODO: check XML's encoding and serve a charset according to the encoding */
                    tokenDefinitionList.append("Network: ").append(MagicLinkInfo.getNetworkNameById(networkId));
                    tokenDefinitionList.append(IOUtils.toString(in, "utf8"));
                    tokenDefinitionList.append("-----------------------");
                }
            }
        }

        if (tokenDefinitionList.length() == 0)
        {
            throw new NoHandlerFoundException("GET", "/" + address, new HttpHeaders());
        }
        else
        {
            return tokenDefinitionList.toString();
        }
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
    public TransactionResult getFunctionResult(ContractAddress contract, AttributeType attr, BigInteger tokenId)
    {
        String addressFunctionKey = contract.address + "-" + attr.id;
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
    public TransactionResult storeAuxData(TransactionResult tResult)
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
    public boolean resolveOptimisedAttr(ContractAddress contract, AttributeType attr, TransactionResult transactionResult)
    {
        return false;
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
}
