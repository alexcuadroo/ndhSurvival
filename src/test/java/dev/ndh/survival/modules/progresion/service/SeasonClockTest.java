package dev.ndh.survival.modules.progresion.service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** Verifica tiempo real, persistencia temporal y fronteras de la temporada. */
class SeasonClockTest {
    private final AtomicLong nanos = new AtomicLong();
    private void advance(long millis) { nanos.addAndGet(millis * 1_000_000); }

    @Test void startsOnlyOnExplicitCommandAndCrossesExactBoundary() {
        var clock = new SeasonClock(nanos::get, 0, false, false);
        advance(SeasonClock.DAY_MILLIS * 2);
        assertEquals(0, clock.day());
        assertEquals(0, clock.elapsedMillis());
        clock.restart();
        assertEquals(1, clock.day());
        advance(SeasonClock.DAY_MILLIS - 1);
        assertEquals(1, clock.day());
        assertEquals(1, clock.remainingMillis());
        advance(1);
        assertEquals(2, clock.day());
        assertEquals(SeasonClock.DAY_MILLIS, clock.remainingMillis());
    }

    @Test void pausePreservesRulesAndResumeCountsOnlyRunningTime() {
        var clock = new SeasonClock(nanos::get, 0, true, false);
        advance(12 * 3_600_000L);
        clock.pause(true);
        advance(5 * SeasonClock.DAY_MILLIS);
        assertEquals(1, clock.day());
        assertEquals(12 * 3_600_000L, clock.elapsedMillis());
        clock.pause(true);
        clock.pause(false);
        advance(12 * 3_600_000L);
        assertEquals(2, clock.day());
    }

    @Test void restartDoesNotCountOfflineTimeAndNeedsNoPlayersOrTicks() {
        var clock = new SeasonClock(nanos::get, 0, true, false);
        advance(5_000);
        long saved = clock.elapsedMillis();
        advance(10 * SeasonClock.DAY_MILLIS);
        var restored = new SeasonClock(nanos::get, saved, true, false);
        assertEquals(5_000, restored.elapsedMillis());
        advance(SeasonClock.DAY_MILLIS);
        assertEquals(2, restored.day());
    }

    @Test void capsAtThirtyAndAllowsAdministrativeRewind() {
        var clock = new SeasonClock(nanos::get, 0, true, false);
        advance(50 * SeasonClock.DAY_MILLIS);
        assertEquals(30, clock.day());
        assertEquals(0, clock.remainingMillis());
        clock.setDay(7);
        assertEquals(7, clock.day());
        clock.restart();
        assertFalse(clock.paused());
        assertEquals(1, clock.day());
        assertThrows(IllegalArgumentException.class, () -> clock.setDay(31));
    }

    @Test void rejectsIncoherentState() {
        assertThrows(IllegalArgumentException.class, () -> new SeasonClock(nanos::get, -1, true, false));
        assertThrows(IllegalArgumentException.class, () -> new SeasonClock(nanos::get, 0, false, true));
    }
}
