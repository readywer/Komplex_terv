package org.movie.database.controller;

import org.movie.database.domain.Client;
import org.movie.database.persistence.ClientRepository;
import org.movie.database.security.UserLoginDetailsService;
import org.movie.database.service.FilmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomePageController {

    @Autowired
    private UserLoginDetailsService userLoginDetailsService;
    @Autowired
    private FilmService filmService;
    @Autowired
    private ClientRepository clientRepository;

    @GetMapping("/home")
    public String home(Model model) {
        Client client = clientRepository.findByUsername(userLoginDetailsService.loadAuthenticatedUsername());
        client.setPassword("");
        model.addAttribute("client", client);
        model.addAttribute("numberOfFilms", filmService.getClientFilms(userLoginDetailsService.loadAuthenticatedUsername()).size());
        return "home-page";
    }
}
