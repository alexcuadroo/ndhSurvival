package dev.ndh.survival.modules.progresion.service;

import dev.ndh.survival.NdhSurvivalPlugin;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.Nullable;

/** Coordina reglas, reloj y mundos exclusivamente desde el hilo del servidor. */
public final class ProgressionService {
    private final NdhSurvivalPlugin plugin;
    private final ProgressionStore store;
    private final SeasonClock clock;
    private final Map<UUID, Difficulty> originals;
    private ProgressionRules rules;
    private @Nullable BukkitTask task;
    private long lastSave;
    private int announcedDay;
    private boolean operational = true;
    private final Map<UUID, Runnable> foodCorrections = new HashMap<>();

    /** Recupera la temporada; propaga errores de disco para impedir un inicio silencioso. */
    public ProgressionService(NdhSurvivalPlugin plugin, ProgressionRules rules) throws IOException {
        this.plugin = plugin;
        this.rules = rules;
        store = new ProgressionStore(plugin.getDataFolder().toPath().resolve("progresion-state.yml"));
        var state = store.load();
        clock = new SeasonClock(System::nanoTime, state.elapsedMillis(), state.started(), state.paused());
        originals = new HashMap<>(state.originals());
        announcedDay = clock.day();
    }

    /** Comienza las comprobaciones; el intervalo en ticks no mide el tiempo de temporada. */
    public void start() {
        if (clock.started()) Bukkit.getWorlds().forEach(this::enforceWorld);
        lastSave = System.nanoTime();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20, 20);
    }

    private void tick() {
        if (!operational) return;
        if (active()) Bukkit.getWorlds().forEach(this::enforceWorld);
        announceIfChanged();
        if (System.nanoTime() - lastSave >= 60_000_000_000L) save();
    }

    /** Indica si las reglas están operativas y existe una temporada. */
    public boolean active() { return operational && clock.started(); }
    /** Indica si el servicio puede aceptar operaciones administrativas. */
    public boolean operational() { return operational; }
    /** Devuelve el reloj para consultas. */
    public SeasonClock clock() { return clock; }
    /** Devuelve el calendario vigente. */
    public ProgressionRules rules() { return rules; }
    /** Publica un calendario ya validado sin cambiar el reloj. */
    public void rules(ProgressionRules rules) { this.rules = rules; }

    /** Encola una corrección para después del consumo y antes de cualquier captura compartida. */
    public void deferFood(UUID player, Runnable correction) {
        foodCorrections.put(player, correction);
        Bukkit.getScheduler().runTask(plugin, this::settleFood);
    }

    /** Resuelve una sola vez las saturaciones pendientes, incluso si ShareStats ya tenía tarea encolada. */
    public void settleFood() {
        var pending = java.util.List.copyOf(foodCorrections.values());
        foodCorrections.clear();
        pending.forEach(Runnable::run);
    }

    /** Ejecuta una operación administrativa validada por el comando. */
    public void change(String action, int day) {
        switch (action) {
            case "iniciar", "reiniciar" -> clock.restart();
            case "pausar" -> clock.pause(true);
            case "continuar" -> clock.pause(false);
            case "dia" -> clock.setDay(day);
            default -> throw new IllegalArgumentException("Operación desconocida");
        }
        if (!save()) return;
        Bukkit.getWorlds().forEach(this::enforceWorld);
        announceIfChanged();
    }

    /** Conserva la dificultad previa antes de imponer Difícil en un mundo. */
    public void enforceWorld(World world) {
        if (!active()) return;
        if (!originals.containsKey(world.getUID())) {
            originals.put(world.getUID(), world.getDifficulty());
            if (!save()) return;
        }
        if (world.getDifficulty() != Difficulty.HARD) world.setDifficulty(Difficulty.HARD);
    }

    /** Persiste el estado; ante error detiene reglas y reloj sin sustituir el archivo previo. */
    public boolean save() {
        try {
            store.save(clock, originals);
            lastSave = System.nanoTime();
            return true;
        } catch (IOException exception) {
            plugin.getLogger().severe("Progresión detenida: " + exception.getMessage());
            operational = false;
            clock.pause(true);
            restoreWorlds();
            return false;
        }
    }

    private void announceIfChanged() {
        if (!active()) return;
        int day = clock.day();
        if (day == announcedDay) return;
        announcedDay = day;
        Component message = MiniMessage.miniMessage().deserialize("<gold>[Progresión] Día " + day
                + (day == 30 ? " — dificultad máxima" : "") + "</gold>");
        Bukkit.broadcast(message);
        Bukkit.getOnlinePlayers().forEach(player -> player.showTitle(Title.title(message, Component.text(rules.changes(day)))));
        plugin.getLogger().info("Progresión: día " + day + ": " + rules.changes(day));
    }

    private void restoreWorlds() {
        Bukkit.getWorlds().forEach(this::restoreWorld);
    }

    /** Restaura también mundos antes de descargarlos para no dejar dificultad impuesta en disco. */
    public void restoreWorld(World world) {
        Difficulty previous = originals.get(world.getUID());
        if (previous != null && world.getDifficulty() == Difficulty.HARD) world.setDifficulty(previous);
    }

    /** Detiene tareas, guarda el tiempo y restaura las dificultades que aún controla el plugin. */
    public void stop() {
        settleFood();
        BukkitTask task = this.task;
        if (task != null) task.cancel();
        if (operational) save();
        restoreWorlds();
        operational = false;
    }
}
