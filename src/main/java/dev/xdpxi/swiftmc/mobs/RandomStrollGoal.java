package dev.xdpxi.swiftmc.mobs;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.ai.GoalSelector;
import net.minestom.server.instance.Instance;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class RandomStrollGoal extends GoalSelector {

    private static final long DELAY = TimeUnit.MILLISECONDS.toNanos(2500);

    private final List<Vec> closePositions;
    private final Random random = new Random();
    private long lastStroll;

    public RandomStrollGoal(EntityCreature entityCreature, int radius) {
        super(entityCreature);
        this.closePositions = getNearbyBlocks(radius);
    }

    private static List<Vec> getNearbyBlocks(int radius) {
        List<Vec> blocks = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    blocks.add(new Vec(x, y, z));
                }
            }
        }
        return blocks;
    }

    @Override
    public boolean shouldStart() {
        return System.nanoTime() - lastStroll >= DELAY;
    }

    @Override
    public void start() {
        Instance instance = entityCreature.getInstance();
        if (instance == null) return;

        int remainingAttempt = closePositions.size();
        while (remainingAttempt-- > 0) {
            final Vec position = closePositions.get(random.nextInt(closePositions.size()));
            final var target = entityCreature.getPosition().add(position);

            int chunkX = Math.floorDiv((int) target.x(), 16);
            int chunkZ = Math.floorDiv((int) target.z(), 16);
            if (!instance.isChunkLoaded(chunkX, chunkZ)) continue;

            if (entityCreature.getNavigator().setPathTo(target)) break;
        }
    }

    @Override
    public void tick(long time) {
    }

    @Override
    public boolean shouldEnd() {
        return true;
    }

    @Override
    public void end() {
        this.lastStroll = System.nanoTime();
    }
}
