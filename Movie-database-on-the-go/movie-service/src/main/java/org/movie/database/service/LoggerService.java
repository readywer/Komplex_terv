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
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Stream;

@Service
public class LoggerService {
    private static final String STORAGE_DIR = "data";
    private static final Logger errorLogger = Logger.getLogger("ErrorLogger");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public LoggerService() {
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
            Path userLogDir = Paths.get(STORAGE_DIR, username);
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

    public static List<Long> getLastWatchedMovies(String username) {
        Path userLogDir = Paths.get(STORAGE_DIR, username);
        Path logFile = userLogDir.resolve("history.log");

        if (!Files.exists(logFile)) {
            return List.of();
        }

        try {
            List<String> lines = Files.readAllLines(logFile);
            Collections.reverse(lines);

            Set<Long> uniqueMovies = new LinkedHashSet<>();

            for (String line : lines) {
                if (line.contains("Watched movie: ")) {
                    try {
                        Long movieId = Long.parseLong(line.substring(line.lastIndexOf(": ") + 2));
                        uniqueMovies.add(movieId);
                        if (uniqueMovies.size() == 4) {
                            break;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return new ArrayList<>(uniqueMovies);
        } catch (IOException e) {
            return List.of();
        }
    }


    public void deleteLogEntry(String username, Long movieID) {
        Path userLogDir = Paths.get(STORAGE_DIR, username);
        Path logFile = userLogDir.resolve("history.log");

        if (!Files.exists(logFile)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(logFile);
            List<String> updatedLines = lines.stream()
                    .filter(line -> !line.contains("Watched movie: " + movieID))
                    .toList();
            Files.write(logFile, updatedLines, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException e) {
            logError("Failed to delete log entry for movieID: " + movieID, e);
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
