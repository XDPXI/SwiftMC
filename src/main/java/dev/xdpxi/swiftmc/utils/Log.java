package dev.xdpxi.swiftmc.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static dev.xdpxi.swiftmc.Main.config;

public class Log {
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String GRAY = "\u001B[90m";

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static String formatMessage(String level, String color, String message, Object... args) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        String thread = Thread.currentThread().getName();
        return String.format("%s[%s] [%s] %s%s%n", color, timestamp, thread, String.format(message, args), RESET);
    }

    public static void debug(String message, Object... args) {
        if (config.debugEnabled) {
            System.out.print(formatMessage("DEBUG", GRAY, message, args));
        }
    }

    public static void info(String message, Object... args) {
        System.out.print(formatMessage("INFO", GREEN, message, args));
    }

    public static void warn(String message, Object... args) {
        System.out.print(formatMessage("WARN", YELLOW, message, args));
    }

    public static void error(String message, Object... args) {
        System.err.print(formatMessage("ERROR", RED, message, args));
    }

    public static void error(Throwable throwable, String message, Object... args) {
        System.err.print(formatMessage("ERROR", RED, message, args));
        throwable.printStackTrace(System.err);
    }
}
