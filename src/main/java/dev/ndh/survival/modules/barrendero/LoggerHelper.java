package dev.ndh.survival.modules.barrendero;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

public class LoggerHelper {
    private final Logger logger;

    public LoggerHelper(JavaPlugin plugin) {
        this.logger = plugin.getLogger();
    }

    public void info(String msg) {
        logger.info("[Barrendero] " + msg);
    }

    public void warn(String msg) {
        logger.warning("[Barrendero] " + msg);
    }

    public void error(String msg, Throwable t) {
        logger.log(Level.SEVERE, "[Barrendero] " + msg, t);
    }
}
