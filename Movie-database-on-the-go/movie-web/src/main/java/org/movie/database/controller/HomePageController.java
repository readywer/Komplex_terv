package org.movie.database.controller;

import org.movie.database.security.UserLoginDetailsService;
import org.movie.database.service.ClientService;
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
    private ClientService clientService;
    @Autowired
    private FilmService filmService;

    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("client", clientService.findProtectedClientByUsername(userLoginDetailsService.loadAuthenticatedUsername()));
        model.addAttribute("numberOfFilms", filmService.getClientFilms(userLoginDetailsService.loadAuthenticatedUsername()).size());
        return "home-page";
    }
}
