package dev.ndh.survival.modules.saludo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.ndh.survival.NdhSurvivalPlugin;
import dev.ndh.survival.core.command.NdhCommandRouter;
import dev.ndh.survival.modules.jointg.listener.TelegramListener;
import dev.ndh.survival.modules.jointg.telegram.TelegramNotifier;
import dev.ndh.survival.modules.saludo.listener.SaludoListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.Test;

/** Regresiones de la separación entre Telegram y la bienvenida en Minecraft. */
class SaludoSeparationTest {
    @Test
    void telegramLeavesJoinMessageAndTitleUntouched() {
        NdhSurvivalPlugin plugin = mock(NdhSurvivalPlugin.class);
        TelegramNotifier notifier = mock(TelegramNotifier.class);
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Alex");
        Component original = Component.text("entrada original");
        PlayerJoinEvent event = new PlayerJoinEvent(player, original);

        new TelegramListener(plugin, notifier).onPlayerJoin(event);

        assertSame(original, event.joinMessage());
        verify(notifier).notifyJoin(plugin, "Alex");
        verify(player, never()).showTitle(any(net.kyori.adventure.title.Title.class));
    }

    @Test
    void saludoRendersPrefixWithoutTelegram() {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Alex");
        PlayerJoinEvent event = new PlayerJoinEvent(player, Component.empty());
        new SaludoListener(ignored -> "<gold>[Admin]</gold>", "", "", 0, 20, 0,
                "<prefix> <player> entró").onPlayerJoin(event);

        assertEquals("[Admin] Alex entró", PlainTextComponentSerializer.plainText().serialize(event.joinMessage()));
        verify(player, never()).showTitle(any(net.kyori.adventure.title.Title.class));
    }

    @Test
    void migratesLegacyWelcomeEvenWithNewDefaults() {
        NdhSurvivalPlugin plugin = mock(NdhSurvivalPlugin.class);
        YamlConfiguration config = new YamlConfiguration();
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("saludo.enabled", false);
        defaults.set("saludo.join-title", "default");
        config.setDefaults(defaults);
        config.set("jointg.enabled", false);
        config.set("jointg.join-title", "antiguo <player>");
        config.set("jointg.chat-join-message", "<prefix> <player>");
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getCommandRouter()).thenReturn(mock(NdhCommandRouter.class));

        new SaludoModule(plugin).onEnable();

        assertEquals("antiguo <player>", config.getString("saludo.join-title"));
        assertEquals("<prefix> <player>", config.getString("saludo.chat-join-message"));
        assertFalse(config.getBoolean("saludo.enabled"));
        verify(plugin).saveConfig();
    }

    @Test
    void explicitSaludoConfigurationWinsOverLegacy() {
        NdhSurvivalPlugin plugin = mock(NdhSurvivalPlugin.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("saludo.enabled", false);
        config.set("saludo.join-title", "nuevo");
        config.set("jointg.join-title", "antiguo");
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getCommandRouter()).thenReturn(mock(NdhCommandRouter.class));

        new SaludoModule(plugin).onEnable();

        assertEquals("nuevo", config.getString("saludo.join-title"));
        verify(plugin, never()).saveConfig();
    }
}
