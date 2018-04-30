package io.stormbird.token.web;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import io.stormbird.token.entity.MagicLinkData;
import io.stormbird.token.entity.SalesOrderMalformed;
import io.stormbird.token.tools.ParseMagicLink;
import io.stormbird.token.web.Service.CryptoFunctions;


@Controller
@SpringBootApplication
@RequestMapping("/")
public class AppSiteController {

    /**
     *     <p th:text="'Contract Address: ' + ${contractAddress} + " />
     <p th:text="'Eth Value of order: ' + ${ethValue} + " />
     <p th:text="'Owner Address: ' + ${ownerAddress} + " />
     <p th:text="'Number of tickets: ' + ${ticketCount} + " />
     */
    private static ParseMagicLink parser = new ParseMagicLink();
    private static CryptoFunctions cryptoFunctions = new CryptoFunctions();

    @GetMapping(value = "/{magicLink}")
    public String getBird(@PathVariable("magicLink") String magicLink, Model model)
    {
        model.addAttribute("base64", magicLink);
        try
        {
            MagicLinkData data = parser.parseUniversalLink(magicLink);
            parser.getOwnerKey(data);
            model.addAttribute("contractAddress", data.contractAddress);
            model.addAttribute("ethValue", data.price);
            model.addAttribute("ownerAddress", data.ownerAddress);
            model.addAttribute("ticketCount", data.ticketCount);
        }
        catch (SalesOrderMalformed e)
        {
            e.printStackTrace();
        }
        return "index";
    }

    @RequestMapping("/")
    public String home(HttpServletRequest request){
        return "index";
    }


	public static void main(String[] args) throws Exception {
		SpringApplication.run(AppSiteController.class, args);
		parser.setCryptoInterface(cryptoFunctions);
	}


	//1. get ticket from URL  <---
    //2. interpret URL extension
    //  - Move SalesOrder code to library <--
    //  - get values out of the order <--
    //  - Update tests
    //3. Add web3j
    //4. Init web3j and get balance
    //5. Display results
}
