package dev.xdpxi.swiftmc.utils;

import com.formdev.flatlaf.FlatDarkLaf;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerLogger {
    public static void setup(String[] args) throws IOException {
        Path logFolder = Path.of("logs");
        Files.createDirectories(logFolder);

        String timestamp = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss").format(new Date());
        File logFile = logFolder.resolve("log_" + timestamp + ".log").toFile();

        PrintStream fileOut = new PrintStream(new FileOutputStream(logFile, true), true);
        System.setOut(fileOut);
        System.setErr(fileOut);

        boolean noGui = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--no-gui")) {
                noGui = true;
                break;
            }
        }

        if (!noGui) {
            launchConsoleUI(fileOut);
        }
    }

    private static void launchConsoleUI(PrintStream fileOut) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JFrame frame = new JFrame("SwiftMC Server Console");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 450);

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(textArea);
        frame.add(scrollPane);

        frame.setVisible(true);

        PrintStream guiOut = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                textArea.append(String.valueOf((char) b));
                textArea.setCaretPosition(textArea.getDocument().getLength());
            }

            @Override
            public void write(byte @NotNull [] b, int off, int len) {
                String s = new String(b, off, len);
                textArea.append(s);
                textArea.setCaretPosition(textArea.getDocument().getLength());
            }
        }, true);

        System.setOut(new PrintStream(new MultiOutputStream(fileOut, guiOut), true));
        System.setErr(new PrintStream(new MultiOutputStream(fileOut, guiOut), true));
    }

    private static class MultiOutputStream extends OutputStream {
        private final OutputStream[] streams;

        public MultiOutputStream(OutputStream... streams) {
            this.streams = streams;
        }

        @Override
        public void write(int b) throws IOException {
            for (OutputStream s : streams) s.write(b);
        }

        @Override
        public void write(byte @NotNull [] b, int off, int len) throws IOException {
            for (OutputStream s : streams) s.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            for (OutputStream s : streams) s.flush();
        }

        @Override
        public void close() throws IOException {
            for (OutputStream s : streams) s.close();
        }
    }
}
