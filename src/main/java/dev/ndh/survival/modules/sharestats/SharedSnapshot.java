package dev.ndh.survival.modules.sharestats;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.List;

record SharedSnapshot(
        double health,
        int foodLevel,
        float saturation,
        float exhaustion,
        int totalExperience,
        int experienceLevel,
        float experienceProgress,
        List<PotionEffect> potionEffects
) {
    SharedSnapshot {
        potionEffects = List.copyOf(potionEffects);
    }

    static SharedSnapshot from(Player player) {
        return new SharedSnapshot(
                player.getHealth(),
                player.getFoodLevel(),
                player.getSaturation(),
                player.getExhaustion(),
                player.getTotalExperience(),
                player.getLevel(),
                player.getExp(),
                List.copyOf(player.getActivePotionEffects())
        );
    }

    SharedSnapshot withHealth(double newHealth) {
        return new SharedSnapshot(
                newHealth,
                foodLevel,
                saturation,
                exhaustion,
                totalExperience,
                experienceLevel,
                experienceProgress,
                potionEffects
        );
    }
}
