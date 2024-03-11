package org.movie.web.controller;

import org.movie.domain.Film;
import org.movie.service.FilmService;
import org.movie.web.security.UserLoginDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class Film_WatcherPageController {

    @Autowired
    private FilmService filmService;
    @Autowired
    private UserLoginDetailsService userLoginDetailsService;

    @GetMapping("/film_watch")
    public ResponseEntity<Resource> getVideo(@RequestParam(name = "filmId") Long filmId) throws IOException {
        Film film = filmService.getFilmById(userLoginDetailsService.loadAuthenticatedUsername(), filmId);
        Path path = Paths.get(film.getFilmpath());
        Resource videoResource = new FileSystemResource(path.toFile());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("video/mp4"));
        headers.setContentLength(videoResource.contentLength());
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());

        return new ResponseEntity<>(videoResource, headers, HttpStatus.OK);
    }
}
