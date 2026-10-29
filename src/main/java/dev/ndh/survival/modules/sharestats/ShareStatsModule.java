package dev.ndh.survival.modules.sharestats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import dev.ndh.survival.NdhSurvivalPlugin;
import dev.ndh.survival.core.module.SurvivalModule;
import dev.ndh.survival.modules.sharestats.command.ShareStatsSubCommand;
import org.jspecify.annotations.Nullable;

/**
 * Módulo ShareStats: comparte salud, hambre, experiencia y efectos de poción
 * entre todos los jugadores.
 */
public final class ShareStatsModule implements SurvivalModule, Listener {

    private final NdhSurvivalPlugin plugin;
    private @Nullable SharedSnapshot sharedSnapshot;
    private @Nullable Object placeholderExpansion;
    private boolean enabled;
    private boolean syncHealth;
    private boolean syncFood;
    private boolean syncExperience;
    private boolean syncPotionEffects;
    private boolean synchronizing;
    private boolean captureScheduled;
    private @Nullable UUID pendingCaptureSource;

    public ShareStatsModule(NdhSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    public NdhSurvivalPlugin getPlugin() {
        return plugin;
    }

    @Override
    public String getId() {
        return "sharestats";
    }

    @Override
    public String getDisplayName() {
        return "ShareStats";
    }

    @Override
    public String getDescription() {
        return "Comparte salud, hambre, experiencia y efectos entre todos los jugadores.";
    }

    @Override
    public void onEnable() {
        loadSettings();

        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getCommandRouter().registerSubCommand(new ShareStatsSubCommand(this));

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            registerPlaceholderExpansion();
        }

        if (enabled) {
            Player firstPlayer = firstOnlinePlayer();
            if (firstPlayer != null) {
                sharedSnapshot = SharedSnapshot.from(firstPlayer);
                applySharedSnapshot();
            }
        }
    }

    private @Nullable Player firstOnlinePlayer() {
        List<Player> online = List.copyOf(Bukkit.getOnlinePlayers());
        return online.isEmpty() ? null : online.get(0);
    }

    @Override
    public void onDisable() {
        Object placeholderExpansion = this.placeholderExpansion;
        if (placeholderExpansion != null) {
            try {
                placeholderExpansion.getClass().getMethod("unregister").invoke(placeholderExpansion);
            } catch (ReflectiveOperationException exception) {
                plugin.getLogger().warning("No se pudo desregistrar la expansión de PlaceholderAPI: "
                        + exception.getMessage());
            }
            this.placeholderExpansion = null;
        }
    }

