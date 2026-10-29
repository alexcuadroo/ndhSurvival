package dev.ndh.survival.modules.invsee.gui;

import dev.ndh.survival.modules.invsee.model.PlayerInventoryData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class InvSeeHolder implements InventoryHolder {

    public static final int GUI_SIZE = 54;

    public static final int SLOT_STATUS_INFO = 40;
    public static final int SLOT_STATS_INFO = 43;
    public static final int SLOT_ENDERCHEST_BTN = 44;

    public static final int SLOT_HELMET = 45;
    public static final int SLOT_CHESTPLATE = 46;
    public static final int SLOT_LEGGINGS = 47;
    public static final int SLOT_BOOTS = 48;
    public static final int SLOT_OFFHAND = 50;
    public static final int SLOT_MAINHAND_MIRROR = 52;

    private final UUID targetUuid;
    private final String targetName;
    private final Inventory inventory;
    private PlayerInventoryData backingData;
    private boolean online;
    private boolean updating = false;

    public InvSeeHolder(UUID targetUuid, String targetName, PlayerInventoryData initialData, boolean online) {
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.backingData = initialData;
        this.online = online;

        Component title = Component.text("Inventario: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(targetName, NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(online ? " (Online)" : " (Offline)",
                        online ? NamedTextColor.GREEN : NamedTextColor.RED));

        this.inventory = Bukkit.createInventory(this, GUI_SIZE, title);
        refreshGuiFromBackingData();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public PlayerInventoryData getBackingData() {
        return backingData;
    }

    public void setBackingData(PlayerInventoryData backingData) {
        this.backingData = backingData;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public boolean isUpdating() {
        return updating;
    }

    public static boolean isDecorativeOrActionSlot(int rawSlot) {
        if (rawSlot < 0 || rawSlot >= GUI_SIZE) {
            return false;
        }
        if (rawSlot >= 36 && rawSlot <= 44) {
            return true;
        }
        return rawSlot == 49 || rawSlot == 51 || rawSlot == 53;
    }

    public void pullFromOnlinePlayer(Player target) {
        PlayerInventory playerInv = target.getInventory();
        ItemStack[] storage = playerInv.getStorageContents();
        for (int i = 0; i < 36; i++) {
            backingData.getStorageContents()[i] = cloneOrNull(storage[i]);
        }
        backingData.setBoots(cloneOrNull(playerInv.getBoots()));
        backingData.setLeggings(cloneOrNull(playerInv.getLeggings()));
        backingData.setChestplate(cloneOrNull(playerInv.getChestplate()));
        backingData.setHelmet(cloneOrNull(playerInv.getHelmet()));
        backingData.setOffhandItem(cloneOrNull(playerInv.getItemInOffHand()));

        ItemStack[] ender = target.getEnderChest().getContents();
        for (int i = 0; i < Math.min(27, ender.length); i++) {
            backingData.getEnderChestContents()[i] = cloneOrNull(ender[i]);
        }

        backingData.setSelectedHotbarSlot(playerInv.getHeldItemSlot());
        backingData.setHealth(target.getHealth());
        backingData.setFoodLevel(target.getFoodLevel());
        backingData.setExpLevel(target.getLevel());
    }

    public void refreshGuiFromBackingData() {
        if (updating) return;
        updating = true;
        try {
            ItemStack[] storage = backingData.getStorageContents();
            for (int playerSlot = 9; playerSlot < 36; playerSlot++) {
                inventory.setItem(playerSlot - 9, cloneOrNull(storage[playerSlot]));
            }
            for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
                inventory.setItem(27 + hotbarSlot, cloneOrNull(storage[hotbarSlot]));
            }

            inventory.setItem(SLOT_HELMET, cloneOrNull(backingData.getHelmet()));
            inventory.setItem(SLOT_CHESTPLATE, cloneOrNull(backingData.getChestplate()));
            inventory.setItem(SLOT_LEGGINGS, cloneOrNull(backingData.getLeggings()));
            inventory.setItem(SLOT_BOOTS, cloneOrNull(backingData.getBoots()));

            inventory.setItem(SLOT_OFFHAND, cloneOrNull(backingData.getOffhandItem()));

            int heldSlot = backingData.getSelectedHotbarSlot();
            inventory.setItem(SLOT_MAINHAND_MIRROR, cloneOrNull(storage[heldSlot]));

            renderDecorations();
        } finally {
            updating = false;
        }
    }

    public void readGuiIntoBackingData(boolean preferMainhandMirror) {
        if (updating) return;
        updating = true;
        try {
            int heldSlot = backingData.getSelectedHotbarSlot();
            int heldGuiSlot = 27 + heldSlot;

            if (preferMainhandMirror) {
                ItemStack mirrorItem = cloneOrNull(inventory.getItem(SLOT_MAINHAND_MIRROR));
                inventory.setItem(heldGuiSlot, cloneOrNull(mirrorItem));
            } else {
                ItemStack hotbarItem = cloneOrNull(inventory.getItem(heldGuiSlot));
                inventory.setItem(SLOT_MAINHAND_MIRROR, cloneOrNull(hotbarItem));
            }

            ItemStack[] storage = backingData.getStorageContents();
            for (int guiSlot = 0; guiSlot < 27; guiSlot++) {
                storage[guiSlot + 9] = cloneOrNull(inventory.getItem(guiSlot));
            }
            for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
                storage[hotbarSlot] = cloneOrNull(inventory.getItem(27 + hotbarSlot));
            }

            backingData.setHelmet(cloneOrNull(inventory.getItem(SLOT_HELMET)));
            backingData.setChestplate(cloneOrNull(inventory.getItem(SLOT_CHESTPLATE)));
            backingData.setLeggings(cloneOrNull(inventory.getItem(SLOT_LEGGINGS)));
            backingData.setBoots(cloneOrNull(inventory.getItem(SLOT_BOOTS)));

            backingData.setOffhandItem(cloneOrNull(inventory.getItem(SLOT_OFFHAND)));
        } finally {
            updating = false;
        }
    }

    public void pushToOnlinePlayer(Player target) {
        PlayerInventory playerInv = target.getInventory();
        ItemStack[] storage = backingData.getStorageContents();
        for (int i = 0; i < 36; i++) {
            playerInv.setItem(i, cloneOrNull(storage[i]));
        }
        playerInv.setHelmet(cloneOrNull(backingData.getHelmet()));
        playerInv.setChestplate(cloneOrNull(backingData.getChestplate()));
        playerInv.setLeggings(cloneOrNull(backingData.getLeggings()));
        playerInv.setBoots(cloneOrNull(backingData.getBoots()));
        playerInv.setItemInOffHand(cloneOrNull(backingData.getOffhandItem()));
    }

    public void renderDecorations() {
        ItemStack filler = createPane(Material.GRAY_STAINED_GLASS_PANE,
                Component.text(" ", NamedTextColor.GRAY), List.of());

        for (int i = 36; i <= 44; i++) {
            inventory.setItem(i, filler);
        }

        inventory.setItem(36, createPane(Material.BLUE_STAINED_GLASS_PANE,
                Component.text("⬇ Casco (Cabeza)", NamedTextColor.AQUA, TextDecoration.BOLD),
                List.of(Component.text("Coloca el casco en el slot inferior (45)", NamedTextColor.GRAY))));
        inventory.setItem(37, createPane(Material.BLUE_STAINED_GLASS_PANE,
                Component.text("⬇ Peto (Pecho)", NamedTextColor.AQUA, TextDecoration.BOLD),
                List.of(Component.text("Coloca el peto en el slot inferior (46)", NamedTextColor.GRAY))));
        inventory.setItem(38, createPane(Material.BLUE_STAINED_GLASS_PANE,
                Component.text("⬇ Grebas (Piernas)", NamedTextColor.AQUA, TextDecoration.BOLD),
                List.of(Component.text("Coloca las grebas en el slot inferior (47)", NamedTextColor.GRAY))));
        inventory.setItem(39, createPane(Material.BLUE_STAINED_GLASS_PANE,
                Component.text("⬇ Botas (Pies)", NamedTextColor.AQUA, TextDecoration.BOLD),
                List.of(Component.text("Coloca las botas en el slot inferior (48)", NamedTextColor.GRAY))));

        Material statusMat = online ? Material.LIME_STAINED_GLASS_PANE : Material.ORANGE_STAINED_GLASS_PANE;
        Component statusTitle = online
                ? Component.text("● Jugador ONLINE", NamedTextColor.GREEN, TextDecoration.BOLD)
                : Component.text("● Jugador OFFLINE", NamedTextColor.GOLD, TextDecoration.BOLD);
        List<Component> statusLore = online
                ? List.of(
                Component.text("Sincronización en tiempo real activa.", NamedTextColor.GRAY),
                Component.text("Cualquier cambio se aplica al instante.", NamedTextColor.YELLOW))
                : List.of(
                Component.text("Editando archivo .dat del jugador.", NamedTextColor.GRAY),
                Component.text("Se guarda automáticamente en disco", NamedTextColor.YELLOW),
                Component.text("y se sincroniza si el jugador entra.", NamedTextColor.YELLOW));
        inventory.setItem(SLOT_STATUS_INFO, createPane(statusMat, statusTitle, statusLore));

        inventory.setItem(41, createPane(Material.PURPLE_STAINED_GLASS_PANE,
                Component.text("⬇ Mano Secundaria (Offhand)", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD),
                List.of(Component.text("El slot 50 debajo contiene la Offhand", NamedTextColor.GRAY))));

        int heldSlot = backingData.getSelectedHotbarSlot();
        inventory.setItem(42, createPane(Material.YELLOW_STAINED_GLASS_PANE,
                Component.text("⬇ Mano Principal (Hotbar #" + (heldSlot + 1) + ")", NamedTextColor.GOLD, TextDecoration.BOLD),
                List.of(
                        Component.text("El slot 52 debajo refleja el item en mano", NamedTextColor.GRAY),
                        Component.text("(Sincronizado con el slot " + (heldSlot + 1) + " de la hotbar)", NamedTextColor.YELLOW)
                )));

        ItemStack statsItem = new ItemStack(Material.BOOK);
        ItemMeta statsMeta = statsItem.getItemMeta();
        if (statsMeta != null) {
            statsMeta.displayName(Component.text("Datos de " + targetName, NamedTextColor.AQUA, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            statsMeta.lore(List.of(
                    Component.text("UUID: ", NamedTextColor.GRAY)
                            .append(Component.text(targetUuid.toString(), NamedTextColor.WHITE))
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Salud: ", NamedTextColor.GRAY)
                            .append(Component.text(String.format("%.1f ❤", backingData.getHealth()), NamedTextColor.RED))
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Comida: ", NamedTextColor.GRAY)
                            .append(Component.text(backingData.getFoodLevel() + " / 20", NamedTextColor.GOLD))
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Nivel XP: ", NamedTextColor.GRAY)
                            .append(Component.text(String.valueOf(backingData.getExpLevel()), NamedTextColor.GREEN))
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Slot Hotbar Activo: ", NamedTextColor.GRAY)
                            .append(Component.text("#" + (heldSlot + 1), NamedTextColor.YELLOW))
                            .decoration(TextDecoration.ITALIC, false)
            ));
            statsItem.setItemMeta(statsMeta);
        }
        inventory.setItem(SLOT_STATS_INFO, statsItem);

        ItemStack enderBtn = new ItemStack(Material.ENDER_CHEST);
        ItemMeta enderMeta = enderBtn.getItemMeta();
        if (enderMeta != null) {
            enderMeta.displayName(Component.text("Abrir Ender Chest", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            enderMeta.lore(List.of(
                    Component.text("Haz clic para inspeccionar el", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Ender Chest de " + targetName, NamedTextColor.LIGHT_PURPLE)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            enderBtn.setItemMeta(enderMeta);
        }
        inventory.setItem(SLOT_ENDERCHEST_BTN, enderBtn);

        inventory.setItem(49, createPane(Material.BLACK_STAINED_GLASS_PANE,
                Component.text("⬅ Armadura | Offhand ➡", NamedTextColor.DARK_GRAY), List.of()));
        inventory.setItem(51, createPane(Material.BLACK_STAINED_GLASS_PANE,
                Component.text("⬅ Offhand | Mano Principal ➡", NamedTextColor.DARK_GRAY), List.of()));
        inventory.setItem(53, createPane(Material.BLACK_STAINED_GLASS_PANE,
                Component.text("⬅ Mano Principal (Hotbar #" + (heldSlot + 1) + ")", NamedTextColor.GOLD),
                List.of(
                        Component.text("Filas 1-3: Inventario (Slots 9-35)", NamedTextColor.GRAY),
                        Component.text("Fila 4: Hotbar (Slots 0-8)", NamedTextColor.GRAY),
                        Component.text("Fila 6: Armadura, Offhand y Mano Principal", NamedTextColor.GRAY)
                )));
    }

    private ItemStack createPane(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream()
                    .map(line -> line.decoration(TextDecoration.ITALIC, false))
                    .toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static @Nullable ItemStack cloneOrNull(@Nullable ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
            return null;
        }
        return item.clone();
    }
}
