package org.movie.web.controller;

import jakarta.validation.Valid;
import org.movie.domain.Client;
import org.movie.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RegistrationPageController {

    @Autowired
    private ClientService clientService;

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("client", new Client());
        return "registration-page";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("game") @Valid Client client, Model model) {
        if (clientService.createClient(client)) {
            return "redirect:/home";
        }
        model.addAttribute("client", client);
        return "registration-page";
    }
}
