package dev.ndh.survival.modules.progresion.service;

import java.util.function.LongSupplier;

/** Reloj monotónico de temporada; nunca cuenta tiempo con el servidor apagado. */
public final class SeasonClock {
    /** Duración real de cada día de temporada. */
    public static final long DAY_MILLIS = 86_400_000L;
    private final LongSupplier nanoTime;
    private long elapsedMillis;
    private long anchor;
    private boolean started;
    private boolean paused;

    /** Crea un reloj con una fuente monotónica sustituible para pruebas. */
    public SeasonClock(LongSupplier nanoTime, long elapsedMillis, boolean started, boolean paused) {
        if (elapsedMillis < 0 || (!started && (elapsedMillis != 0 || paused))) {
            throw new IllegalArgumentException("Estado de temporada incoherente");
        }
        this.nanoTime = nanoTime;
        this.elapsedMillis = Math.min(elapsedMillis, 29 * DAY_MILLIS);
        this.started = started;
        this.paused = paused;
        anchor = nanoTime.getAsLong();
    }

    /** Devuelve tiempo acumulado, limitado al comienzo del día 30. */
    public long elapsedMillis() {
        return Math.min(29 * DAY_MILLIS, elapsedMillis
                + (started && !paused ? Math.max(0, (nanoTime.getAsLong() - anchor) / 1_000_000) : 0));
    }

    /** Devuelve el día actual, o cero si aún no comenzó la temporada. */
    public int day() { return started ? (int) (1 + elapsedMillis() / DAY_MILLIS) : 0; }
    /** Indica si existe una temporada. */
    public boolean started() { return started; }
    /** Indica si el avance está pausado. */
    public boolean paused() { return paused; }
    /** Devuelve los milisegundos restantes hasta el siguiente día. */
    public long remainingMillis() { return day() >= 30 ? 0 : DAY_MILLIS - elapsedMillis() % DAY_MILLIS; }
    /** Cambia la pausa conservando el tiempo transcurrido. */
    public void pause(boolean value) {
        elapsedMillis = elapsedMillis();
        anchor = nanoTime.getAsLong();
        paused = value;
    }
    /** Inicia o reinicia una temporada en el día uno. */
    public void restart() { started = true; paused = false; setDay(1); }
    /** Sitúa una temporada existente al comienzo del día indicado. */
    public void setDay(int day) {
        if (!started || day < 1 || day > 30) throw new IllegalArgumentException("Día inválido");
        elapsedMillis = (day - 1L) * DAY_MILLIS;
        anchor = nanoTime.getAsLong();
    }
}
