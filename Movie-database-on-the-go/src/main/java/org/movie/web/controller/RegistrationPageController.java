package org.movie.web.controller;

import jakarta.validation.Valid;
import org.movie.domain.Client;
import org.movie.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RegistrationPageController {

    @Autowired
    private MovieService movieService;
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("client", new Client());
        return "registration-page";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("game") @Valid Client client, BindingResult result, Model model) {
        if (movieService.createClient(client)){
            System.out.println("siker");
            return "redirect:/home";
        }
        System.out.println("f");
        model.addAttribute("client", client);
        return "registration-page";
    }
}
