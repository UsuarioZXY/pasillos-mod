package com.pasillos.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pasillos.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Generador del "Nivel 0": un laberinto infinito de habitaciones amarillas.
 *
 * Capas verticales (la dimension mide 32 de alto pero solo se usan 8):
 *   y=0  bedrock
 *   y=1  moqueta (suelo)
 *   y=2-5 aire / paredes (4 bloques de alto)
 *   y=6  techo (placas o paneles fluorescentes)
 *   y=7  bedrock (tapa, no se puede salir por arriba)
 *
 * Planta: cuadricula de celdas de 8x8. Cada celda puede abrir una puerta hacia el oeste o el norte
 * (arbol binario, asi ninguna celda queda aislada) mas puertas extra al azar. Una de cada seis
 * regiones de 4x4 celdas es un salon abierto con solo pilares. Todo se deriva de un hash,
 * por lo que el mapa es infinito y siempre igual.
 */
public class LiminalChunkGenerator extends ChunkGenerator {

    public static final Codec<LiminalChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
            Codec.INT.optionalFieldOf("level", 0).forGetter(gen -> gen.level)
    ).apply(instance, instance.stable(LiminalChunkGenerator::new)));

    public static final int CELL = 8;
    public static final int FLOOR_Y = 1;
    public static final int AIR_MIN_Y = 2;
    public static final int AIR_MAX_Y = 5;
    public static final int CEILING_Y = 6;
    public static final int CAP_Y = 7;

    private final int level;

    public LiminalChunkGenerator(BiomeSource biomeSource, int level) {
        super(biomeSource);
        this.level = level;
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    // ------------------------------------------------------------------------------------------
    // Generacion
    // ------------------------------------------------------------------------------------------

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState randomState,
                                                        StructureManager structureManager, ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);

        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        BlockState carpet = ModBlocks.CARPET.get().defaultBlockState();
        BlockState ceiling = ModBlocks.CEILING_TILE.get().defaultBlockState();
        BlockState lamp = ModBlocks.FLUORESCENT_LIGHT.get().defaultBlockState();
        BlockState wallpaper = ModBlocks.WALLPAPER.get().defaultBlockState();

        for (int cx = 0; cx < 16; cx++) {
            for (int cz = 0; cz < 16; cz++) {
                int x = chunkPos.getMinBlockX() + cx;
                int z = chunkPos.getMinBlockZ() + cz;

                BlockState wall = wallAt(x, z);
                boolean lonePillar = wall == null && isLonePillar(x, z);
                boolean light = hasLight(x, z);
                boolean hole = hasCeilingHole(x, z);

                for (int y = 0; y <= CAP_Y; y++) {
                    BlockState state = null;
                    if (y == 0 || y == CAP_Y) {
                        state = bedrock;
                    } else if (y == FLOOR_Y) {
                        state = carpet;
                    } else if (y <= AIR_MAX_Y) {
                        if (wall != null) {
                            state = wall;
                        } else if (lonePillar && y <= AIR_MIN_Y + 1) {
                            state = wallpaper;
                        }
                    } else if (y == CEILING_Y && !hole) {
                        state = light ? lamp : ceiling;
                    }

                    if (state != null) {
                        chunk.setBlockState(pos.set(x, y, z), state, false);
                        oceanFloor.update(cx, y, cz, state);
                        worldSurface.update(cx, y, cz, state);
                    }
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    // ------------------------------------------------------------------------------------------
    // Diseno del laberinto
    // ------------------------------------------------------------------------------------------

    /** Devuelve el bloque de pared en esta columna, o null si es espacio libre. */
    @Nullable
    private BlockState wallAt(int x, int z) {
        int gx = Math.floorDiv(x, CELL);
        int gz = Math.floorDiv(z, CELL);
        int lx = Math.floorMod(x, CELL);
        int lz = Math.floorMod(z, CELL);

        BlockState plain = ModBlocks.WALLPAPER.get().defaultBlockState();
        BlockState stained = ModBlocks.WALLPAPER_STAINED.get().defaultBlockState();

        if (lx == 0 && lz == 0) {
            return plain; // pilar de esquina, siempre presente
        }
        if (lx == 0) {
            if (!solidEdge(gx, gz, lz, true)) {
                return null;
            }
            return isStained(gx, gz, true) ? stained : plain;
        }
        if (lz == 0) {
            if (!solidEdge(gx, gz, lx, false)) {
                return null;
            }
            return isStained(gx, gz, false) ? stained : plain;
        }
        return null;
    }

    /**
     * Pared que separa la celda (gx,gz) de su vecina del oeste (west=true) o del norte (west=false).
     * t es la posicion a lo largo de la pared (1..7). Devuelve true si en ese punto hay muro.
     */
    private boolean solidEdge(int gx, int gz, int t, boolean west) {
        int ox = west ? gx - 1 : gx;
        int oz = west ? gz : gz - 1;

        boolean hallHere = isHall(gx, gz);
        boolean hallThere = isHall(ox, oz);

        // dentro de un mismo salon abierto no hay paredes
        if (hallHere && hallThere && (gx >> 2) == (ox >> 2) && (gz >> 2) == (oz >> 2)) {
            return false;
        }

        int h = hash(gx, gz, salt(west ? 11 : 13));
        boolean door;
        if (hallHere || hallThere) {
            door = (h & 3) != 0; // los salones tienen muchas entradas
        } else {
            boolean carveWest = (hash(gx, gz, salt(5)) & 1) == 0; // arbol binario
            door = (west == carveWest) || ((h >> 4) & 3) == 0;
        }
        if (!door) {
            return true;
        }
        int start = 1 + ((h >> 8) & 0xFFFF) % 4; // la puerta mide 3 y empieza entre 1 y 4
        return !(t >= start && t < start + 3);
    }

    private boolean isHall(int gx, int gz) {
        return hash(gx >> 2, gz >> 2, salt(3)) % 6 == 0;
    }

    private boolean isStained(int gx, int gz, boolean west) {
        return hash(gx, gz, salt(west ? 17 : 19)) % 7 == 0;
    }

    /** Panel de luz 2x2 en el centro de cada celda; una de cada seis celdas esta a oscuras. */
    private boolean hasLight(int x, int z) {
        int lx = Math.floorMod(x, CELL);
        int lz = Math.floorMod(z, CELL);
        if ((lx != 3 && lx != 4) || (lz != 3 && lz != 4)) {
            return false;
        }
        int gx = Math.floorDiv(x, CELL);
        int gz = Math.floorDiv(z, CELL);
        return hash(gx, gz, salt(7)) % 6 != 0;
    }

    /** Un unico bloque suelto de dos de alto en mitad de una habitacion vacia. Nadie sabe por que esta ahi. */
    private boolean isLonePillar(int x, int z) {
        if (Math.floorMod(x, CELL) != 5 || Math.floorMod(z, CELL) != 5) {
            return false;
        }
        return hash(Math.floorDiv(x, CELL), Math.floorDiv(z, CELL), salt(9)) % 19 == 0;
    }

    /** Un agujero en el techo que deja ver la tapa de bedrock. */
    private boolean hasCeilingHole(int x, int z) {
        int lx = Math.floorMod(x, CELL);
        int lz = Math.floorMod(z, CELL);
        if (lx < 2 || lx > 5 || lz < 2 || lz > 5) {
            return false;
        }
        return hash(Math.floorDiv(x, CELL), Math.floorDiv(z, CELL), salt(23)) % 41 == 0;
    }

    private int salt(int base) {
        return level * 101 + base;
    }

    private static int hash(int x, int z, int salt) {
        long h = (long) x * 0x9E3779B97F4A7C15L ^ (long) z * 0xC2B2AE3D27D4EB4FL ^ (long) salt * 0x165667B19E3779F9L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (int) (h ^ (h >>> 32)) & 0x7FFFFFFF;
    }

    /** Posicion segura (suelo libre) para aparecer cerca de unas coordenadas dadas. */
    public static BlockPos safeSpawnNear(int x, int z) {
        int gx = Math.floorDiv(x, CELL);
        int gz = Math.floorDiv(z, CELL);
        return new BlockPos(gx * CELL + 2, AIR_MIN_Y, gz * CELL + 2);
    }

    // ------------------------------------------------------------------------------------------
    // Metodos obligatorios de ChunkGenerator
    // ------------------------------------------------------------------------------------------

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager,
                             StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState randomState,
                             ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public int getGenDepth() {
        return 32;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor,
                             RandomState randomState) {
        return CAP_Y + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
        BlockState[] states = new BlockState[CAP_Y + 1];
        for (int y = 0; y <= CAP_Y; y++) {
            if (y == 0 || y == CAP_Y) {
                states[y] = Blocks.BEDROCK.defaultBlockState();
            } else if (y == FLOOR_Y) {
                states[y] = ModBlocks.CARPET.get().defaultBlockState();
            } else if (y == CEILING_Y) {
                states[y] = ModBlocks.CEILING_TILE.get().defaultBlockState();
            } else {
                states[y] = Blocks.AIR.defaultBlockState();
            }
        }
        return new NoiseColumn(heightAccessor.getMinBuildHeight(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
    }
}
