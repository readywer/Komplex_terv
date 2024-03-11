package org.movie.web.controller;

import org.movie.domain.Film;
import org.movie.service.FilmService;
import org.movie.web.security.UserLoginDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class Film_DetailsPageController {

    @Autowired
    private FilmService filmService;
    @Autowired
    private UserLoginDetailsService userLoginDetailsService;

    @GetMapping("/film_details")
    public String filmDetails(@RequestParam(name = "filmId") Long filmId, Model model) {
        Film film = filmService.getFilmById(userLoginDetailsService.loadAuthenticatedUsername(),filmId);
        model.addAttribute("film", film);
        return "film_details-page";
    }
}