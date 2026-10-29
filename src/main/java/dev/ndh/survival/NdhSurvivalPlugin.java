package dev.ndh.survival;

import dev.ndh.survival.core.command.NdhCommandRouter;
import dev.ndh.survival.core.module.ModuleManager;
import dev.ndh.survival.modules.barrendero.BarrenderoModule;
import dev.ndh.survival.modules.creeperask.CreeperAskModule;
import dev.ndh.survival.modules.invsee.InvSeeModule;
import dev.ndh.survival.modules.jointg.JoinTgModule;
import dev.ndh.survival.modules.rtp.RtpModule;
import dev.ndh.survival.modules.sharestats.ShareStatsModule;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * Plugin principal unificado ndhSurvival.
 * Agrupa módulos de experiencia survival bajo un comando central {@code /ndh} con autocompletado
 * y una arquitectura extensible mediante {@link dev.ndh.survival.core.module.SurvivalModule}.
 */
public final class NdhSurvivalPlugin extends JavaPlugin {

    private final ModuleManager moduleManager = new ModuleManager(this);
    private final NdhCommandRouter commandRouter = new NdhCommandRouter(this);

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Registrar el comando unificado /ndh
        PluginCommand ndhCmd = Objects.requireNonNull(getCommand("ndh"), "El comando 'ndh' no está definido en plugin.yml");
        ndhCmd.setExecutor(commandRouter);
        ndhCmd.setTabCompleter(commandRouter);

        // Registrar todos los módulos survival (para añadir futuras funciones basta con registrar un nuevo SurvivalModule aquí)
        registerModules();

        // Iniciar todos los módulos registrados
        this.moduleManager.enableAll();

        getLogger().info("ndhSurvival habilitado con " + moduleManager.getModules().size() + " módulos activos bajo /ndh.");
    }

    private void registerModules() {
        moduleManager.registerModule(new dev.ndh.survival.modules.progresion.ProgresionModule(this));
        moduleManager.registerModule(new CreeperAskModule(this));
        moduleManager.registerModule(new InvSeeModule(this));
        moduleManager.registerModule(new JoinTgModule(this));
        moduleManager.registerModule(new BarrenderoModule(this));
        moduleManager.registerModule(new ShareStatsModule(this));
        moduleManager.registerModule(new RtpModule(this));
    }

    @Override
    public void onDisable() {
        this.moduleManager.disableAll();
        getLogger().info("ndhSurvival deshabilitado correctamente.");
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public NdhCommandRouter getCommandRouter() {
        return commandRouter;
    }
}
