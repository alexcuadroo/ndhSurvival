package dev.ndh.survival.modules.creeperask.session;

import java.util.UUID;

public record CreeperSession(UUID playerId, UUID creeperId, boolean creeperAiEnabled) {
}
