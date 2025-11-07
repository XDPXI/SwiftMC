package dev.xdpxi.swiftmc.nether;

import dev.xdpxi.swiftmc.utils.Log;
import net.hollowcube.polar.PolarLoader;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;

import java.nio.file.Files;
import java.nio.file.Path;

public class NetherManager {
    private static InstanceContainer netherInstance;
    private static InstanceContainer overworldInstance;

    public static void init(InstanceContainer overworld) throws Exception {
        overworldInstance = overworld;

        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        netherInstance = instanceManager.createInstanceContainer();

        // Setup Nether world
        Path worldFolder = Path.of("worlds");
        Files.createDirectories(worldFolder);
        Path netherFile = worldFolder.resolve("nether.polar");
        netherInstance.setChunkLoader(new PolarLoader(netherFile));
        netherInstance.setGenerator(new NetherGenerator());

        Log.info("Nether dimension initialized successfully.");
    }

    public static InstanceContainer getNetherInstance() {
        return netherInstance;
    }

    public static InstanceContainer getOverworldInstance() {
        return overworldInstance;
    }
}