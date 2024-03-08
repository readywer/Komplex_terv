package org.movie;

import org.movie.service.MovieService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        MovieService movieService;
        SpringApplication.run(Main.class);
    }
}