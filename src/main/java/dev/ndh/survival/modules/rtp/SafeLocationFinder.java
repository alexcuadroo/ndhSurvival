package dev.ndh.survival.modules.rtp;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * Buscador asíncrono de ubicaciones seguras para Random Teleport (RTP) utilizando
 * {@link World#getChunkAtAsync(int, int)} de Paper API para no congelar el Main Thread.
 */
public final class SafeLocationFinder {

    private final JavaPlugin plugin;
    private RtpConfig config;

    public SafeLocationFinder(JavaPlugin plugin, RtpConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void setConfig(RtpConfig config) {
        this.config = config;
    }

    /**
     * Inicia una búsqueda asíncrona de una ubicación segura en el mundo indicado.
     *
     * @param world    Mundo donde buscar
     * @param callback Callback invocado en el hilo principal con la {@link Location} segura o {@code null} si falló
     */
    public void findSafeLocation(World world, Consumer<@Nullable Location> callback) {
        attemptFind(world, 1, config.getMaxAttempts(), callback);
    }

    private void attemptFind(World world, int currentAttempt, int maxAttempts, Consumer<@Nullable Location> callback) {
        if (currentAttempt > maxAttempts) {
            callback.accept(null);
            return;
        }

        int[] coords = generateRandomCoordinates(world);
        int blockX = coords[0];
        int blockZ = coords[1];
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;

        // Cargar el chunk de forma 100% asíncrona usando Paper API
        world.getChunkAtAsync(chunkX, chunkZ).whenComplete((chunk, throwable) -> {
            if (throwable != null || chunk == null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        attemptFind(world, currentAttempt + 1, maxAttempts, callback));
                return;
            }

            // Asegurar que la inspección de bloques se ejecute en el hilo principal (con el chunk ya cargado en memoria)
            Bukkit.getScheduler().runTask(plugin, () -> {
                Location safe = scanColumnForSafeSpot(world, chunk, blockX, blockZ);
                if (safe != null) {
                    callback.accept(safe);
                } else {
                    attemptFind(world, currentAttempt + 1, maxAttempts, callback);
                }
            });
        });
    }

    /**
     * Genera coordenadas X y Z aleatorias entre {@code minRadius} y {@code maxRadius}
     * respetando además los límites del {@link WorldBorder} del mundo.
     */
    private int[] generateRandomCoordinates(World world) {
        double centerX = config.isUseSpawnCenter() ? world.getSpawnLocation().getX() : config.getCenterX();
        double centerZ = config.isUseSpawnCenter() ? world.getSpawnLocation().getZ() : config.getCenterZ();

        WorldBorder border = world.getWorldBorder();
        Location borderCenter = border.getCenter();
        double borderHalfSize = Math.max(16.0, (border.getSize() / 2.0) - 4.0);

        int minR = config.getMinRadius();
        int maxR = config.getMaxRadius();

        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Intentar encontrar un punto dentro del anillo [minR, maxR] y dentro del WorldBorder
        for (int i = 0; i < 15; i++) {
            double angle = random.nextDouble(0.0, Math.PI * 2.0);
            // Distribución uniforme en el área del anillo
            double minSq = (double) minR * minR;
            double maxSq = (double) maxR * maxR;
            double distance = Math.sqrt(random.nextDouble(minSq, Math.max(minSq + 1.0, maxSq)));

            int x = (int) Math.round(centerX + Math.cos(angle) * distance);
            int z = (int) Math.round(centerZ + Math.sin(angle) * distance);

            if (Math.abs(x - borderCenter.getX()) <= borderHalfSize
                    && Math.abs(z - borderCenter.getZ()) <= borderHalfSize) {
                return new int[]{x, z};
            }
        }

        // Fallback ajustado al WorldBorder si el maxRadius superaba el borde del mundo
        double clampedMax = Math.min(maxR, borderHalfSize);
        double clampedMin = Math.min(minR, Math.max(0.0, clampedMax - 16.0));
        double angle = random.nextDouble(0.0, Math.PI * 2.0);
        double distance = random.nextDouble(clampedMin, Math.max(clampedMin + 1.0, clampedMax));

        int x = (int) Math.round(borderCenter.getX() + Math.cos(angle) * distance);
        int z = (int) Math.round(borderCenter.getZ() + Math.sin(angle) * distance);
        return new int[]{x, z};
    }

    /**
     * Escanea verticalmente la columna (blockX, blockZ) dentro del chunk ya cargado
     * para hallar un suelo seguro y 2 bloques libres de aire/pasables para el jugador.
     */
    private @Nullable Location scanColumnForSafeSpot(World world, Chunk chunk, int blockX, int blockZ) {
        if (!chunk.isLoaded()) {
            return null;
        }

        WorldBorder border = world.getWorldBorder();
        Location testBorderLoc = new Location(world, blockX + 0.5, 64, blockZ + 0.5);
        if (!border.isInside(testBorderLoc)) {
            return null;
        }

        if (config.isAvoidOceanBiomes() && world.getEnvironment() == World.Environment.NORMAL) {
            Biome biome = world.getBiome(blockX, 64, blockZ);
            String biomeKey = biome.getKey().getKey().toLowerCase(Locale.ROOT);
            if (biomeKey.contains("ocean") || biomeKey.contains("deep_ocean") || biomeKey.contains("river")) {
                return null;
            }
        }

        if (world.getEnvironment() == World.Environment.NETHER) {
            return scanNetherColumn(world, blockX, blockZ);
        }

        // Overworld / End / Custom: usar getHighestBlockYAt con el chunk ya cargado en memoria
        int highestY = world.getHighestBlockYAt(blockX, blockZ);
        int minY = world.getMinHeight();

        if (highestY <= minY) {
            return null;
        }

        // Si evitar hojas está activo y el bloque más alto son hojas, intentamos escanear también debajo del árbol
        for (int y = highestY; y > minY + 1; y--) {
            Block ground = world.getBlockAt(blockX, y, blockZ);
            Material groundType = ground.getType();

            if (groundType.isAir()) {
                continue;
            }

            if (!config.isBlockSafeGround(groundType)) {
                // Si el bloque superior es agua, lava o peligroso, descartamos la columna
                // (a menos que sean hojas y queramos mirar el suelo firme bajo la copa del árbol)
                if (config.isAvoidLeaves() && org.bukkit.Tag.LEAVES.isTagged(groundType)) {
                    continue;
                }
                return null;
            }

            Block feet = world.getBlockAt(blockX, y + 1, blockZ);
            Block head = world.getBlockAt(blockX, y + 2, blockZ);

            if (isSafeAirSpace(feet) && isSafeAirSpace(head)) {
                return new Location(world, blockX + 0.5, y + 1.0, blockZ + 0.5);
            }

            // En Overworld normal solo probamos la superficie superior real
            break;
        }

        return null;
    }

    /**
     * En el Nether existe techo de bedrock en Y=127, por lo que buscamos entre Y=32 y Y=120.
     */
    private @Nullable Location scanNetherColumn(World world, int blockX, int blockZ) {
        for (int y = 120; y >= 32; y--) {
            Block ground = world.getBlockAt(blockX, y, blockZ);
            if (!config.isBlockSafeGround(ground.getType())) {
                continue;
            }

            Block feet = world.getBlockAt(blockX, y + 1, blockZ);
            Block head = world.getBlockAt(blockX, y + 2, blockZ);
            if (isSafeAirSpace(feet) && isSafeAirSpace(head)) {
                return new Location(world, blockX + 0.5, y + 1.0, blockZ + 0.5);
            }
        }
        return null;
    }

    private boolean isSafeAirSpace(Block block) {
        Material type = block.getType();
        if (type.isAir()) {
            return true;
        }
        if (block.isLiquid()) {
            return false;
        }
        if (type == Material.FIRE || type == Material.SOUL_FIRE
                || type == Material.SWEET_BERRY_BUSH || type == Material.POWDER_SNOW
                || type == Material.WITHER_ROSE || type == Material.COBWEB
                || type == Material.CACTUS || type == Material.POINTED_DRIPSTONE) {
            return false;
        }
        return block.isPassable() && !type.isSolid();
    }
}
