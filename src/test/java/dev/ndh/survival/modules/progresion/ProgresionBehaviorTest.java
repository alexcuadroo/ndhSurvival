package dev.ndh.survival.modules.progresion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import dev.ndh.survival.NdhSurvivalPlugin;
import dev.ndh.survival.modules.progresion.command.ProgresionSubCommand;
import dev.ndh.survival.modules.progresion.listener.ProgresionListener;
import dev.ndh.survival.modules.progresion.service.ProgressionRules;
import dev.ndh.survival.modules.progresion.service.ProgressionService;
import dev.ndh.survival.modules.progresion.service.SeasonClock;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.random.RandomGenerator;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Verifica cancelaciones, nutrición final y permisos administrativos. */
class ProgresionBehaviorTest {
    private ProgresionModule module;
    private NdhSurvivalPlugin plugin;
    private ProgressionService service;
    private ProgresionListener listener;
    private SeasonClock clock;

    @BeforeEach void setup() {
        module = mock(ProgresionModule.class);
        plugin = mock(NdhSurvivalPlugin.class);
        when(plugin.getName()).thenReturn("ndhSurvival");
        when(plugin.namespace()).thenReturn("ndhsurvival");
        service = mock(ProgressionService.class);
        when(module.service()).thenReturn(service);
        when(service.active()).thenReturn(true);
        when(service.operational()).thenReturn(true);
        clock = new SeasonClock(() -> 0, 0, true, false);
        clock.setDay(30);
        when(service.clock()).thenReturn(clock);
        var yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(
                getClass().getResourceAsStream("/config.yml"), StandardCharsets.UTF_8));
        when(service.rules()).thenReturn(ProgressionRules.load(yaml.getConfigurationSection("progresion.dias")));
        listener = new ProgresionListener(module, plugin, mock(RandomGenerator.class));
    }

    @Test void breadSaturationIsResolvedOnceBeforeSharedSnapshotAndRespectsCancellation() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isValid()).thenReturn(true);
        when(player.getFoodLevel()).thenReturn(10);
        when(player.getSaturation()).thenReturn(1f);
        ItemStack bread = mock(ItemStack.class);
        when(bread.getType()).thenReturn(Material.BREAD);
        when(bread.clone()).thenReturn(bread);
        var event = new FoodLevelChangeEvent(player, 15, bread);
        List<Runnable> corrections = new ArrayList<>();
        doAnswer(invocation -> { corrections.add(invocation.getArgument(1)); return null; })
                .when(service).deferFood(any(), any());
        listener.onFood(event);
        assertEquals(12, event.getFoodLevel());
        corrections.removeFirst().run();
        verify(player).setSaturation(3f);
        event.setCancelled(true);
        listener.onFood(event);
        assertTrue(corrections.isEmpty());
        event.setCancelled(false);
        listener.onFood(event);
        event.setCancelled(true); // Cancelación posterior a HIGHEST.
        corrections.removeFirst().run();
        verify(player, times(1)).setSaturation(anyFloat());
    }

    @Test void disabledProgressionAndDayOneLeaveBreadUntouched() {
        Player player = mock(Player.class);
        ItemStack bread = mock(ItemStack.class);
        when(bread.getType()).thenReturn(Material.BREAD);
        when(bread.clone()).thenReturn(bread);
        var event = new FoodLevelChangeEvent(player, 15, bread);
        clock.setDay(1);
        listener.onFood(event);
        assertEquals(15, event.getFoodLevel());
        when(service.active()).thenReturn(false);
        clock.setDay(30);
        listener.onFood(event);
        assertEquals(15, event.getFoodLevel());
        verify(service, never()).deferFood(any(), any());
    }

    @Test void exhaustionOnlyChangesWalkingAndSprintingAndHonorsCancellation() {
        Player player = mock(Player.class);
        var sprint = new EntityExhaustionEvent(player, EntityExhaustionEvent.ExhaustionReason.SPRINT, 1);
        listener.onExhaustion(sprint);
        assertEquals(1.2f, sprint.getExhaustion(), 0.0001);
        var attack = new EntityExhaustionEvent(player, EntityExhaustionEvent.ExhaustionReason.ATTACK, 1);
        listener.onExhaustion(attack);
        assertEquals(1, attack.getExhaustion());
        sprint.setExhaustion(1);
        sprint.setCancelled(true);
        listener.onExhaustion(sprint);
        assertEquals(1, sprint.getExhaustion());
    }

    @Test void publicCommandsCannotMutateAndCompletionHidesAdminOperations() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("ndh.progresion.ver")).thenReturn(true);
        var command = new ProgresionSubCommand(module, plugin);
        assertTrue(command.hasPermission(sender));
        assertEquals(List.of("estado", "calendario"), command.tabComplete(sender, "progresion", new String[] {""}));
        command.execute(sender, "progresion", new String[] {"dia", "30"});
        verify(service, never()).change(anyString(), anyInt());
        assertTrue(command.tabComplete(sender, "progresion", new String[] {"reiniciar", ""}).isEmpty());
    }

    @Test void restartRequiresConfirmationAndDayMustBeInRange() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("ndh.progresion.admin")).thenReturn(true);
        var command = new ProgresionSubCommand(module, plugin);
        command.execute(sender, "progresion", new String[] {"reiniciar"});
        command.execute(sender, "progresion", new String[] {"dia", "31"});
        command.execute(sender, "progresion", new String[] {"iniciar"});
        verify(service, never()).change(anyString(), anyInt());
        command.execute(sender, "progresion", new String[] {"reiniciar", "confirmar"});
        verify(service).change("reiniciar", 1);
    }

    @Test void nonNaturalSpawnsAndCancelledSpawnsNeverModifyCreatures() {
        var zombie = mock(org.bukkit.entity.Zombie.class);
        var cancelled = new org.bukkit.event.entity.CreatureSpawnEvent(zombie,
                org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.NATURAL);
        cancelled.setCancelled(true);
        listener.onSpawn(cancelled);
        for (var reason : org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.values()) {
            if (reason == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.NATURAL) continue;
            listener.onSpawn(new org.bukkit.event.entity.CreatureSpawnEvent(zombie, reason));
        }
        verify(zombie, never()).setBaby();
        verify(zombie, never()).setAdult();
        verify(zombie, never()).getPersistentDataContainer();
    }
}
