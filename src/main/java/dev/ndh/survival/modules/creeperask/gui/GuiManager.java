package dev.ndh.survival.modules.creeperask.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class GuiManager {
    public static final int ACCEPT_SLOT = 3;
    public static final int DENY_SLOT = 5;

    private GuiManager() {
    }

    public static void open(Player player) {
        AskInventoryHolder holder = new AskInventoryHolder();
        Inventory inventory = Bukkit.createInventory(holder, 9, Component.text("¿Puedo explotar?"));
        holder.setInventory(inventory);
        inventory.setItem(ACCEPT_SLOT, button(Material.GREEN_STAINED_GLASS, Component.text("Sí, explota")));
        inventory.setItem(DENY_SLOT, button(Material.RED_STAINED_GLASS, Component.text("No, aléjate")));
        player.openInventory(inventory);
    }

    private static ItemStack button(Material material, Component name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        item.setItemMeta(meta);
        return item;
    }
}
