package dev.ndh.survival.modules.jointg.integration;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

/**
 * Provides a player's chat prefix for the join/chat messages.
 */
public interface PrefixProvider {

    String resolve(Player player);

    static @Nullable PrefixProvider createIfAvailable(Plugin plugin) {
        if (plugin.getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
            try {
                return new LuckPermsPrefixProvider(plugin);
            } catch (Throwable t) {
                plugin.getComponentLogger().warn(
                        "LuckPerms is installed but its API could not be loaded; prefixes will not be shown.", t);
            }
        }
        return null;
    }
}
