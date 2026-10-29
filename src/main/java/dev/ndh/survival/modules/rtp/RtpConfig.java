package dev.ndh.survival.modules.rtp;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Configuración tipada para el módulo Random Teleport (RTP).
 */
public final class RtpConfig {

    private final boolean enabled;
    private final int minRadius;
    private final int maxRadius;
    private final boolean useSpawnCenter;
    private final double centerX;
    private final double centerZ;
    private final int maxAttempts;
    private final int cooldownSeconds;
    private final int warmupSeconds;
    private final boolean cancelOnMove;
    private final boolean cancelOnDamage;
    private final int fallProtectionSeconds;
    private final boolean avoidLeaves;
    private final boolean avoidOceanBiomes;
    private final Set<String> allowedWorlds;
    private final Set<Material> unsafeBlocks;
    private final boolean soundsEnabled;
    private final boolean particlesEnabled;

    // Mensajes en formato MiniMessage
    private final String prefix;
    private final String msgDisabled;
    private final String msgWorldDisabled;
    private final String msgAlreadySearching;
    private final String msgCooldown;
    private final String msgSearching;
    private final String msgWarmupStart;
    private final String msgWarmupTick;
    private final String msgWarmupCancelledMove;
    private final String msgWarmupCancelledDamage;
    private final String msgTeleportSuccess;
    private final String msgSearchFailed;

    public RtpConfig(@Nullable ConfigurationSection sec) {
        if (sec == null) {
            this.enabled = true;
            this.minRadius = 250;
            this.maxRadius = 2500;
            this.useSpawnCenter = true;
            this.centerX = 0.0;
            this.centerZ = 0.0;
            this.maxAttempts = 10;
            this.cooldownSeconds = 60;
            this.warmupSeconds = 3;
            this.cancelOnMove = true;
            this.cancelOnDamage = true;
            this.fallProtectionSeconds = 6;
            this.avoidLeaves = true;
            this.avoidOceanBiomes = true;
            this.allowedWorlds = Set.of("world");
            this.unsafeBlocks = buildDefaultUnsafeBlocks();
            this.soundsEnabled = true;
            this.particlesEnabled = true;

            this.prefix = "<gold>[RTP]</gold>";
            this.msgDisabled = "{prefix} <red>El teletransporte aleatorio está desactivado temporalmente.</red>";
            this.msgWorldDisabled = "{prefix} <red>No puedes usar /rtp en este mundo.</red>";
            this.msgAlreadySearching = "{prefix} <yellow>Ya tienes un teletransporte aleatorio en curso.</yellow>";
            this.msgCooldown = "{prefix} <red>Debes esperar <yellow>{seconds} segundos</yellow> más para usar /rtp.</red>";
            this.msgSearching = "{prefix} <gray>Buscando un lugar seguro...</gray>";
            this.msgWarmupStart = "{prefix} <yellow>Lugar seguro encontrado. No te muevas ni recibas daño durante <gold>{seconds}s</gold>...</yellow>";
            this.msgWarmupTick = "<gold>Teletransportando en <yellow>{seconds}s</yellow>...</gold>";
            this.msgWarmupCancelledMove = "{prefix} <red>Teletransporte cancelado porque te moviste.</red>";
            this.msgWarmupCancelledDamage = "{prefix} <red>Teletransporte cancelado porque recibiste daño.</red>";
            this.msgTeleportSuccess = "{prefix} <green>¡Has sido teletransportado a un lugar seguro! <gray>({x}, {y}, {z})</gray></green>";
            this.msgSearchFailed = "{prefix} <red>No se pudo encontrar un lugar seguro tras {attempts} intentos. Inténtalo de nuevo en unos momentos.</red>";
            return;
        }

        this.enabled = sec.getBoolean("enabled", true);
        this.minRadius = Math.max(0, sec.getInt("min-radius", 250));
        this.maxRadius = Math.max(this.minRadius + 16, sec.getInt("max-radius", 2500));
        this.useSpawnCenter = sec.getBoolean("use-spawn-center", true);
        this.centerX = sec.getDouble("center-x", 0.0);
        this.centerZ = sec.getDouble("center-z", 0.0);
        this.maxAttempts = Math.max(1, Math.min(30, sec.getInt("max-attempts", 10)));
        this.cooldownSeconds = Math.max(0, sec.getInt("cooldown-seconds", 60));
        this.warmupSeconds = Math.max(0, sec.getInt("warmup-seconds", 3));
        this.cancelOnMove = sec.getBoolean("cancel-on-move", true);
        this.cancelOnDamage = sec.getBoolean("cancel-on-damage", true);
        this.fallProtectionSeconds = Math.max(0, sec.getInt("fall-protection-seconds", 6));
        this.avoidLeaves = sec.getBoolean("avoid-leaves", true);
        this.avoidOceanBiomes = sec.getBoolean("avoid-ocean-biomes", true);

        List<String> worldsList = sec.getStringList("allowed-worlds");
        Set<String> worlds = new HashSet<>();
        for (String w : worldsList) {
            if (w != null && !w.isBlank()) {
                worlds.add(w.trim());
            }
        }
        this.allowedWorlds = worlds;

        this.unsafeBlocks = buildDefaultUnsafeBlocks();
        List<String> extraUnsafe = sec.getStringList("unsafe-blocks");
        for (String matName : extraUnsafe) {
            if (matName == null || matName.isBlank()) continue;
            Material mat = Material.matchMaterial(matName.trim().toUpperCase(Locale.ROOT));
            if (mat != null) {
                this.unsafeBlocks.add(mat);
            }
        }

        this.soundsEnabled = sec.getBoolean("effects.sounds", true);
        this.particlesEnabled = sec.getBoolean("effects.particles", true);

        this.prefix = sec.getString("messages.prefix", "<gold>[RTP]</gold>");
        this.msgDisabled = sec.getString("messages.disabled",
                "{prefix} <red>El teletransporte aleatorio está desactivado temporalmente.</red>");
        this.msgWorldDisabled = sec.getString("messages.world-disabled",
                "{prefix} <red>No puedes usar /rtp en este mundo.</red>");
        this.msgAlreadySearching = sec.getString("messages.already-searching",
                "{prefix} <yellow>Ya tienes un teletransporte aleatorio en curso.</yellow>");
        this.msgCooldown = sec.getString("messages.cooldown",
                "{prefix} <red>Debes esperar <yellow>{seconds} segundos</yellow> más para usar /rtp.</red>");
        this.msgSearching = sec.getString("messages.searching",
                "{prefix} <gray>Buscando un lugar seguro...</gray>");
        this.msgWarmupStart = sec.getString("messages.warmup-start",
                "{prefix} <yellow>Lugar seguro encontrado. No te muevas ni recibas daño durante <gold>{seconds}s</gold>...</yellow>");
        this.msgWarmupTick = sec.getString("messages.warmup-tick",
                "<gold>Teletransportando en <yellow>{seconds}s</yellow>...</gold>");
        this.msgWarmupCancelledMove = sec.getString("messages.cancelled-move",
                "{prefix} <red>Teletransporte cancelado porque te moviste.</red>");
        this.msgWarmupCancelledDamage = sec.getString("messages.cancelled-damage",
                "{prefix} <red>Teletransporte cancelado porque recibiste daño.</red>");
        this.msgTeleportSuccess = sec.getString("messages.teleport-success",
                "{prefix} <green>¡Has sido teletransportado a un lugar seguro! <gray>({x}, {y}, {z})</gray></green>");
        this.msgSearchFailed = sec.getString("messages.search-failed",
                "{prefix} <red>No se pudo encontrar un lugar seguro tras {attempts} intentos. Inténtalo de nuevo en unos momentos.</red>");
    }

