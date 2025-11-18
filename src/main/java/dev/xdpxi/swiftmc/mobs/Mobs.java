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

    private static final Map<EntityType, Integer> MOB_HEALTH = Map.<EntityType, Integer>ofEntries(
            Map.entry(EntityType.ALLAY, 20),
            Map.entry(EntityType.AXOLOTL, 14),
            Map.entry(EntityType.BAT, 6),
            Map.entry(EntityType.CAMEL, 32),
            Map.entry(EntityType.CAT, 10),
            Map.entry(EntityType.CHICKEN, 4),
            Map.entry(EntityType.COD, 3),
            Map.entry(EntityType.COW, 10),
            Map.entry(EntityType.DONKEY, 15),
            Map.entry(EntityType.FROG, 10),
            Map.entry(EntityType.GLOW_SQUID, 10),
            Map.entry(EntityType.HORSE, 30),
            Map.entry(EntityType.MOOSHROOM, 10),
            Map.entry(EntityType.MULE, 30),
            Map.entry(EntityType.OCELOT, 10),
            Map.entry(EntityType.PARROT, 6),
            Map.entry(EntityType.PIG, 10),
            Map.entry(EntityType.RABBIT, 3),
            Map.entry(EntityType.SALMON, 3),
            Map.entry(EntityType.SHEEP, 8),
            Map.entry(EntityType.SKELETON_HORSE, 15),
            Map.entry(EntityType.SNIFFER, 14),
            Map.entry(EntityType.SNOW_GOLEM, 4),
            Map.entry(EntityType.SQUID, 10),
            Map.entry(EntityType.STRIDER, 20),
            Map.entry(EntityType.TADPOLE, 6),
            Map.entry(EntityType.TROPICAL_FISH, 3),
            Map.entry(EntityType.TURTLE, 30),
            Map.entry(EntityType.VILLAGER, 20),
            Map.entry(EntityType.WANDERING_TRADER, 20),
            Map.entry(EntityType.BEE, 10),
            Map.entry(EntityType.CAVE_SPIDER, 12),
            Map.entry(EntityType.DOLPHIN, 10),
            Map.entry(EntityType.ENDERMAN, 40),
            Map.entry(EntityType.FOX, 10),
            Map.entry(EntityType.GOAT, 10),
            Map.entry(EntityType.IRON_GOLEM, 100),
            Map.entry(EntityType.LLAMA, 30),
            Map.entry(EntityType.PIGLIN, 16),
            Map.entry(EntityType.PANDA, 20),
            Map.entry(EntityType.POLAR_BEAR, 30),
            Map.entry(EntityType.PUFFERFISH, 3),
            Map.entry(EntityType.SPIDER, 16),
            Map.entry(EntityType.TRADER_LLAMA, 30),
            Map.entry(EntityType.WOLF, 8),
            Map.entry(EntityType.ZOMBIFIED_PIGLIN, 20),
            Map.entry(EntityType.BLAZE, 20),
            Map.entry(EntityType.CREEPER, 20),
            Map.entry(EntityType.DROWNED, 20),
            Map.entry(EntityType.ELDER_GUARDIAN, 80),
            Map.entry(EntityType.ENDERMITE, 8),
            Map.entry(EntityType.EVOKER, 24),
            Map.entry(EntityType.GHAST, 10),
            Map.entry(EntityType.GUARDIAN, 30),
            Map.entry(EntityType.HOGLIN, 40),
            Map.entry(EntityType.HUSK, 20),
            Map.entry(EntityType.PHANTOM, 20),
            Map.entry(EntityType.PIGLIN_BRUTE, 50),
            Map.entry(EntityType.PILLAGER, 24),
            Map.entry(EntityType.RAVAGER, 100),
            Map.entry(EntityType.SHULKER, 30),
            Map.entry(EntityType.SILVERFISH, 8),
            Map.entry(EntityType.SKELETON, 20),
            Map.entry(EntityType.STRAY, 20),
            Map.entry(EntityType.VEX, 14),
            Map.entry(EntityType.VINDICATOR, 24),
            Map.entry(EntityType.WITCH, 26),
            Map.entry(EntityType.WITHER_SKELETON, 20),
            Map.entry(EntityType.ZOGLIN, 40),
            Map.entry(EntityType.ZOMBIE, 20),
            Map.entry(EntityType.ZOMBIE_VILLAGER, 20),
            Map.entry(EntityType.WARDEN, 500),
            Map.entry(EntityType.ENDER_DRAGON, 200),
            Map.entry(EntityType.WITHER, 300)
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

    public static int getMobHealth(EntityType type) {
        return MOB_HEALTH.getOrDefault(type, 0);
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
