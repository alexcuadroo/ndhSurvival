package dev.ndh.survival.modules.progresion.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import dev.ndh.survival.NdhSurvivalPlugin;
import dev.ndh.survival.modules.progresion.ProgresionModule;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Comprueba restauración de mundos, recarga transaccional y errores de persistencia. */
class ProgressionLifecycleTest {
    @TempDir(factory = ProgressionStoreTest.WorkspaceTempDirectory.class) Path directory;

    private NdhSurvivalPlugin plugin() {
        var plugin = mock(NdhSurvivalPlugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        return plugin;
    }

    private World world(Difficulty difficulty) {
        var world = mock(World.class);
        when(world.getUID()).thenReturn(UUID.randomUUID());
        var current = new AtomicReference<>(difficulty);
        when(world.getDifficulty()).thenAnswer(invocation -> current.get());
        doAnswer(invocation -> { current.set(invocation.getArgument(0)); return null; }).when(world).setDifficulty(any());
        return world;
    }

    @Test void worldsAreSavedBeforeModificationAndRestoredOnlyIfStillHard() throws Exception {
        var plugin = plugin();
        var rules = ProgressionRules.load(ProgressionRulesTest.defaults().getConfigurationSection("progresion.dias"));
        var service = new ProgressionService(plugin, rules);
        var normal = world(Difficulty.NORMAL);
        var easy = world(Difficulty.EASY);
        var hard = world(Difficulty.HARD);
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getWorlds).thenReturn(List.of(normal, easy, hard));
            service.enforceWorld(normal);
            assertEquals(Difficulty.NORMAL, normal.getDifficulty());
            service.clock().restart();
            service.enforceWorld(normal);
            service.enforceWorld(easy);
            service.enforceWorld(hard);
            assertEquals(Difficulty.HARD, normal.getDifficulty());
            assertEquals(Difficulty.NORMAL, new ProgressionStore(directory.resolve("progresion-state.yml"))
                    .load().originals().get(normal.getUID()));
            service.restoreWorld(normal); // Descarga del mundo.
            assertEquals(Difficulty.NORMAL, normal.getDifficulty());
            service.enforceWorld(normal); // Carga posterior.
            easy.setDifficulty(Difficulty.PEACEFUL); // Cambio externo antes de apagar.
            service.stop();
            assertEquals(Difficulty.NORMAL, normal.getDifficulty());
            assertEquals(Difficulty.PEACEFUL, easy.getDifficulty());
            assertEquals(Difficulty.HARD, hard.getDifficulty());
        }
    }

    @Test void invalidReloadKeepsServiceAndCorruptStateCannotStart() throws Exception {
        var plugin = plugin();
        var yaml = ProgressionRulesTest.defaults();
        yaml.set("progresion.enabled", true);
        when(plugin.getConfig()).thenReturn(yaml);
        var module = new ProgresionModule(plugin);
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getWorlds).thenReturn(List.of());
            bukkit.when(Bukkit::getScheduler).thenReturn(mock(BukkitScheduler.class));
            module.onReload();
            var previous = module.service();
            yaml.set("progresion.dias.3.zombi-bebe", 2);
            assertThrows(IllegalStateException.class, module::onReload);
            assertSame(previous, module.service());
            assertEquals(0.15, module.service().rules().value(3, "zombi-bebe", 0));
            module.onDisable();
            yaml.set("progresion.dias.3.zombi-bebe", 0.15);
            Path state = directory.resolve("progresion-state.yml");
            Files.writeString(state, "estado corrupto");
            assertThrows(IllegalStateException.class, module::onReload);
            assertNull(module.service());
            assertEquals("estado corrupto", Files.readString(state));
        }
    }

    @Test void failedAtomicSaveBlocksRulesWithoutChangingWorld() throws Exception {
        var plugin = plugin();
        var service = new ProgressionService(plugin, ProgressionRules.load(
                ProgressionRulesTest.defaults().getConfigurationSection("progresion.dias")));
        service.clock().restart();
        // Un directorio no vacío en la ruta de destino fuerza un error real de reemplazo.
        Path destination = Files.createDirectory(directory.resolve("progresion-state.yml"));
        Files.writeString(destination.resolve("keep"), "original");
        var world = world(Difficulty.NORMAL);
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getWorlds).thenReturn(List.of(world));
            service.enforceWorld(world);
            assertFalse(service.operational());
            assertFalse(service.active());
            assertTrue(service.clock().paused());
            assertEquals(Difficulty.NORMAL, world.getDifficulty());
            assertEquals("original", Files.readString(destination.resolve("keep")));
        }
    }
}