    private static Set<Material> buildDefaultUnsafeBlocks() {
        Set<Material> set = EnumSet.noneOf(Material.class);
        set.add(Material.WATER);
        set.add(Material.LAVA);
        set.add(Material.FIRE);
        set.add(Material.SOUL_FIRE);
        set.add(Material.CACTUS);
        set.add(Material.MAGMA_BLOCK);
        set.add(Material.CAMPFIRE);
        set.add(Material.SOUL_CAMPFIRE);
        set.add(Material.SWEET_BERRY_BUSH);
        set.add(Material.POWDER_SNOW);
        set.add(Material.WITHER_ROSE);
        set.add(Material.POINTED_DRIPSTONE);
        set.add(Material.COBWEB);
        set.add(Material.NETHER_PORTAL);
        set.add(Material.END_PORTAL);
        set.add(Material.END_GATEWAY);
        set.add(Material.BEDROCK);
        return set;
    }

    public boolean isBlockSafeGround(Material groundType) {
        if (groundType == null || groundType.isAir() || !groundType.isSolid()) {
            return false;
        }
        if (unsafeBlocks.contains(groundType)) {
            return false;
        }
        if (avoidLeaves && Tag.LEAVES.isTagged(groundType)) {
            return false;
        }
        return true;
    }

    public boolean isWorldAllowed(String worldName) {
        if (allowedWorlds.isEmpty()) {
            return true;
        }
        for (String allowed : allowedWorlds) {
            if (allowed.equalsIgnoreCase(worldName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMinRadius() {
        return minRadius;
    }

    public int getMaxRadius() {
        return maxRadius;
    }

    public boolean isUseSpawnCenter() {
        return useSpawnCenter;
    }

    public double getCenterX() {
        return centerX;
    }

    public double getCenterZ() {
        return centerZ;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public int getWarmupSeconds() {
        return warmupSeconds;
    }

    public boolean isCancelOnMove() {
        return cancelOnMove;
    }

    public boolean isCancelOnDamage() {
        return cancelOnDamage;
    }

    public int getFallProtectionSeconds() {
        return fallProtectionSeconds;
    }

    public boolean isAvoidLeaves() {
        return avoidLeaves;
    }

    public boolean isAvoidOceanBiomes() {
        return avoidOceanBiomes;
    }

    public Set<String> getAllowedWorlds() {
        return allowedWorlds;
    }

    public boolean isSoundsEnabled() {
        return soundsEnabled;
    }

    public boolean isParticlesEnabled() {
        return particlesEnabled;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getMsgDisabled() {
        return msgDisabled;
    }

    public String getMsgWorldDisabled() {
        return msgWorldDisabled;
    }

    public String getMsgAlreadySearching() {
        return msgAlreadySearching;
    }

    public String getMsgCooldown() {
        return msgCooldown;
    }

    public String getMsgSearching() {
        return msgSearching;
    }

    public String getMsgWarmupStart() {
        return msgWarmupStart;
    }

    public String getMsgWarmupTick() {
        return msgWarmupTick;
    }

    public String getMsgWarmupCancelledMove() {
        return msgWarmupCancelledMove;
    }

    public String getMsgWarmupCancelledDamage() {
        return msgWarmupCancelledDamage;
    }

    public String getMsgTeleportSuccess() {
        return msgTeleportSuccess;
    }

    public String getMsgSearchFailed() {
        return msgSearchFailed;
    }
}
