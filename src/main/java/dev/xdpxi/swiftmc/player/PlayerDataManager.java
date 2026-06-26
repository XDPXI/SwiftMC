package dev.xdpxi.swiftmc.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlayerDataManager {

    private static final Path PLAYER_FOLDER = Paths.get("players");
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    private static final ExecutorService IO_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    static {
        try {
            if (!Files.exists(PLAYER_FOLDER)) {
                Files.createDirectories(PLAYER_FOLDER);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static @NonNull CompletableFuture<Void> savePlayer(Player player) {
        PlayerData data = new PlayerData(player);
        return saveDataAsync(player.getUuid(), data);
    }

    @Contract("_, _ -> new")
    static @NonNull CompletableFuture<Void> saveDataAsync(UUID uuid, PlayerData data) {
        return CompletableFuture.runAsync(() -> {
            try {
                Path file = PLAYER_FOLDER.resolve(uuid + ".json");
                Files.writeString(file, GSON.toJson(data));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, IO_EXECUTOR);
    }

    public static void loadPlayer(@NonNull Player player) {
        UUID uuid = player.getUuid();
        loadDataAsync(uuid).thenAccept(data -> {
            if (data == null) return;

            // Schedule next tick
            player.scheduleNextTick(_ -> {
                player.teleport(data.toPos());
                if (data.gameMode != null) {
                    player.setGameMode(data.gameMode);
                }
                data.applyInventory(player);
                player.setPermissionLevel(data.op ? 4 : 0);
                player.refreshCommands();
            });
        });
    }

    @Contract("_ -> new")
    static @NonNull CompletableFuture<PlayerData> loadDataAsync(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path file = PLAYER_FOLDER.resolve(uuid + ".json");
                if (!Files.exists(file)) return null;

                String json = Files.readString(file);
                return GSON.fromJson(json, PlayerData.class);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }, IO_EXECUTOR);
    }

    @Contract("_, _ -> new")
    public static @NonNull CompletableFuture<Boolean> setOp(String username, boolean op) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!Files.exists(PLAYER_FOLDER)) return false;
                for (Path file : Files.newDirectoryStream(PLAYER_FOLDER, "*.json")) {
                    String json = Files.readString(file);
                    PlayerData data = GSON.fromJson(json, PlayerData.class);
                    if (data != null && username.equalsIgnoreCase(data.username)) {
                        data.op = op;
                        Files.writeString(file, GSON.toJson(data));
                        // Apply immediately if the player is online
                        for (Player online : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                            if (online.getUsername().equalsIgnoreCase(username)) {
                                online.setPermissionLevel(op ? 4 : 0);
                                online.refreshCommands();
                                break;
                            }
                        }
                        return true;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }, IO_EXECUTOR);
    }

    public static void shutdownExecutor() {
        IO_EXECUTOR.shutdown();
    }
}
