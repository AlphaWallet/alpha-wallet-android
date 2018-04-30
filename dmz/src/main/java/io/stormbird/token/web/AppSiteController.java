package io.stormbird.token.web;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import io.stormbird.token.entity.MagicLinkData;
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

    @GetMapping(value = "/{magicLink}")
    public String decodeLink(@PathVariable("magicLink") String magicLink, Model model)
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

            //find out the contract name, symbol and balance
            //have to use blocking gets here
            //TODO: we should be able to update components here instead of waiting
            String contractName = txHandler.getName(data.contractAddress);
            model.addAttribute("contractName", contractName);

            List<BigInteger> balanceArray = txHandler.getBalanceArray(data.ownerAddress, data.contractAddress);
            List<BigInteger> selection = new ArrayList<>();
            //convert balance to match selection
            for (int index : data.tickets)
            {
                if (balanceArray.size() > index && !balanceArray.get(index).equals(BigInteger.ZERO))
                    selection.add(balanceArray.get(index));
            }

            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (BigInteger b : selection)
            {
                sb.append(",");
                sb.append(Numeric.toHexString(b.toByteArray()));
            }
            sb.append("]");
            model.addAttribute("tickets", sb.toString());
        }
        catch (SalesOrderMalformed e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
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
