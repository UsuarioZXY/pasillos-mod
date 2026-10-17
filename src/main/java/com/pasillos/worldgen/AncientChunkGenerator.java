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
 * Mundo plano y demasiado ordenado: un suelo perfectamente liso, arboles identicos plantados en una
 * cuadricula de 7x7 y lineas de tierra seca que marcan esa cuadricula. Cada 49 bloques hay un pilar de
 * cobblestone donde deberia estar un arbol. Todo depende solo de las coordenadas, asi que es igual en
 * todos los mundos y se extiende para siempre.
 */
public class AncientChunkGenerator extends ChunkGenerator {

    public static final Codec<AncientChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
    ).apply(instance, instance.stable(AncientChunkGenerator::new)));

    public static final int HEIGHT = 128;
    public static final int FLAT_Y = 64;
    /** Separacion entre arboles, en bloques. */
    public static final int GRID = 7;
    private static final int TRUNK = 5;
    /** Cada PILLAR_EVERY celdas, en ambos ejes, el arbol se sustituye por un pilar. */
    private static final int PILLAR_EVERY = 7;

    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
    private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
    private static final BlockState COARSE = Blocks.COARSE_DIRT.defaultBlockState();
    private static final BlockState BEDROCK = Blocks.BEDROCK.defaultBlockState();
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

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState randomState,
                                                        StructureManager structureManager, ChunkAccess chunk) {
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
                fillGround(column, x, z);
                addPattern(column, x, z);

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

    private static void fillGround(BlockState[] column, int x, int z) {
        boolean line = Math.floorMod(x, GRID) == 0 || Math.floorMod(z, GRID) == 0;
        for (int y = 0; y <= FLAT_Y; y++) {
            if (y == 0) {
                column[y] = BEDROCK;
            } else if (y == FLAT_Y) {
                column[y] = line ? COARSE : GRASS;
            } else if (y >= FLAT_Y - 3) {
                column[y] = DIRT;
            } else {
                column[y] = STONE;
            }
        }
    }

    /** Un arbol (o un pilar) en el centro de cada celda de la cuadricula; todos exactamente iguales. */
    private static void addPattern(BlockState[] column, int x, int z) {
        int gx = Math.floorDiv(x, GRID);
        int gz = Math.floorDiv(z, GRID);
        int dx = x - (gx * GRID + 3);
        int dz = z - (gz * GRID + 3);
        if (Math.abs(dx) > 2 || Math.abs(dz) > 2) {
            return;
        }

        boolean pillar = Math.floorMod(gx, PILLAR_EVERY) == 0 && Math.floorMod(gz, PILLAR_EVERY) == 0;
        if (pillar) {
            if (dx == 0 && dz == 0) {
                for (int y = FLAT_Y + 1; y <= FLAT_Y + 3; y++) {
                    column[y] = COBBLE;
                }
            }
            return;
        }

        if (dx == 0 && dz == 0) {
            for (int y = FLAT_Y + 1; y <= FLAT_Y + TRUNK; y++) {
                column[y] = LOG;
            }
        }
        int top = FLAT_Y + TRUNK + 1;
        for (int y = FLAT_Y + TRUNK - 2; y <= top; y++) {
            int r = y <= FLAT_Y + TRUNK - 1 ? 2 : 1;
            int adx = Math.abs(dx);
            int adz = Math.abs(dz);
            if (adx > r || adz > r) {
                continue;
            }
            if (adx == r && adz == r && (r == 2 || y == top)) {
                continue;
            }
            if (column[y] == null) {
                column[y] = LEAVES;
            }
        }
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
        return FLAT_Y - 1;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor,
                             RandomState randomState) {
        return FLAT_Y + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
        BlockState[] states = new BlockState[HEIGHT];
        for (int y = 0; y < HEIGHT; y++) {
            if (y > FLAT_Y) {
                states[y] = AIR;
            } else if (y == FLAT_Y) {
                states[y] = GRASS;
            } else if (y >= FLAT_Y - 3) {
                states[y] = DIRT;
            } else {
                states[y] = STONE;
            }
        }
        return new NoiseColumn(heightAccessor.getMinBuildHeight(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
    }
}
