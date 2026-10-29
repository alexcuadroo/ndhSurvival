package dev.ndh.survival.modules.creeperask.listener;

import dev.ndh.survival.modules.creeperask.session.SessionManager;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExplosionPrimeEvent;

public final class CreeperExplosionListener implements Listener {
    private final SessionManager sessionManager;

    public CreeperExplosionListener(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Creeper creeper) {
            if (this.sessionManager.isPending(creeper)) {
                event.setCancelled(true);
                creeper.setIgnited(false);
            }
        }
    }
}
