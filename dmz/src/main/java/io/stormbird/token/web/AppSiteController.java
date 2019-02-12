package io.stormbird.token.web;

import io.stormbird.token.tools.TokenDefinition;
import io.stormbird.token.util.DateTimeFactory;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.stormbird.token.entity.MagicLinkData;
import io.stormbird.token.entity.NonFungibleToken;
import io.stormbird.token.entity.SalesOrderMalformed;
import io.stormbird.token.tools.ParseMagicLink;
import io.stormbird.token.web.Ethereum.TransactionHandler;
import io.stormbird.token.web.Service.CryptoFunctions;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import org.xml.sax.SAXException;

import static io.stormbird.token.tools.Convert.getEthString;
import static io.stormbird.token.tools.Convert.getEthStringSzabo;
import static io.stormbird.token.tools.ParseMagicLink.currencyLink;
import static io.stormbird.token.tools.ParseMagicLink.spawnable;


@Controller
@SpringBootApplication
@RequestMapping("/")
public class AppSiteController {

    private static ParseMagicLink parser = new ParseMagicLink();
    private static CryptoFunctions cryptoFunctions = new CryptoFunctions();
    private static TransactionHandler txHandler = new TransactionHandler();
    private static Map<String, File> addresses;

    @GetMapping(value = "/apple-app-site-association", produces = "application/json")
    @ResponseBody
    public String getAppleDeepLinkConfigure() {
        //return "apple-app-site-association";
        return "{\n" +
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
    }

    @GetMapping("/")
    public RedirectView home(RedirectAttributes attributes){
        return new RedirectView("http://alphawallet.io");
    }

    @GetMapping(value = "/{UniversalLink}")
    public String handleUniversalLink(@PathVariable("UniversalLink") String universalLink, @RequestHeader("User-Agent") String agent, Model model)
            throws IOException, SAXException, NoHandlerFoundException
    {
        MagicLinkData data;
        model.addAttribute("base64", universalLink);
        try
        {
            data = parser.parseUniversalLink(universalLink);
        }
        catch (SalesOrderMalformed e)
        {
            return "error"; // TODO: give nice error
        }
        parser.getOwnerKey(data);

        switch (data.contractType)
        {
            case currencyLink:
                return handleCurrencyLink(data, agent, model);
            case spawnable:
            default:
                return handleTokenLink(data, agent, model);
        }
    }

    private String handleTokenLink(MagicLinkData data, String agent, Model model) throws IOException, SAXException, NoHandlerFoundException
    {
        TokenDefinition definition = null;
        File xml = null;
        for (String address : addresses.keySet()) {
            xml = addresses.get(address);
            // TODO: when xml-schema-v1 is merged, produce a new "default XML" to fill the role of fallback.
            if (address.equals(data.contractAddress)) { // this works as contractAddress is always in lowercase
                break;
            }
        }
        if (xml == null) {
            /* this is impossible to happen, because at least 1 xml should present or main() bails out */
            throw new NoHandlerFoundException("GET", "/" + data.contractAddress, new HttpHeaders());
        }
        try(FileInputStream in = new FileInputStream(xml)) {
            // TODO: give more detail in the error
            // TODO: reflect on this: should the page bail out for contracts with completely no matching XML?
            definition = new TokenDefinition(in, new Locale("en"));
        }

        model.addAttribute("tokenName", definition.getTokenName());
        model.addAttribute("link", data);
        model.addAttribute("linkPrice", getEthString(data.price));

        try {
            updateContractInfo(model, data.contractAddress);
        } catch (Exception e) {
            /* The link points to a non-existing contract - most
	     * likely from a different chainID. Now, if Ethereum node
	     * is offline, this may get triggered too. */
            model.addAttribute("tokenAvailable", "unattainable");
            return "index";
        }

        try {
            updateTokenInfo(model, data, definition);
        } catch (Exception e) {
            /* although contract is okay, we can't getting
	     * tokens. This could be caused by a wrong signature. The
	     * case that the tokens are redeemd is handled inside, not
	     * as an exception */
            model.addAttribute("tokenAvailable", "unavailable");
            return "index";
        }

        if (Calendar.getInstance().getTime().after(new Date(data.expiry*1000))){
            model.addAttribute("tokenAvailable", "expired");
        } else {
            model.addAttribute("tokenAvailable", "available");
        }
        return "index";
    }

