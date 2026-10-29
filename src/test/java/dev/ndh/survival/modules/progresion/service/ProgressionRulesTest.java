package dev.ndh.survival.modules.progresion.service;

import static org.junit.jupiter.api.Assertions.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/** Verifica el calendario distribuido y el rechazo de configuraciones inválidas. */
class ProgressionRulesTest {
    @Test void babyProbabilityUsesOneRollAndExactBoundary() {
        var rules = ProgressionRules.load(defaults().getConfigurationSection("progresion.dias"));
        var random = org.mockito.Mockito.mock(java.util.random.RandomGenerator.class);
        org.mockito.Mockito.when(random.nextDouble()).thenReturn(0.149999, 0.15, 0.349999, 0.35);
        assertTrue(rules.rollBaby(3, random));
        assertFalse(rules.rollBaby(3, random));
        assertTrue(rules.rollBaby(30, random));
        assertFalse(rules.rollBaby(30, random));
        org.mockito.Mockito.verify(random, org.mockito.Mockito.times(4)).nextDouble();
    }
    static YamlConfiguration defaults() {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(
                ProgressionRulesTest.class.getResourceAsStream("/config.yml"), StandardCharsets.UTF_8));
    }

    @Test void defaultsMatchMilestonesAndReplaceRatherThanMultiply() {
        var rules = ProgressionRules.load(defaults().getConfigurationSection("progresion.dias"));
        assertEquals(5, rules.value(1, "pan-comida", 5));
        assertEquals(3, rules.value(2, "pan-comida", 5));
        assertEquals(2, rules.value(30, "pan-comida", 5));
        assertEquals(0.15, rules.value(3, "zombi-bebe", 0));
        assertEquals(0.30, rules.value(24, "zombi-salud", 0));
        assertEquals(0.35, rules.value(30, "zombi-bebe", 0));
        assertEquals(0.25, rules.value(30, "esqueleto-proyectil", 0));
        assertEquals(0.20, rules.value(30, "agotamiento", 0));
        assertEquals(0.25, rules.value(30, "arana-salud", 0));
    }

    @Test void rejectsMissingDaysUnknownRulesAndInvalidNumbers() {
        for (Object invalid : new Object[] {-0.1, 1.1, Double.NaN, "muchos"}) {
            var yaml = defaults();
            yaml.set("progresion.dias.3.zombi-bebe", invalid);
            assertThrows(IllegalArgumentException.class, () -> ProgressionRules.load(yaml.getConfigurationSection("progresion.dias")));
        }
        var extraDay = defaults();
        extraDay.createSection("progresion.dias.31");
        assertThrows(IllegalArgumentException.class, () -> ProgressionRules.load(extraDay.getConfigurationSection("progresion.dias")));
        var missingDay = defaults();
        missingDay.set("progresion.dias.7", null);
        var section = missingDay.getConfigurationSection("progresion.dias");
        assertThrows(IllegalArgumentException.class, () -> ProgressionRules.load(section));
    }

    @Test void rejectsFractionalNutritionAndUnknownKeyWithoutChangingPublishedRules() {
        var yaml = defaults();
        var previous = ProgressionRules.load(yaml.getConfigurationSection("progresion.dias"));
        yaml.set("progresion.dias.2.pan-comida", 2.5);
        assertThrows(IllegalArgumentException.class, () -> ProgressionRules.load(yaml.getConfigurationSection("progresion.dias")));
        assertEquals(3, previous.value(2, "pan-comida", 5));
        yaml.set("progresion.dias.2.pan-comida", 3);
        yaml.set("progresion.dias.2.desconocida", 0.1);
        assertThrows(IllegalArgumentException.class, () -> ProgressionRules.load(yaml.getConfigurationSection("progresion.dias")));
    }
}
