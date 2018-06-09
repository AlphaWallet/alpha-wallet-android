package io.stormbird.token.web;

import io.stormbird.token.tools.TokenDefinition;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import io.stormbird.token.entity.MagicLinkData;
import io.stormbird.token.entity.NonFungibleToken;
import io.stormbird.token.entity.SalesOrderMalformed;
import io.stormbird.token.tools.ParseMagicLink;
import io.stormbird.token.web.Ethereum.TransactionHandler;
import io.stormbird.token.web.Service.CryptoFunctions;


@Controller
@SpringBootApplication
@RequestMapping("/")
public class AppSiteController {

    private static ParseMagicLink parser = new ParseMagicLink();
    private static CryptoFunctions cryptoFunctions = new CryptoFunctions();
    private static TransactionHandler txHandler = new TransactionHandler();
    private static TokenDefinition definitionParser;

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

    @RequestMapping("/")
    public String home(HttpServletRequest request){
        return "index";
    }

    /* TODO: 3 types of instructions.
     * 1) link redeemable, not redeemd and not expired;
     * 2) link not redeemable; 3) iOS instructios */
    @GetMapping(value = "/{UniversalLink}")
    public String handleUniversalLink(@PathVariable("UniversalLink") String universalLink, @RequestHeader("User-Agent") String agent, Model model)
    {
        MagicLinkData data;
        model.addAttribute("base64", universalLink);
        try {
            data = parser.parseUniversalLink(universalLink);
        } catch(SalesOrderMalformed e) {
            return "error";
        }
        parser.getOwnerKey(data);
        model.addAttribute("tokenCount", data.ticketCount);
        model.addAttribute("tokenName", definitionParser.getTokenName());
        model.addAttribute("link", data);
        // model.addAttribute("linkExp");

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
            updateTokenInfo(model, data);
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

    private void updateContractInfo(Model model, String contractAddress) {
        //find out the contract name, symbol and balance
        //have to use blocking gets here
        //TODO: we should be able to update components here instead of waiting
        String contractName = txHandler.getName(contractAddress);
        model.addAttribute("contractName", contractName);
    }

    private void updateTokenInfo(Model model, MagicLinkData data) throws Exception {
        List<BigInteger> balanceArray = txHandler.getBalanceArray(data.ownerAddress, data.contractAddress);

        List<NonFungibleToken> selection = Arrays.stream(data.tickets)
                .mapToObj(i -> balanceArray.get(i))
                .filter(tokenId -> !tokenId.equals(BigInteger.ZERO))
                .map(tokenId -> new NonFungibleToken(tokenId, definitionParser))
                .collect(Collectors.toList());

        for (NonFungibleToken token : selection) {
            String sides = token.getAttribute("countryA").text;
            sides += " - " + token.getAttribute("countryB").text;
            model.addAttribute("ticketSides", sides);
            model.addAttribute("ticketDate", token.getDate("dd MMM HH:mm"));
            model.addAttribute("ticketMatch", token.getAttribute("match").text);
            model.addAttribute("ticketCategory", token.getAttribute("category").text);
            break; // we only need 1 token's info. rest assumed to be the same
        }

        if (selection.size() != data.tickets.length)
            throw new Exception("Some or all non-fungiable tokens are not owned by the claimed owner");
    }

	public static void main(String[] args) throws Exception {
		SpringApplication.run(AppSiteController.class, args);
		parser.setCryptoInterface(cryptoFunctions);
        File file = new File("../contracts/TicketingContract.xml");
        definitionParser = new TokenDefinition(new FileInputStream(file), new Locale("en"));
	}
}
