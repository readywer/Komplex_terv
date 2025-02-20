package org.movie.database.service;

import org.movie.database.domain.Film;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class VideoConverterService {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final LinkedBlockingQueue<ConversionTask> queue = new LinkedBlockingQueue<>();
    private static volatile boolean isProcessing = false;

    private static final String FFMPEG_PATH = new File("ffmpeg/bin/ffmpeg").getAbsolutePath();

    private static final boolean DEBUG_MODE = false;

    public static void addToQueue(Path inputFilePath, int quality, Film film, String username) {
        int clampedQuality = Math.max(15, Math.min(30, quality));
        queue.offer(new ConversionTask(inputFilePath, clampedQuality, film, username));
        processQueue();
    }

    private static synchronized void processQueue() {
        if (!isProcessing && !queue.isEmpty()) {
            isProcessing = true;
            executor.submit(() -> {
                try {
                    while (!queue.isEmpty()) {
                        ConversionTask task = queue.poll();
                        if (task != null) {
                            boolean success = convertVideo(task.inputFilePath, task.quality);
                            if (success) {
                                deleteOriginalFile(String.valueOf(task.inputFilePath));
                                modifyFilmPath(task.username, task.film);
                            }
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    isProcessing = false;
                }
            });
        }
    }

    private static boolean convertVideo(Path inputFilePath, int quality) throws IOException, InterruptedException {
        File inputFile = new File(String.valueOf(inputFilePath));
        if (!inputFile.exists()) {
            System.err.println("Hiba: A fájl nem létezik: " + inputFilePath);
            return false;
        }

        String outputFilePath = inputFile.getParent() + File.separator +
                inputFile.getName().replaceAll("\\.\\w+$", ".mp4");

        String gpuCodec = getAvailableGPUCodec();
        String preset;
        List<String> qualityArgs;

        switch (gpuCodec) {
            case "hevc_qsv":
                preset = "quality";
                qualityArgs = Arrays.asList("-cq", String.valueOf(quality));
                break;
            case "hevc_nvenc":
                preset = "slow";
                qualityArgs = Arrays.asList("-cq", String.valueOf(quality));
                break;
            case "hevc_amf":
                preset = "quality";
                qualityArgs = Arrays.asList("-cq", String.valueOf(quality));
                break;
            default:
                gpuCodec = "libx265";
                preset = "medium";
                qualityArgs = Arrays.asList("-crf", String.valueOf(quality));
        }
        String filePath = String.valueOf(inputFilePath);
        List<String> command = new ArrayList<>();
        command.addAll(Arrays.asList(
                FFMPEG_PATH, "-i", filePath,
                "-c:v", gpuCodec, "-preset", preset,
                "-threads", "14", "-vsync", "cfr"
        ));
        command.addAll(qualityArgs);
        //command.addAll(Arrays.asList("-c:a", "copy", "-c:s", "copy", "-map_chapters", "0")); //ac-3...not supported
        command.addAll(Arrays.asList("-c:a", "aac", "-b:a", "192k", "-c:s", "copy", "-map_chapters", "0"));
        command.add(outputFilePath);

        if (DEBUG_MODE) {
            System.out.println("FFmpeg Parancs: " + String.join(" ", command));
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (DEBUG_MODE) {
                    System.out.println(line);
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("Hiba: FFmpeg sikertelen konverzió. Kilépési kód: " + exitCode);
            return false;
        } else if (DEBUG_MODE) {
            System.out.println("Konverzió befejezve: " + outputFilePath);
        }

        return true;
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
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", "wmic path win32_videocontroller get name");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.toLowerCase().contains("name")) {
                        gpuList.add(line);
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            return "libx265";
        }

        boolean hasIntel = gpuList.stream().anyMatch(gpu -> gpu.toLowerCase().contains("intel"));
        boolean hasNvidia = gpuList.stream().anyMatch(gpu -> gpu.toLowerCase().contains("nvidia"));
        boolean hasAmd = gpuList.stream().anyMatch(gpu -> gpu.toLowerCase().contains("amd") || gpu.toLowerCase().contains("radeon"));

        if (hasIntel) return "hevc_qsv";
        if (hasNvidia) return "hevc_nvenc";
        if (hasAmd) return "hevc_amf";

        return "libx265";
    }

    private static class ConversionTask {
        Path inputFilePath;
        int quality;
        Film film;
        String username;

        ConversionTask(Path inputFilePath, int quality, Film film, String username) {
            this.inputFilePath = inputFilePath;
            this.quality = quality;
            this.film = film;
            this.username = username;
        }
    }

    private static void modifyFilmPath(String username, Film film) {
        FilmService filmService = new FilmService();
        film.setFilmPath(film.getFilmPath().replaceAll("\\.\\w+$", ".mp4"));
        filmService.deleteFilmByIdFromJson(username, film.getId());
        filmService.addFilmToClient(username, film);
    }
}
