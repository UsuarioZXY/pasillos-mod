package com.pasillos.worldgen;

import com.pasillos.PasillosConfig;
import net.minecraft.core.BlockPos;

/**
 * La forma de la cueva del Nivel -1. Todo son funciones puras de las coordenadas: una sola cueva
 * gigante, redonda e irregular, centrada en (0, 0), con pilares de piedra sueltos. Fuera de ella solo hay roca.
 */
public final class CaveShape {

    public static final int HEIGHT = 128;
    private static final int PILLAR_CELL = 36;

    /** Radio configurado de la cueva en bloques (con valor de reserva si la config aun no esta cargada). */
    public static int radius() {
        try {
            return PasillosConfig.CAVE_RADIUS.get();
        } catch (Throwable t) {
            return 140;
        }
    }

    // ---- ruido de valores, suave, en [-1, 1] ----

    private static int hash(int x, int z, int salt) {
        int h = x * 374761393 + z * 668265263 + salt * 1274126177;
        h = (h ^ (h >>> 13)) * 1103515245;
        return h ^ (h >>> 16);
    }

    private static double rnd(int x, int z, int salt) {
        return (hash(x, z, salt) & 0xFFFF) / 32767.5D - 1.0D;
    }

    private static double smooth(double t) {
        return t * t * (3.0D - 2.0D * t);
    }

    public static double noise(double x, double z, double scale, int salt) {
        double fx = x / scale;
        double fz = z / scale;
        int ix = (int) Math.floor(fx);
        int iz = (int) Math.floor(fz);
        double tx = smooth(fx - ix);
        double tz = smooth(fz - iz);
        double a = rnd(ix, iz, salt);
        double b = rnd(ix + 1, iz, salt);
        double c = rnd(ix, iz + 1, salt);
        double d = rnd(ix + 1, iz + 1, salt);
        double top = a + (b - a) * tx;
        double bottom = c + (d - c) * tx;
        return top + (bottom - top) * tz;
    }

    /** Distancia al borde: menor que 1 = dentro de la cueva, 1 o mas = roca solida. */
    public static double edge(int radius, int x, int z) {
        double d = Math.sqrt((double) x * x + (double) z * z);
        double ang = Math.atan2(z, x);
        double r = radius * (0.84 + 0.09 * Math.sin(3.0 * ang + 1.3) + 0.05 * Math.sin(5.0 * ang + 0.4)
                + 0.03 * Math.sin(9.0 * ang + 2.2));
        return d / r + 0.05 * noise(x, z, 11.0, 5);
    }

    /** Altura del ultimo bloque de suelo (se pisa en +1), o -1 si la columna es roca maciza. */
    public static int floor(int radius, int x, int z) {
        double t = edge(radius, x, z);
        if (t >= 1.0) {
            return -1;
        }
        double f = 24.0 + 5.0 * noise(x, z, 28.0, 1) + 2.0 * noise(x, z, 9.0, 2);
        if (t > 0.85) {
            double k = (t - 0.85) / 0.15;
            f += k * k * 6.0;
        }
        return (int) Math.round(f);
    }

    /** Altura del primer bloque de techo (roca), o -1 si la columna es roca maciza. */
    public static int ceiling(int radius, int x, int z) {
        double t = edge(radius, x, z);
        if (t >= 1.0) {
            return -1;
        }
        double c = 64.0 + 9.0 * noise(x, z, 40.0, 3) + 3.0 * noise(x, z, 12.0, 4);
        double f = 24.0 + 5.0 * noise(x, z, 28.0, 1) + 2.0 * noise(x, z, 9.0, 2);
        if (t > 0.8) {
            double k = (t - 0.8) / 0.2;
            c -= k * k * (c - f);
        }
        return (int) Math.round(c);
    }

    /** True si (x, z) esta en un pilar de piedra a la altura y (los pilares se estrechan por el medio). */
    public static boolean pillar(int radius, int x, int z, int y, int floor, int ceiling) {
        int cx = Math.floorDiv(x, PILLAR_CELL);
        int cz = Math.floorDiv(z, PILLAR_CELL);
        int h = hash(cx, cz, 91);
        if (Math.floorMod(h, 100) >= 55) {
            return false;
        }
        int px = cx * PILLAR_CELL + 6 + Math.floorMod(h >>> 8, 24);
        int pz = cz * PILLAR_CELL + 6 + Math.floorMod(h >>> 16, 24);
        double r0 = 2.5 + Math.floorMod(h >>> 4, 3);
        double span = Math.max(1, ceiling - floor);
        double mid = Math.abs(2.0 * (y - floor) / span - 1.0);
        double r = r0 * (0.55 + 0.45 * mid);
        double dx = x - px;
        double dz = z - pz;
        return dx * dx + dz * dz <= r * r;
    }

    /** Un sitio abierto donde aparecer (a los pies del jugador), o null si no se encuentra. */
    public static BlockPos findSpawn(java.util.Random random) {
        int radius = radius();
        for (int i = 0; i < 80; i++) {
            double ang = random.nextDouble() * Math.PI * 2.0;
            double dist = random.nextDouble() * radius * 0.55;
            int x = (int) Math.round(Math.cos(ang) * dist);
            int z = (int) Math.round(Math.sin(ang) * dist);
            int f = floor(radius, x, z);
            int c = ceiling(radius, x, z);
            if (f < 0 || c - f < 10) {
                continue;
            }
            boolean blocked = false;
            for (int dx = -1; dx <= 1 && !blocked; dx++) {
                for (int dz = -1; dz <= 1 && !blocked; dz++) {
                    int ff = floor(radius, x + dx, z + dz);
                    int cc = ceiling(radius, x + dx, z + dz);
                    for (int y = f + 1; y <= f + 3 && !blocked; y++) {
                        if (ff < 0 || y <= ff || y >= cc || pillar(radius, x + dx, z + dz, y, ff, cc)) {
                            blocked = true;
                        }
                    }
                }
            }
            if (!blocked) {
                return new BlockPos(x, f + 1, z);
            }
        }
        return new BlockPos(0, 40, 0);
    }

    private CaveShape() {
    }
}
