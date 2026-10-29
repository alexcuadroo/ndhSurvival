package dev.ndh.survival.modules.progresion.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;

/** Calendario inmutable de cambios acumulativos, validado antes de publicarlo. */
public final class ProgressionRules {
    private static final Set<String> KEYS = Set.of("pan-comida", "pan-saturacion", "zombi-bebe",
            "zombi-salud", "zombi-dano", "zombi-velocidad", "esqueleto-salud", "esqueleto-proyectil",
            "arana-salud", "arana-dano", "arana-velocidad", "agotamiento");
    private final Map<Integer, Map<String, Double>> changes;

    private ProgressionRules(Map<Integer, Map<String, Double>> changes) { this.changes = Map.copyOf(changes); }

    /** Lee los treinta días; rechaza claves, días y valores fuera de rango. */
    public static ProgressionRules load(ConfigurationSection root) {
        Map<Integer, Map<String, Double>> changes = new LinkedHashMap<>();
        for (String key : root.getKeys(false)) {
            int day;
            try { day = Integer.parseInt(key); }
            catch (NumberFormatException exception) { throw new IllegalArgumentException("Día inválido: " + key); }
            if (day < 1 || day > 30 || !key.equals(Integer.toString(day))) {
                throw new IllegalArgumentException("Día inválido: " + key);
            }
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) throw new IllegalArgumentException("Se esperaba una sección para día " + day);
            Map<String, Double> values = new LinkedHashMap<>();
            for (String rule : section.getKeys(false)) {
                Object raw = section.get(rule);
                if (!KEYS.contains(rule) || !(raw instanceof Number number)) {
                    throw new IllegalArgumentException("Regla inválida: " + rule);
                }
                double value = number.doubleValue();
                double max = rule.equals("pan-comida") || rule.equals("pan-saturacion") ? 20 : 1;
                if (!Double.isFinite(value) || value < 0 || value > max
                        || (rule.equals("pan-comida") && value != Math.rint(value))) {
                    throw new IllegalArgumentException("Valor inválido: " + rule);
                }
                values.put(rule, value);
            }
            changes.put(day, Map.copyOf(values));
        }
        for (int day = 1; day <= 30; day++) {
            if (!changes.containsKey(day)) throw new IllegalArgumentException("Falta el día " + day);
        }
        return new ProgressionRules(changes);
    }

    /** Obtiene un valor acumulado hasta el día solicitado. */
    public double value(int day, String key, double fallback) {
        double value = fallback;
        for (int i = 1; i <= day; i++) value = changes.getOrDefault(i, Map.of()).getOrDefault(key, value);
        return value;
    }

    /** Realiza una sola tirada usando la probabilidad total del día. */
    public boolean rollBaby(int day, java.util.random.RandomGenerator random) {
        return random.nextDouble() < value(day, "zombi-bebe", 0.05);
    }

    /** Describe los cambios del día, con orden estable. */
    public String changes(int day) {
        String details = describe(changes.get(day));
        return day == 1 ? "Dificultad Difícil" + (details.isEmpty() ? "" : "; " + details) : details;
    }

    /** Describe todas las reglas activas de un día. */
    public String accumulated(int day) {
        Map<String, Double> result = new java.util.TreeMap<>();
        for (int i = 1; i <= day; i++) result.putAll(changes.get(i));
        return "Difícil" + (result.isEmpty() ? "" : "; " + describe(result));
    }

    private String describe(Map<String, Double> values) {
        return new java.util.TreeMap<>(values).entrySet().stream().map(entry -> {
            String label = switch (entry.getKey()) {
                case "pan-comida" -> "Comida del pan";
                case "pan-saturacion" -> "Saturación del pan";
                case "zombi-bebe" -> "Probabilidad de zombis bebés";
                case "zombi-salud" -> "Salud de zombis";
                case "zombi-dano" -> "Daño de zombis";
                case "zombi-velocidad" -> "Velocidad de zombis";
                case "esqueleto-salud" -> "Salud de esqueletos";
                case "esqueleto-proyectil" -> "Daño de proyectiles de esqueletos";
                case "arana-salud" -> "Salud de arañas";
                case "arana-dano" -> "Daño de arañas";
                case "arana-velocidad" -> "Velocidad de arañas";
                case "agotamiento" -> "Agotamiento al caminar/correr";
                default -> throw new IllegalArgumentException("Regla desconocida");
            };
            boolean points = entry.getKey().startsWith("pan-");
            String amount = java.math.BigDecimal.valueOf(entry.getValue() * (points ? 1 : 100))
                    .stripTrailingZeros().toPlainString();
            return label + ": " + (points || entry.getKey().equals("zombi-bebe") ? "" : "+")
                    + amount + (points ? " puntos" : "%");
        }).collect(java.util.stream.Collectors.joining("; "));
    }
}
