package org.movie.database.controller;

import org.movie.database.domain.Client;
import org.movie.database.domain.Film;
import org.movie.database.persistence.ClientRepository;
import org.movie.database.service.ClientService;
import org.movie.database.service.FilmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Controller
public class AdminPageController {
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private FilmService filmService;
    @Autowired
    private ClientService clientService;

    @GetMapping("/admin")
    public String admin(Model model) {
        List<Client> clients = (List<Client>) clientRepository.findAll();
        model.addAttribute("clients",clients);
        return "admin-page";
    }

    @GetMapping("/admin/client_details")
    public String filmDetailsAdmin(@RequestParam(name = "clientId") Long clientId, Model model) {
        Client client = clientRepository.findById(clientId).orElse(null);
        List<Film> films = filmService.getClientFilms(client.getUsername());
        model.addAttribute("usableSpace", filmService.formatStorageSize(filmService.getBytes()));
        model.addAttribute("usedSpace", filmService.formatStorageSize(filmService.getTotalStorageUsed(client.getUsername())));
        model.addAttribute("films", films);
        model.addAttribute("client", client);
        return "client_details-page";
    }

    @GetMapping("/admin/data/{folder}/{folder2}/{filename:.+}")
    public ResponseEntity<byte[]> getImage(@PathVariable String folder, @PathVariable String folder2, @PathVariable String filename) throws IOException {
        String imagePath = "data/" + folder + "/" + folder2 + "/" + filename;

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

    @PostMapping("/admin/client_delete")
    public String deleteClient(@RequestParam(name = "clientId") Long clientId) {
        clientService.deleteClient(clientId);
        return "redirect:/admin";
    }
}
