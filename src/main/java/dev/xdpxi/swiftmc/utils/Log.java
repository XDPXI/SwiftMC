package dev.xdpxi.swiftmc.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.zip.GZIPOutputStream;

import static dev.xdpxi.swiftmc.Main.config;

public class Log {

    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String GRAY = "\u001B[90m";

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private static BufferedWriter fileWriter;
    private static Path logFilePath;

    static {
        try {
            Files.createDirectories(Path.of("logs"));
            String logFileName = "logs/log_" + LocalDateTime.now().format(FILE_TIME_FORMAT) + ".log";
            logFilePath = Path.of(logFileName);
            fileWriter = Files.newBufferedWriter(logFilePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println(RED + "Failed to initialize log file: " + e.getMessage() + RESET);
            e.printStackTrace();
        }
    }

    private static String formatMessage(String level, String color, String message, Object... args) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        String thread = Thread.currentThread().getName();
        return String.format("%s[%s] [%s] %s%s%n", color, timestamp, thread, String.format(message, args), RESET);
    }

    private static void write(String formattedMessage) {
        System.out.print(formattedMessage);
        try {
            if (fileWriter != null) {
                fileWriter.write(formattedMessage.replaceAll("\u001B\\[[;\\d]*m", "")); // Remove ANSI colors
                fileWriter.flush();
            }
        } catch (IOException e) {
            if (Objects.equals(e.getMessage(), "Stream closed")) return;
            System.err.println(RED + "Failed to write to log file: " + e.getMessage() + RESET);
            e.printStackTrace();
        }
    }

    public static void debug(String message, Object... args) {
        if (config.debugEnabled) write(formatMessage("DEBUG", GRAY, message, args));
    }

    public static void info(String message, Object... args) {
        write(formatMessage("INFO", GREEN, message, args));
    }

    public static void warn(String message, Object... args) {
        write(formatMessage("WARN", YELLOW, message, args));
    }

    public static void error(String message, Object... args) {
        write(formatMessage("ERROR", RED, message, args));
    }

    public static void error(Throwable throwable, String message, Object... args) {
        write(formatMessage("ERROR", RED, message, args));
        throwable.printStackTrace(System.err);
        try {
            if (fileWriter != null) {
                throwable.printStackTrace();
                fileWriter.flush();
            }
        } catch (IOException e) {
            System.err.println(RED + "Failed to write throwable to log file: " + e.getMessage() + RESET);
        }
    }
    
    public static void close() {
        if (fileWriter == null) return;
        try {
            fileWriter.close();

            Path gzPath = Path.of(logFilePath.toString() + ".gz");
            try (GZIPOutputStream gos = new GZIPOutputStream(Files.newOutputStream(gzPath));
                 InputStream fis = Files.newInputStream(logFilePath)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    gos.write(buffer, 0, len);
                }
            }

            Files.deleteIfExists(logFilePath);
        } catch (IOException e) {
            System.err.println(RED + "Failed to close/compress log file: " + e.getMessage() + RESET);
            e.printStackTrace();
        }
    }
}
