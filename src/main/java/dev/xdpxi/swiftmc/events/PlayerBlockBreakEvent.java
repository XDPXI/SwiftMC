package dev.xdpxi.swiftmc.events;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerBlockBreakEvent {
    public static void addListener(GlobalEventHandler globalEventHandler, InstanceContainer instanceContainer) {
        globalEventHandler.addListener(net.minestom.server.event.player.PlayerBlockBreakEvent.class, event -> {
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
