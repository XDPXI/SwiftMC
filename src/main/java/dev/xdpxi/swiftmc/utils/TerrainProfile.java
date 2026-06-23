package dev.xdpxi.swiftmc.utils;

public interface TerrainProfile {
    int getHeight(int worldX, int worldZ);

    default double getTreeProbability(int worldX, int worldZ) {
        return 0.001;
    }
}