    private String handleCurrencyLink(MagicLinkData data, String agent, Model model) throws IOException, SAXException, NoHandlerFoundException
    {
        model.addAttribute("link", data);
        model.addAttribute("linkValue", getEthStringSzabo(data.amount));
        model.addAttribute("title", "XDAI Currency Drop");
        model.addAttribute("currency", "XDai"); //TODO: detect network and fill correct value

        try {
            updateContractInfo(model, data.contractAddress);
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

    private void updateContractInfo(Model model, String contractAddress) {
        //find out the contract name, symbol and balance
        //have to use blocking gets here
        //TODO: we should be able to update components here instead of waiting
        String contractName = txHandler.getName(contractAddress);
        model.addAttribute("contractName", contractName);
    }

    private void updateTokenInfo(Model model, MagicLinkData data, TokenDefinition definition) throws Exception {
        // TODO: use the locale negotiated with user agent (content-negotiation) instead of English
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM HH:mm", Locale.ENGLISH);
        List<BigInteger> balanceArray = txHandler.getBalanceArray(data.ownerAddress, data.contractAddress);

        List<NonFungibleToken> selection = Arrays.stream(data.tickets)
                .mapToObj(i -> balanceArray.get(i))
                .filter(tokenId -> !tokenId.equals(BigInteger.ZERO))
                .map(tokenId -> new NonFungibleToken(tokenId, definition))
                .collect(Collectors.toList());

        for (NonFungibleToken token : selection) {
            String sides = token.getAttribute("countryA").text;
            sides += " - " + token.getAttribute("countryB").text;
            model.addAttribute("ticketSides", sides);
            model.addAttribute("ticketDate",
                               DateTimeFactory.getDateTime(token.getAttribute("time")).format(dateFormat));
            model.addAttribute("ticketMatch", token.getAttribute("match").text);
            model.addAttribute("ticketCategory", token.getAttribute("category").text);
            break; // we only need 1 token's info. rest assumed to be the same
        }

        if (selection.size() != data.tickets.length)
            throw new Exception("Some or all non-fungiable tokens are not owned by the claimed owner");
    }

    private static Path repoDir;

    @Value("${repository.dir}")
    public void setRepoDir(String value) {
        repoDir = Paths.get(value);
    }

    public static void main(String[] args) throws IOException { // TODO: should run System.exit() if IOException
        SpringApplication.run(AppSiteController.class, args);
        parser.setCryptoInterface(cryptoFunctions);

        try (Stream<Path> dirStream = Files.walk(repoDir)) {
            addresses = dirStream.filter(path -> path.toString().toLowerCase().endsWith(".xml"))
                    .filter(Files::isRegularFile)
                    .filter(Files::isReadable)
                    .map(path -> getContractAddresses(path))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toMap(
                            entry -> entry.getKey().toLowerCase(),
                            entry -> new File(entry.getValue())));
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
            addresses.forEach((addr, xml) -> System.out.println(addr + ":" + xml.getPath()));
        }
	}

	private static Set<Map.Entry<String, String>> getContractAddresses(Path path) {
        HashMap<String, String> map = new HashMap<>();
        try (InputStream input = Files.newInputStream(path)) {
            TokenDefinition token = new TokenDefinition(input, new Locale("en"));
            token.addresses.keySet().stream().forEach(address -> map.put(address, path.toString()));
            return map.entrySet();
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e); // make it safe to use in stream
        }
    }

    @GetMapping(value = "/0x{address}", produces = MediaType.TEXT_XML_VALUE) // TODO: use regexp 0x[0-9a-fA-F]{20}
    public @ResponseBody String getContractBehaviour(@PathVariable("address") String address)
            throws IOException, NoHandlerFoundException
    {
        /* TODO: should parse the address, do checksum, store in a byte160 */
        address = "0x" + address.toLowerCase();
        if (addresses.containsKey(address)) {
            File file = addresses.get(address);
            try (FileInputStream in = new FileInputStream(file)) {
                /* TODO: check XML's encoding and serve a charset according to the encoding */
                return IOUtils.toString(in, "utf8");
            }
        } else {
            throw new NoHandlerFoundException("GET", "/" + address, new HttpHeaders());
        }
    }
}
