package org.movie.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.movie.domain.Client;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MovieService {

    private final String storageDir = "data"; // A fájlok mentésére szolgáló mappa elérési útvonala
    @Getter
    private final String[] allowedExtensions = {"mp4", "avi", "mkv", "mov"}; // Engedélyezett fájlkiterjesztések
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private FilmRepository filmRepository;

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

    //TODO hiba visszajelzés
    public boolean isValidClient(Client client) {
        if (!isValidName(client.getName())) {
            return false;
        }
        if (!isValidUsername(client.getUsername())) {
            return false;
        }
        if (!isValidPassword(client.getPassword())) {
            return false;
        }
        if (!isValidEmail(client.getEmail())) {
            return false;
        }
        return true;
    }

    private boolean isValidUsername(String username) {
        // Ellenőrizze, hogy a felhasználónév üres-e
        if (username.isEmpty()) {
            return false;
        }

        // Ellenőrizze a hosszt
        if (username.length() < 3 || username.length() > 32) {
            return false;
        }

        // Ellenőrizze, hogy a felhasználónév betűvel kezdődik
        if (!Character.isLetter(username.charAt(0))) {
            return false;
        }

        // Ellenőrizze az engedélyezett karaktereket
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9.,_-]+$");
        Matcher matcher = pattern.matcher(username);
        return matcher.matches();
    }

    private boolean isValidName(String name) {
        // Ellenőrizze, hogy a felhasználónév üres-e
        if (name.isEmpty()) {
            return false;
        }

        // Ellenőrizze a hosszt
        if (name.length() < 3 || name.length() > 32) {
            return false;
        }
        // Ellenőrizze, hogy a felhasználónév betűvel kezdődik
        return Character.isLetter(name.charAt(0));
    }

    private boolean isValidPassword(String password) {
        // Ellenőrizze, hogy a jelszó üres-e
        if (password.isEmpty()) {
            return false;
        }

        // Ellenőrizze a hosszt
        if (password.length() < 6) {
            return false;
        }

        // Ellenőrizze legalább egy kisbetűt, egy nagybetűt és egy számot
        Pattern pattern = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,}$");
        Matcher matcher = pattern.matcher(password);
        return matcher.matches();
    }

    private boolean isValidEmail(String email) {
        // Ellenőrizze, hogy az e-mail cím üres-e
        if (email.isEmpty()) {
            return false;
        }

        // Ellenőrizze az e-mail cím reguláris kifejezéssel
        Pattern pattern = Pattern.compile("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)])");
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
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

    public boolean uploadFilm(String username, Film film, MultipartFile file) {
        film.setFilename(file.getOriginalFilename());
        addFilmDataToClient(username, film);
        storeVideoFile(username, file);
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
