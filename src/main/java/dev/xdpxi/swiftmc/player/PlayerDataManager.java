package dev.xdpxi.swiftmc.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minestom.server.entity.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PlayerDataManager {
    private static final Path PLAYER_FOLDER = Paths.get("players");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static {
        try {
            if (!Files.exists(PLAYER_FOLDER)) {
                Files.createDirectories(PLAYER_FOLDER);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void savePlayer(Player player) {
        try {
            PlayerData data = new PlayerData(player);
            Path file = PLAYER_FOLDER.resolve(player.getUsername() + ".json");
            Files.writeString(file, GSON.toJson(data));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadPlayer(Player player) {
        try {
            Path file = PLAYER_FOLDER.resolve(player.getUsername() + ".json");
            if (!Files.exists(file)) return;

            String json = Files.readString(file);
            PlayerData data = GSON.fromJson(json, PlayerData.class);

            // Schedule next tick
            player.scheduleNextTick(_ -> {
                player.teleport(data.toPos());
                if (data.gameMode != null) {
                    player.setGameMode(data.gameMode);
                }
                data.applyInventory(player);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
