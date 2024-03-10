package org.movie.web.controller;

import org.movie.service.FilmService;
import org.movie.web.security.UserLoginDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FilmsPageController {

    @Autowired
    private FilmService filmService;
    @Autowired
    private UserLoginDetailsService userLoginDetailsService;
    @GetMapping("/films")
    public String films(Model model) {
        model.addAttribute("films",filmService.getClientFilms(userLoginDetailsService.loadAuthenticatedUsername()));
        return "films-page";
    }
}
