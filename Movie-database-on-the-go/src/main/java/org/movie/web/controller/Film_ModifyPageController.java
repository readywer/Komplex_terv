package org.movie.web.controller;

import jakarta.validation.Valid;
import org.movie.domain.Category;
import org.movie.domain.Film;
import org.movie.service.FilmService;
import org.movie.web.security.UserLoginDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
public class Film_ModifyPageController {

    @Autowired
    private FilmService filmService;

    @Autowired
    private UserLoginDetailsService userLoginDetailsService;

    @GetMapping("/film_modify")
    public String filmModify(@RequestParam(name = "filmId") Long filmId, Model model) {
        model.addAttribute("film", filmService.getFilmById(userLoginDetailsService.loadAuthenticatedUsername(), filmId));
        List<Category> categories = new ArrayList<>(Arrays.asList(Category.values()));
        model.addAttribute("categories", categories);
        return "film_modify-page";
    }

    @PostMapping("/film_modify")
    public String modify(@ModelAttribute("film") @Valid Film film, Model model, MultipartFile imageFile) {
        if (filmService.modifyFilm(userLoginDetailsService.loadAuthenticatedUsername(), film, imageFile)) {
            return "redirect:/films";
        }
        model.addAttribute("film", film);
        List<Category> categories = new ArrayList<>(Arrays.asList(Category.values()));
        model.addAttribute("categories", categories);
        model.addAttribute("nameFError", "A filmnév már foglalt.");
        return "film_modify-page";
    }
}
