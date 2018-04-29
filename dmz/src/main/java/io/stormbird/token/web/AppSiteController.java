package io.stormbird.token.web;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
@SpringBootApplication
public class AppSiteController {

   @GetMapping("/")
    public String index(@RequestParam(name="base64", required=false, defaultValue="BCD4") String base64, Model model) {
        model.addAttribute("base64", base64);
        return "index";
    }

	public static void main(String[] args) throws Exception {
		SpringApplication.run(AppSiteController.class, args);
	}
}
