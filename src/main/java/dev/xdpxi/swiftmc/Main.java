package dev.xdpxi.swiftmc;

import dev.lu15.voicechat.VoiceChat;
import dev.xdpxi.swiftmc.commands.Deop;
import dev.xdpxi.swiftmc.commands.Gamemode;
import dev.xdpxi.swiftmc.commands.Op;
import dev.xdpxi.swiftmc.commands.Stop;
import dev.xdpxi.swiftmc.commands.Teleport;
import dev.xdpxi.swiftmc.events.*;
import dev.xdpxi.swiftmc.mobs.MobSpawner;
import dev.xdpxi.swiftmc.player.PlayerDataManager;
import dev.xdpxi.swiftmc.plugin.PluginManager;
import dev.xdpxi.swiftmc.terrain.TerrainGenerator;
import dev.xdpxi.swiftmc.utils.Config;
import dev.xdpxi.swiftmc.utils.Log;
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
import org.jspecify.annotations.NonNull;
import rocks.minestom.placement.Registrations;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Main {

    public static Config config;
    private static Path lockFile;
    private static InstanceContainer instanceContainer;
    private static PolarLoader polarLoader;
    private static PluginManager pluginManager;
    private static MobSpawner mobSpawner;
    private static volatile boolean isShuttingDown = false;

    static void main(String @NonNull [] args) {
        try {
            server();
        } catch (Exception e) {
            Log.error("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void server() throws Exception {
        // Get the path of the running JAR
        File jarFile = new File(
                Main.class.getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI()
        );

        // Check for lock file
        lockFile = Path.of(jarFile.getPath().replaceFirst("\\.jar$", ".lck"));
        if (Files.exists(lockFile)) {
            Log.error(
                    "Server is already running or did not shut down properly!"
            );
            Log.error(
                    "If you're sure the server is not running, delete the lock file: " +
                            lockFile
            );
            System.exit(1);
            return;
        }

        // Create lock file
        try {
            Files.writeString(
                    lockFile,
                    "Server started at: " + java.time.LocalDateTime.now()
            );
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
        if (config.velocity) {
            minecraftServer = MinecraftServer.init(
                    new Auth.Velocity(config.velocitySecret)
            );
            Log.info("Server initialized with Velocity support.");
        } else if (config.online) {
            minecraftServer = MinecraftServer.init(new Auth.Online());
            Log.info("Server initialized in online mode.");
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
            Log.error(
                    "MinestomFluids initialization failed: " + e.getMessage()
            );
            e.printStackTrace();
        }

        // Init Simple Voice Chat Minestom
        try {
            if (config.simpleVoiceChat) {
                VoiceChat.builder("0.0.0.0", config.port).enable();
                Log.info("Simple Voice Chat initialized successfully.");
            }
        } catch (Exception e) {
            Log.error(
                    "Simple Voice Chat initialization failed: " + e.getMessage()
            );
            e.printStackTrace();
        }

        // Init Block Placement Rules
        try {
            Registrations.registerAllVanilla(MinecraftServer.getBlockManager());
            Log.info("Block Placement Rules initialized successfully.");
        } catch (Exception e) {
            Log.error("Block Placement Rules initialization failed: " + e.getMessage());
            e.printStackTrace();
        }

        // Instances
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        instanceContainer = instanceManager.createInstanceContainer();
        Log.debug("InstanceContainer created.");

        // Polar world loader
        Path worldFolder = Path.of("worlds");
        Files.createDirectories(worldFolder);
        Path polarFile = worldFolder.resolve("overworld.polar");

        polarLoader = new PolarLoader(polarFile);
        polarLoader.setParallel(true);
        instanceContainer.setChunkLoader(polarLoader);
        Log.info("Polar world loader set for instance.");

        // Terrain Generator
        TerrainGenerator.init(config.chunkGenerationThreads);
        instanceContainer.setGenerator(new TerrainGenerator());
        Log.info("Custom terrain generator applied.");

        // Enable chunk lighting
        instanceContainer.setChunkSupplier(LightingChunk::new);
        Log.info("Chunk lighting enabled.");

        // Events
        GlobalEventHandler globalEventHandler =
                MinecraftServer.getGlobalEventHandler();
        Log.debug("GlobalEventHandler obtained.");

        AsyncPlayerConfigurationEvent.addListener(
                globalEventHandler,
                instanceContainer
        );
        EntityDamageEvent.addListener(globalEventHandler);
        EntityDeathEvent.addListener(globalEventHandler);
        MobFallDamageEvent.addListener(globalEventHandler);
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
        MinecraftServer.getCommandManager().register(new Gamemode());
        MinecraftServer.getCommandManager().register(new Stop());
        MinecraftServer.getCommandManager().register(new Teleport());
        MinecraftServer.getCommandManager().register(new Op());
        MinecraftServer.getCommandManager().register(new Deop());
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
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    if (!isShuttingDown) {
                        shutdown();
                    }
                })
        );

        // Start server
        try {
            minecraftServer.start("0.0.0.0", config.port);
            Log.info("Server started on 0.0.0.0:" + config.port);
            Log.info("Server is ready for players!");

            // Listen for stop command from GUI
            new Thread(
                    () -> {
                        try (
                                var reader = new java.io.BufferedReader(
                                        new java.io.InputStreamReader(System.in)
                                )
                        ) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.equalsIgnoreCase("stop")) {
                                    Log.info("Received stop command.");
                                    shutdown();
                                    System.exit(0);
                                } else if (line.toLowerCase().startsWith("op ")) {
                                    String name = line.substring(3).trim();
                                    if (!name.isEmpty()) {
                                        PlayerDataManager.setOp(name, true).thenAccept(found -> {
                                            if (found) Log.info("Opped " + name + ".");
                                            else Log.warn("Player '" + name + "' has never joined the server.");
                                        });
                                    }
                                } else if (line.toLowerCase().startsWith("deop ")) {
                                    String name = line.substring(5).trim();
                                    if (!name.isEmpty()) {
                                        PlayerDataManager.setOp(name, false).thenAccept(found -> {
                                            if (found) Log.info("Deopped " + name + ".");
                                            else Log.warn("Player '" + name + "' has never joined the server.");
                                        });
                                    }
                                }
                            }
                        } catch (Exception e) {
                            if (!isShuttingDown) {
                                Log.error(
                                        "Command listener failed: " + e.getMessage()
                                );
                            }
                        }
                    },
                    "Command-Listener"
            )
                    .start();
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

        Log.info("Server is shutting down!");

        // Save all player data
        List<CompletableFuture<Void>> saveFutures = new ArrayList<>();
        for (var player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            String username = player.getUsername();
            saveFutures.add(
                    PlayerDataManager.savePlayer(player)
                            .thenRun(() -> Log.info("Saved data for " + username))
                            .exceptionally(e -> {
                                Log.error("Failed to save data for " + username + ": " + e.getMessage());
                                return null;
                            })
            );
        }
        try {
            CompletableFuture.allOf(saveFutures.toArray(new CompletableFuture[0]))
                    .get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Log.error("Timed out waiting for all player data to save after 30 seconds.");
        } catch (Exception e) {
            Log.error("Error while waiting for player data saves: " + e.getMessage());
        }
        PlayerDataManager.shutdownExecutor();

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
        } catch (Exception e) {
            Log.error("Failed to save world: " + e.getMessage());
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
