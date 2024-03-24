package org.movie.database.controller;

import org.movie.database.domain.Film;
import org.movie.database.security.UserLoginDetailsService;
import org.movie.database.service.FilmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        model.addAttribute("films", films);
        return "films-page";
    }

    @GetMapping("/data/{folder}/{folder2}/{filename:.+}")
    public ResponseEntity<byte[]> getImage(@PathVariable String folder, @PathVariable String folder2, @PathVariable String filename) throws IOException {
        String username = userLoginDetailsService.loadAuthenticatedUsername();
        String imagePath = "data/" + username + "/" + folder2 + "/" + filename;

        // Ellenőrizze, hogy a kép létezik-e
        Path path = Path.of(imagePath);
        if (!Files.exists(path)) {
            // Ha a kép nem létezik, 404 Not Found hibát küldünk vissza
            return ResponseEntity.notFound().build();
        }

        // Olvassa be a képet byte tömbbe
        byte[] imageBytes = Files.readAllBytes(path);

        // Az Image típusú tartalom típusának beállítása
        // A kép típusa függ a MIME típustól, ebben az esetben feltételezzük, hogy PNG formátumú
        MediaType mediaType = MediaType.IMAGE_PNG;

        // Válasz küldése a byte tömbbel és a megfelelő tartalom típussal
        return ResponseEntity.ok().contentType(mediaType).body(imageBytes);
    }
}
