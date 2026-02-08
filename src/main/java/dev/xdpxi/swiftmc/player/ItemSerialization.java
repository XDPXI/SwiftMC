package dev.xdpxi.swiftmc.player;

import java.util.Objects;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

public class ItemSerialization {

    public final int materialId;
    public final int amount;

    public ItemSerialization(ItemStack stack) {
        this.materialId = stack.material().id();
        this.amount = stack.amount();
    }

    public ItemStack toItemStack() {
        return ItemStack.of(
            Objects.requireNonNull(Material.fromId(materialId)),
            amount
        );
    }
}
