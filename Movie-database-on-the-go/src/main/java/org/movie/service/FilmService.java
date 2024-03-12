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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
        File file = Paths.get(basePath, "film.json").toFile();

        if (!file.exists() || file.length() == 0) {
            // Ha a fájl nem létezik vagy üres, visszaadjuk az üres listát
            return new ArrayList<>();
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            TypeReference<List<Film>> typeRef = new TypeReference<>() {
            };
            return objectMapper.readValue(file, typeRef);
        } catch (IOException e) {
            throw new RuntimeException("IO error happened while reading personal properties: " + e);
        }
    }

    public Film getFilmById(String username, Long filmId) {
        List<Film> films = getClientFilms(username);
        for (Film film : films) {
            if (film.getId() != null && film.getId().equals(filmId)) {
                return film;
            }
        }
        return null; // Ha nem található film az adott id-jel, null értékkel tér vissza
    }

    //TODO: check film name for / and other problematic characters
    public boolean uploadFilm(String username, Film film, MultipartFile file, MultipartFile picture) {
        film.setFilmpath(file.getOriginalFilename());
        film.setPicturepath(picture.getOriginalFilename());
        System.out.println(film.getName());
        if(!isValidFilm(film,username)){
            System.out.println("hiba");
            return false;
        }
        film.setFilmpath(storageDir + "/" + username + "/" + film.getName() + "/" + file.getOriginalFilename());
        film.setPicturepath(storageDir + "/" + username + "/" + film.getName() + "/" + picture.getOriginalFilename());
        addFilmDataToClient(username, film);
        //storeVideoFile(username, file, film);
        //storeImageFile(username, picture, film);
        CompletableFuture<Void> videoFileFuture = CompletableFuture.runAsync(() -> storeVideoFile(username, file, film));
        CompletableFuture<Void> imageFileFuture = CompletableFuture.runAsync(() -> storeImageFile(username, picture, film));

        // Várakozás, amíg mindkét művelet be nem fejeződik be
        try {
            CompletableFuture.allOf(videoFileFuture, imageFileFuture).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
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
            Path uploadPath = Paths.get(storageDir + "/" + username + "/" + film.getName()).toAbsolutePath().normalize();
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

    private void storeImageFile(String username, MultipartFile imageFile, Film film) {
        try {
            // Ellenőrizzük, hogy a feltöltött fájl kép-e
            if (!imageFile.getContentType().startsWith("image/")) {
                throw new IllegalArgumentException("Csak képfájlok engedélyezettek.");
            }

            // Ellenőrizzük, hogy a feltöltési mappa létezik-e, ha nem, létrehozzuk
            Path uploadPath = Paths.get(storageDir + "/" + username + "/" + film.getName()).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // A fájlt mentjük a feltöltési mappába
            Path imagePath = uploadPath.resolve(imageFile.getOriginalFilename());
            Files.copy(imageFile.getInputStream(), imagePath);
        } catch (IOException ex) {
            throw new RuntimeException("Nem sikerült a fájlt elmenteni. Kérjük, próbálja újra!", ex);
        }
    }

    public boolean isValidFilm(Film film, String username){
        if(!isValidName(film.getName(),username)){
            return false;
        }
        if (film.getFilmpath().contains(".")) {
            return false;
        }
        if (film.getPicturepath().contains(".")) {
            return false;
        }
        if(film.getRecommendedAge() >= 0 && film.getRecommendedAge() <= 18){
            return false;
        }
        return true;
    }

    private boolean isValidName(String name, String username){
        if ( name.isEmpty()) {
            return false;
        }
        if (name.contains("/") || name.contains("\\")) {
            return false;
        }
        for(Film film : getClientFilms(username)){
            if (film.getName().equals(name)){
                return false;
            }
        }
        return true;
    }
    public BufferedImage readStoredImageFile(String filename) {
        try {
            // A kép elérési útjának meghatározása
            Path imagePath = Paths.get(filename).toAbsolutePath().normalize();

            // Kép beolvasása
            BufferedImage image = ImageIO.read(imagePath.toFile());

            return image;
        } catch (IOException ex) {
            throw new RuntimeException("Nem sikerült a képet beolvasni.", ex);
        }
    }

    public String readStoredImageFileAsBase64(String filename) {
        try {
            // A kép elérési útjának meghatározása
            Path imagePath = Paths.get(filename).toAbsolutePath().normalize();
            System.out.println(imagePath);
            // Kép beolvasása BufferedImage-be
            BufferedImage image = ImageIO.read(imagePath.toFile());

            // Kép átalakítása base64-kódolt stringgé
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            byte[] imageBytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException ex) {
            throw new RuntimeException("Nem sikerült a képet beolvasni vagy base64-kódolni.", ex);
        }
    }
}
