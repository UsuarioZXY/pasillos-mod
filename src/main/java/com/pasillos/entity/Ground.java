package com.pasillos.entity;

import com.pasillos.registry.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/** Encuentra donde pisar en las dos dimensiones: el suelo plano del Nivel 0 y el interior de la cueva del Nivel -1. */
public final class Ground {

    public static final int NONE = Integer.MIN_VALUE;

    /** Altura de los pies de alguien en (x, z), o NONE si no hay un hueco de 4 bloques con suelo. refY es una pista. */
    public static int standY(Level level, int x, int z, int refY) {
        if (level.dimension().equals(ModDimensions.NIVEL_0)) {
            return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int from = Math.min(refY + 10, level.getMaxBuildHeight() - 6);
        int to = Math.max(refY - 14, level.getMinBuildHeight() + 2);
        for (int y = from; y >= to; y--) {
            pos.set(x, y, z);
            if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
                continue;
            }
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                    && level.getBlockState(pos.above(2)).isAir() && level.getBlockState(pos.above(3)).isAir()) {
                return y;
            }
        }
        return NONE;
    }

    /** "De noche" para el Asechador: la noche normal en el Nivel 0 y siempre en la cueva. */
    public static boolean isDark(Level level) {
        return level.dimension().equals(ModDimensions.NIVEL_MENOS_1) || level.isNight();
    }

    private Ground() {
    }
}
