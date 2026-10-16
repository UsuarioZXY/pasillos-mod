package com.pasillos.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Mundo "antiguo": colinas suaves de hierba, lagos, arena en las orillas, robles sueltos y algo de carbon.
 * Sin cuevas, sin animales, sin flores, sin aldeas. Todo es muy parecido a un Minecraft de 2010 y esta vacio.
 * El terreno depende solo de las coordenadas, asi que es igual en todos los mundos.
 */
public class AncientChunkGenerator extends ChunkGenerator {

    public static final Codec<AncientChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
    ).apply(instance, instance.stable(AncientChunkGenerator::new)));

    public static final int HEIGHT = 128;
    public static final int SEA = 60;

    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
    private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
    private static final BlockState SAND = Blocks.SAND.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState BEDROCK = Blocks.BEDROCK.defaultBlockState();
    private static final BlockState COAL = Blocks.COAL_ORE.defaultBlockState();
    private static final BlockState LOG = Blocks.OAK_LOG.defaultBlockState();
    private static final BlockState LEAVES = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    private static final BlockState COBBLE = Blocks.COBBLESTONE.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    public AncientChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
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
                                                        net.minecraft.world.level.StructureManager structureManager,
                                                        ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        BlockState[] column = new BlockState[HEIGHT];

        for (int cx = 0; cx < 16; cx++) {
            for (int cz = 0; cz < 16; cz++) {
                int x = chunkPos.getMinBlockX() + cx;
                int z = chunkPos.getMinBlockZ() + cz;

                Arrays.fill(column, null);
                int h = surfaceHeight(x, z);
                fillTerrain(column, x, z, h);
                addTreesAndPillars(column, x, z, h);

                for (int y = 0; y < HEIGHT; y++) {
                    BlockState state = column[y];
                    if (state == null) {
                        continue;
                    }
                    chunk.setBlockState(pos.set(x, y, z), state, false);
                    oceanFloor.update(cx, y, cz, state);
                    worldSurface.update(cx, y, cz, state);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    private static void fillTerrain(BlockState[] column, int x, int z, int h) {
        boolean beach = h <= SEA + 1;
        for (int y = 0; y <= h; y++) {
            BlockState state;
            if (y == 0 || (y == 1 && hash(x, z, 5) % 3 == 0)) {
                state = BEDROCK;
            } else if (y == h) {
                state = beach ? SAND : GRASS;
            } else if (y >= h - 3) {
                state = beach ? SAND : DIRT;
            } else if (y < 50 && hash(x * 7 + y, z * 13 - y, 6) % 110 == 0) {
                state = COAL;
            } else {
                state = STONE;
            }
            column[y] = state;
        }
        for (int y = h + 1; y <= SEA; y++) {
            column[y] = WATER;
        }
    }

    /** Robles dispersos y, muy de vez en cuando, un pilar de cobblestone suelto que nadie puso ahi. */
    private static void addTreesAndPillars(BlockState[] column, int x, int z, int h) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int tx = x + dx;
                int tz = z + dz;
                if (!isTreeCenter(tx, tz)) {
                    continue;
                }
                int base = surfaceHeight(tx, tz);
                if (base <= SEA + 1) {
                    continue;
                }
                int trunk = 4 + hash(tx, tz, 32) % 2;
                if (dx == 0 && dz == 0) {
                    for (int y = base + 1; y <= base + trunk; y++) {
                        column[y] = LOG;
                    }
                }
                for (int y = base + trunk - 2; y <= base + trunk + 1; y++) {
                    int r = y <= base + trunk - 1 ? 2 : 1;
                    int adx = Math.abs(dx);
                    int adz = Math.abs(dz);
                    if (adx > r || adz > r) {
                        continue;
                    }
                    if (adx == r && adz == r && (r == 2 || y == base + trunk + 1)) {
                        continue;
                    }
                    if (y < HEIGHT && column[y] == null) {
                        column[y] = LEAVES;
                    }
                }
            }
        }

        if (h > SEA + 1 && hash(x, z, 41) % 6000 == 0 && column[h + 1] == null) {
            column[h + 1] = COBBLE;
            column[h + 2] = COBBLE;
            column[h + 3] = COBBLE;
        }
    }

    private static boolean isTreeCenter(int x, int z) {
        return hash(x, z, 31) % 60 == 0;
    }

    // ------------------------------------------------------------------------------------------
    // Ruido
    // ------------------------------------------------------------------------------------------

    public static int surfaceHeight(int x, int z) {
        double n = noise(x / 52.0D, z / 52.0D, 1) * 15.0D
                + noise(x / 20.0D, z / 20.0D, 2) * 5.0D
                + noise(x / 7.0D, z / 7.0D, 3) * 1.3D;
        return 67 + (int) Math.floor(n);
    }

    /** Ruido de valor suavizado, devuelve un numero entre -1 y 1. */
    private static double noise(double x, double z, int salt) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        double fx = smooth(x - x0);
        double fz = smooth(z - z0);
        double a = corner(x0, z0, salt);
        double b = corner(x0 + 1, z0, salt);
        double c = corner(x0, z0 + 1, salt);
        double d = corner(x0 + 1, z0 + 1, salt);
        double top = a + (b - a) * fx;
        double bottom = c + (d - c) * fx;
        return top + (bottom - top) * fz;
    }

    private static double corner(int x, int z, int salt) {
        return (hash(x, z, salt + 700) & 0xFFFF) / 32767.5D - 1.0D;
    }

    private static double smooth(double t) {
        return t * t * (3.0D - 2.0D * t);
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
        return HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return SEA;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor,
                             RandomState randomState) {
        return Math.max(surfaceHeight(x, z), SEA) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
        int h = surfaceHeight(x, z);
        BlockState[] states = new BlockState[HEIGHT];
        for (int y = 0; y < HEIGHT; y++) {
            if (y <= h) {
                states[y] = y == h ? GRASS : (y >= h - 3 ? DIRT : STONE);
            } else if (y <= SEA) {
                states[y] = WATER;
            } else {
                states[y] = AIR;
            }
        }
        return new NoiseColumn(heightAccessor.getMinBuildHeight(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
    }
}
