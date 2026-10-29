package dev.ndh.survival.modules.progresion.listener;

import dev.ndh.survival.NdhSurvivalPlugin;
import dev.ndh.survival.modules.progresion.ProgresionModule;
import dev.ndh.survival.modules.progresion.service.ProgressionService;
import java.util.random.RandomGenerator;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.persistence.PersistentDataType;

/** Aplica reglas en eventos sin crear entidades, cargar chunks ni alterar otros módulos. */
public final class ProgresionListener implements Listener {
    private final ProgresionModule module;
    private final NdhSurvivalPlugin plugin;
    private final RandomGenerator random;
    private final NamespacedKey projectileDamage;
    private final NamespacedKey processed;

    /** Crea el listener con aleatoriedad de producción. */
    public ProgresionListener(ProgresionModule module, NdhSurvivalPlugin plugin) {
        this(module, plugin, RandomGenerator.getDefault());
    }

    /** Crea el listener con aleatoriedad sustituible para pruebas. */
    public ProgresionListener(ProgresionModule module, NdhSurvivalPlugin plugin, RandomGenerator random) {
        this.module = module;
        this.plugin = plugin;
        this.random = random;
        projectileDamage = new NamespacedKey(plugin, "progresion_proyectil");
        processed = new NamespacedKey(plugin, "progresion_spawn");
    }

    /** Reduce nutrición y deja la saturación resuelta antes de la captura diferida de ShareStats. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent event) {
        ProgressionService service = module.service();
        var item = event.getItem();
        if (service == null || !service.active() || item == null || item.getType() != Material.BREAD
                || event.isCancelled()) return;
        if (service.rules().value(service.clock().day(), "pan-comida", -1) < 0
                && service.rules().value(service.clock().day(), "pan-saturacion", -1) < 0) return;
        var player = event.getEntity();
        double nutrition = service.rules().value(service.clock().day(), "pan-comida", 5);
        float saturation = (float) service.rules().value(service.clock().day(), "pan-saturacion", 6);
        float before = player.getSaturation();
        event.setFoodLevel(Math.min(20, player.getFoodLevel() + (int) nutrition));
        service.deferFood(player.getUniqueId(), () -> {
            if (!event.isCancelled() && player.isValid()) {
                player.setSaturation(Math.min(player.getFoodLevel(), before + saturation));
            }
        });
    }

    /** Aumenta únicamente agotamiento causado por caminar o correr. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExhaustion(EntityExhaustionEvent event) {
        ProgressionService service = module.service();
        if (service == null || !service.active() || event.isCancelled()) return;
        switch (event.getExhaustionReason()) {
            case WALK, SPRINT -> event.setExhaustion((float) (event.getExhaustion()
                    * (1 + service.rules().value(service.clock().day(), "agotamiento", 0))));
            default -> { }
        }
    }

    /** Personaliza una sola vez únicamente apariciones naturales de las tres especies exactas. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        ProgressionService service = module.service();
        if (service == null || !service.active() || event.isCancelled()
                || event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        LivingEntity entity = event.getEntity();
        String prefix = switch (entity.getType()) {
            case ZOMBIE -> "zombi";
            case SKELETON -> "esqueleto";
            case SPIDER -> "arana";
            default -> "";
        };
        if (prefix.isEmpty() || entity.getPersistentDataContainer().has(processed)) return;
        entity.getPersistentDataContainer().set(processed, PersistentDataType.BYTE, (byte) 1);
        int day = service.clock().day();
        if (entity instanceof Zombie zombie && service.rules().value(day, "zombi-bebe", -1) >= 0) {
            if (service.rules().rollBaby(day, random)) zombie.setBaby();
            else zombie.setAdult();
        }
        modify(entity, Attribute.MAX_HEALTH, service.rules().value(day, prefix + "-salud", 0));
        modify(entity, Attribute.ATTACK_DAMAGE, service.rules().value(day, prefix + "-dano", 0));
        modify(entity, Attribute.MOVEMENT_SPEED, service.rules().value(day, prefix + "-velocidad", 0));
        if (entity.getType() == EntityType.SKELETON) {
            entity.getPersistentDataContainer().set(projectileDamage, PersistentDataType.DOUBLE,
                    service.rules().value(day, "esqueleto-proyectil", 0));
        }
    }

    private void modify(LivingEntity entity, Attribute attribute, double amount) {
        var instance = entity.getAttribute(attribute);
        if (instance == null || amount == 0) return;
        NamespacedKey key = new NamespacedKey(plugin, "progresion_" + attribute.getKey().getKey());
        for (var modifier : instance.getModifiers()) {
            if (modifier.getKey().equals(key)) instance.removeModifier(modifier);
        }
        double health = entity.getHealth();
        double oldMax = attribute == Attribute.MAX_HEALTH ? instance.getValue() : 0;
        instance.addModifier(new AttributeModifier(key, instance.getBaseValue() * amount, AttributeModifier.Operation.ADD_NUMBER));
        if (attribute == Attribute.MAX_HEALTH) entity.setHealth(Math.min(instance.getValue(), health * instance.getValue() / oldMax));
    }

    /** Conserva el daño de la criatura de origen incluso si desaparece antes del impacto. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof LivingEntity shooter) {
            Double amount = shooter.getPersistentDataContainer().get(projectileDamage, PersistentDataType.DOUBLE);
            if (amount != null) event.getEntity().getPersistentDataContainer().set(projectileDamage, PersistentDataType.DOUBLE, amount);
        }
    }

    /** Modifica el evento original para que ShareStats y RTP observen el resultado habitual. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        ProgressionService service = module.service();
        if (service == null || !service.active() || !(event.getDamager() instanceof Projectile projectile)) return;
        Double amount = projectile.getPersistentDataContainer().get(projectileDamage, PersistentDataType.DOUBLE);
        if (amount != null) event.setDamage(event.getDamage() * (1 + amount));
    }

    /** Incluye mundos cargados después de comenzar la temporada. */
    @EventHandler public void onWorldLoad(WorldLoadEvent event) {
        ProgressionService service = module.service();
        if (service != null) service.enforceWorld(event.getWorld());
    }

    /** Evita dejar Difícil persistido en mundos descargados al desactivar después el módulo. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        ProgressionService service = module.service();
        if (service != null) service.restoreWorld(event.getWorld());
    }

    /** Informa del día global a jugadores nuevos y habituales. */
    @EventHandler public void onJoin(PlayerJoinEvent event) {
        ProgressionService service = module.service();
        if (service != null && service.active()) {
            event.getPlayer().sendMessage(Component.text("[Progresión] Día " + service.clock().day()
                    + (service.clock().day() == 30 ? " — dificultad máxima" : "")
                    + (service.clock().paused() ? " (pausada)" : "")));
        }
    }
}
