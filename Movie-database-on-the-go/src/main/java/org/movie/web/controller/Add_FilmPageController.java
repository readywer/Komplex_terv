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
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
public class Add_FilmPageController {

    @Autowired
    private FilmService filmService;

    @Autowired
    private UserLoginDetailsService userLoginDetailsService;

    @GetMapping("/add_film")
    public String home(Model model) {
        model.addAttribute("film", new Film());
        List<Category> categories = new ArrayList<>();
        categories.addAll(Arrays.asList(Category.values()));
        model.addAttribute("categories", categories);
        return "add_film-page";
    }

    @PostMapping("/add_film")
    public String register(@ModelAttribute("film") @Valid Film film, Model model, MultipartFile file, MultipartFile imageFile) {
        if (filmService.uploadFilm(userLoginDetailsService.loadAuthenticatedUsername(), film, file, imageFile)) {
            return "redirect:/add_film";
        }
        model.addAttribute("film", film);
        List<Category> categories = new ArrayList<>();
        categories.addAll(Arrays.asList(Category.values()));
        model.addAttribute("categories", categories);
        model.addAttribute("allowedExtensions", filmService.getAllowedExtensions());
        model.addAttribute("nameFError", "A filmnév már foglalt.");
        return "add_film-page";
    }
}
