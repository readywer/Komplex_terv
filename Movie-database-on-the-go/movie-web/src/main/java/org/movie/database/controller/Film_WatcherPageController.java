package org.movie.database.controller;

import org.movie.database.domain.Film;
import org.movie.database.security.UserLoginDetailsService;
import org.movie.database.service.FilmService;
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

    //TODO html5 csak mp4-et tud lejátszani a többit csak letölti
    @GetMapping("/film_watch")
    public ResponseEntity<Resource> getVideo(@RequestParam(name = "filmId") Long filmId) throws IOException {
        Film film = filmService.getFilmById(userLoginDetailsService.loadAuthenticatedUsername(), filmId);
        Path path = Paths.get(film.getFilmpath());
        Resource videoResource = new FileSystemResource(path.toFile());

        String contentType;
        String fileName = path.getFileName().toString();
        String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);

        contentType = switch (fileExtension.toLowerCase()) {
            case "mp4" -> "video/mp4";
            case "avi" -> "video/x-msvideo";
            case "mkv" -> "video/x-matroska";
            case "mov" -> "video/quicktime";
            default ->
                // Ha az adott kiterjesztés nem támogatott, akkor hibát dobunk
                    throw new IllegalArgumentException("Unsupported file format");
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(contentType));
        headers.setContentLength(videoResource.contentLength());
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());

        return new ResponseEntity<>(videoResource, headers, HttpStatus.OK);
    }
}
