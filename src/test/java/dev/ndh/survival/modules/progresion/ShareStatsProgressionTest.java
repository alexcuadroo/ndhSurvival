package dev.ndh.survival.modules.progresion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import dev.ndh.survival.NdhSurvivalPlugin;
import dev.ndh.survival.core.module.ModuleManager;
import dev.ndh.survival.modules.progresion.listener.ProgresionListener;
import dev.ndh.survival.modules.progresion.service.ProgressionRules;
import dev.ndh.survival.modules.progresion.service.ProgressionService;
import dev.ndh.survival.modules.progresion.service.ProgressionStoreTest.WorkspaceTempDirectory;
import dev.ndh.survival.modules.sharestats.ShareStatsModule;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Comprueba la captura compartida real aunque se haya programado antes de comer. */
class ShareStatsProgressionTest {
    @TempDir(factory = WorkspaceTempDirectory.class) Path directory;

    private Player player(AtomicInteger food, AtomicReference<Float> saturation) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        when(player.isValid()).thenReturn(true);
        when(player.getFoodLevel()).thenAnswer(invocation -> food.get());
        when(player.getSaturation()).thenAnswer(invocation -> saturation.get());
        doAnswer(invocation -> { food.set(invocation.getArgument(0)); return null; }).when(player).setFoodLevel(anyInt());
        doAnswer(invocation -> { saturation.set(invocation.getArgument(0)); return null; }).when(player).setSaturation(anyFloat());
        return player;
    }

    @Test void pendingCaptureSeesReducedBreadAndCopiesItOnceToBothPlayers() throws Exception {
        var plugin = mock(NdhSurvivalPlugin.class);
        when(plugin.namespace()).thenReturn("ndhsurvival");
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        var yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(getClass().getResourceAsStream("/config.yml"), java.nio.charset.StandardCharsets.UTF_8));
        yaml.set("sharestats.enabled", true);
        yaml.set("sharestats.sync.health", false);
        yaml.set("sharestats.sync.experience", false);
        yaml.set("sharestats.sync.potion-effects", false);
        when(plugin.getConfig()).thenReturn(yaml);
        var progression = mock(ProgresionModule.class);
        var manager = mock(ModuleManager.class);
        when(plugin.getModuleManager()).thenReturn(manager);
        when(manager.getModule("progresion")).thenReturn(Optional.of(progression));
        var service = new ProgressionService(plugin, ProgressionRules.load(yaml.getConfigurationSection("progresion.dias")));
        when(progression.service()).thenReturn(service);
        service.clock().restart();
        service.clock().setDay(2);
        var food1 = new AtomicInteger(10);
        var saturation1 = new AtomicReference<>(1f);
        var food2 = new AtomicInteger(10);
        var saturation2 = new AtomicReference<>(1f);
        Player first = player(food1, saturation1);
        Player second = player(food2, saturation2);
        var tasks = new ArrayDeque<Runnable>();
        var scheduler = mock(BukkitScheduler.class);
        doAnswer(invocation -> { tasks.add(invocation.getArgument(1)); return null; }).when(scheduler).runTask(eq(plugin), any(Runnable.class));
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(first, second));
            bukkit.when(() -> Bukkit.getPlayer(first.getUniqueId())).thenReturn(first);
            var shared = new ShareStatsModule(plugin);
            shared.onReload();
            var damage = mock(EntityDamageEvent.class);
            when(damage.getEntity()).thenReturn(first);
            shared.onDamage(damage); // La captura ya está encolada antes del consumo.
            ItemStack bread = mock(ItemStack.class);
            when(bread.getType()).thenReturn(Material.BREAD);
            when(bread.clone()).thenReturn(bread);
            var consume = new FoodLevelChangeEvent(first, 15, bread);
            new ProgresionListener(progression, plugin).onFood(consume);
            food1.set(consume.getFoodLevel());
            saturation1.set(7f); // Estado vanilla antes de la corrección diferida.
            shared.onFoodLevelChange(consume);
            while (!tasks.isEmpty()) tasks.removeFirst().run();
            assertEquals(13, food1.get());
            assertEquals(13, food2.get());
            assertEquals(4f, saturation1.get());
            assertEquals(4f, saturation2.get());
            service.settleFood();
            assertEquals(4f, saturation2.get());
        }
    }
}
