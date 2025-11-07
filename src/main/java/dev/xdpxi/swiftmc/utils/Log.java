package dev.xdpxi.swiftmc.utils;

public class Log {
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String GRAY = "\u001B[90m";

    public static void debug(String message, Object... args) {
        System.out.printf(GRAY + "[Debug] " + message + RESET + "%n", args);
    }

    public static void info(String message, Object... args) {
        System.out.printf(GREEN + "[Info] " + message + RESET + "%n", args);
    }

    public static void warn(String message, Object... args) {
        System.out.printf(YELLOW + "[Warn] " + message + RESET + "%n", args);
    }

    public static void error(String message, Object... args) {
        System.err.printf(RED + "[Error] " + message + RESET + "%n", args);
    }
}
