package org.movie.web.controller;

import org.movie.web.security.UserLoginDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class Film_WatcherPageController {

    @Autowired
    private UserLoginDetailsService userLoginDetailsService;

    @GetMapping("/bug.mp4")
    public ResponseEntity<Resource> getVideo() throws IOException {
        String filePath = "data/kissi/Test/truck.mp4";
        Path path = Paths.get(filePath);
        System.out.println(path);
        Resource videoResource = new FileSystemResource(path.toFile());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("video/mp4"));
        headers.setContentLength(videoResource.contentLength());
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());

        return new ResponseEntity<>(videoResource, headers, HttpStatus.OK);
    }
}