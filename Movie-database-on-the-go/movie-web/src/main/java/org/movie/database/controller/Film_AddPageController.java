package org.movie.database.controller;

import org.movie.database.domain.Category;
import org.movie.database.domain.Film;
import org.movie.database.security.UserLoginDetailsService;
import org.movie.database.service.FilmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
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
        if (filmService.isStorageExceeded(userLoginDetailsService.loadAuthenticatedUsername())) {
            model.addAttribute("errMessage", "Nincs elegendő tárhely a film feltöltéséhez.");
            return "error";
        }
        return "film_add-page";
    }

    @PostMapping("/film_add")
    @ResponseBody
    public ResponseEntity<String> filmAdd(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("actors") List<String> actors,
            @RequestParam("releaseDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate releaseDate,
            @RequestParam("recommendedAge") int recommendedAge,
            @RequestParam("quality") int quality,
            @RequestParam("categories") List<Category> categories) {

        // Film objektum létrehozása
        Film film = new Film();
        film.setName(name);
        film.setDescription(description);
        film.setActors(actors);
        film.setReleaseDate(String.valueOf(releaseDate));
        film.setRecommendedAge(recommendedAge);
        film.setCategories(categories);

        boolean success = filmService.uploadFilm(userLoginDetailsService.loadAuthenticatedUsername(), film, file, imageFile, quality);

        return success ? ResponseEntity.ok("Success") : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed");
    }
}
