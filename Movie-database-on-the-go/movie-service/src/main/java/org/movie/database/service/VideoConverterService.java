package org.movie.database.service;

import org.movie.database.domain.Film;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class VideoConverterService {
    @Autowired
    private LoggerService loggerService;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final LinkedBlockingQueue<Map<String, Object>> queue = new LinkedBlockingQueue<>();
    private static volatile boolean isProcessing = false;

    private static final String FFMPEG_PATH = new File("ffmpeg/bin/ffmpeg").getAbsolutePath();
    private static final boolean DEBUG_MODE = false;

    public static void addToQueue(Path inputFilePath, int quality, Film film, String username) {
        queue.offer(Map.of(
                "inputFilePath", inputFilePath,
                "quality", Math.max(15, Math.min(30, quality)),
                "film", film,
                "username", username
        ));
        processQueue();
    }

    private static synchronized void processQueue() {
        if (isProcessing || queue.isEmpty()) return;

        isProcessing = true;
        executor.execute(() -> {
            try {
                Map<String, Object> task;
                while ((task = queue.poll()) != null) {
                    Path inputFilePath = (Path) task.get("inputFilePath");
                    int quality = (int) task.get("quality");
                    Film film = (Film) task.get("film");
                    String username = (String) task.get("username");

                    if (convertVideo(inputFilePath, quality)) {
                        modifyFilmPath(username, film);
                    }
                }
            } catch (Exception e) {
                LoggerService.logConversion("Hiba a videófeldolgozás során.", e);
            } finally {
                isProcessing = false;
            }
        });
    }

    private static boolean convertVideo(Path inputFilePath, int quality) {
        File inputFile = inputFilePath.toFile();
        if (!inputFile.exists()) {
            LoggerService.logConversion("A fájl nem létezik: " + inputFilePath, null);
            return false;
        }
        String outputFileName = "convert_" + inputFile.getName().replaceAll("\\.\\w+$", ".mp4");
        Path outputFilePath = inputFile.getParentFile().toPath().resolve(outputFileName);
        String gpuCodec = getAvailableGPUCodec();
        List<String> qualityArgs = getQualityArgs(gpuCodec, quality);
        String thread = String.valueOf(Runtime.getRuntime().availableProcessors() - 2);
        List<String> command = new ArrayList<>(List.of(
                FFMPEG_PATH, "-y", "-i", inputFilePath.toString(),
                "-c:v", gpuCodec, "-preset", getPreset(gpuCodec),
                "-threads", thread, "-fps_mode", "cfr"
        ));
        command.addAll(qualityArgs);
        command.addAll(List.of("-c:a", "aac", "-b:a", "192k", "-map_chapters", "0", outputFilePath.toString()));
        if (DEBUG_MODE) LoggerService.logConversion("FFmpeg Parancs: " + String.join(" ", command), null);
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command).redirectErrorStream(true);
            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                if (DEBUG_MODE) {
                    reader.lines().forEach(line -> LoggerService.logConversion(line, null));
                }
            }

            if (process.waitFor() != 0) {
                LoggerService.logConversion("FFmpeg sikertelen konverzió.", null);
                Files.deleteIfExists(outputFilePath);
                return false;
            }

            Files.delete(inputFilePath);
            Path finalFilePath = inputFile.getParentFile().toPath().resolve(inputFile.getName().replaceAll("\\.\\w+$", ".mp4"));
            Files.move(outputFilePath, finalFilePath);

            if (DEBUG_MODE) LoggerService.logConversion("Konverzió befejezve: " + finalFilePath, null);
            return true;
        } catch (IOException | InterruptedException e) {
            LoggerService.logConversion("Hiba a konvertálás során.", e);
            return false;
        }
    }

    private static List<String> getQualityArgs(String codec, int quality) {
        return switch (codec) {
            case "hevc_nvenc", "hevc_amf" -> List.of("-cq", String.valueOf(quality));
            case "hevc_qsv" -> List.of("-global_quality", String.valueOf(quality));
            default -> List.of("-crf", String.valueOf(quality));
        };
    }

    private static String getPreset(String codec) {
        return switch (codec) {
            case "hevc_qsv", "hevc_amf" -> "medium";
            case "hevc_nvenc" -> "medium";
            default -> "fast";
        };
    }

    private static String getAvailableGPUCodec() {
        List<String> gpuList = new ArrayList<>();

        try {
            Process process = new ProcessBuilder("cmd.exe", "/c", "wmic path win32_videocontroller get name")
                    .redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                reader.lines().map(String::trim).filter(line -> !line.isEmpty() && !line.equalsIgnoreCase("Name"))
                        .forEach(gpuList::add);
            }
            process.waitFor();
        } catch (Exception e) {
            return "libx265";
        }

        if (gpuList.stream().anyMatch(gpu -> gpu.toLowerCase().contains("intel"))) return "hevc_qsv";
        if (gpuList.stream().anyMatch(gpu -> gpu.toLowerCase().contains("nvidia"))) return "hevc_nvenc";
        if (gpuList.stream().anyMatch(gpu -> gpu.toLowerCase().contains("amd") || gpu.toLowerCase().contains("radeon")))
            return "hevc_amf";

        return "libx265";
    }

    private static void modifyFilmPath(String username, Film film) {
        FilmService filmService = new FilmService();
        film.setFilmPath(film.getFilmPath().replaceAll("\\.\\w+$", ".mp4"));
        film.setProcessing(false);
        filmService.deleteFilmByIdFromJson(username, film.getId());
        filmService.addFilmToClient(username, film);
    }
}
