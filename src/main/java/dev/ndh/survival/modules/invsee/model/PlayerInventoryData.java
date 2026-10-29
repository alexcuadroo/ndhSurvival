package dev.ndh.survival.modules.invsee.model;

import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Representa los datos completos de inventario y equipamiento de un jugador
 * (ya sea leído desde un jugador online o desde su archivo .dat offline).
 */
public class PlayerInventoryData {

    private final UUID playerUuid;
    private final String playerName;

    // 0-8: Hotbar, 9-35: Main storage (36 slots en total)
    private final @Nullable ItemStack[] storageContents = new ItemStack[36];

    // Armadura: 0 = Boots (feet), 1 = Leggings (legs), 2 = Chestplate (chest), 3 = Helmet (head)
    private final @Nullable ItemStack[] armorContents = new ItemStack[4];

    // Mano secundaria (offhand)
    private @Nullable ItemStack offhandItem;

    // Ender Chest (27 slots)
    private final @Nullable ItemStack[] enderChestContents = new ItemStack[27];

    // Información extra leída del jugador o su NBT
    private int selectedHotbarSlot = 0;
    private double health = 20.0;
    private int foodLevel = 20;
    private int expLevel = 0;

    public PlayerInventoryData(UUID playerUuid, String playerName) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public @Nullable ItemStack[] getStorageContents() {
        return storageContents;
    }

    public @Nullable ItemStack[] getArmorContents() {
        return armorContents;
    }

    public @Nullable ItemStack getHelmet() {
        return armorContents[3];
    }

    public void setHelmet(@Nullable ItemStack item) {
        armorContents[3] = item;
    }

    public @Nullable ItemStack getChestplate() {
        return armorContents[2];
    }

    public void setChestplate(@Nullable ItemStack item) {
        armorContents[2] = item;
    }

    public @Nullable ItemStack getLeggings() {
        return armorContents[1];
    }

    public void setLeggings(@Nullable ItemStack item) {
        armorContents[1] = item;
    }

    public @Nullable ItemStack getBoots() {
        return armorContents[0];
    }

    public void setBoots(@Nullable ItemStack item) {
        armorContents[0] = item;
    }

    public @Nullable ItemStack getOffhandItem() {
        return offhandItem;
    }

    public void setOffhandItem(@Nullable ItemStack offhandItem) {
        this.offhandItem = offhandItem;
    }

    public @Nullable ItemStack[] getEnderChestContents() {
        return enderChestContents;
    }

    public int getSelectedHotbarSlot() {
        return selectedHotbarSlot;
    }

    public void setSelectedHotbarSlot(int selectedHotbarSlot) {
        this.selectedHotbarSlot = Math.max(0, Math.min(8, selectedHotbarSlot));
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public int getFoodLevel() {
        return foodLevel;
    }

    public void setFoodLevel(int foodLevel) {
        this.foodLevel = foodLevel;
    }

    public int getExpLevel() {
        return expLevel;
    }

    public void setExpLevel(int expLevel) {
        this.expLevel = expLevel;
    }
}
