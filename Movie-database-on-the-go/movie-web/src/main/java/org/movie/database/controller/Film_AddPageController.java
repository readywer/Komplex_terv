package org.movie.database.controller;

import jakarta.validation.Valid;
import org.movie.database.domain.Category;
import org.movie.database.domain.Film;
import org.movie.database.security.UserLoginDetailsService;
import org.movie.database.service.FilmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
public class Film_AddPageController {

    @Autowired
    private FilmService filmService;

    @Autowired
    private UserLoginDetailsService userLoginDetailsService;

    @GetMapping("/film_add")
    public String filmAddPage(Model model) {
        model.addAttribute("film", new Film());
        List<Category> categories = new ArrayList<>(Arrays.asList(Category.values()));
        model.addAttribute("categories", categories);
        return "film_add-page";
    }

    @PostMapping("/film_add")
    public String filmAdd(@Valid Film film, Model model, MultipartFile file, MultipartFile imageFile) {
        if (filmService.uploadFilm(userLoginDetailsService.loadAuthenticatedUsername(), film, file, imageFile)) {
            return "redirect:/films";
        }
        model.addAttribute("film", film);
        List<Category> categories = new ArrayList<>(Arrays.asList(Category.values()));
        model.addAttribute("categories", categories);
        model.addAttribute("nameFError", "The name empty.");
        return "film_add-page";
    }
}
