package org.movie.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.movie.domain.Client;
import org.movie.domain.Film;
import org.movie.persistance.ClientRepository;
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
public class MovieService {

    @Autowired
    private ClientRepository clientRepository;
    private final String storageDir = "data"; // A fájlok mentésére szolgáló mappa elérési útvonala
    @Getter
    private final String[] allowedExtensions = {"mp4", "avi", "mkv", "mov"}; // Engedélyezett fájlkiterjesztések

    @Transactional
    public boolean createClient(Client client) {
        if (isValidClient(client)) {
            createDirectory(client);
            clientRepository.save(client);
            return true;
        }
        return false;
    }

    private void createDirectory(Client client) {
        String folderPath = storageDir + "/" + client.getUsername();
        try {
            Path directoryPath = Paths.get(folderPath);
            Files.createDirectories(directoryPath);
            String filePath = folderPath + "/film.json";
            Path file = Paths.get(filePath);
            if (!Files.exists(file)) {
                Files.createFile(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //TODO more robust validation
    public boolean isValidClient(Client client) {
        return !client.getName().isEmpty() && !client.getUsername().isEmpty() &&
                !client.getPassword().isEmpty() && !client.getEmail().isEmpty();
    }

    public Client findClientByUsername(String username) {
        for (Client client : clientRepository.findAll()) {
            if (client.getUsername().equals(username)) {
                return client;
            }
        }
        return null;
    }

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

    //TODO fix id
    public boolean uploadFilm(String username, Film film, MultipartFile file) {
        film.setFilename(file.getOriginalFilename());
        addFilmDataToClient(username,film);
        storeVideoFile(username,file);
        return true;
    }

    private void addFilmDataToClient(String username, Film filmToAdd) {
        String basePath = storageDir + "/" + username;
        ObjectMapper objectMapper = new ObjectMapper();

        List<Film> films = getClientFilms(username);
        films.add(filmToAdd);
        try {
            File file = new File(basePath, "film.json");
            objectMapper.writeValue(file, films);
        } catch (IOException e) {
            throw new RuntimeException("IO error happened while adding a film: " + e);
        }
    }

    private void storeVideoFile(String username, MultipartFile file) {
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
            Path uploadPath = Paths.get(storageDir + "/" + username).toAbsolutePath().normalize();
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
