package org.movie.web.controller;

import org.movie.domain.Film;
import org.movie.service.FilmService;
import org.movie.web.security.UserLoginDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.awt.image.BufferedImage;
import java.util.List;

@Controller
public class FilmsPageController {

    @Autowired
    private FilmService filmService;
    @Autowired
    private UserLoginDetailsService userLoginDetailsService;

    @GetMapping("/films")
    public String films(Model model) {
        List<Film> films = filmService.getClientFilms(userLoginDetailsService.loadAuthenticatedUsername());

        for (Film film : films) {
            String picturePath = film.getPicturepath();
            String base64Image = filmService.readStoredImageFileAsBase64(picturePath);
            model.addAttribute("image_" + film.getName(), base64Image);
        }
        model.addAttribute("films", films);

        return "films-page";
    }
}
