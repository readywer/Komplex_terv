package org.movie.database.controller;

import org.movie.database.domain.Film;
import org.movie.database.security.UserLoginDetailsService;
import org.movie.database.service.FilmService;
import org.movie.database.service.LoggerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class Film_WatcherPageController {

    @Autowired
    private FilmService filmService;
    @Autowired
    private LoggerService loggerService;
    @Autowired
    private UserLoginDetailsService userLoginDetailsService;

    @GetMapping("/film_watch")
    public String watchFilm(@RequestParam(name = "filmId") Long filmId, Model model) {
        Film film = filmService.getFilmById(userLoginDetailsService.loadAuthenticatedUsername(), filmId);
        model.addAttribute("film", film);
        loggerService.logUserActivity(userLoginDetailsService.loadAuthenticatedUsername(), film.getId());
        return "film_watcher-page"; // Thymeleaf nézet
    }

    @GetMapping("/film_stream")
    public ResponseEntity<Resource> streamVideo(@RequestParam(name = "filmId") Long filmId) {
        Film film = filmService.getFilmById(userLoginDetailsService.loadAuthenticatedUsername(), filmId);
        Path path = Paths.get(film.getFilmPath());
        Resource videoResource = new FileSystemResource(path.toFile());

        String contentType;
        String fileName = path.getFileName().toString();
        String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);

        contentType = switch (fileExtension.toLowerCase()) {
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "ogg" -> "video/ogg";
            default -> throw new IllegalArgumentException("Unsupported file format");
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(contentType));
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());

        return new ResponseEntity<>(videoResource, headers, HttpStatus.OK);
    }
}
