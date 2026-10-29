package dev.ndh.survival.modules.sharestats;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public final class ShareStatsExpansion extends PlaceholderExpansion {
    private final ShareStatsModule module;

    public ShareStatsExpansion(ShareStatsModule module) {
        this.module = module;
    }

    @Override
    public String getIdentifier() {
        return "sharestats";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", module.getPlugin().getPluginMeta().getAuthors());
    }

    @Override
    public String getVersion() {
        return module.getPlugin().getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        String value = module.placeholderValue(player, params);
        return value != null ? value : "";
    }
}
