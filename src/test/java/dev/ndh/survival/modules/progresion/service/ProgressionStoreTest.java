package dev.ndh.survival.modules.progresion.service;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.io.IOException;
import org.bukkit.Difficulty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.io.TempDirFactory;
import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Verifica persistencia real en disco y bloqueo de estados corruptos. */
public class ProgressionStoreTest {
    @TempDir(factory = WorkspaceTempDirectory.class) Path directory;

    /** Usa target para que las operaciones atómicas permanezcan dentro del workspace. */
    public static class WorkspaceTempDirectory implements TempDirFactory {
        @Override public Path createTempDirectory(AnnotatedElementContext element, ExtensionContext context) throws IOException {
            return Files.createTempDirectory(Path.of("target").toAbsolutePath(), "progresion-test-");
        }
    }

    @Test void missingStateDoesNotStartSeasonAndRoundTripPreservesPauseAndWorlds() throws Exception {
        var store = new ProgressionStore(directory.resolve("state.yml"));
        assertFalse(store.load().started());
        var clock = new SeasonClock(() -> 0, 2 * SeasonClock.DAY_MILLIS + 123, true, true);
        var worlds = Map.of(UUID.randomUUID(), Difficulty.NORMAL, UUID.randomUUID(), Difficulty.EASY);
        store.save(clock, worlds);
        var restored = store.load();
        assertEquals(clock.elapsedMillis(), restored.elapsedMillis());
        assertTrue(restored.started());
        assertTrue(restored.paused());
        assertEquals(worlds, restored.originals());
        clock.restart();
        store.save(clock, worlds);
        assertEquals(0, store.load().elapsedMillis());
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
    }

    @Test void rejectsCorruptAndIncompleteFilesWithoutOverwritingThem() throws Exception {
        Path file = directory.resolve("state.yml");
        for (String corrupt : new String[] {"", "version: 1\nstarted: true", "[invalid yaml",
                "version: 1\nelapsed-millis: -1\nstarted: true\npaused: false\nworlds: {}"}) {
            Files.writeString(file, corrupt);
            assertThrows(IOException.class, () -> new ProgressionStore(file).load());
            assertEquals(corrupt, Files.readString(file));
        }
    }
}
