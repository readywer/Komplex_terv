package org.movie.web.controller;

import org.movie.service.MovieService;
import org.movie.web.security.UserLoginDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomePageController {

    @Autowired
    private UserLoginDetailsService userLoginDetailsService;

    @Autowired
    private MovieService movieService;

    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("client", movieService.findProtectedClientByUsername(userLoginDetailsService.loadAuthenticatedUsername()));
        model.addAttribute("numberOfFilms", movieService.getClientFilms(userLoginDetailsService.loadAuthenticatedUsername()).size());
        return "home-page";
    }
}
