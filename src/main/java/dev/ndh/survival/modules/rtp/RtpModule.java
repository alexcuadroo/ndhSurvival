package dev.ndh.survival.modules.rtp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import dev.ndh.survival.NdhSurvivalPlugin;
import dev.ndh.survival.core.module.SurvivalModule;
import dev.ndh.survival.modules.rtp.command.RtpAdminSubCommand;
import dev.ndh.survival.modules.rtp.command.RtpPublicCommand;
import dev.ndh.survival.modules.rtp.listener.RtpListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jspecify.annotations.Nullable;

/**
 * Módulo Random Teleport (RTP):
 * Búsqueda asíncrona de lugar seguro, warmup estático, cooldown por jugador,
 * efectos visuales/sonoros y resistencia contra daño de caída.
 */
public final class RtpModule implements SurvivalModule {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final NdhSurvivalPlugin plugin;
    private @Nullable RtpConfig config;
    private @Nullable SafeLocationFinder locationFinder;

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> searchingPlayers = new HashSet<>();
    private final Map<UUID, WarmupSession> activeWarmups = new HashMap<>();

    public RtpModule(NdhSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    public NdhSurvivalPlugin getPlugin() {
        return plugin;
    }

    public @Nullable RtpConfig getConfig() {
        return config;
    }

    @Override
    public String getId() {
        return "rtp";
    }

    @Override
    public String getDisplayName() {
        return "RandomTeleport (RTP)";
    }

    @Override
    public String getDescription() {
        return "Teletransporte aleatorio seguro asíncrono (/rtp) con warmup, cooldown y administración (/ndh rtp).";
    }

    @Override
    public void onEnable() {
        RtpConfig config = new RtpConfig(plugin.getConfig().getConfigurationSection("rtp"));
        this.config = config;
        this.locationFinder = new SafeLocationFinder(plugin, config);

        // Registrar listener de warmup
        plugin.getServer().getPluginManager().registerEvents(new RtpListener(this), plugin);

        // Registrar subcomando administrativo /ndh rtp
        plugin.getCommandRouter().registerSubCommand(new RtpAdminSubCommand(this));

        // Registrar comando público /rtp para usuarios normales
        PluginCommand rtpCommand = plugin.getCommand("rtp");
        if (rtpCommand != null) {
            RtpPublicCommand publicExecutor = new RtpPublicCommand(this);
            rtpCommand.setExecutor(publicExecutor);
            rtpCommand.setTabCompleter(publicExecutor);
        } else {
            plugin.getLogger().warning("El comando 'rtp' no está definido en plugin.yml.");
        }
    }

    @Override
    public void onDisable() {
        cancelAllSessions();
        cooldowns.clear();
    }

    @Override
    public void onReload() {
        RtpConfig config = new RtpConfig(plugin.getConfig().getConfigurationSection("rtp"));
        this.config = config;
        SafeLocationFinder locationFinder = this.locationFinder;
        if (locationFinder != null) {
            locationFinder.setConfig(config);
        }
        if (!config.isEnabled()) {
            cancelAllSessions();
        }
    }

    @Override
    public boolean isEnabled() {
        return config != null && config.isEnabled();
    }

    public void clearAllCooldowns() {
        cooldowns.clear();
    }

    public void clearCooldown(UUID uuid) {
        cooldowns.remove(uuid);
    }

    private void cancelAllSessions() {
        searchingPlayers.clear();
        for (WarmupSession session : new ArrayList<>(activeWarmups.values())) {
            session.cancelTask();
        }
        activeWarmups.clear();
    }

    /**
     * Inicia el proceso de RTP para un jugador.
     */
    public void startPlayerRtp(Player player, boolean forceByAdmin) {
        RtpConfig config = this.config;
        SafeLocationFinder locationFinder = this.locationFinder;
        if (config == null || locationFinder == null) {
            return;
        }

        UUID uuid = player.getUniqueId();

        if (!forceByAdmin && !isEnabled()) {
            sendFormatted(player, config.getMsgDisabled());
            return;
        }

        World world = player.getWorld();
        if (!forceByAdmin && !config.isWorldAllowed(world.getName())) {
            sendFormatted(player, config.getMsgWorldDisabled());
            return;
        }

        if (searchingPlayers.contains(uuid) || activeWarmups.containsKey(uuid)) {
            sendFormatted(player, config.getMsgAlreadySearching());
            return;
        }

        boolean bypassCooldown = forceByAdmin
                || player.hasPermission("rtp.bypass.cooldown")
                || player.hasPermission("ndh.admin");

        if (!bypassCooldown && config.getCooldownSeconds() > 0) {
            long remainingSeconds = getRemainingCooldownSeconds(uuid);
            if (remainingSeconds > 0) {
                String msg = config.getMsgCooldown()
                        .replace("{seconds}", String.valueOf(remainingSeconds));
                sendFormatted(player, msg);
                return;
            }
        }

        searchingPlayers.add(uuid);
        sendFormatted(player, config.getMsgSearching());

        if (config.isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.2f);
        }
        if (config.isParticlesEnabled()) {
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0.0, 1.0, 0.0), 25, 0.4, 0.8, 0.4,
                    0.1);
        }

        locationFinder.findSafeLocation(world, safeDestination -> {
            searchingPlayers.remove(uuid);
            if (!player.isOnline()) {
                return;
            }

            if (safeDestination == null) {
                String failMsg = config.getMsgSearchFailed()
                        .replace("{attempts}", String.valueOf(config.getMaxAttempts()));
                sendFormatted(player, failMsg);
                if (config.isSoundsEnabled()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                }
                return;
            }

            safeDestination.setYaw(player.getLocation().getYaw());
            safeDestination.setPitch(player.getLocation().getPitch());

            boolean bypassWarmup = forceByAdmin
                    || config.getWarmupSeconds() <= 0
                    || player.hasPermission("rtp.bypass.warmup")
                    || player.hasPermission("ndh.admin");

            if (bypassWarmup) {
                executeTeleport(player, safeDestination, !bypassCooldown);
            } else {
                beginWarmup(player, safeDestination, !bypassCooldown);
            }
        });
    }

    private void beginWarmup(Player player, Location destination, boolean applyCooldown) {
        RtpConfig config = this.config;
        if (config == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        int warmupSec = config.getWarmupSeconds();

        String startMsg = config.getMsgWarmupStart()
                .replace("{seconds}", String.valueOf(warmupSec));
        sendFormatted(player, startMsg);

        WarmupSession session = new WarmupSession(player, player.getLocation().clone(), destination, warmupSec,
                applyCooldown);
        activeWarmups.put(uuid, session);
        session.start();
    }

    private void executeTeleport(Player player, Location destination, boolean applyCooldown) {
        RtpConfig config = this.config;
        if (config == null || !player.isOnline()) {
            return;
        }

        if (applyCooldown && config.getCooldownSeconds() > 0) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        }

        if (config.isParticlesEnabled()) {
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0.0, 1.0, 0.0), 40, 0.4, 0.9, 0.4,
                    0.2);
        }

        int fallTicks = config.getFallProtectionSeconds() * 20;
        if (fallTicks > 0) {
            player.setFallDistance(0f);
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, fallTicks, 4, false, false, true));
        }

        player.teleportAsync(destination).thenAccept(success -> {
            if (!success || !player.isOnline()) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline())
                    return;

                player.setFallDistance(0f);
                if (config.isSoundsEnabled()) {
                    player.playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    player.playSound(destination, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                }
                if (config.isParticlesEnabled()) {
                    destination.getWorld().spawnParticle(
                            Particle.REVERSE_PORTAL,
                            destination.clone().add(0.0, 1.0, 0.0),
                            45, 0.5, 0.9, 0.5, 0.06);
                }

                String successMsg = config.getMsgTeleportSuccess()
                        .replace("{x}", String.valueOf(destination.getBlockX()))
                        .replace("{y}", String.valueOf(destination.getBlockY()))
                        .replace("{z}", String.valueOf(destination.getBlockZ()))
                        .replace("{world}", destination.getWorld().getName());
                sendFormatted(player, successMsg);
            });
        });
    }

    public long getRemainingCooldownSeconds(UUID uuid) {
        RtpConfig config = this.config;
        if (config == null) {
            return 0L;
        }
        Long lastUsed = cooldowns.get(uuid);
        if (lastUsed == null) {
            return 0L;
        }
        long elapsedMillis = System.currentTimeMillis() - lastUsed;
        long cooldownMillis = config.getCooldownSeconds() * 1000L;
        if (elapsedMillis >= cooldownMillis) {
            cooldowns.remove(uuid);
            return 0L;
        }
        return Math.max(1L, (long) Math.ceil((cooldownMillis - elapsedMillis) / 1000.0));
    }

    public void handlePlayerMove(PlayerMoveEvent event) {
        RtpConfig config = this.config;
        if (config == null || !config.isCancelOnMove()) {
            return;
        }
        Player player = event.getPlayer();
        WarmupSession session = activeWarmups.get(player.getUniqueId());
        if (session == null) {
            return;
        }

        Location to = event.getTo();
        Location initial = session.initialLocation;
        if (to == null) {
            return;
        }

        if (!to.getWorld().equals(initial.getWorld())
                || to.getBlockX() != initial.getBlockX()
                || to.getBlockY() != initial.getBlockY()
                || to.getBlockZ() != initial.getBlockZ()) {
            activeWarmups.remove(player.getUniqueId());
            session.cancelTask();
            sendFormatted(player, config.getMsgWarmupCancelledMove());
            player.sendActionBar(Component.empty());
            if (config.isSoundsEnabled()) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            }
        }
    }

    public void handlePlayerDamage(EntityDamageEvent event) {
        RtpConfig config = this.config;
        if (config == null || !config.isCancelOnDamage()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        WarmupSession session = activeWarmups.remove(player.getUniqueId());
        if (session != null) {
            session.cancelTask();
            sendFormatted(player, config.getMsgWarmupCancelledDamage());
            player.sendActionBar(Component.empty());
            if (config.isSoundsEnabled()) {
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1.0f);
            }
        }
    }

    public void handlePlayerQuit(UUID uuid) {
        searchingPlayers.remove(uuid);
        WarmupSession session = activeWarmups.remove(uuid);
        if (session != null) {
            session.cancelTask();
        }
    }

    private void sendFormatted(CommandSender receiver, String rawTemplate) {
        if (rawTemplate == null || rawTemplate.isBlank()) {
            return;
        }
        RtpConfig config = this.config;
        String prefix = config != null ? config.getPrefix() : "";
        String replaced = rawTemplate.replace("{prefix}", prefix);
        receiver.sendMessage(MINI.deserialize(replaced));
    }

    private final class WarmupSession {
        private final Player player;
        private final Location initialLocation;
        private final Location destination;
        private final boolean applyCooldown;
        private int secondsLeft;
        private @Nullable BukkitTask task;

        private WarmupSession(Player player, Location initialLocation, Location destination, int warmupSeconds,
                boolean applyCooldown) {
            this.player = player;
            this.initialLocation = initialLocation;
            this.destination = destination;
            this.secondsLeft = warmupSeconds;
            this.applyCooldown = applyCooldown;
        }

        private void start() {
            this.task = new BukkitRunnable() {
                @Override
                public void run() {
                    RtpConfig config = RtpModule.this.config;
                    if (config == null) {
                        cancel();
                        return;
                    }
                    if (!player.isOnline()) {
                        activeWarmups.remove(player.getUniqueId());
                        cancel();
                        return;
                    }

                    if (secondsLeft <= 0) {
                        activeWarmups.remove(player.getUniqueId());
                        player.sendActionBar(Component.empty());
                        cancel();
                        executeTeleport(player, destination, applyCooldown);
                        return;
                    }

                    String tickRaw = config.getMsgWarmupTick()
                            .replace("{prefix}", config.getPrefix())
                            .replace("{seconds}", String.valueOf(secondsLeft));
                    player.sendActionBar(MINI.deserialize(tickRaw));

                    if (config.isSoundsEnabled()) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
                    }
                    if (config.isParticlesEnabled()) {
                        player.getWorld().spawnParticle(
                                Particle.PORTAL,
                                player.getLocation().add(0.0, 1.0, 0.0),
                                20, 0.35, 0.7, 0.35, 0.15);
                    }

                    secondsLeft--;
                }
            }.runTaskTimer(plugin, 0L, 20L);
        }

        private void cancelTask() {
            BukkitTask task = this.task;
            if (task != null) {
                task.cancel();
                this.task = null;
            }
        }
    }
}
