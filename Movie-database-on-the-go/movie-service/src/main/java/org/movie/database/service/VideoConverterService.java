package org.movie.database.service;

import org.movie.database.domain.Film;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class VideoConverterService {
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
                        deleteOriginalFile(inputFilePath.toString());
                        modifyFilmPath(username, film);
                    }
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                isProcessing = false;
            }
        });
    }

    private static boolean convertVideo(Path inputFilePath, int quality) throws IOException, InterruptedException {
        File inputFile = inputFilePath.toFile();
        if (!inputFile.exists()) {
            System.err.println("Hiba: A fájl nem létezik: " + inputFilePath);
            return false;
        }

        String outputFilePath = inputFile.getParent() + File.separator + inputFile.getName().replaceAll("\\.\\w+$", ".mp4");
        String gpuCodec = getAvailableGPUCodec();
        List<String> qualityArgs = getQualityArgs(gpuCodec, quality);

        List<String> command = new ArrayList<>(List.of(
                FFMPEG_PATH, "-i", inputFilePath.toString(),
                "-c:v", gpuCodec, "-preset", getPreset(gpuCodec),
                "-threads", "14", "-vsync", "cfr"
        ));
        command.addAll(qualityArgs);
        command.addAll(List.of("-c:a", "aac", "-b:a", "192k", "-c:s", "copy", "-map_chapters", "0", outputFilePath));

        if (DEBUG_MODE) System.out.println("FFmpeg Parancs: " + String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command).redirectErrorStream(true);
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            if (DEBUG_MODE) reader.lines().forEach(System.out::println);
        }

        if (process.waitFor() != 0) {
            System.err.println("Hiba: FFmpeg sikertelen konverzió.");
            return false;
        }

        if (DEBUG_MODE) System.out.println("Konverzió befejezve: " + outputFilePath);
        return true;
    }

    private static List<String> getQualityArgs(String codec, int quality) {
        return switch (codec) {
            case "hevc_qsv", "hevc_nvenc", "hevc_amf" -> List.of("-cq", String.valueOf(quality));
            default -> List.of("-crf", String.valueOf(quality));
        };
    }

    private static String getPreset(String codec) {
        return switch (codec) {
            case "hevc_qsv", "hevc_amf" -> "quality";
            case "hevc_nvenc" -> "slow";
            default -> "medium";
        };
    }

    private static void deleteOriginalFile(String filePath) {
        File file = new File(filePath);
        if (file.exists() && file.delete()) {
            System.out.println("Eredeti fájl törölve: " + filePath);
        } else {
            System.err.println("Hiba: Nem sikerült törölni a fájlt: " + filePath);
        }
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
