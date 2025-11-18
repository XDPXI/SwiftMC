package dev.xdpxi.swiftmc.mobs;

import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.item.Material;

import java.util.List;
import java.util.Map;

public class Mobs {
    private static final Map<EntityType, Integer> MOB_SPEEDS = Map.ofEntries(
            Map.entry(EntityType.CHICKEN, 8),
            Map.entry(EntityType.COW, 4),
            Map.entry(EntityType.PIG, 4),
            Map.entry(EntityType.SHEEP, 4),
            Map.entry(EntityType.CREEPER, 4),
            Map.entry(EntityType.VILLAGER, 4),
            Map.entry(EntityType.SLIME, 4),
            Map.entry(EntityType.MAGMA_CUBE, 4),
            Map.entry(EntityType.WITCH, 4),
            Map.entry(EntityType.IRON_GOLEM, 4),
            Map.entry(EntityType.SNOW_GOLEM, 4),
            Map.entry(EntityType.ZOMBIE, 6),
            Map.entry(EntityType.SKELETON, 6),
            Map.entry(EntityType.BLAZE, 6),
            Map.entry(EntityType.SPIDER, 8),
            Map.entry(EntityType.WOLF, 8),
            Map.entry(EntityType.HORSE, 14),
            Map.entry(EntityType.RABBIT, 12),
            Map.entry(EntityType.BAT, 10),
            Map.entry(EntityType.DOLPHIN, 10),
            Map.entry(EntityType.ENDERMAN, 7),
            Map.entry(EntityType.BEE, 7),
            Map.entry(EntityType.GHAST, 5),
            Map.entry(EntityType.SQUID, 3)
    );

    public static List<EntityCreature> getSpawnedMobs() {
        return MobSpawner.getSpawnedMobs();
    }

    public static EntityType getEntityTypeFromSpawnEgg(Material material) {
        return switch (material.name()) {
            case "minecraft:chicken_spawn_egg" -> EntityType.CHICKEN;
            case "minecraft:cow_spawn_egg" -> EntityType.COW;
            case "minecraft:pig_spawn_egg" -> EntityType.PIG;
            case "minecraft:sheep_spawn_egg" -> EntityType.SHEEP;
            case "minecraft:zombie_spawn_egg" -> EntityType.ZOMBIE;
            case "minecraft:skeleton_spawn_egg" -> EntityType.SKELETON;
            case "minecraft:spider_spawn_egg" -> EntityType.SPIDER;
            case "minecraft:creeper_spawn_egg" -> EntityType.CREEPER;
            case "minecraft:wolf_spawn_egg" -> EntityType.WOLF;
            case "minecraft:horse_spawn_egg" -> EntityType.HORSE;
            case "minecraft:rabbit_spawn_egg" -> EntityType.RABBIT;
            case "minecraft:bat_spawn_egg" -> EntityType.BAT;
            case "minecraft:villager_spawn_egg" -> EntityType.VILLAGER;
            case "minecraft:enderman_spawn_egg" -> EntityType.ENDERMAN;
            case "minecraft:blaze_spawn_egg" -> EntityType.BLAZE;
            case "minecraft:ghast_spawn_egg" -> EntityType.GHAST;
            case "minecraft:slime_spawn_egg" -> EntityType.SLIME;
            case "minecraft:magma_cube_spawn_egg" -> EntityType.MAGMA_CUBE;
            case "minecraft:witch_spawn_egg" -> EntityType.WITCH;
            case "minecraft:squid_spawn_egg" -> EntityType.SQUID;
            case "minecraft:dolphin_spawn_egg" -> EntityType.DOLPHIN;
            case "minecraft:bee_spawn_egg" -> EntityType.BEE;
            case "minecraft:iron_golem_spawn_egg" -> EntityType.IRON_GOLEM;
            case "minecraft:snow_golem_spawn_egg" -> EntityType.SNOW_GOLEM;
            default -> null;
        };
    }

    public static int getMobSpeed(EntityType type) {
        return MOB_SPEEDS.getOrDefault(type, 0);
    }

    public static boolean isPassiveMob(EntityType type) {
        return  type == EntityType.CHICKEN || type == EntityType.COW ||
                type == EntityType.PIG || type == EntityType.SHEEP ||
                type == EntityType.RABBIT || type == EntityType.HORSE ||
                type == EntityType.WOLF || type == EntityType.BAT ||
                type == EntityType.SQUID || type == EntityType.DOLPHIN ||
                type == EntityType.BEE;
    }
}
