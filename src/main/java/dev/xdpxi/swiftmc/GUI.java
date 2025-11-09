package dev.xdpxi.swiftmc;

import com.formdev.flatlaf.FlatDarkLaf;
import dev.xdpxi.swiftmc.utils.Log;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;

public class GUI extends JFrame {
    private final JTextArea logArea;
    private final JButton startButton;
    private final JButton stopButton;
    private final JButton restartButton;
    private volatile boolean serverRunning = false;
    private Process serverProcess;

    public GUI() {
        setTitle("SwiftMC Launcher");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Bottom panel with buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        startButton = new JButton("Start Server");
        stopButton = new JButton("Stop Server");
        restartButton = new JButton("Restart Server");

        stopButton.setEnabled(false);
        restartButton.setEnabled(false);

        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());
        restartButton.addActionListener(e -> restartServer());

        bottomPanel.add(startButton);
        bottomPanel.add(stopButton);
        bottomPanel.add(restartButton);

        // Add components
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Redirect console to the text area
        redirectOutput();

        // Handle window closing
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (serverRunning) {
                    stopServer();
                    try {
                        Thread.sleep(1000); // Give server time to shut down
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.exit(0);
            }
        });
    }

    public static void launch() {
        // Set FlatLaf Dark theme
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf Dark theme: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            GUI gui = new GUI();
            gui.setVisible(true);
        });
    }

    private void redirectOutput() {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) {
                SwingUtilities.invokeLater(() -> {
                    logArea.append(String.valueOf((char) b));
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                });
            }

            @Override
            public void write(byte @NotNull [] b, int off, int len) {
                SwingUtilities.invokeLater(() -> {
                    logArea.append(new String(b, off, len));
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                });
            }
        };

        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }

    private void startServer() {
        try {
            logArea.setText("");
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            restartButton.setEnabled(true);

            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    "-cp",
                    System.getProperty("java.class.path"),
                    "dev.xdpxi.swiftmc.Main",
                    "--nogui"
            );
            pb.redirectErrorStream(true);
            serverProcess = pb.start();

            serverRunning = true;

            // Read server output in background thread
            new Thread(() -> {
                try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(serverProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String logLine = line;
                        SwingUtilities.invokeLater(() -> {
                            logArea.append(logLine + "\n");
                            logArea.setCaretPosition(logArea.getDocument().getLength());
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, "Server-Output-Reader").start();

            Log.info("Server process started.");

        } catch (Exception e) {
            Log.error("Failed to start server process: " + e.getMessage());
            e.printStackTrace();
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            restartButton.setEnabled(false);
        }
    }

    private void stopServer() {
        if (serverRunning && serverProcess != null) {
            Log.info("Sending stop command to server...");
            serverRunning = false;

            try {
                // Send "stop" to server stdin
                serverProcess.getOutputStream().write("stop\n".getBytes());
                serverProcess.getOutputStream().flush();

                // Wait up to 5 seconds for graceful shutdown
                if (!serverProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    Log.warn("Server did not exit in time; forcing termination...");
                    serverProcess.destroyForcibly();
                }
            } catch (Exception e) {
                Log.error("Error stopping server: " + e.getMessage());
                serverProcess.destroyForcibly();
            }

            serverProcess = null;
            SwingUtilities.invokeLater(() -> {
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                restartButton.setEnabled(false);
                Log.info("Server stopped.");
            });
        }
    }

    private void restartServer() {
        Log.info("Restarting server...");
        stopServer();
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                SwingUtilities.invokeLater(this::startServer);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}