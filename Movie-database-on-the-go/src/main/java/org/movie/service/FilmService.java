package org.movie.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.movie.domain.Film;
import org.movie.persistance.ClientRepository;
import org.movie.persistance.FilmRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

@Service
public class FilmService {

    @Getter
    private final String storageDir = "data"; // A fájlok mentésére szolgáló mappa elérési útvonala
    @Getter
    private final String[] allowedExtensions = {"mp4", "avi", "mkv", "mov"}; // Engedélyezett fájlkiterjesztések

    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private FilmRepository filmRepository;


    public List<Film> getClientFilms(String username) {
        String basePath = storageDir + "/" + username;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            TypeReference<List<Film>> typeRef = new TypeReference<>() {
            };
            return objectMapper.readValue(Paths.get(basePath, "film.json").toFile(), typeRef);
        } catch (IOException e) {
            throw new RuntimeException("IO error happened while reading personal properties: " + e);
        }
    }

    //TODO: check film name for / and other problematic characters
    public boolean uploadFilm(String username, Film film, MultipartFile file) {
        film.setFilmpath(storageDir + "/" + username+ "/"+film.getName()+"/"+ file.getOriginalFilename());
        System.out.println(film.getFilmpath());
        addFilmDataToClient(username, film);
        storeVideoFile(username, file,film);
        return true;
    }

    @Transactional
    private void addFilmDataToClient(String username, Film filmToAdd) {
        String basePath = storageDir + "/" + username;
        ObjectMapper objectMapper = new ObjectMapper();
        filmRepository.saveAll(getClientFilms(username));
        filmRepository.save(filmToAdd);
        Iterable<Film> films = filmRepository.findAll();
        filmRepository.deleteAll();
        try {
            File file = new File(basePath, "film.json");
            objectMapper.writeValue(file, films);
        } catch (IOException e) {
            throw new RuntimeException("IO error happened while adding a film: " + e);
        }
    }

    private void storeVideoFile(String username, MultipartFile file, Film film) {
        try {
            // Ellenőrizzük, hogy a feltöltött fájl kiterjesztése videó-e
            boolean isValidExtension = false;
            for (String extension : allowedExtensions) {
                if (Objects.requireNonNull(file.getOriginalFilename()).toLowerCase().endsWith(extension)) {
                    isValidExtension = true;
                    break;
                }
            }
            if (!isValidExtension) {
                throw new IllegalArgumentException("Csak videófájlok engedélyezettek.");
            }

            // Ellenőrizzük, hogy a feltöltési mappa létezik-e, ha nem, létrehozzuk
            Path uploadPath = Paths.get(storageDir + "/" + username+ "/"+film.getName()).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // A fájlt mentjük a feltöltési mappába
            Path filePath = uploadPath.resolve(file.getOriginalFilename());
            Files.copy(file.getInputStream(), filePath);
        } catch (IOException ex) {
            throw new RuntimeException("Could not store the file. Please try again!", ex);
        }
    }
}
