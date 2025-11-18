package dev.xdpxi.swiftmc.events;

import dev.xdpxi.swiftmc.mobs.Mobs;
import dev.xdpxi.swiftmc.utils.Log;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.ai.goal.RandomStrollGoal;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;

import java.util.List;

public class PlayerUseItemEvent {
    public static void addListener(GlobalEventHandler globalEventHandler) {
        globalEventHandler.addListener(net.minestom.server.event.player.PlayerUseItemEvent.class, event -> {
            Player player = event.getPlayer();
            Material material = event.getItemStack().material();

            EntityType mobType = Mobs.getEntityTypeFromSpawnEgg(material);
            if (mobType == null) return;

            Pos spawnPos = getSpawnPosition(player);
            if (spawnPos == null) {
                Log.debug("No valid spawn position found for " + mobType.name());
                return;
            }

            spawnMob(player, mobType, spawnPos);
            Log.debug(player.getUsername() + " spawned " + mobType.name() + " at " +
                    spawnPos.blockX() + ", " + spawnPos.blockY() + ", " + spawnPos.blockZ());
        });
    }

    private static Pos getSpawnPosition(Player player) {
        Pos eyePos = player.getPosition().add(0, player.getEyeHeight(), 0);
        Vec direction = eyePos.direction();

        // Raycast up to 10 blocks to find where the player is looking
        for (double distance = 0.5; distance <= 10; distance += 0.5) {
            Vec targetVec = eyePos.asVec().add(direction.mul(distance));
            int x = (int) Math.floor(targetVec.x());
            int y = (int) Math.floor(targetVec.y());
            int z = (int) Math.floor(targetVec.z());

            Block targetBlock = player.getInstance().getBlock(x, y, z);

            // If we hit a solid block, try to spawn on top of it
            if (!targetBlock.isAir() && !targetBlock.compare(Block.WATER)) {
                Block above = player.getInstance().getBlock(x, y + 1, z);
                Block above2 = player.getInstance().getBlock(x, y + 2, z);

                // Check if there's space above the block
                if (above.isAir() && above2.isAir()) {
                    return new Pos(x + 0.5, y + 1, z + 0.5);
                }

                // If no space on top, try the block before this one
                Vec prevVec = eyePos.asVec().add(direction.mul(distance - 0.5));
                int prevX = (int) Math.floor(prevVec.x());
                int prevY = (int) Math.floor(prevVec.y());
                int prevZ = (int) Math.floor(prevVec.z());

                Block prevBelow = player.getInstance().getBlock(prevX, prevY - 1, prevZ);
                Block prevAt = player.getInstance().getBlock(prevX, prevY, prevZ);
                Block prevAbove = player.getInstance().getBlock(prevX, prevY + 1, prevZ);

                if (!prevBelow.isAir() && !prevBelow.compare(Block.WATER) &&
                        prevAt.isAir() && prevAbove.isAir()) {
                    return new Pos(prevX + 0.5, prevY, prevZ + 0.5);
                }
            }
        }

        // If raycast didn't find anything, try spawning in front of player on ground
        Vec fallbackVec = eyePos.asVec().add(direction.mul(3));
        int fallbackX = (int) Math.floor(fallbackVec.x());
        int fallbackZ = (int) Math.floor(fallbackVec.z());

        // Search down for ground
        for (int y = (int) Math.floor(fallbackVec.y()); y > player.getPosition().blockY() - 5; y--) {
            Block below = player.getInstance().getBlock(fallbackX, y - 1, fallbackZ);
            Block at = player.getInstance().getBlock(fallbackX, y, fallbackZ);
            Block above = player.getInstance().getBlock(fallbackX, y + 1, fallbackZ);

            if (!below.isAir() && !below.compare(Block.WATER) &&
                    at.isAir() && above.isAir()) {
                return new Pos(fallbackX + 0.5, y, fallbackZ + 0.5);
            }
        }

        return null;
    }

    private static void spawnMob(Player player, EntityType type, Pos pos) {
        EntityCreature mob = new EntityCreature(type);

        if (Mobs.isPassiveMob(type)) {
            mob.addAIGroup(List.of(new RandomStrollGoal(mob, 20)), List.of());
        }
        mob.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue((double) Mobs.getMobSpeed(type) / 100);

        mob.setInstance(player.getInstance(), pos);
    }
}
