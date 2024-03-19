package org.movie.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.movie.domain.Film;
import org.movie.persistance.FilmRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FilmService {

    @Getter
    private final String storageDir = "data"; // A fájlok mentésére szolgáló mappa elérési útvonala
    @Getter
    private final String[] allowedFilmExtensions = {"mp4", "avi", "mkv", "mov"}; // Engedélyezett fájlkiterjesztések
    private final String[] allowedPictureExtensions = {"jpg", "png", "gif", "tif", "bmp", "jpeg"};

    @Autowired
    private FilmRepository filmRepository;

    private static byte[] convertToJpg(BufferedImage originalImage) throws IOException {
        // Hozzunk létre egy új BufferedImage-t, hogy JPG formátumú legyen
        BufferedImage jpgImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        jpgImage.createGraphics().drawImage(originalImage, 0, 0, java.awt.Color.WHITE, null);

        // Konvertáljuk a BufferedImage-t byte tömbbé
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(jpgImage, "jpg", outputStream);
        byte[] jpgBytes = outputStream.toByteArray();
        outputStream.close();

        return jpgBytes;
    }

    private static byte[] resizeImage(BufferedImage originalImage, int maxWidth, int maxHeight) throws IOException {
        // Szélesség és magasság ellenőrzése
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        int newWidth = originalWidth;
        int newHeight = originalHeight;

        // Képarány megtartása
        if (originalWidth > maxWidth || originalHeight > maxHeight) {
            double widthRatio = (double) maxWidth / originalWidth;
            double heightRatio = (double) maxHeight / originalHeight;
            double ratio = Math.min(widthRatio, heightRatio);

            newWidth = (int) (originalWidth * ratio);
            newHeight = (int) (originalHeight * ratio);
        }

        // Új BufferedImage létrehozása a méretezéshez
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();

        // BufferedImage-t byte tömbbé konvertálása
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, "jpg", outputStream);
        byte[] resizedBytes = outputStream.toByteArray();
        outputStream.close();

        return resizedBytes;
    }

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

    public boolean uploadFilm(String username, Film film, MultipartFile file, MultipartFile picture) {
        film.setFilmpath(file.getOriginalFilename());
        if (picture.getName().isEmpty()) {
            film.setPicturepath(picture.getOriginalFilename());
        }
        if (!isValidFilm(film, username)) {
            return false;
        }
        if (!picture.isEmpty()) {
            film.setPicturepath(storageDir + "/" + username + "/" + film.getName() + "/" + picture.getOriginalFilename().replaceAll("\\.\\w+$", ".jpg"));
            storeImageFile(username, picture, film);
        }
        film.setFilmpath(storageDir + "/" + username + "/" + film.getName() + "/" + file.getOriginalFilename());
        addFilmDataToClient(username, film);
        storeVideoFile(username, file, film);
        return true;
    }

    public boolean isValidFilm(Film film, String username) {
        if (!isValidName(film.getName(), username)) {
            return false;
        }
        if (film.getFilmpath().contains(".") && countOccurrences(film.getFilmpath(), '.') > 1) {
            return false;
        }
        if (film.getPicturepath().contains(".") && countOccurrences(film.getPicturepath(), '.') > 1) {
            return false;
        }
        if (film.getRecommendedAge() < 0 || film.getRecommendedAge() > 18) {
            return false;
        }
        return true;
    }

    private int countOccurrences(String str, char character) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == character) {
                count++;
            }
        }
        return count;
    }

    private boolean isValidName(String name, String username) {
        if (name.isEmpty()) {
            return false;
        }
        for (Film film : getClientFilms(username)) {
            if (film.getName().equals(name)) {
                return false;
            }
        }
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9.,_-]+$");
        Matcher matcher = pattern.matcher(username);
        return matcher.matches();
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
            for (String extension : allowedFilmExtensions) {
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

    private void validateImageFile(MultipartFile imageFile) {
        // Ellenőrizzük, hogy a feltöltött fájl kép-e
        if (!imageFile.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("Csak képfájlok engedélyezettek.");
        }
    }

    private void createDirectoryIfNotExists(String username, String filmName) throws IOException {
        // Ellenőrizzük, hogy a feltöltési mappa létezik-e, ha nem, létrehozzuk
        Path uploadPath = Paths.get(storageDir + "/" + username + "/" + filmName).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
    }

    private String getTargetFileName(String originalFileName) {
        String fileName = originalFileName;
        if (fileName != null) {
            String lowercaseFileName = fileName.toLowerCase();
            if (lowercaseFileName.endsWith(".png") ||
                    lowercaseFileName.endsWith(".jpeg") ||
                    lowercaseFileName.endsWith(".gif") ||
                    lowercaseFileName.endsWith(".bmp") ||
                    lowercaseFileName.endsWith(".tiff")) {

                fileName = fileName.substring(0, fileName.length() - 4) + ".jpg";
            }
        }
        return fileName;
    }

    private void storeImageFile(String username, MultipartFile imageFile, Film film) {
        try {
            validateImageFile(imageFile);
            createDirectoryIfNotExists(username, film.getName());

            // Kép konvertálása JPG formátummá
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageFile.getBytes()));
            byte[] jpgBytes = convertToJpg(originalImage);

            // Kép átméretezése
            int maxWidth = 480;
            int maxHeight = 720;
            byte[] resizedBytes = resizeImage(originalImage, maxWidth, maxHeight);

            // A fájlt mentjük a feltöltési mappába
            String fileName = getTargetFileName(imageFile.getOriginalFilename());
            Path uploadPath = Paths.get(storageDir + "/" + username + "/" + film.getName()).toAbsolutePath().normalize();
            Path imagePath = uploadPath.resolve(fileName);
            Files.write(imagePath, resizedBytes);
        } catch (IOException ex) {
            throw new RuntimeException("Nem sikerült a fájlt elmenteni. Kérjük, próbálja újra!", ex);
        }
    }


    public void deleteFilm(String username, Long filmId) {
        deleteFolder(username, getFilmById(username, filmId).getName());
        deleteFilmByIdFromJson(username, filmId);
    }

    private void deleteFolder(String username, String folderName) {
        // Elérési útvonal felépítése
        String path = storageDir + "/" + username + "/" + folderName;
        File folder = new File(path);

        // Ellenőrizzük, hogy a mappa létezik-e
        if (!folder.exists()) {
            System.out.println("A megadott mappa nem létezik.");
            return;
        }

        // Ellenőrizzük, hogy a megadott elérési útvonal egy mappa-e
        if (!folder.isDirectory()) {
            System.out.println("A megadott elérési útvonal nem egy mappa.");
            return;
        }

        // Rekurzívan töröljük a mappa tartalmát
        deleteContents(folder);

        // Töröljük magát a mappát
        if (folder.delete()) {
            System.out.println("A mappa és annak tartalma sikeresen törölve lett.");
        } else {
            System.out.println("Nem sikerült törölni a mappát és annak tartalmát.");
        }
    }

    private void deleteContents(File folder) {
        File[] contents = folder.listFiles();
        if (contents != null) {
            for (File file : contents) {
                if (file.isDirectory()) {
                    // Rekurzívan töröljük a mappa tartalmát
                    deleteContents(file);
                }
                // Töröljük a fájlokat vagy a mappákat
                file.delete();
            }
        }
    }

    private void deleteFilmByIdFromJson(String username, Long filmId) {
        String basePath = storageDir + "/" + username;
        ObjectMapper objectMapper = new ObjectMapper();
        List<Film> films = getClientFilms(username);
        Film film = getFilmById(username, filmId);
        System.out.println(film.getName());
        films.remove(film);
        try {
            File file = new File(basePath, "film.json");
            objectMapper.writeValue(file, films);
        } catch (IOException e) {
            throw new RuntimeException("IO error happened while adding a film: " + e);
        }
    }

    public boolean modifyFilm(String username, Film film, MultipartFile picture) {
        Film ogFilm = getFilmById(username, film.getId());
        film.setFilmpath(ogFilm.getFilmpath());
        film.setPicturepath(ogFilm.getPicturepath());
        if (!isValidFilm(film, username)) {
            return false;
        }
        if (!picture.isEmpty()) {
            File pictureFile = new File(film.getPicturepath());
            boolean deleted = pictureFile.delete();
            film.setPicturepath(storageDir + "/" + username + "/" + film.getName() + "/" + picture.getOriginalFilename().replaceAll("\\.\\w+$", ".jpg"));
            storeImageFile(username, picture, film);
        }
        deleteFilmByIdFromJson(username, film.getId());
        addFilmDataToClient(username, film);
        return true;
    }

    private void moveVideoFile(String username, Film sourceFilm, Film destinationFilm) {
        try {
            // Ellenőrizzük, hogy a feltöltési mappa létezik-e, ha nem, létrehozzuk
            Path uploadPathSource = Paths.get(storageDir + "/" + username + "/" + sourceFilm.getName()).toAbsolutePath().normalize();
            Path uploadPathDestination = Paths.get(storageDir + "/" + username + "/" + destinationFilm.getName()).toAbsolutePath().normalize();

            if (!Files.exists(uploadPathDestination)) {
                Files.createDirectories(uploadPathDestination);
            }

            // A fájlt másoljuk a cél mappába
            Files.copy(uploadPathSource, uploadPathDestination.resolve(sourceFilm.getFilmpath()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException("Could not store the file. Please try again!", ex);
        }
    }
}
