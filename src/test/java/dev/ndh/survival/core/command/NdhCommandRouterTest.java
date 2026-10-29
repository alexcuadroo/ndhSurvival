package dev.ndh.survival.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NdhCommandRouterTest {

    private NdhCommandRouter router;
    private SimpleTestSender adminSender;
    private SimpleTestSender regularSender;

    static class SimpleTestSender implements CommandSender {
        private final boolean op;
        private final Set<String> permissions = new HashSet<>();

        SimpleTestSender(boolean op) {
            this.op = op;
        }

        void addPermission(String perm) {
            permissions.add(perm);
        }

        @Override
        public boolean isOp() {
            return op;
        }

        @Override
        public void setOp(boolean value) {
        }

        @Override
        public boolean isPermissionSet(@NotNull String name) {
            return permissions.contains(name) || op;
        }

        @Override
        public boolean isPermissionSet(@NotNull Permission perm) {
            return isPermissionSet(perm.getName());
        }

        @Override
        public boolean hasPermission(@NotNull String name) {
            return op || permissions.contains(name);
        }

        @Override
        public boolean hasPermission(@NotNull Permission perm) {
            return hasPermission(perm.getName());
        }

        @Override
        public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name,
                boolean value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value,
                int ticks) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, int ticks) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeAttachment(@NotNull PermissionAttachment attachment) {
        }

        @Override
        public void recalculatePermissions() {
        }

        @Override
        public @NotNull Set<PermissionAttachmentInfo> getEffectivePermissions() {
            return Set.of();
        }

        @Override
        public void sendMessage(@NotNull String message) {
        }

        @Override
        public void sendMessage(@NotNull String... messages) {
        }

        @Override
        public void sendMessage(@Nullable UUID sender, @NotNull String message) {
        }

        @Override
        public void sendMessage(@Nullable UUID sender, @NotNull String... messages) {
        }

        @Override
        public @NotNull Server getServer() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull String getName() {
            return "TestSender";
        }

        @Override
        public @NotNull Spigot spigot() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull net.kyori.adventure.text.Component name() {
            return net.kyori.adventure.text.Component.text(getName());
        }
    }

    @BeforeEach
    void setUp() {
        router = new NdhCommandRouter(null);
        adminSender = new SimpleTestSender(true);
        regularSender = new SimpleTestSender(false);
    }

    @Test
    @DisplayName("Tab completion de nivel 1 sugiere únicamente nombres canónicos en español y nunca alias redundantes")
    void testTabCompleteOnlySuggestsCanonicalNames() {
        router.registerSubCommand(new NdhSubCommand() {
            @Override
            public @NotNull String getName() {
                return "barrendero";
            }

            @Override
            public @NotNull List<String> getAliases() {
                return List.of("clean", "clearlag", "limpieza");
            }

            @Override
            public @NotNull String getDescription() {
                return "Limpieza de items";
            }

            @Override
            public @NotNull String getUsage() {
                return "/ndh barrendero";
            }

            @Override
            public void execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
            }
        });

        router.registerSubCommand(new NdhSubCommand() {
            @Override
            public @NotNull String getName() {
                return "creeper";
            }

            @Override
            public @NotNull List<String> getAliases() {
                return List.of("creeperask");
            }

            @Override
            public @NotNull String getDescription() {
                return "Control de creepers";
            }

            @Override
            public @NotNull String getUsage() {
                return "/ndh creeper";
            }

            @Override
            public void execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
            }
        });

        List<String> completions = router.onTabComplete(adminSender, null, "ndh", new String[] { "" });
        assertNotNull(completions);

        // Debe contener los nombres canónicos
        assertTrue(completions.contains("barrendero"));
        assertTrue(completions.contains("creeper"));
        assertTrue(completions.contains("ayuda"));
        assertTrue(completions.contains("estado"));
        assertTrue(completions.contains("recargar"));

        // NUNCA debe contener alias en el autocompletado de nivel 1
        assertFalse(completions.contains("clean"));
        assertFalse(completions.contains("clearlag"));
        assertFalse(completions.contains("limpieza"));
        assertFalse(completions.contains("creeperask"));
        assertFalse(completions.contains("help"));
        assertFalse(completions.contains("status"));
        assertFalse(completions.contains("reload"));
    }

    @Test
    @DisplayName("Ejecución con alias sigue resolviendo el subcomando correcto")
    void testAliasExecutionResolvesCorrectSubCommand() {
        NdhSubCommand sub = new NdhSubCommand() {
            @Override
            public @NotNull String getName() {
                return "inventario";
            }

            @Override
            public @NotNull List<String> getAliases() {
                return List.of("openinv", "invsee", "inv");
            }

            @Override
            public @NotNull String getDescription() {
                return "Inspección de inventario";
            }

            @Override
            public @NotNull String getUsage() {
                return "/ndh inventario <jugador>";
            }

            @Override
            public void execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
            }
        };

        router.registerSubCommand(sub);

        assertTrue(router.getSubCommand("inventario").isPresent());
        assertTrue(router.getSubCommand("invsee").isPresent());
        assertTrue(router.getSubCommand("openinv").isPresent());
        assertTrue(router.getSubCommand("inv").isPresent());
        assertEquals("inventario", router.getSubCommand("invsee").get().getName());
    }

    @Test
    @DisplayName("Completado respeta permisos del sender")
    void testTabCompleteRespectsPermissions() {
        NdhSubCommand secretSub = new NdhSubCommand() {
            @Override
            public @NotNull String getName() {
                return "secreto";
            }

            @Override
            public @NotNull String getDescription() {
                return "Comando secreto";
            }

            @Override
            public @NotNull String getUsage() {
                return "/ndh secreto";
            }

            @Override
            public String getPermission() {
                return "ndh.admin.secret";
            }

            @Override
            public void execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
            }
        };

        router.registerSubCommand(secretSub);

        List<String> adminSuggestions = router.onTabComplete(adminSender, null, "ndh", new String[] { "sec" });
        assertNotNull(adminSuggestions);
        assertTrue(adminSuggestions.contains("secreto"));

        List<String> userSuggestions = router.onTabComplete(regularSender, null, "ndh", new String[] { "sec" });
        assertNotNull(userSuggestions);
        assertFalse(userSuggestions.contains("secreto"));
    }
}
