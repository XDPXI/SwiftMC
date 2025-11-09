package dev.xdpxi.swiftmc;

import com.formdev.flatlaf.FlatDarkLaf;
import dev.xdpxi.swiftmc.api.plugin.PluginManager;
import dev.xdpxi.swiftmc.utils.Log;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GUI extends JFrame {
    private JTextArea logArea;
    private JButton startButton;
    private JButton stopButton;
    private JButton restartButton;
    private JPanel pluginsPanel;
    private final List<PluginRow> pluginRows = new ArrayList<>();
    private volatile boolean serverRunning = false;
    private Process serverProcess;

    public GUI() {
        setTitle("SwiftMC Launcher");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();

        // Console Tab
        JPanel consolePanel = createConsolePanel();
        tabbedPane.addTab("Console", consolePanel);

        // Plugins Tab
        JPanel pluginsTab = createPluginsPanel();
        tabbedPane.addTab("Plugins", pluginsTab);

        add(tabbedPane);

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
            Main.setGuiInstance(gui);
            gui.setVisible(true);
        });
    }

    private JPanel createConsolePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Bottom panel with buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
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

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createPluginsPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Plugins list panel
        pluginsPanel = new JPanel();
        pluginsPanel.setLayout(new BoxLayout(pluginsPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(pluginsPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Refresh button
        JButton refreshButton = new JButton("Refresh Plugin List");
        refreshButton.addActionListener(e -> refreshPluginsList());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.add(refreshButton);

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Initial load
        refreshPluginsList();

        return mainPanel;
    }

    private void refreshPluginsList() {
        pluginsPanel.removeAll();
        pluginRows.clear();

        File pluginsFolder = new File("plugins");
        if (!pluginsFolder.exists()) {
            pluginsFolder.mkdirs();
        }

        File[] pluginFiles = pluginsFolder.listFiles((dir, name) -> name.endsWith(".jar"));

        if (pluginFiles == null || pluginFiles.length == 0) {
            JLabel noPluginsLabel = new JLabel("No plugins found in plugins folder");
            noPluginsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            noPluginsLabel.setForeground(Color.GRAY);
            pluginsPanel.add(Box.createVerticalStrut(20));
            pluginsPanel.add(noPluginsLabel);
        } else {
            // Sort alphabetically
            Arrays.sort(pluginFiles, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

            for (File file : pluginFiles) {
                PluginRow row = new PluginRow(file);
                pluginRows.add(row);
                pluginsPanel.add(row.getPanel());
                pluginsPanel.add(Box.createVerticalStrut(5));
            }
        }

        pluginsPanel.revalidate();
        pluginsPanel.repaint();
    }

    public void setPluginManager(PluginManager pluginManager) {
    }

    private static class PluginRow {
        private final JPanel panel;

        public PluginRow(File pluginFile) {

            panel = new JPanel();
            panel.setLayout(new BorderLayout(10, 0));
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.GRAY, 1),
                    new EmptyBorder(10, 15, 10, 15)
            ));
            panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

            // Left side - plugin info
            JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 5));
            infoPanel.setOpaque(false);

            JLabel nameLabel = new JLabel(pluginFile.getName());
            nameLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));

            infoPanel.add(nameLabel);

            panel.add(infoPanel, BorderLayout.CENTER);
        }

        public JPanel getPanel() {
            return panel;
        }
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