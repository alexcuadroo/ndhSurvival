package dev.ndh.survival.modules.creeperask.session;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

public final class SessionManager {
    private final Map<UUID, CreeperSession> sessionsByPlayer = new HashMap<>();
    private final Set<UUID> pendingCreepers = new HashSet<>();
    private final NamespacedKey acceptedCreeperKey;

    public SessionManager(JavaPlugin plugin) {
        this.acceptedCreeperKey = new NamespacedKey(plugin, "accepted_creeper");
    }

    public boolean canAsk(Player player, Creeper creeper) {
        return !this.sessionsByPlayer.containsKey(player.getUniqueId())
                && !this.pendingCreepers.contains(creeper.getUniqueId())
                && !creeper.getPersistentDataContainer().has(this.acceptedCreeperKey, PersistentDataType.BYTE);
    }

    public void start(Player player, Creeper creeper) {
        CreeperSession session = new CreeperSession(player.getUniqueId(), creeper.getUniqueId(), creeper.hasAI());
        this.sessionsByPlayer.put(player.getUniqueId(), session);
        this.pendingCreepers.add(creeper.getUniqueId());
        creeper.setIgnited(false);
        creeper.setTarget(null);
        creeper.setAI(false);
        creeper.setVelocity(creeper.getVelocity().zero());
    }

    public boolean isPending(Creeper creeper) {
        return this.pendingCreepers.contains(creeper.getUniqueId());
    }

    public boolean hasSession(UUID playerId) {
        return this.sessionsByPlayer.containsKey(playerId);
    }

    public void accept(Player player) {
        CreeperSession session = this.removeSession(player.getUniqueId());
        if (session != null) {
            this.findCreeper(session.creeperId()).ifPresent((creeper) -> {
                creeper.getPersistentDataContainer().set(this.acceptedCreeperKey, PersistentDataType.BYTE, (byte) 1);
                this.restoreCreeper(creeper, session);
            });
        }
    }

    public boolean deny(Player player) {
        CreeperSession session = this.removeSession(player.getUniqueId());
        if (session == null) {
            return false;
        } else {
            this.findCreeper(session.creeperId()).ifPresent((creeper) -> {
                creeper.getWorld().spawnParticle(Particle.FIREWORK, creeper.getLocation().add(0.0, 1.0, 0.0), 35, 0.45,
                        0.55, 0.45, 0.03);
                creeper.remove();
            });
            return true;
        }
    }

    public void releaseAll() {
        Collection<CreeperSession> sessions = Set.copyOf(this.sessionsByPlayer.values());
        this.sessionsByPlayer.clear();
        this.pendingCreepers.clear();

        for (CreeperSession session : sessions) {
            Player player = Bukkit.getPlayer(session.playerId());
            if (player != null) {
                player.closeInventory();
            }

            this.findCreeper(session.creeperId()).ifPresent(creeper -> this.restoreCreeper(creeper, session));
        }
    }

    private @Nullable CreeperSession removeSession(UUID playerId) {
        CreeperSession session = this.sessionsByPlayer.remove(playerId);
        if (session != null) {
            this.pendingCreepers.remove(session.creeperId());
        }

        return session;
    }

    private Optional<Creeper> findCreeper(UUID id) {
        Entity entity = Bukkit.getEntity(id);
        if (entity instanceof Creeper creeper && creeper.isValid()) {
            return Optional.of(creeper);
        }

        return Optional.empty();
    }

    private void restoreCreeper(Creeper creeper, CreeperSession session) {
        creeper.setIgnited(false);
        creeper.setAI(session.creeperAiEnabled());
    }
}
