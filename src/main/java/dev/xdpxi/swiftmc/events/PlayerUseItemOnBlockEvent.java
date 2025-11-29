package dev.xdpxi.swiftmc.events;

import dev.xdpxi.swiftmc.Main;
import dev.xdpxi.swiftmc.mobs.MobSpawner;
import dev.xdpxi.swiftmc.mobs.Mobs;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.Material;

public class PlayerUseItemOnBlockEvent {
    public static void addListener(GlobalEventHandler globalEventHandler) {
        globalEventHandler.addListener(net.minestom.server.event.player.PlayerUseItemOnBlockEvent.class, event -> {
            Player player = event.getPlayer();
            Material material = event.getItemStack().material();

            int x = event.getPosition().blockX();
            int y = event.getPosition().blockY();
            int z = event.getPosition().blockZ();

            switch (event.getBlockFace()) {
                case TOP -> y += 1;
                case BOTTOM -> y -= 1;
                case NORTH -> z -= 1;
                case SOUTH -> z += 1;
                case WEST -> x -= 1;
                case EAST -> x += 1;
            }

            // Water bukkit
            if (material == Material.WATER_BUCKET) {
                // Place the block
                player.getInstance().placeBlock(
                        new BlockHandler.Placement(
                                Block.WATER,
                                Block.AIR,
                                Main.getInstanceContainer(),
                                new Pos(x, y, z)
                        )
                );
                return;
            }

            // Lava bukkit
            if (material == Material.LAVA_BUCKET) {
                player.getInstance().placeBlock(
                        new BlockHandler.Placement(
                                Block.LAVA,
                                Block.AIR,
                                Main.getInstanceContainer(),
                                new Pos(x, y, z)
                        )
                );
                return;
            }

            // Mob Spawn Egg
            EntityType mobType = Mobs.getEntityTypeFromSpawnEgg(material);
            if (mobType == null) return;

            Block above = player.getInstance().getBlock(x, y, z);
            Block above2 = player.getInstance().getBlock(x, y, z);
            if (above.isAir() && above2.isAir()) {
                MobSpawner.spawnMob(mobType, new Pos(x + 0.5, y, z + 0.5));
            }
        });
    }
}
