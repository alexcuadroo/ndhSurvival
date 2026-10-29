package dev.ndh.survival.modules.invsee;

import dev.ndh.survival.NdhSurvivalPlugin;
import dev.ndh.survival.modules.invsee.command.EnderChestSubCommand;
import dev.ndh.survival.modules.invsee.command.InventarioSubCommand;
import dev.ndh.survival.core.module.SurvivalModule;
import dev.ndh.survival.modules.invsee.gui.EnderSeeHolder;
import dev.ndh.survival.modules.invsee.gui.InvSeeHolder;
import dev.ndh.survival.modules.invsee.listener.InventorySyncListener;
import dev.ndh.survival.modules.invsee.manager.SessionManager;
import dev.ndh.survival.modules.invsee.storage.OfflineDataAccess;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Módulo InvSee: permite ver y editar inventarios, armadura, mano secundaria y Ender Chest (online y offline).
 * Registra subcomandos en /ndh:
 * - /ndh openinv <jugador|uuid> [ender] (alias: invsee, inv)
 * - /ndh endersee <jugador|uuid> (alias: ecsee, openender)
 */
public final class InvSeeModule implements SurvivalModule {

    private final NdhSurvivalPlugin plugin;
    private @Nullable OfflineDataAccess offlineDataAccess;
    private @Nullable SessionManager sessionManager;
    private @Nullable BukkitTask syncTask;
    private @Nullable InventorySyncListener listener;

    public InvSeeModule(NdhSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "invsee";
    }

    @Override
    public String getDisplayName() {
        return "InvSee";
    }

    @Override
    public String getDescription() {
        return "Inspección y edición en tiempo real de inventarios, equipamiento y Ender Chest (online/offline).";
    }

    @Override
    public void onEnable() {
        registerSubCommands();
        onReload();
    }

    /** Aplica el interruptor de configuración sin duplicar tareas ni listeners. */
    @Override
    public void onReload() {
        boolean enabled = plugin.getConfig().getBoolean("invsee.enabled", false);
        if (!enabled) {
            onDisable();
            return;
        }
        if (isEnabled()) return;
        OfflineDataAccess offlineDataAccess = new OfflineDataAccess(plugin);
        SessionManager sessionManager = new SessionManager(plugin, offlineDataAccess);
        this.offlineDataAccess = offlineDataAccess;
        this.sessionManager = sessionManager;

        InventorySyncListener listener = new InventorySyncListener(plugin, sessionManager);
        this.listener = listener;
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);

