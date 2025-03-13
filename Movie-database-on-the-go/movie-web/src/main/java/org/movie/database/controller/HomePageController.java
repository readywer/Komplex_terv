package org.movie.database.controller;

import org.movie.database.domain.Client;
import org.movie.database.domain.Film;
import org.movie.database.persistence.ClientRepository;
import org.movie.database.security.UserLoginDetailsService;
import org.movie.database.service.ClientService;
import org.movie.database.service.FilmService;
import org.movie.database.service.LoggerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.Objects;

@Controller
public class HomePageController {

    @Autowired
    private UserLoginDetailsService userLoginDetailsService;
    @Autowired
    private FilmService filmService;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private ClientService clientService;

    @GetMapping("/home")
    public String home(Model model) {
        Client client = clientRepository.findByUsername(userLoginDetailsService.loadAuthenticatedUsername());
        client.setPassword("");
        model.addAttribute("usableSpace", filmService.formatStorageSize(filmService.getBytes()));
        model.addAttribute("usedSpace", filmService.formatStorageSize(filmService.getTotalStorageUsed(client.getUsername())));
        model.addAttribute("client", client);
        model.addAttribute("numberOfFilms", filmService.getClientFilms(userLoginDetailsService.loadAuthenticatedUsername()).size());
        List<Long> lastWatchedMovieIds = LoggerService.getLastWatchedMovies(client.getUsername());
        List<Film> films = lastWatchedMovieIds.stream()
                .map(filmId -> filmService.getFilmById(client.getUsername(), filmId)) // Lambda használata
                .filter(Objects::nonNull) // Eltávolítja a null értékeket
                .toList();
        model.addAttribute("films", films);
        return "home-page";
    }

    @PostMapping("/client_delete")
    public String deleteClient() {
        Client client = clientRepository.findByUsername(userLoginDetailsService.loadAuthenticatedUsername());
        clientService.deleteClient(client.getId());
        return "redirect:/logout";
    }
}
