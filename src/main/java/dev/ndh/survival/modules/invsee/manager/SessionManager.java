package dev.ndh.survival.modules.invsee.manager;

import dev.ndh.survival.modules.invsee.gui.EnderSeeHolder;
import dev.ndh.survival.modules.invsee.gui.InvSeeHolder;
import dev.ndh.survival.modules.invsee.model.PlayerInventoryData;
import dev.ndh.survival.modules.invsee.storage.OfflineDataAccess;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private final JavaPlugin plugin;
    private final OfflineDataAccess offlineDataAccess;

    private final Map<UUID, InvSeeHolder> activeInvSessions = new ConcurrentHashMap<>();
    private final Map<UUID, EnderSeeHolder> activeEnderSessions = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> returnToInvSeeOnEnderClose = new ConcurrentHashMap<>();
    private final Set<UUID> programmaticSwitchingViewers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, PlayerInventoryData> pendingOfflineEdits = new ConcurrentHashMap<>();

    public SessionManager(JavaPlugin plugin, OfflineDataAccess offlineDataAccess) {
        this.plugin = plugin;
        this.offlineDataAccess = offlineDataAccess;
    }

    public Map<UUID, InvSeeHolder> getActiveInvSessions() {
        return activeInvSessions;
    }

    public Map<UUID, EnderSeeHolder> getActiveEnderSessions() {
        return activeEnderSessions;
    }

    public void openInventorySee(Player viewer, OfflinePlayer target) {
        returnToInvSeeOnEnderClose.remove(viewer.getUniqueId());
        UUID targetUuid = target.getUniqueId();
        String knownName = target.getName();
        String targetName = knownName != null ? knownName : targetUuid.toString().substring(0, 8);
        Player onlineTarget = target.getPlayer();

        InvSeeHolder existing = activeInvSessions.get(targetUuid);
        if (existing != null) {
            if (onlineTarget != null && onlineTarget.isOnline()) {
                existing.setOnline(true);
                existing.pullFromOnlinePlayer(onlineTarget);
                existing.refreshGuiFromBackingData();
            }
            openInventorySafely(viewer, existing.getInventory());
            return;
        }

        if (onlineTarget != null && onlineTarget.isOnline()) {
            PlayerInventoryData data = getOrCreateSharedData(targetUuid, targetName, onlineTarget);
            InvSeeHolder holder = new InvSeeHolder(targetUuid, targetName, data, true);
            holder.pullFromOnlinePlayer(onlineTarget);
            holder.refreshGuiFromBackingData();
            activeInvSessions.put(targetUuid, holder);
            openInventorySafely(viewer, holder.getInventory());
            return;
        }

        if (!viewer.hasPermission("invsee.offline") && !viewer.hasPermission("ndh.admin")) {
            viewer.sendMessage(Component.text("No tienes permiso para ver inventarios de jugadores desconectados.", NamedTextColor.RED));
            return;
        }

        if (!offlineDataAccess.hasPlayerData(targetUuid)) {
            viewer.sendMessage(Component.text("El jugador '" + targetName + "' nunca ha entrado al servidor o no tiene archivo .dat.", NamedTextColor.RED));
            return;
        }

        viewer.sendMessage(Component.text("Cargando datos offline de " + targetName + "...", NamedTextColor.GRAY));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerInventoryData loadedData = offlineDataAccess.loadOfflineData(targetUuid, targetName);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!viewer.isOnline()) return;

                if (loadedData == null) {
                    viewer.sendMessage(Component.text("Error al leer el archivo de datos (.dat) de " + targetName + ".", NamedTextColor.RED));
                    return;
                }

                Player nowOnline = Bukkit.getPlayer(targetUuid);
                boolean isNowOnline = (nowOnline != null && nowOnline.isOnline());

                EnderSeeHolder enderSession = activeEnderSessions.get(targetUuid);
                PlayerInventoryData sharedData = (enderSession != null) ? enderSession.getBackingData() : loadedData;

                InvSeeHolder holder = activeInvSessions.computeIfAbsent(
                        targetUuid,
                        k -> new InvSeeHolder(targetUuid, targetName, sharedData, isNowOnline)
                );

                if (nowOnline != null && isNowOnline) {
                    holder.setOnline(true);
                    holder.pullFromOnlinePlayer(nowOnline);
                }
                holder.refreshGuiFromBackingData();
                openInventorySafely(viewer, holder.getInventory());
            });
        });
    }

    public void openEnderChestSee(Player viewer, OfflinePlayer target, boolean returnToInvSeeOnEsc) {
        UUID targetUuid = target.getUniqueId();
        String knownName = target.getName();
        String targetName = knownName != null ? knownName : targetUuid.toString().substring(0, 8);
        Player onlineTarget = target.getPlayer();

        if (returnToInvSeeOnEsc) {
            returnToInvSeeOnEnderClose.put(viewer.getUniqueId(), targetUuid);
        } else {
            returnToInvSeeOnEnderClose.remove(viewer.getUniqueId());
        }

        EnderSeeHolder existing = activeEnderSessions.get(targetUuid);
        if (existing != null) {
            if (onlineTarget != null && onlineTarget.isOnline()) {
                existing.setOnline(true);
                existing.pullFromOnlinePlayer(onlineTarget);
                existing.refreshGuiFromBackingData();
            }
            openInventorySafely(viewer, existing.getInventory());
            return;
        }

        if (onlineTarget != null && onlineTarget.isOnline()) {
            PlayerInventoryData data = getOrCreateSharedData(targetUuid, targetName, onlineTarget);
            EnderSeeHolder holder = new EnderSeeHolder(targetUuid, targetName, data, true);
            holder.pullFromOnlinePlayer(onlineTarget);
            holder.refreshGuiFromBackingData();
            activeEnderSessions.put(targetUuid, holder);
            openInventorySafely(viewer, holder.getInventory());
            return;
        }

        if (!viewer.hasPermission("invsee.offline") && !viewer.hasPermission("ndh.admin")) {
            returnToInvSeeOnEnderClose.remove(viewer.getUniqueId());
            viewer.sendMessage(Component.text("No tienes permiso para ver el Ender Chest de jugadores desconectados.", NamedTextColor.RED));
            return;
        }

        if (!offlineDataAccess.hasPlayerData(targetUuid)) {
            returnToInvSeeOnEnderClose.remove(viewer.getUniqueId());
            viewer.sendMessage(Component.text("El jugador '" + targetName + "' nunca ha entrado al servidor o no tiene archivo .dat.", NamedTextColor.RED));
            return;
        }

        viewer.sendMessage(Component.text("Cargando Ender Chest offline de " + targetName + "...", NamedTextColor.GRAY));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerInventoryData loadedData = offlineDataAccess.loadOfflineData(targetUuid, targetName);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!viewer.isOnline()) return;

                if (loadedData == null) {
                    returnToInvSeeOnEnderClose.remove(viewer.getUniqueId());
                    viewer.sendMessage(Component.text("Error al leer el archivo de datos (.dat) de " + targetName + ".", NamedTextColor.RED));
                    return;
                }

                Player nowOnline = Bukkit.getPlayer(targetUuid);
                boolean isNowOnline = (nowOnline != null && nowOnline.isOnline());

                InvSeeHolder invSession = activeInvSessions.get(targetUuid);
                PlayerInventoryData sharedData = (invSession != null) ? invSession.getBackingData() : loadedData;

                EnderSeeHolder holder = activeEnderSessions.computeIfAbsent(
                        targetUuid,
                        k -> new EnderSeeHolder(targetUuid, targetName, sharedData, isNowOnline)
                );

                if (nowOnline != null && isNowOnline) {
                    holder.setOnline(true);
                    holder.pullFromOnlinePlayer(nowOnline);
                }
                holder.refreshGuiFromBackingData();
                openInventorySafely(viewer, holder.getInventory());
            });
        });
    }

    public void openEnderChestSee(Player viewer, OfflinePlayer target) {
        openEnderChestSee(viewer, target, true);
    }

    public void cancelReturnToInvSee(UUID viewerUuid) {
        returnToInvSeeOnEnderClose.remove(viewerUuid);
    }

    private void openInventorySafely(Player viewer, org.bukkit.inventory.Inventory inventory) {
        programmaticSwitchingViewers.add(viewer.getUniqueId());
        try {
            viewer.openInventory(inventory);
        } finally {
            programmaticSwitchingViewers.remove(viewer.getUniqueId());
        }
    }

    public void handleInvSeeGuiModification(InvSeeHolder holder, boolean preferMainhandMirror) {
        if (holder.isUpdating()) return;

        holder.readGuiIntoBackingData(preferMainhandMirror);

        Player onlineTarget = Bukkit.getPlayer(holder.getTargetUuid());
        if (onlineTarget != null && onlineTarget.isOnline()) {
            holder.setOnline(true);
            holder.pushToOnlinePlayer(onlineTarget);
            onlineTarget.updateInventory();
        } else {
            holder.setOnline(false);
            PlayerInventoryData data = holder.getBackingData();
            pendingOfflineEdits.put(holder.getTargetUuid(), data);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                offlineDataAccess.saveOfflineData(data, true, false);
            });
        }
    }

    public void handleEnderSeeGuiModification(EnderSeeHolder holder) {
        if (holder.isUpdating()) return;

        holder.readGuiIntoBackingData();

        Player onlineTarget = Bukkit.getPlayer(holder.getTargetUuid());
        if (onlineTarget != null && onlineTarget.isOnline()) {
            holder.setOnline(true);
            holder.pushToOnlinePlayer(onlineTarget);
            onlineTarget.updateInventory();
        } else {
            holder.setOnline(false);
            PlayerInventoryData data = holder.getBackingData();
            pendingOfflineEdits.put(holder.getTargetUuid(), data);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                offlineDataAccess.saveOfflineData(data, false, true);
            });
        }
    }

    public void syncFromOnlineTargetToViewers(Player target) {
        UUID uuid = target.getUniqueId();
        InvSeeHolder invHolder = activeInvSessions.get(uuid);
        if (invHolder != null && !invHolder.isUpdating()) {
            invHolder.setOnline(true);
            invHolder.pullFromOnlinePlayer(target);
            invHolder.refreshGuiFromBackingData();
        }

        EnderSeeHolder enderHolder = activeEnderSessions.get(uuid);
        if (enderHolder != null && !enderHolder.isUpdating()) {
            enderHolder.setOnline(true);
            enderHolder.pullFromOnlinePlayer(target);
            enderHolder.refreshGuiFromBackingData();
        }
    }

    public void handleTargetPlayerJoin(Player joiningPlayer) {
        UUID uuid = joiningPlayer.getUniqueId();
        InvSeeHolder invHolder = activeInvSessions.get(uuid);
        EnderSeeHolder enderHolder = activeEnderSessions.get(uuid);
        PlayerInventoryData pendingData = pendingOfflineEdits.remove(uuid);

        if (invHolder != null) {
            invHolder.setOnline(true);
            invHolder.readGuiIntoBackingData(false);
            invHolder.pushToOnlinePlayer(joiningPlayer);
            invHolder.pullFromOnlinePlayer(joiningPlayer);
            invHolder.refreshGuiFromBackingData();
        } else if (pendingData != null) {
            InvSeeHolder temp = new InvSeeHolder(uuid, joiningPlayer.getName(), pendingData, true);
            temp.pushToOnlinePlayer(joiningPlayer);
        }

        if (enderHolder != null) {
            enderHolder.setOnline(true);
            enderHolder.readGuiIntoBackingData();
            enderHolder.pushToOnlinePlayer(joiningPlayer);
            enderHolder.pullFromOnlinePlayer(joiningPlayer);
            enderHolder.refreshGuiFromBackingData();
        } else if (pendingData != null) {
            EnderSeeHolder tempEnder = new EnderSeeHolder(uuid, joiningPlayer.getName(), pendingData, true);
            tempEnder.pushToOnlinePlayer(joiningPlayer);
        }

        joiningPlayer.updateInventory();
    }

    public void handleTargetPlayerQuit(Player quittingPlayer) {
        UUID uuid = quittingPlayer.getUniqueId();
        InvSeeHolder invHolder = activeInvSessions.get(uuid);
        if (invHolder != null) {
            invHolder.pullFromOnlinePlayer(quittingPlayer);
            invHolder.setOnline(false);
            invHolder.refreshGuiFromBackingData();
        }

        EnderSeeHolder enderHolder = activeEnderSessions.get(uuid);
        if (enderHolder != null) {
            enderHolder.pullFromOnlinePlayer(quittingPlayer);
            enderHolder.setOnline(false);
            enderHolder.refreshGuiFromBackingData();
        }
    }

    public void handleInvSeeClose(InvSeeHolder holder) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (holder.getInventory().getViewers().isEmpty()
                    && !returnToInvSeeOnEnderClose.containsValue(holder.getTargetUuid())) {
                activeInvSessions.remove(holder.getTargetUuid(), holder);
                if (!holder.isOnline()) {
                    PlayerInventoryData data = holder.getBackingData();
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        offlineDataAccess.saveOfflineData(data, true, false);
                        if (!activeEnderSessions.containsKey(holder.getTargetUuid())) {
                            pendingOfflineEdits.remove(holder.getTargetUuid());
                        }
                    });
                }
            }
        });
    }

    public void handleEnderSeeClose(Player viewer, EnderSeeHolder holder) {
        boolean isProgrammatic = programmaticSwitchingViewers.contains(viewer.getUniqueId());
        UUID returnTargetUuid = isProgrammatic ? null : returnToInvSeeOnEnderClose.remove(viewer.getUniqueId());

        if (viewer.hasPermission("invsee.enderchest.edit") || viewer.hasPermission("ndh.admin")) {
            holder.readGuiIntoBackingData();
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (returnTargetUuid != null && viewer.isOnline() && (viewer.hasPermission("invsee.use") || viewer.hasPermission("ndh.admin"))) {
                OfflinePlayer target = Bukkit.getOfflinePlayer(returnTargetUuid);
                openInventorySee(viewer, target);
            }

            if (holder.getInventory().getViewers().isEmpty()) {
                activeEnderSessions.remove(holder.getTargetUuid(), holder);
                if (!holder.isOnline()) {
                    PlayerInventoryData data = holder.getBackingData();
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        offlineDataAccess.saveOfflineData(data, false, true);
                        if (!activeInvSessions.containsKey(holder.getTargetUuid())) {
                            pendingOfflineEdits.remove(holder.getTargetUuid());
                        }
                    });
                }
            }
        });
    }

    public void flushAllOnShutdown() {
        returnToInvSeeOnEnderClose.clear();
        for (InvSeeHolder holder : activeInvSessions.values()) {
            if (!holder.isOnline()) {
                holder.readGuiIntoBackingData(false);
                offlineDataAccess.saveOfflineData(holder.getBackingData(), true, false);
            }
            new java.util.ArrayList<>(holder.getInventory().getViewers()).forEach(human -> human.closeInventory());
        }
        for (EnderSeeHolder holder : activeEnderSessions.values()) {
            if (!holder.isOnline()) {
                holder.readGuiIntoBackingData();
                offlineDataAccess.saveOfflineData(holder.getBackingData(), false, true);
            }
            new java.util.ArrayList<>(holder.getInventory().getViewers()).forEach(human -> human.closeInventory());
        }
        activeInvSessions.clear();
        activeEnderSessions.clear();
        pendingOfflineEdits.clear();
    }

    private PlayerInventoryData getOrCreateSharedData(UUID targetUuid, String targetName, Player onlineTarget) {
        InvSeeHolder inv = activeInvSessions.get(targetUuid);
        if (inv != null) return inv.getBackingData();
        EnderSeeHolder ender = activeEnderSessions.get(targetUuid);
        if (ender != null) return ender.getBackingData();

        PlayerInventoryData data = new PlayerInventoryData(targetUuid, targetName);
        if (onlineTarget != null) {
            data.setSelectedHotbarSlot(onlineTarget.getInventory().getHeldItemSlot());
        }
        return data;
    }
}
