package dev.ndh.survival.modules.saludo.listener;

import dev.ndh.survival.modules.saludo.integration.PrefixProvider;
import java.time.Duration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jspecify.annotations.Nullable;

/** Presenta los títulos y el mensaje de entrada con prefijos de LuckPerms. */
public final class SaludoListener implements Listener {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final @Nullable PrefixProvider prefixProvider;
    private final String titleTemplate;
    private final String subtitleTemplate;
    private final Title.Times times;
    private final @Nullable String chatJoinMessage;

    /**
     * Crea la bienvenida con tiempos expresados en ticks.
     * @param prefixProvider proveedor opcional de prefijos
     * @param titleTemplate título en MiniMessage
     * @param subtitleTemplate subtítulo en MiniMessage
     * @param fadeInTicks duración de entrada
     * @param stayTicks duración visible
     * @param fadeOutTicks duración de salida
     * @param chatJoinMessage mensaje de entrada, vacío para ocultarlo
     */
    public SaludoListener(@Nullable PrefixProvider prefixProvider,
                          String titleTemplate, String subtitleTemplate, int fadeInTicks, int stayTicks, int fadeOutTicks,
                          @Nullable String chatJoinMessage) {
        this.prefixProvider = prefixProvider;
        this.titleTemplate = titleTemplate;
        this.subtitleTemplate = subtitleTemplate;
        this.times = Title.Times.times(
                Duration.ofMillis(fadeInTicks * 50L),
                Duration.ofMillis(stayTicks * 50L),
                Duration.ofMillis(fadeOutTicks * 50L));
        this.chatJoinMessage = chatJoinMessage;
    }

    /** @param event entrada del jugador */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.joinMessage(null);

        Player player = event.getPlayer();
        String playerName = player.getName();

        String prefix = prefixProvider != null ? prefixProvider.resolve(player) : "";

        Component title = MINI_MESSAGE.deserialize(titleTemplate, Placeholder.unparsed("player", playerName));
        Component subtitle = MINI_MESSAGE.deserialize(subtitleTemplate, Placeholder.unparsed("player", playerName));

        if (!titleTemplate.isBlank() || !subtitleTemplate.isBlank()) {
            player.showTitle(Title.title(title, subtitle, times));
        }

        if (chatJoinMessage != null && !chatJoinMessage.isBlank()) {
            event.joinMessage(MINI_MESSAGE.deserialize(chatJoinMessage,
                    Placeholder.unparsed("player", playerName),
                    Placeholder.component("prefix", MINI_MESSAGE.deserialize(prefix))));
        }

    }

}
