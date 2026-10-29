package dev.ndh.survival.core.module;

import dev.ndh.survival.NdhSurvivalPlugin;
import dev.ndh.survival.core.command.impl.ModulosSubCommand;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Verifica cambios en caliente, recuperación de errores y permisos. */
class ModuleManagerTest {
    @Test void togglesAllRegisteredModulesWithoutRegisteringThemAgain() {
        var plugin = mock(NdhSurvivalPlugin.class);
        var config = new YamlConfiguration();
        when(plugin.getConfig()).thenReturn(config);
        var manager = new ModuleManager(plugin);
        for (String id : List.of("creeperask", "invsee", "saludo", "jointg", "barrendero", "sharestats", "rtp", "progresion")) {
            var module = mock(SurvivalModule.class);
            when(module.getId()).thenReturn(id);
            when(module.isEnabled()).thenAnswer(call -> config.getBoolean(id + ".enabled"));
            manager.registerModule(module);
            manager.setEnabled(id, true);
            assertTrue(module.isEnabled());
            manager.setEnabled(id, false);
            assertFalse(module.isEnabled());
            verify(module, times(2)).onReload();
            verify(module, never()).onEnable();
        }
        verify(plugin, times(16)).saveConfig();
        assertFalse(config.getBoolean("jointg.telegram-enabled"));
    }

    @Test void failedActivationRestoresConfigAndDoesNotSave() {
        var plugin = mock(NdhSurvivalPlugin.class);
        var config = new YamlConfiguration();
        config.set("jointg.enabled", false);
        config.set("jointg.telegram-enabled", false);
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        var module = mock(SurvivalModule.class);
        when(module.getId()).thenReturn("jointg");
        var manager = new ModuleManager(plugin);
        manager.registerModule(module);
        assertThrows(IllegalStateException.class, () -> manager.setEnabled("jointg", true));
        assertFalse(config.getBoolean("jointg.enabled"));
        assertFalse(config.getBoolean("jointg.telegram-enabled"));
        verify(plugin, never()).saveConfig();
        verify(module, times(2)).onReload();
    }

    @Test void commandRequiresAdminAndResolvesSpanishNames() {
        var plugin = mock(NdhSurvivalPlugin.class);
        var manager = mock(ModuleManager.class);
        when(plugin.getModuleManager()).thenReturn(manager);
        var command = new ModulosSubCommand(plugin);
        var sender = mock(CommandSender.class);
        command.execute(sender, "modulos", new String[] {"inventarios", "activar"});
        verifyNoInteractions(manager);
        assertTrue(command.tabComplete(sender, "modulos", new String[] {""}).isEmpty());
        when(sender.hasPermission("ndh.admin")).thenReturn(true);
        command.execute(sender, "modulos", new String[] {"inventarios", "activar"});
        verify(manager).setEnabled("invsee", true);
        command.execute(sender, "modulos", new String[] {"saludo", "desactivar"});
        verify(manager).setEnabled("saludo", false);
        assertEquals(List.of("activar", "desactivar", "estado"),
                command.tabComplete(sender, "modulos", new String[] {"inventarios", ""}));
        assertFalse(command.tabComplete(sender, "modulos", new String[] {""}).contains("invsee"));
    }
}
