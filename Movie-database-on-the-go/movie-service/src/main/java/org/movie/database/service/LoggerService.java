package org.movie.database.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Stream;

@Service
public class LoggerService {
    private static final Logger errorLogger = Logger.getLogger("ErrorLogger");
    private final FilmService filmService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public LoggerService(FilmService filmService) {
        this.filmService = filmService;
        setupErrorLogger();
    }

    private void setupErrorLogger() {
        try {
            Path logDir = Paths.get("logs");
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }
            Path errorLogFile = logDir.resolve("errors.log");
            FileHandler fileHandler = new FileHandler(errorLogFile.toString(), true);
            fileHandler.setFormatter(new SimpleFormatter());
            errorLogger.addHandler(fileHandler);
            errorLogger.setUseParentHandlers(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logUserActivity(String username, Long movieID) {
        try {
            String storageDir = filmService.getStorageDir();
            Path userLogDir = Paths.get(storageDir, username);
            if (!Files.exists(userLogDir)) {
                Files.createDirectories(userLogDir);
            }
            Path logFile = userLogDir.resolve("history.log");
            String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
            String logMessage = timestamp + " - Watched movie: " + movieID + "\n";

            Files.write(logFile, logMessage.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            logError("Failed to log user activity", e);
        }
    }

    public void logError(String message, Throwable throwable) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        errorLogger.log(Level.SEVERE, timestamp + " - " + message, throwable);
        cleanupOldLogs();
    }

    private void cleanupOldLogs() {
        Path logDir = Paths.get("logs");
        if (!Files.exists(logDir)) {
            return;
        }
        try (Stream<Path> files = Files.list(logDir)) {
            LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
            files.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".log"))
                    .forEach(file -> {
                        try {
                            FileTime fileTime = Files.getLastModifiedTime(file);
                            LocalDate fileDate = Instant.ofEpochMilli(fileTime.toMillis())
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate();
                            if (fileDate.isBefore(oneMonthAgo)) {
                                Files.delete(file);
                            }
                        } catch (IOException e) {
                            errorLogger.log(Level.WARNING, "Failed to delete old log file: " + file, e);
                        }
                    });
        } catch (IOException e) {
            errorLogger.log(Level.WARNING, "Failed to clean up old logs", e);
        }
    }
}
