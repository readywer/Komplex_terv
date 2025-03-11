package org.movie.database.controller;

import jakarta.validation.Valid;
import org.movie.database.domain.Client;
import org.movie.database.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
    public String register(@Valid Client client, Model model) {
        String result = clientService.createClient(client);
        if (result == null) {
            return "redirect:/home";
        }
        model.addAttribute("nameError", result);
        model.addAttribute("client", client);
        return "registration-page";
    }
}
