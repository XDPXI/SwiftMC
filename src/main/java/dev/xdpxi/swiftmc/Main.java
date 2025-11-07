package dev.xdpxi.swiftmc;

import dev.xdpxi.swiftmc.utils.Config;
import dev.xdpxi.swiftmc.utils.ServerLogger;
import dev.xdpxi.swiftmc.utils.TerrainGenerator;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.item.PickupItemEvent;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.ping.Status;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public class Main {
    public static Config config;

    static void main(String[] args) throws Exception {
        // Setup config
        config = Config.loadOrCreate();

        // Setup logging
        ServerLogger.setup(args);

        // Init server
        MinecraftServer minecraftServer = MinecraftServer.init();

        // Instances
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer instanceContainer = instanceManager.createInstanceContainer();

        // Terrain Generator
        instanceContainer.setGenerator(new TerrainGenerator());

        // Events
        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();

        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final Player player = event.getPlayer();
            event.setSpawningInstance(instanceContainer);
            player.setRespawnPoint(new Pos(0, 64, 0));
        });

        globalEventHandler.addListener(PlayerSpawnEvent.class, event -> {
            final Player player = event.getPlayer();

            // Set game mode to survival so inventory works properly
            player.setGameMode(GameMode.SURVIVAL);

            // Enable item pickup
            player.setCanPickupItem(true);
        });

        globalEventHandler.addListener(PlayerBlockBreakEvent.class, event -> {
            Block block = event.getBlock();
            Player player = event.getPlayer();

            // Get block drop
            Material dropMaterial = getBlockDrop(block);

            if (dropMaterial != null) {
                // Create item stack
                ItemStack itemStack = ItemStack.of(dropMaterial, 1);

                // Spawn item entity
                Vec blockPos = event.getBlockPosition().add(0.5, 0.5, 0.5).asVec();
                Vec velocity = new Vec(
                        (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.2,
                        0.2,
                        (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.2
                );

                ItemEntity itemEntity = new ItemEntity(itemStack);
                itemEntity.setInstance(player.getInstance(), Pos.fromPoint(blockPos));
                itemEntity.setVelocity(velocity);
                itemEntity.setPickupDelay(Duration.ofMillis(500));
            }
        });

        globalEventHandler.addListener(PickupItemEvent.class, event -> {
            Player player = (Player) event.getLivingEntity();
            ItemEntity itemEntity = event.getItemEntity();

            // Add the item to player's inventory
            boolean added = player.getInventory().addItemStack(itemEntity.getItemStack());

            if (added) {
                // Play pickup sound
                player.playSound(Sound.sound(Key.key("entity.item.pickup"), Sound.Source.PLAYER, 0.2f, 1.0f));
            } else {
                // Inventory full, cancel the pickup
                event.setCancelled(true);
            }
        });

        globalEventHandler.addListener(ServerListPingEvent.class, event -> {
            int onlinePlayers = MinecraftServer.getConnectionManager().getOnlinePlayerCount();

            event.setStatus(Status.builder()
                    .description(Component.text("Welcome to my Minecraft server!", NamedTextColor.GOLD))
                    .playerInfo(Status.PlayerInfo.builder()
                            .onlinePlayers(onlinePlayers)
                            .maxPlayers(config.maxPlayers)
                            .build())
                    .playerInfo(onlinePlayers, config.maxPlayers)
                    .versionInfo(new Status.VersionInfo("1.21.10", 773))
                    .build());
        });

        // Start server
        minecraftServer.start("0.0.0.0", config.port);
    }

    private static Material getBlockDrop(Block block) {
        // Map blocks to their drops
        return switch (block.name()) {
            case "minecraft:grass_block", "minecraft:dirt" -> Material.DIRT;
            case "minecraft:stone" -> Material.COBBLESTONE;
            case "minecraft:sand" -> Material.SAND;
            case "minecraft:water" -> null; // Water doesn't drop
            case "minecraft:oak_log" -> Material.OAK_LOG;
            case "minecraft:oak_leaves" -> ThreadLocalRandom.current().nextInt(20) == 0 ? Material.OAK_SAPLING : null;
            default -> {
                // Try to get the material directly from the block
                try {
                    yield Material.fromId(block.id());
                } catch (Exception e) {
                    yield null;
                }
            }
        };
    }
}