package dev.ndh.survival.modules.progresion.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Difficulty;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/** Persistencia atómica del reloj y de la dificultad original de cada mundo. */
public final class ProgressionStore {
    private final Path path;

    /** Estado recuperado de disco. */
    public record State(long elapsedMillis, boolean started, boolean paused, Map<UUID, Difficulty> originals) { }

    /** Crea el almacén para el archivo indicado. */
    public ProgressionStore(Path path) { this.path = path; }

    /** Lee y valida estrictamente el archivo; un archivo ausente representa una instalación nueva. */
    public State load() throws IOException {
        if (!Files.exists(path)) return new State(0, false, false, Map.of());
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(Files.readString(path));
            if (yaml.getInt("version", -1) != 1 || !yaml.isLong("elapsed-millis") && !yaml.isInt("elapsed-millis")
                    || !yaml.isBoolean("started") || !yaml.isBoolean("paused")) {
                throw new IllegalArgumentException("Campos de estado ausentes o inválidos");
            }
            long elapsed = yaml.getLong("elapsed-millis");
            boolean started = yaml.getBoolean("started");
            boolean paused = yaml.getBoolean("paused");
            if (elapsed < 0 || elapsed > 29 * SeasonClock.DAY_MILLIS
                    || !started && (elapsed != 0 || paused)) throw new IllegalArgumentException("Estado incoherente");
            Map<UUID, Difficulty> originals = new LinkedHashMap<>();
            var section = yaml.getConfigurationSection("worlds");
            if (section == null) throw new IllegalArgumentException("Falta worlds");
            for (String key : section.getKeys(false)) {
                originals.put(UUID.fromString(key), Difficulty.valueOf(section.getString(key, "")));
            }
            return new State(elapsed, started, paused, Map.copyOf(originals));
        } catch (InvalidConfigurationException | IllegalArgumentException exception) {
            throw new IOException("Estado de progresión corrupto; se requiere reparación manual", exception);
        }
    }

    /** Guarda mediante reemplazo atómico; si no está disponible falla sin sobrescribir el estado anterior. */
    public void save(SeasonClock clock, Map<UUID, Difficulty> originals) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("version", 1);
        yaml.set("elapsed-millis", clock.elapsedMillis());
        yaml.set("started", clock.started());
        yaml.set("paused", clock.paused());
        yaml.createSection("worlds");
        originals.forEach((id, difficulty) -> yaml.set("worlds." + id, difficulty.name()));
        Files.createDirectories(path.getParent());
        Path temp = Files.createTempFile(path.getParent(), "progresion-", ".tmp");
        try {
            Files.writeString(temp, yaml.saveToString(), StandardCharsets.UTF_8);
            Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }
}
