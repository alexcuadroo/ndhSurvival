package dev.ndh.survival.modules.jointg.telegram;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import org.jspecify.annotations.Nullable;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.plugin.Plugin;

public final class TelegramNotifier {
    private final ComponentLogger logger;
    private final String botToken;
    private final String chatId;
    private final String joinMessage;
    private final String quitMessage;
    private final String deathMessage;
    private final String startMessage;
    private final boolean enabled;
    private final @Nullable HttpClient client;

    private TelegramNotifier(ComponentLogger logger, String botToken, String chatId, String joinMessage,
                             String quitMessage, String deathMessage, String startMessage, boolean enabled) {
        this.logger = logger;
        this.botToken = botToken;
        this.chatId = chatId;
        this.joinMessage = joinMessage;
        this.quitMessage = quitMessage;
        this.deathMessage = deathMessage;
        this.startMessage = startMessage;
        this.enabled = enabled;
        this.client = enabled
                ? HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
                : null;
    }

    public static TelegramNotifier enabled(ComponentLogger logger, String botToken, String chatId,
                                           String joinMessage, String quitMessage, String deathMessage, String startMessage) {
        Objects.requireNonNull(logger, "logger");
        return new TelegramNotifier(logger, botToken, chatId, joinMessage, quitMessage, deathMessage, startMessage, true);
    }

    public static TelegramNotifier disabled(ComponentLogger logger) {
        Objects.requireNonNull(logger, "logger");
        return new TelegramNotifier(logger, "", "", "", "", "", "", false);
    }

    public boolean isEnabled() {
        return enabled;
    }

    private void sendMessage(Plugin plugin, String message) {
        if (!enabled || client == null || message == null || message.isEmpty()) {
            return;
        }

        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        String body = "chat_id=" + encode(chatId) + "&text=" + encode(message);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            try {
                HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() >= 400) {
                    logger.warn("Telegram notification failed with HTTP " + response.statusCode() + ".");
                }
            } catch (IOException e) {
                logger.warn("Failed to send Telegram notification", e);
            } catch (InterruptedException e) {
                logger.warn("Telegram notification was interrupted", e);
                Thread.currentThread().interrupt();
            }
        });
    }

    public void close() {
        if (client == null) {
            return;
        }
        try {
            client.close();
        } catch (Exception e) {
            logger.warn("Error closing HTTP client", e);
        }
    }

    public void notifyJoin(Plugin plugin, String playerName) {
        sendMessage(plugin, joinMessage.replace("%player%", playerName));
    }

    public void notifyQuit(Plugin plugin, String playerName) {
        sendMessage(plugin, quitMessage.replace("%player%", playerName));
    }

    public void notifyStart(Plugin plugin) {
        sendMessage(plugin, startMessage);
    }

    public void notifyDeath(Plugin plugin, String playerName, String reason, String world, int x, int y, int z) {
        String msg = deathMessage
                .replace("%player%", playerName)
                .replace("%reason%", reason)
                .replace("%world%", world)
                .replace("%x%", String.valueOf(x))
                .replace("%y%", String.valueOf(y))
                .replace("%z%", String.valueOf(z));
        sendMessage(plugin, msg);
    }

    private static String encode(String input) {
        return URLEncoder.encode(input, StandardCharsets.UTF_8);
    }
}
