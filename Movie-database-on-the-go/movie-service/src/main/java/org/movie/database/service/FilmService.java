package org.movie.database.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.movie.database.domain.Film;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class FilmService {

    @Getter
    private final String storageDir = "data"; // A fájlok mentésére szolgáló mappa elérési útvonala
    @Getter
    private final String[] allowedFilmExtensions = {".mp4", ".webm", ".ogg", ".mkv", ".avi", ".mov", ".flv", ".wmv", ".ts"}; // Engedélyezett fájlkiterjesztések
    private final String[] allowedPictureExtensions = {"jpg", "png", "gif", "tif", "bmp", "jpeg"};
    @SuppressWarnings("FieldCanBeLocal")
    private final int maxWidth = 480;
    @SuppressWarnings("FieldCanBeLocal")
    private final int maxHeight = 720;
    @Getter
    private final long bytes = 32_212_254_720L;

    private static byte[] resizeImage(BufferedImage originalImage, @SuppressWarnings("SameParameterValue") int maxWidth, @SuppressWarnings("SameParameterValue") int maxHeight) throws IOException {
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

        // Új kép létrehozása a maxWidth és maxHeight mérettel, fekete háttérrel
        BufferedImage finalImage = new BufferedImage(maxWidth, maxHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = finalImage.createGraphics();

        // Fekete háttér kitöltése
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, maxWidth, maxHeight);

        // Kép középre igazítása
        int xOffset = (maxWidth - newWidth) / 2;
        int yOffset = (maxHeight - newHeight) / 2;

        // A méretezett kép megrajzolása a közepén
        g.drawImage(originalImage, xOffset, yOffset, newWidth, newHeight, null);
        g.dispose();

        // BufferedImage-t byte tömbbé konvertálása
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(finalImage, "jpg", outputStream);
        byte[] resizedBytes = outputStream.toByteArray();
        outputStream.close();

        return resizedBytes;
    }

    public Film getFilmById(String username, Long filmId) {
        List<Film> films = getClientFilms(username);
        return films.stream()
                .filter(film -> Objects.equals(film.getId(), filmId))
                .findFirst()
                .orElse(null); // Ha nem található film az adott id-vel, null értékkel tér vissza
    }

    public boolean uploadFilm(String username, Film film, MultipartFile file, MultipartFile picture, int quality) {
        if(isStorageExceeded(username)){
            return  false;
        }
        film.setFilmPath(file.getOriginalFilename());
        List<Film> films = getClientFilms(username);
        film.setId(getNextAvailableId(films));
        film.setUploadDate(LocalDate.now().toString());
        if (picture.getName().isEmpty()) {
            film.setPicturePath(picture.getOriginalFilename());
        }
        if (isValidFilm(film)) {
            return false;
        }
        if (!picture.isEmpty()) {
            film.setPicturePath(storageDir + "/" + username + "/" + film.getId() + "/" + Objects.requireNonNull(picture.getOriginalFilename()).replaceAll("\\.\\w+$", ".jpg"));
            storeImageFile(username, picture, film);
        }
        film.setFilmPath(storageDir + "/" + username + "/" + film.getId() + "/" + file.getOriginalFilename());
        addFilmToClient(username, film);
        storeVideoFile(username, file, film, quality);
        return true;
    }

    public boolean isValidFilm(Film film) {
        // Ellenőrzi, hogy a név üres vagy csak egy szóköz
        if (film.getName() == null || film.getName().isBlank()) {
            return false;
        }

        // Fájlok kiterjesztésének ellenőrzése
        if (!hasValidExtension(film.getFilmPath(), allowedFilmExtensions)) {
            return false;
        }

        if (!hasValidExtension(film.getPicturePath(), allowedFilmExtensions)) {
            return false;
        }

        // Ajánlott életkor ellenőrzése
        return film.getRecommendedAge() >= 0 && film.getRecommendedAge() <= 18;
    }

    // Ellenőrzi, hogy a fájl egy engedélyezett kiterjesztéssel végződik-e
    private boolean hasValidExtension(String filePath, String[] validExtensions) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }

        for (String ext : validExtensions) {
            if (filePath.toLowerCase().endsWith(ext)) {
                return true;
            }
        }

        return false;
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

    private void saveFilmsToJson(String username, List<Film> films) {
        String basePath = storageDir + "/" + username;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            File file = new File(basePath, "film.json");
            objectMapper.writeValue(file, films);
        } catch (IOException e) {
            throw new RuntimeException("IO error happened while saving films to JSON: " + e);
        }
    }

    @Transactional
    protected void addFilmToClient(String username, Film filmToAdd) {
        List<Film> films = getClientFilms(username);
        films.add(filmToAdd);
        saveFilmsToJson(username, films);
    }

    private long getNextAvailableId(List<Film> films) {
        return films.isEmpty() ? 1 : films.getLast().getId() + 1;
    }

    protected void deleteFilmByIdFromJson(String username, Long filmId) {
        List<Film> films = getClientFilms(username);
        Film film = getFilmById(username, filmId);
        films.remove(film);
        saveFilmsToJson(username, films);
    }

    private void storeVideoFile(String username, MultipartFile file, Film film, int quality) {
        Path filePath;
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
            Path uploadPath = Paths.get(storageDir + "/" + username + "/" + film.getId()).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // A fájlt mentjük a feltöltési mappába
            filePath = uploadPath.resolve(file.getOriginalFilename());
            Files.copy(file.getInputStream(), filePath);
        } catch (IOException ex) {
            throw new RuntimeException("Could not store the file. Please try again!", ex);
        }
        VideoConverterService.addToQueue(filePath, quality, film, username);
    }

    private void createDirectoryIfNotExists(String username, Long filmId) throws IOException {
        // Ellenőrizzük, hogy a feltöltési mappa létezik-e, ha nem, létrehozzuk
        Path uploadPath = Paths.get(storageDir + "/" + username + "/" + filmId).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
    }

    private String getTargetFileName(String originalFileName) {
        String fileName = originalFileName;
        if (fileName != null) {
            String lowercaseFileName = fileName.toLowerCase();
            for (String extension : allowedPictureExtensions) {
                if (lowercaseFileName.endsWith("." + extension)) {
                    // Ha a fájl végződése megfelel valamelyik engedélyezett kiterjesztésnek, konvertáljuk azt JPG formátumra
                    fileName = fileName.substring(0, fileName.length() - (extension.length() + 1)) + ".jpg";
                    break;
                }
            }
        }
        return fileName;
    }

    private void storeImageFile(String username, MultipartFile imageFile, Film film) {
        try {
            validateImageFile(imageFile);
            createDirectoryIfNotExists(username, film.getId());
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageFile.getBytes()));

            byte[] resizedBytes = resizeImage(originalImage, maxWidth, maxHeight);

            // A fájlt mentjük a feltöltési mappába
            String fileName = getTargetFileName(imageFile.getOriginalFilename());
            Path uploadPath = Paths.get(storageDir + "/" + username + "/" + film.getId()).toAbsolutePath().normalize();
            Path imagePath = uploadPath.resolve(fileName);
            Files.write(imagePath, resizedBytes);
        } catch (IOException ex) {
            throw new RuntimeException("Nem sikerült a fájlt elmenteni. Kérjük, próbálja újra!", ex);
        }
    }

    private void validateImageFile(MultipartFile imageFile) {
        // Ellenőrizzük a fájl kiterjesztését
        String originalFileName = imageFile.getOriginalFilename();
        if (originalFileName != null) {
            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toLowerCase();
            boolean isValidExtension = false;
            for (String extension : allowedPictureExtensions) {
                if (extension.equals(fileExtension)) {
                    isValidExtension = true;
                    break;
                }
            }
            if (!isValidExtension) {
                throw new IllegalArgumentException("Csak a következő kiterjesztéseket fogadjuk el: " + Arrays.toString(allowedPictureExtensions));
            }
        } else {
            throw new IllegalArgumentException("A fájlnak kiterjesztést kell tartalmaznia.");
        }
    }

    public void deleteFilm(String username, Long filmId) {
        deleteFolder(username, getFilmById(username, filmId).getId());
        deleteFilmByIdFromJson(username, filmId);
    }

    private void deleteFolder(String username, Long folderName) {
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

    public boolean modifyFilm(String username, Film film, MultipartFile picture) {
        Film ogFilm = getFilmById(username, film.getId());
        film.setFilmPath(ogFilm.getFilmPath());
        film.setPicturePath(ogFilm.getPicturePath());
        if (!isValidFilm(film)) {
            return false;
        }

        if (!picture.isEmpty()) {
            File pictureFile = new File(film.getPicturePath());
            if (pictureFile.delete()) {
                return false;
            }
            film.setPicturePath(storageDir + "/" + username + "/" + film.getId() + "/" + Objects.requireNonNull(picture.getOriginalFilename()).replaceAll("\\.\\w+$", ".jpg"));
            storeImageFile(username, picture, film);
        }
        deleteFilmByIdFromJson(username, film.getId());
        addFilmToClient(username, film);
        return true;
    }

    public long getTotalStorageUsed(String username) {
        Path userDirectory = Paths.get(storageDir, username);

        if (!Files.exists(userDirectory) || !Files.isDirectory(userDirectory)) {
            return 0;
        }

        try {
            return Files.walk(userDirectory)
                    .filter(Files::isRegularFile)
                    .mapToLong(file -> {
                        try {
                            return Files.size(file);
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            throw new RuntimeException("Nem sikerült kiszámítani a tárhelyhasználatot.", e);
        }
    }

    public String formatStorageSize(long bytes) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        double size = bytes;
        int unitIndex = 0;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.1f %s", size, units[unitIndex]);
    }
    public boolean isStorageExceeded(String username) {
        long usedStorage = getTotalStorageUsed(username);
        return usedStorage > bytes;
    }
}