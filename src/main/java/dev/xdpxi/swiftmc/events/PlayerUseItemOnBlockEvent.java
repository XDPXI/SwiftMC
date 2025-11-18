package dev.xdpxi.swiftmc.events;

import dev.xdpxi.swiftmc.mobs.Mobs;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.ai.goal.RandomStrollGoal;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;

import java.util.List;

public class PlayerUseItemOnBlockEvent {
    public static void addListener(GlobalEventHandler globalEventHandler) {
        globalEventHandler.addListener(net.minestom.server.event.player.PlayerUseItemOnBlockEvent.class, event -> {
            Player player = event.getPlayer();
            Material material = event.getItemStack().material();

            EntityType mobType = Mobs.getEntityTypeFromSpawnEgg(material);
            if (mobType == null) return;

            int x = event.getPosition().blockX();
            int y = event.getPosition().blockY();
            int z = event.getPosition().blockZ();

            // Try spawn on top of the clicked block
            Block above = player.getInstance().getBlock(x, y + 1, z);
            Block above2 = player.getInstance().getBlock(x, y + 2, z);
            if (above.isAir() && above2.isAir()) {
                spawnMob(player, mobType, new Pos(x + 0.5, y + 1, z + 0.5));
            }
        });
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