    @Override
    public void onReload() {
        loadSettings();
        if (enabled) {
            Player firstPlayer = firstOnlinePlayer();
            if (firstPlayer != null) {
                captureAndApply(firstPlayer);
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private void registerPlaceholderExpansion() {
        try {
            Class<?> expansionClass = Class.forName(
                    "dev.ndh.survival.modules.sharestats.ShareStatsExpansion", true,
                    plugin.getClass().getClassLoader());
            Object expansion = expansionClass.getConstructor(ShareStatsModule.class).newInstance(this);
            expansionClass.getMethod("register").invoke(expansion);
            if (expansion != null) {
                placeholderExpansion = expansion;
            }
            plugin.getLogger().info("PlaceholderAPI detectado; expansión %sharestats_*% registrada.");
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().warning("PlaceholderAPI está instalado, pero no se pudo registrar la expansión: "
                    + exception.getMessage());
        }
    }

    private void loadSettings() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("sharestats");
        if (sec == null) {
            sec = plugin.getConfig().createSection("sharestats");
        }
        enabled = sec.getBoolean("enabled", true);
        syncHealth = sec.getBoolean("sync.health", true);
        syncFood = sec.getBoolean("sync.food", true);
        syncExperience = sec.getBoolean("sync.experience", true);
        syncPotionEffects = sec.getBoolean("sync.potion-effects", true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled) {
            return;
        }

        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!enabled || !player.isOnline()) {
                return;
            }
            if (sharedSnapshot == null) {
                captureAndApply(player);
            } else {
                applySnapshotTo(player);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        scheduleCapture(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRegainHealth(EntityRegainHealthEvent event) {
        scheduleCapture(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        scheduleCapture(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExperienceChange(PlayerExpChangeEvent event) {
        scheduleCapture(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLevelChange(PlayerLevelChangeEvent event) {
        scheduleCapture(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPotionEffectChange(EntityPotionEffectEvent event) {
        scheduleCapture(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!enabled || !syncHealth || synchronizing) {
            return;
        }

        SharedSnapshot snapshot = sharedSnapshot;
        if (snapshot == null) {
            snapshot = SharedSnapshot.from(event.getEntity());
        }
        sharedSnapshot = snapshot.withHealth(0.0);
        Bukkit.getScheduler().runTask(plugin, this::applySharedSnapshot);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!enabled) {
            return;
        }

        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (enabled && player.isOnline() && !player.isDead()) {
                captureAndApply(player);
            }
        });
    }

    private void scheduleCapture(Entity entity) {
        if (!enabled || synchronizing || !(entity instanceof Player player)) {
            return;
        }

        pendingCaptureSource = player.getUniqueId();
        if (captureScheduled) {
            return;
        }

        captureScheduled = true;
        Bukkit.getScheduler().runTask(plugin, () -> {
            captureScheduled = false;
            UUID sourceId = pendingCaptureSource;
            pendingCaptureSource = null;
            if (!enabled || sourceId == null) {
                return;
            }

            Player source = Bukkit.getPlayer(sourceId);
            if (source != null && source.isOnline() && !source.isDead()) {
                captureAndApply(source);
            }
        });
    }

    private void captureAndApply(Player source) {
        if (!enabled || synchronizing || source.isDead()) {
            return;
        }
        plugin.getModuleManager().getModule("progresion").ifPresent(module -> {
            if (module instanceof dev.ndh.survival.modules.progresion.ProgresionModule progression) {
                var service = progression.service();
                if (service != null) service.settleFood();
            }
        });
        sharedSnapshot = SharedSnapshot.from(source);
        applySharedSnapshot();
    }

    private void applySharedSnapshot() {
        SharedSnapshot snapshot = this.sharedSnapshot;
        if (!enabled || snapshot == null || synchronizing) {
            return;
        }

        synchronizing = true;
        try {
            for (Player player : List.copyOf(Bukkit.getOnlinePlayers())) {
                if (!player.isDead()) {
                    applySnapshotTo(player, snapshot);
                }
            }
        } finally {
            synchronizing = false;
        }
    }

    private void applySnapshotTo(Player player) {
        SharedSnapshot snapshot = this.sharedSnapshot;
        if (snapshot == null) {
            return;
        }
        applySnapshotTo(player, snapshot);
    }

    private void applySnapshotTo(Player player, SharedSnapshot snapshot) {
        if (!enabled || player.isDead()) {
            return;
        }

        boolean wasSynchronizing = synchronizing;
        synchronizing = true;
        try {
            if (syncPotionEffects) {
                applyPotionEffects(player, snapshot);
            }
            if (syncFood) {
                player.setFoodLevel(snapshot.foodLevel());
                player.setSaturation(snapshot.saturation());
                player.setExhaustion(snapshot.exhaustion());
            }
            if (syncExperience) {
                player.setTotalExperience(snapshot.totalExperience());
                player.setLevel(snapshot.experienceLevel());
                player.setExp(snapshot.experienceProgress());
            }
            if (syncHealth) {
                AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
                double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 20.0;
                double health = Math.min(snapshot.health(), maxHealth);
                player.setHealth(Math.max(0.0, health));
            }
        } finally {
            synchronizing = wasSynchronizing;
        }
    }

    private void applyPotionEffects(Player player, SharedSnapshot snapshot) {
        Map<PotionEffectType, PotionEffect> desiredEffects = new TreeMap<>(
                Comparator.comparing(effectType -> effectType.getKey().toString()));
        for (PotionEffect effect : snapshot.potionEffects()) {
            desiredEffects.put(effect.getType(), effect);
        }

        for (PotionEffect current : List.copyOf(player.getActivePotionEffects())) {
            if (!desiredEffects.containsKey(current.getType())) {
                player.removePotionEffect(current.getType());
            }
        }

        for (PotionEffect effect : desiredEffects.values()) {
            player.addPotionEffect(effect);
        }
    }

    public void setStatsEnabled(boolean shouldEnable) {
        this.enabled = shouldEnable;
        plugin.getConfig().set("sharestats.enabled", enabled);
        plugin.saveConfig();

        if (enabled) {
            sharedSnapshot = null;
            Player firstPlayer = firstOnlinePlayer();
            if (firstPlayer != null) {
                captureAndApply(firstPlayer);
            }
        }
    }

    public @Nullable String placeholderValue(@Nullable OfflinePlayer offlinePlayer, String parameter) {
        String key = parameter.toLowerCase(Locale.ROOT);
        if (key.equals("enabled")) {
            return Boolean.toString(enabled);
        }

        Player player = offlinePlayer == null ? null : offlinePlayer.getPlayer();
        SharedSnapshot snapshot = player == null ? sharedSnapshot : SharedSnapshot.from(player);
        if (snapshot == null) {
            return "";
        }

        return switch (key) {
            case "health" -> format(player == null ? snapshot.health() : player.getHealth());
            case "max_health" -> {
                if (player == null)
                    yield "";
                AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
                yield format(attr != null ? attr.getValue() : 20.0);
            }
            case "food" -> Integer.toString(player == null ? snapshot.foodLevel() : player.getFoodLevel());
            case "saturation" -> format(player == null ? snapshot.saturation() : player.getSaturation());
            case "level" -> Integer.toString(player == null ? snapshot.experienceLevel() : player.getLevel());
            case "exp" -> format(player == null ? snapshot.experienceProgress() : player.getExp());
            case "total_exp" -> Integer.toString(player == null
                    ? snapshot.totalExperience()
                    : player.getTotalExperience());
            case "effects" -> formatEffects(player == null
                    ? snapshot.potionEffects()
                    : List.copyOf(player.getActivePotionEffects()));
            default -> null;
        };
    }

    private String formatEffects(List<PotionEffect> effects) {
        List<String> names = new ArrayList<>();
        effects.stream()
                .sorted(Comparator.comparing(effect -> effect.getType().getKey().toString()))
                .forEach(effect -> names.add(effect.getType().getKey().getKey()
                        + ":" + (effect.getAmplifier() + 1)));
        return String.join(",", names);
    }

    private String format(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
