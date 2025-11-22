package dev.xdpxi.swiftmc;

import dev.xdpxi.swiftmc.commands.Adventure;
import dev.xdpxi.swiftmc.commands.Creative;
import dev.xdpxi.swiftmc.commands.Spectator;
import dev.xdpxi.swiftmc.commands.Survival;
import dev.xdpxi.swiftmc.events.*;
import dev.xdpxi.swiftmc.mobs.MobSpawner;
import dev.xdpxi.swiftmc.player.PlayerDataManager;
import dev.xdpxi.swiftmc.plugin.PluginManager;
import dev.xdpxi.swiftmc.utils.Config;
import dev.xdpxi.swiftmc.utils.Log;
import dev.xdpxi.swiftmc.utils.TerrainGenerator;
import io.github.togar2.fluids.MinestomFluids;
import io.github.togar2.pvp.MinestomPvP;
import io.github.togar2.pvp.feature.CombatFeatureSet;
import io.github.togar2.pvp.feature.CombatFeatures;
import io.github.togar2.pvp.utils.CombatVersion;
import net.hollowcube.polar.PolarLoader;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.LightingChunk;

import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Main {
    public static Config config;
    private static Path lockFile;
    private static InstanceContainer instanceContainer;
    private static PolarLoader polarLoader;
    private static Path polarFile;
    private static Path polarGzFile;
    private static PluginManager pluginManager;
    private static MobSpawner mobSpawner;
    private static volatile boolean isShuttingDown = false;

    static void main(String[] args) {
        boolean fromGui = args.length > 0 && args[0].equals("--nogui");

        if (!fromGui && System.console() == null && !GraphicsEnvironment.isHeadless()) {
            GUI.launch();
        } else {
            try {
                server();
            } catch (Exception e) {
                Log.error("Failed to start server: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    static void server() throws Exception {
        // Get the path of the running JAR
        File jarFile = new File(
                Main.class
                        .getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI()
        );

        // Check for lock file
        lockFile = Path.of(jarFile.getPath().replaceFirst("\\.jar$", ".lck"));
        if (Files.exists(lockFile)) {
            Log.error("Server is already running or did not shut down properly!");
            Log.error("If you're sure the server is not running, delete the lock file: " + lockFile);
            System.exit(1);
            return;
        }

        // Create lock file
        try {
            Files.writeString(lockFile, "Server started at: " + java.time.LocalDateTime.now());
            Log.info("Lock file created.");
        } catch (Exception e) {
            Log.error("Failed to create lock file: " + e.getMessage());
            System.exit(1);
            return;
        }

        Log.info("Starting server setup...");

        // Setup config
        try {
            config = Config.loadOrCreate();
            Log.info("Configuration loaded successfully.");
        } catch (Exception e) {
            Log.error("Failed to load configuration: " + e.getMessage());
            throw e;
        }

        // Init server
        MinecraftServer minecraftServer;
        if (config.velocityEnabled) {
            minecraftServer = MinecraftServer.init(new Auth.Velocity(config.velocitySecret));
            Log.info("Server initialized with Velocity support.");
        } else {
            minecraftServer = MinecraftServer.init();
            Log.info("Server initialized in offline mode.");
        }

        // Init Minestom PVP
        try {
            MinestomPvP.init();
            Log.info("MinestomPvP initialized successfully.");
        } catch (Exception e) {
            Log.error("MinestomPvP initialization failed: " + e.getMessage());
            e.printStackTrace();
        }

        // Init Minestom Fluids
        try {
            MinestomFluids.init();
            Log.info("MinestomFluids initialized successfully.");
        } catch (Exception e) {
            Log.error("MinestomFluids initialization failed: " + e.getMessage());
            e.printStackTrace();
        }

        // Instances
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        instanceContainer = instanceManager.createInstanceContainer();
        Log.debug("InstanceContainer created.");

        // Polar world loader
        Path worldFolder = Path.of("worlds");
        Files.createDirectories(worldFolder);
        polarFile = worldFolder.resolve("overworld.polar");
        polarGzFile = worldFolder.resolve("overworld.polar.gz");

        // Decompress world if .gz exists
        if (Files.exists(polarGzFile) && !Files.exists(polarFile)) {
            Log.info("Decompressing world from " + polarGzFile + "...");
            try (GZIPInputStream gis = new GZIPInputStream(Files.newInputStream(polarGzFile))) {
                Files.copy(gis, polarFile);
                Files.deleteIfExists(polarGzFile);
                Log.info("World decompressed successfully and .gz file deleted.");
            } catch (Exception e) {
                Log.error("Failed to decompress world: " + e.getMessage());
                e.printStackTrace();
            }
        }

        polarLoader = new PolarLoader(polarFile);
        instanceContainer.setChunkLoader(polarLoader);
        Log.info("Polar world loader set for instance.");

        // Terrain Generator
        instanceContainer.setGenerator(new TerrainGenerator());
        Log.info("Custom terrain generator applied.");

        // Enable chunk lighting
        instanceContainer.setChunkSupplier(LightingChunk::new);
        Log.info("Chunk lighting enabled.");

        // Events
        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        Log.debug("GlobalEventHandler obtained.");

        AsyncPlayerConfigurationEvent.addListener(globalEventHandler, instanceContainer);
        EntityDamageEvent.addListener(globalEventHandler);
        EntityDeathEvent.addListener(globalEventHandler);
        ItemDropEvent.addListener(globalEventHandler);
        PickupItemEvent.addListener(globalEventHandler);
        PlayerBlockBreakEvent.addListener(globalEventHandler);
        PlayerDeathEvent.addListener(globalEventHandler);
        PlayerDisconnectEvent.addListener(globalEventHandler);
        PlayerSpawnEvent.addListener(globalEventHandler);
        PlayerUseItemOnBlockEvent.addListener(globalEventHandler);
        ServerListPingEvent.addListener(globalEventHandler);
        Log.info("Event listeners registered.");

        // Mob Spawner
        mobSpawner = new MobSpawner(instanceContainer);
        mobSpawner.start();
        Log.info("Mob spawner initialized.");

        // Commands
        MinecraftServer.getCommandManager().register(new Spectator());
        MinecraftServer.getCommandManager().register(new Creative());
        MinecraftServer.getCommandManager().register(new Survival());
        MinecraftServer.getCommandManager().register(new Adventure());
        Log.info("Commands registered.");

        // Minestom PVP Events
        CombatFeatureSet featureSet = CombatFeatures.empty()
                .version(CombatVersion.MODERN)
                .remove(CombatFeatures.VANILLA_TRIDENT.featureType())
                .add(CombatFeatures.VANILLA_FALL)
                .add(CombatFeatures.VANILLA_ARMOR)
                .add(CombatFeatures.VANILLA_BLOCK)
                .add(CombatFeatures.VANILLA_ATTACK_COOLDOWN)
                .add(CombatFeatures.VANILLA_CRITICAL)
                .add(CombatFeatures.VANILLA_DEATH_MESSAGE)
                .add(CombatFeatures.VANILLA_DAMAGE)
                .add(CombatFeatures.VANILLA_ATTACK)
                .add(CombatFeatures.VANILLA_EQUIPMENT)
                .add(CombatFeatures.VANILLA_BOW)
                .add(CombatFeatures.VANILLA_CROSSBOW)
                .add(CombatFeatures.VANILLA_FISHING_ROD)
                .add(CombatFeatures.VANILLA_TOTEM)
                .add(CombatFeatures.VANILLA_EFFECT)
                .add(CombatFeatures.VANILLA_ENCHANTMENT)
                .add(CombatFeatures.VANILLA_EXHAUSTION)
                .add(CombatFeatures.VANILLA_EXPLOSION)
                .add(CombatFeatures.VANILLA_EXPLOSIVE)
                .add(CombatFeatures.VANILLA_FOOD)
                .add(CombatFeatures.VANILLA_SWEEPING)
                .add(CombatFeatures.VANILLA_ITEM_COOLDOWN)
                .add(CombatFeatures.VANILLA_ITEM_DAMAGE)
                .add(CombatFeatures.VANILLA_KNOCKBACK)
                .add(CombatFeatures.VANILLA_MISC_PROJECTILE)
                .add(CombatFeatures.VANILLA_PLAYER_STATE)
                .add(CombatFeatures.VANILLA_POTION)
                .add(CombatFeatures.VANILLA_REGENERATION)
                .add(CombatFeatures.VANILLA_PROJECTILE_ITEM)
                .build();
        globalEventHandler.addChild(featureSet.createNode());
        Log.info("Combat features enabled.");

        // Add MinestomFluids Events
        globalEventHandler.addChild(MinestomFluids.events());
        Log.info("Fluid events enabled.");

        // Initialize Plugin System
        pluginManager = new PluginManager();
        pluginManager.loadPlugins();
        pluginManager.enablePlugins();

        // Save world when closing server
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!isShuttingDown) {
                shutdown();
            }
        }));

        // Start server
        try {
            minecraftServer.start("0.0.0.0", config.port);
            Log.info("Server started on 0.0.0.0:" + config.port);
            Log.info("Server is ready for players!");

            // Listen for stop command from GUI
            new Thread(() -> {
                try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(System.in))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.equalsIgnoreCase("stop")) {
                            Log.info("Received stop command.");
                            shutdown();
                            System.exit(0);
                        }
                    }
                } catch (Exception e) {
                    if (!isShuttingDown) {
                        Log.error("Command listener failed: " + e.getMessage());
                    }
                }
            }, "Command-Listener").start();

        } catch (Exception e) {
            Log.error("Failed to start server: " + e.getMessage());
            throw e;
        }
    }

    public static void shutdown() {
        if (isShuttingDown) {
            return;
        }
        isShuttingDown = true;

        Log.info("=== Server shutdown initiated ===");

        // Save all player data
        MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player -> {
            try {
                PlayerDataManager.savePlayer(player);
                Log.info("Saved data for " + player.getUsername());
            } catch (Exception e) {
                Log.error("Failed to save data for " + player.getUsername() + ": " + e.getMessage());
            }
        });

        // Disable plugins
        if (pluginManager != null) {
            try {
                pluginManager.disablePlugins();
            } catch (Exception e) {
                Log.error("Error disabling plugins: " + e.getMessage());
            }
        }

        // Save the world
        try {
            Log.info("Saving world...");
            polarLoader.saveInstance(instanceContainer);
            polarLoader.saveChunks(instanceContainer.getChunks());
            Log.info("World saved successfully.");

            // Compress the world file
            if (Files.exists(polarFile)) {
                Log.info("Compressing world to " + polarGzFile + "...");
                try (GZIPOutputStream gos = new GZIPOutputStream(Files.newOutputStream(polarGzFile))) {
                    Files.copy(polarFile, gos);
                }
                Files.deleteIfExists(polarFile);
                Log.info("World compressed and uncompressed file deleted.");
            }
        } catch (Exception e) {
            Log.error("Failed to save/compress world: " + e.getMessage());
            e.printStackTrace();
        }

        // Delete lock file
        try {
            Files.deleteIfExists(lockFile);
            Log.info("Lock file deleted.");
        } catch (Exception e) {
            Log.error("Failed to delete lock file: " + e.getMessage());
        }

        // Stop server
        MinecraftServer.stopCleanly();

        // Close log
        Log.close();
    }

    public static PluginManager getPluginManager() {
        return pluginManager;
    }

    public static InstanceContainer getInstanceContainer() {
        return instanceContainer;
    }

    public static MobSpawner getMobSpawner() {
        return mobSpawner;
    }

    public static boolean isShuttingDown() {
        return isShuttingDown;
    }
}
