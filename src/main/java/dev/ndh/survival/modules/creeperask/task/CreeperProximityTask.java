package dev.ndh.survival.modules.creeperask.task;

import dev.ndh.survival.modules.creeperask.CreeperAskModule;
import dev.ndh.survival.modules.creeperask.gui.GuiManager;
import dev.ndh.survival.modules.creeperask.session.SessionManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.bukkit.GameMode;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class CreeperProximityTask extends BukkitRunnable {
    private static final double RADIUS = 5.0;
    private static final double RADIUS_SQUARED = RADIUS * RADIUS;
    private final JavaPlugin plugin;
    private final CreeperAskModule module;
    private final SessionManager sessionManager;

    public CreeperProximityTask(JavaPlugin plugin, CreeperAskModule module, SessionManager sessionManager) {
        this.plugin = plugin;
        this.module = module;
        this.sessionManager = sessionManager;
    }

    @Override
    public void run() {
        if (!this.module.isEnabled()) {
            return;
        }

        List<Player> onlinePlayers = List.copyOf(this.plugin.getServer().getOnlinePlayers());
        List<Player> eligiblePlayers = new ArrayList<>();
        for (Player candidate : onlinePlayers) {
            if (this.isEligible(candidate)) {
                eligiblePlayers.add(candidate);
            }
        }

        for (Player player : eligiblePlayers) {
            Optional<Creeper> nearestCreeper = player.getNearbyEntities(RADIUS, RADIUS, RADIUS).stream()
                    .filter(Creeper.class::isInstance)
                    .map(Creeper.class::cast)
                    .filter(Creeper::isValid)
                    .filter(creeper -> this.sessionManager.canAsk(player, creeper))
                    .filter(creeper -> this.isClosestEligiblePlayer(player, creeper, eligiblePlayers))
                    .min(Comparator.comparingDouble(creeper ->
                            creeper.getLocation().distanceSquared(player.getLocation())));

            nearestCreeper.ifPresent(creeper -> {
                this.sessionManager.start(player, creeper);
                GuiManager.open(player);
            });
        }
    }

    private boolean isEligible(Player player) {
        return player.isOnline() && (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE);
    }

    private boolean isClosestEligiblePlayer(Player player, Creeper creeper, List<Player> eligiblePlayers) {
        Optional<Player> closest = eligiblePlayers.stream()
                .filter(candidate -> candidate.getWorld().equals(creeper.getWorld()))
                .filter(candidate -> candidate.getLocation().distanceSquared(creeper.getLocation()) <= RADIUS_SQUARED)
                .filter(candidate -> this.sessionManager.canAsk(candidate, creeper))
                .min(Comparator.comparingDouble(candidate ->
                        candidate.getLocation().distanceSquared(creeper.getLocation())));

        return closest.filter(player::equals).isPresent();
    }
}
