package dev.ndh.survival.modules.creeperask.gui;

import java.util.Objects;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.Nullable;

public final class AskInventoryHolder implements InventoryHolder {
    private @Nullable Inventory inventory;

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return Objects.requireNonNull(this.inventory, "Inventory has not been initialized");
    }
}