        this.syncTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (InvSeeHolder holder : sessionManager.getActiveInvSessions().values()) {
                if (holder.isOnline() && !holder.isUpdating()) {
                    Player target = Bukkit.getPlayer(holder.getTargetUuid());
                    if (target != null && target.isOnline()) {
                        holder.pullFromOnlinePlayer(target);
                        holder.refreshGuiFromBackingData();
                    }
                }
            }
            for (EnderSeeHolder holder : sessionManager.getActiveEnderSessions().values()) {
                if (holder.isOnline() && !holder.isUpdating()) {
                    Player target = Bukkit.getPlayer(holder.getTargetUuid());
                    if (target != null && target.isOnline()) {
                        holder.pullFromOnlinePlayer(target);
                        holder.refreshGuiFromBackingData();
                    }
                }
            }
        }, 10L, 10L);

    }

    /** Indica si la inspección de inventarios está habilitada. */
    @Override
    public boolean isEnabled() {
        return sessionManager != null;
    }

    @Override
    public void onDisable() {
        BukkitTask syncTask = this.syncTask;
        if (syncTask != null) {
            syncTask.cancel();
            this.syncTask = null;
        }
        SessionManager sessionManager = this.sessionManager;
        if (sessionManager != null) {
            sessionManager.flushAllOnShutdown();
        }
        InventorySyncListener listener = this.listener;
        if (listener != null) org.bukkit.event.HandlerList.unregisterAll(listener);
        this.listener = null;
        this.sessionManager = null;
        this.offlineDataAccess = null;
    }

    private void registerSubCommands() {
        plugin.getCommandRouter().registerSubCommand(new InventarioSubCommand(this));
        plugin.getCommandRouter().registerSubCommand(new EnderChestSubCommand(this));
    }

    public void executeInvSee(CommandSender sender, String[] args) {
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage(Component.text("Este comando solo puede ser ejecutado por un jugador dentro del servidor.", NamedTextColor.RED));
            return;
        }

        if (!viewer.hasPermission("invsee.use") && !viewer.hasPermission("ndh.admin")) {
            viewer.sendMessage(Component.text("No tienes permiso para usar este comando.", NamedTextColor.RED));
            return;
        }

        SessionManager sessionManager = this.sessionManager;
        if (sessionManager == null) {
            viewer.sendMessage(Component.text("El módulo InvSee no está inicializado.", NamedTextColor.RED));
            return;
        }

        if (args.length < 1) {
            viewer.sendMessage(Component.text("Uso: /ndh openinv <jugador|uuid> [ender]", NamedTextColor.YELLOW));
            return;
        }

        String targetArg = args[0];
        boolean openEnder = args.length >= 2 && (args[1].equalsIgnoreCase("ender") || args[1].equalsIgnoreCase("ec"));

        resolveOfflinePlayer(viewer, targetArg, target -> {
            if (target == null) {
                viewer.sendMessage(Component.text("No se encontró al jugador '" + targetArg + "'.", NamedTextColor.RED));
                return;
            }

            Player onlineTarget = target.getPlayer();
            if (onlineTarget != null && onlineTarget.hasPermission("invsee.exempt")
                    && !viewer.hasPermission("invsee.exempt.bypass")
                    && !viewer.hasPermission("ndh.admin")
                    && !onlineTarget.getUniqueId().equals(viewer.getUniqueId())) {
                viewer.sendMessage(Component.text("No puedes inspeccionar el inventario de este jugador (está protegido).", NamedTextColor.RED));
                return;
            }

            if (openEnder) {
                if (!viewer.hasPermission("invsee.enderchest") && !viewer.hasPermission("ndh.admin")) {
                    viewer.sendMessage(Component.text("No tienes permiso para inspeccionar el Ender Chest.", NamedTextColor.RED));
                    return;
                }
                sessionManager.openEnderChestSee(viewer, target, true);
            } else {
                sessionManager.openInventorySee(viewer, target);
            }
        });
    }

    public void executeEnderSee(CommandSender sender, String[] args) {
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage(Component.text("Este comando solo puede ser ejecutado por un jugador dentro del servidor.", NamedTextColor.RED));
            return;
        }

        if (!viewer.hasPermission("invsee.enderchest") && !viewer.hasPermission("ndh.admin")) {
            viewer.sendMessage(Component.text("No tienes permiso para usar este comando.", NamedTextColor.RED));
            return;
        }

        SessionManager sessionManager = this.sessionManager;
        if (sessionManager == null) {
            viewer.sendMessage(Component.text("El módulo InvSee no está inicializado.", NamedTextColor.RED));
            return;
        }

        if (args.length < 1) {
            viewer.sendMessage(Component.text("Uso: /ndh endersee <jugador|uuid>", NamedTextColor.YELLOW));
            return;
        }

        String targetArg = args[0];
        resolveOfflinePlayer(viewer, targetArg, target -> {
            if (target == null) {
                viewer.sendMessage(Component.text("No se encontró al jugador '" + targetArg + "'.", NamedTextColor.RED));
                return;
            }

            Player onlineTarget = target.getPlayer();
            if (onlineTarget != null && onlineTarget.hasPermission("invsee.exempt")
                    && !viewer.hasPermission("invsee.exempt.bypass")
                    && !viewer.hasPermission("ndh.admin")
                    && !onlineTarget.getUniqueId().equals(viewer.getUniqueId())) {
                viewer.sendMessage(Component.text("No puedes inspeccionar el Ender Chest de este jugador (está protegido).", NamedTextColor.RED));
                return;
            }

            sessionManager.openEnderChestSee(viewer, target);
        });
    }

    public List<String> suggestInvSee(String[] args) {
        if (args.length <= 1) {
            return suggestPlayers(args);
        }
        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return List.of("ender").stream()
                    .filter(s -> s.startsWith(prefix))
                    .toList();
        }
        return List.of();
    }

    public List<String> suggestPlayers(String[] args) {
        if (args.length == 0) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList();
        }
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return Arrays.stream(Bukkit.getOfflinePlayers())
                    .map(OfflinePlayer::getName)
                    .filter(name -> name != null && name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .limit(30)
                    .toList();
        }
        return List.of();
    }

    public void resolveOfflinePlayer(Player viewer, String input, Consumer<OfflinePlayer> callback) {
        Player online = Bukkit.getPlayerExact(input);
        if (online != null) {
            callback.accept(online);
            return;
        }

        try {
            UUID uuid = UUID.fromString(input);
            OfflinePlayer byUuid = Bukkit.getOfflinePlayer(uuid);
            callback.accept(byUuid);
            return;
        } catch (IllegalArgumentException ignored) {
        }

        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(input);
        if (cached != null) {
            callback.accept(cached);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer found = null;
            for (OfflinePlayer candidate : Bukkit.getOfflinePlayers()) {
                if (candidate.getName() != null && candidate.getName().equalsIgnoreCase(input)) {
                    found = candidate;
                    break;
                }
            }
            OfflinePlayer finalFound = found;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (viewer.isOnline()) {
                    callback.accept(finalFound);
                }
            });
        });
    }
}
