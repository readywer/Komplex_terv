package org.movie.database.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.movie.database.domain.Film;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class FilmService {
    //TODO upload page upload state, home last watched film, recommended, admin page
    @Getter
    private final String storageDir = "data"; // A fájlok mentésére szolgáló mappa elérési útvonala
    @Getter
    private final String[] allowedFilmExtensions = {"mp4", "webm", "ogg", "mkv", "avi", "mov", "flv", "wmv", "ts"};
    private final String[] allowedPictureExtensions = {"jpg", "png", "gif", "tif", "bmp", "jpeg"};
    private final int maxWidth = 480;
    private final int maxHeight = 720;
    @Getter
    private final long bytes = 32_212_254_720L;
    @Autowired
    private LoggerService loggerService;

    public Film getFilmById(String username, Long filmId) {
        List<Film> films = getClientFilms(username);
        Film film = films.stream()
                .filter(f -> Objects.equals(f.getId(), filmId))
                .findFirst()
                .orElse(null);

        if (film == null) {
            loggerService.logError("Film not found: ID " + filmId + " for user " + username, null);
        }

        return film;
    }

    public boolean uploadFilm(String username, Film film, MultipartFile file, MultipartFile picture, int quality) {
        try {
            if (isStorageExceeded(username)) {
                return false;
            }
            film.setFilmPath(file.getOriginalFilename());
            List<Film> films = getClientFilms(username);
            film.setId(getNextAvailableId(films));
            film.setUploadDate(LocalDate.now().toString());

            if (picture.getName().isEmpty()) {
                film.setPicturePath(picture.getOriginalFilename());
            }
            if (!isValidFilm(film)) {
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
        } catch (Exception e) {
            loggerService.logError("Failed to upload film for user: " + username, e);
            return false;
        }
    }

    private boolean isValidFilm(Film film) {
        // Ellenőrzi, hogy a név üres vagy csak egy szóköz
        if (film.getName() == null || film.getName().isBlank()) {
            return false;
        }
        // Fájlok kiterjesztésének ellenőrzése
        if (!hasValidExtension(film.getFilmPath(), allowedFilmExtensions)) {
            return false;
        }
        if (!hasValidExtension(film.getPicturePath(), allowedPictureExtensions)) {
            return false;
        }
        // Ajánlott életkor ellenőrzése
        return film.getRecommendedAge() >= 0 && film.getRecommendedAge() <= 18;
    }

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

        if (!file.exists()) {
            loggerService.logError("A film.json fájl nem létezik: " + file.getAbsolutePath(), null);
            return new ArrayList<>();
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            TypeReference<List<Film>> typeRef = new TypeReference<>() {
            };
            return objectMapper.readValue(file, typeRef);
        } catch (IOException e) {
            loggerService.logError("Hiba történt a film.json beolvasásakor: " + file.getAbsolutePath(), e);
            return new ArrayList<>(); // Hiba esetén üres listát adunk vissza
        }
    }

    private boolean saveFilmsToJson(String username, List<Film> films) {
        String basePath = storageDir + "/" + username;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            File file = new File(basePath, "film.json");
            objectMapper.writeValue(file, films);
            return true;
        } catch (IOException e) {
            loggerService.logError("Hiba történt a filmek JSON fájlba mentése közben: " + e.getMessage(), e);
            return false;
        }
    }

    @Transactional
    protected boolean addFilmToClient(String username, Film filmToAdd) {
        List<Film> films = getClientFilms(username);
        films.add(filmToAdd);
        return saveFilmsToJson(username, films);
    }

    private long getNextAvailableId(List<Film> films) {
        return films.isEmpty() ? 1 : films.getLast().getId() + 1;
    }

    protected boolean deleteFilmByIdFromJson(String username, Long filmId) {
        try {
            List<Film> films = getClientFilms(username);
            Film film = getFilmById(username, filmId);

            if (film == null) {
                loggerService.logError("Failed to remove film from list: Film ID " + filmId + " for user " + username, null);
                return false;
            }

            if (!films.remove(film)) {
                loggerService.logError("Failed to remove film from list: Film ID " + filmId + " for user " + username, null);
                return false;
            }

            return !saveFilmsToJson(username, films);

        } catch (Exception e) {
            loggerService.logError("Unexpected error while deleting film ID " + filmId + " for user " + username, e);
            return false;
        }
    }

    private boolean storeVideoFile(String username, MultipartFile file, Film film, int quality) {
        try {
            if (!validateFile(file, allowedFilmExtensions)) {
                return false;
            }

            if (!createDirectoryIfNotExists(username, film.getId())) {
                return false;
            }

            // A fájlt mentjük a feltöltési mappába
            Path uploadPath = Paths.get(storageDir, username, String.valueOf(film.getId())).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(file.getOriginalFilename());

            Files.copy(file.getInputStream(), filePath);
            VideoConverterService.addToQueue(filePath, quality, film, username);
            return true;
        } catch (Exception ex) {
            loggerService.logError("Nem sikerült a fájlt elmenteni: " + file.getOriginalFilename(), ex);
            return false;
        }
    }

    private boolean createDirectoryIfNotExists(String username, Long filmId) {
        Path uploadPath = Paths.get(storageDir, username, String.valueOf(filmId)).toAbsolutePath().normalize();
        if (Files.exists(uploadPath)) {
            return true;
        }
        try {
            Files.createDirectories(uploadPath);
            return true;
        } catch (IOException e) {
            loggerService.logError("Nem sikerült létrehozni a könyvtárat: " + uploadPath, e);
            return false;
        }
    }

    private boolean storeImageFile(String username, MultipartFile imageFile, Film film) {
        try {
            if (!validateFile(imageFile, allowedPictureExtensions)) {
                return false;
            }

            String originalFileName = imageFile.getOriginalFilename();
            String targetFileName = originalFileName.replaceFirst("\\.[^.]+$", ".jpg");

            if (!createDirectoryIfNotExists(username, film.getId())) {
                return false;
            }

            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageFile.getBytes()));

            if (originalImage == null) {
                loggerService.logError("Nem sikerült beolvasni a képet: " + originalFileName, null);
                return false;
            }

            byte[] resizedBytes = resizeImage(originalImage, maxWidth, maxHeight);
            Path uploadPath = Paths.get(storageDir, username, String.valueOf(film.getId())).toAbsolutePath().normalize();
            Path imagePath = uploadPath.resolve(targetFileName);
            Files.write(imagePath, resizedBytes);
            return true;
        } catch (IOException ex) {
            loggerService.logError("Nem sikerült a kép fájlt elmenteni: " + imageFile.getOriginalFilename(), ex);
            return false;
        }
    }

    private byte[] resizeImage(BufferedImage originalImage, int maxWidth, int maxHeight) {
        try {
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

            // Új kép létrehozása a maxWidth és maxHeight mérettel
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

        } catch (IOException e) {
            loggerService.logError("Failed to resize image: " + e.getMessage(), e);
            return new byte[0]; // Üres tömb visszaadása hiba esetén
        } catch (Exception e) {
            loggerService.logError("Unexpected error while resizing image.", e);
            return new byte[0]; // Üres tömb visszaadása váratlan hiba esetén
        }
    }

    private boolean validateFile(MultipartFile imageFile, String[] allowedExtensions) {
        String originalFileName = imageFile.getOriginalFilename();
        if (originalFileName == null || !originalFileName.contains(".")) {
            loggerService.logError("A fájlnak kiterjesztést kell tartalmaznia.", null);
            return false;
        }

        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toLowerCase();
        if (!Arrays.asList(allowedExtensions).contains(fileExtension)) {
            loggerService.logError("Nem megfelelő kiterjesztés: " + fileExtension + Arrays.toString(allowedExtensions), null);
            return false;
        }

        return true;
    }

    public boolean deleteFilm(String username, Long filmId) {
        Film film = getFilmById(username, filmId);

        if (film == null) {
            loggerService.logError("Failed to delete film: Film with ID " + filmId + " not found for user " + username, null);
            return false;
        }

        if (!deleteFolder(username, film.getId())) {
            return false;
        }

        return !deleteFilmByIdFromJson(username, filmId);
    }

    private boolean deleteFolder(String username, Long folderName) {
        // Elérési útvonal felépítése
        String path = storageDir + "/" + username + "/" + folderName;
        File folder = new File(path);

        // Ellenőrizzük, hogy a mappa létezik-e
        if (!folder.exists()) {
            loggerService.logError("Folder does not exist: " + path, null);
            return false;
        }

        // Ellenőrizzük, hogy a megadott elérési útvonal egy mappa-e
        if (!folder.isDirectory()) {
            loggerService.logError("Not a directory: " + path, null);
            return false;
        }

        // Rekurzívan töröljük a mappa tartalmát
        if (!deleteContents(folder)) {
            return false;
        }

        // Töröljük magát a mappát
        if (folder.delete()) {
            return true;
        } else {
            loggerService.logError("Failed to delete folder: " + path, null);
            return false;
        }
    }

    private boolean deleteContents(File folder) {
        File[] contents = folder.listFiles();
        boolean success = true;

        if (contents != null) {
            for (File file : contents) {
                if (file.isDirectory()) {
                    // Rekurzívan töröljük a mappa tartalmát
                    if (!deleteContents(file)) {
                        loggerService.logError("Failed to delete directory contents: " + file.getAbsolutePath(), null);
                        success = false;
                    }
                }
                // Töröljük a fájlokat vagy az üres mappákat
                if (!file.delete()) {
                    loggerService.logError("Failed to delete file or directory: " + file.getAbsolutePath(), null);
                    success = false;
                }
            }
        }

        return success;
    }

    @Transactional
    public boolean modifyFilm(String username, Film film, MultipartFile picture) {
        try {
            Film ogFilm = getFilmById(username, film.getId());
            if (ogFilm == null) {
                return false;
            }

            film.setFilmPath(ogFilm.getFilmPath());
            film.setPicturePath(ogFilm.getPicturePath());

            if (!isValidFilm(film)) {
                return false;
            }

            if (!picture.isEmpty()) {
                File pictureFile = new File(film.getPicturePath());
                if (!pictureFile.delete()) {
                    loggerService.logError("Failed to delete existing picture: " + film.getPicturePath() + " for user " + username, null);
                    return false;
                }

                film.setPicturePath(storageDir + "/" + username + "/" + film.getId() + "/" +
                        Objects.requireNonNull(picture.getOriginalFilename()).replaceAll("\\.\\w+$", ".jpg"));

                if (!storeImageFile(username, picture, film)) {
                    return false;
                }
            }

            if (deleteFilmByIdFromJson(username, film.getId())) {
                return false;
            }

            return addFilmToClient(username, film);
        } catch (Exception e) {
            loggerService.logError("Unexpected error while modifying film for user: " + username, e);
            return false;
        }
    }

    public long getTotalStorageUsed(String username) {
        Path userDirectory = Paths.get(storageDir, username);

        if (!Files.exists(userDirectory) || !Files.isDirectory(userDirectory)) {
            return 0;
        }

        try (Stream<Path> files = Files.walk(userDirectory)) {
            return files.filter(Files::isRegularFile)
                    .mapToLong(file -> {
                        try {
                            return Files.size(file);
                        } catch (IOException e) {
                            loggerService.logError("Failed to get file size: " + file, e);
                            return 0;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            loggerService.logError("Failed to calculate storage usage for user: " + username, e);
            return 0;
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