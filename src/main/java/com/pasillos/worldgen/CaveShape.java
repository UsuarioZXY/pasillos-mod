package com.pasillos.worldgen;

import com.pasillos.PasillosConfig;
import net.minecraft.core.BlockPos;

/**
 * La forma del Nivel -1: una red de tuneles estrechos y altos de piedra que se cruzan, se ramifican,
 * hacen callejones sin salida y se abren en alguna sala pequena. Todo son funciones puras de las
 * coordenadas. Fuera de los tuneles solo hay roca maciza.
 *
 * La red es una cuadricula de celdas de 20 bloques con un nodo (cruce o sala) en cada una, algo
 * desplazado; entre nodos vecinos hay un tunel o no, segun un hash.
 */
public final class CaveShape {

    public static final int HEIGHT = 128;
    private static final int CELL = 20;
    /** Altura del suelo base. */
    private static final int BASE_FLOOR = 40;

    /** Radio (bloques) hasta donde llega la red de tuneles; fuera es roca. */
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

    // ---- la red ----

    private static double nodeX(int cx, int cz) {
        return cx * CELL + 10 + 5.0 * rnd(cx, cz, 11);
    }

    private static double nodeZ(int cx, int cz) {
        return cz * CELL + 10 + 5.0 * rnd(cx, cz, 12);
    }

    private static boolean hasNode(int radius, int cx, int cz) {
        double x = nodeX(cx, cz);
        double z = nodeZ(cx, cz);
        return x * x + z * z <= (double) radius * radius;
    }

    private static boolean edgeEast(int radius, int cx, int cz) {
        return hasNode(radius, cx, cz) && hasNode(radius, cx + 1, cz) && Math.floorMod(hash(cx, cz, 21), 100) < 66;
    }

    private static boolean edgeSouth(int radius, int cx, int cz) {
        return hasNode(radius, cx, cz) && hasNode(radius, cx, cz + 1) && Math.floorMod(hash(cx, cz, 22), 100) < 66;
    }

    private static double segmentDistance(double px, double pz, double ax, double az, double bx, double bz) {
        double abx = bx - ax;
        double abz = bz - az;
        double len2 = abx * abx + abz * abz;
        double t = len2 < 1.0E-6 ? 0.0 : ((px - ax) * abx + (pz - az) * abz) / len2;
        t = Math.max(0.0, Math.min(1.0, t));
        double dx = px - (ax + abx * t);
        double dz = pz - (az + abz * t);
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Para una columna (x, z): {ultimo bloque de suelo, primer bloque de techo}, o null si es roca maciza.
     * Se camina en suelo + 1 y hay {techo - suelo - 1} bloques de aire encima.
     */
    public static int[] column(int radius, int x, int z) {
        // las paredes ondulan un poco
        double wx = x + 3.0 * noise(x, z, 13.0, 31);
        double wz = z + 3.0 * noise(x, z, 13.0, 32);
        double width = 2.6 + 0.8 * noise(x, z, 17.0, 7);

        int cellX = Math.floorDiv((int) Math.floor(wx), CELL);
        int cellZ = Math.floorDiv((int) Math.floor(wz), CELL);
        int best = -1;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int cx = cellX + dx;
                int cz = cellZ + dz;
                if (!hasNode(radius, cx, cz)) {
                    continue;
                }
                double nx = nodeX(cx, cz);
                double nz = nodeZ(cx, cz);

                // sala en el nodo
                double roomR = 4.5 + 1.5 * (rnd(cx, cz, 13) + 1.0);
                double dn = Math.sqrt((wx - nx) * (wx - nx) + (wz - nz) * (wz - nz));
                if (dn < roomR) {
                    double k = dn / roomR;
                    best = Math.max(best, 9 - (int) Math.round(3.0 * k * k));
                }
                // tuneles que salen del nodo hacia el este y el sur
                if (edgeEast(radius, cx, cz)) {
                    double d = segmentDistance(wx, wz, nx, nz, nodeX(cx + 1, cz), nodeZ(cx + 1, cz));
                    if (d < width) {
                        double k = d / width;
                        best = Math.max(best, 6 - (int) Math.round(2.0 * k * k));
                    }
                }
                if (edgeSouth(radius, cx, cz)) {
                    double d = segmentDistance(wx, wz, nx, nz, nodeX(cx, cz + 1), nodeZ(cx, cz + 1));
                    if (d < width) {
                        double k = d / width;
                        best = Math.max(best, 6 - (int) Math.round(2.0 * k * k));
                    }
                }
            }
        }
        if (best < 3) {
            return null;
        }
        int floor = BASE_FLOOR + (int) Math.round(3.0 * noise(x, z, 30.0, 1));
        return new int[]{floor, floor + best + 1};
    }

    /** Un sitio abierto donde aparecer (a los pies del jugador): una sala con al menos un tunel. */
    public static BlockPos findSpawn(java.util.Random random) {
        int radius = radius();
        int cells = Math.max(1, radius / CELL);
        for (int i = 0; i < 200; i++) {
            int cx = random.nextInt(cells * 2 + 1) - cells;
            int cz = random.nextInt(cells * 2 + 1) - cells;
            if (!hasNode(radius, cx, cz)) {
                continue;
            }
            if (!(edgeEast(radius, cx, cz) || edgeSouth(radius, cx, cz) || edgeEast(radius, cx - 1, cz)
                    || edgeSouth(radius, cx, cz - 1))) {
                continue;
            }
            int x = (int) Math.round(nodeX(cx, cz));
            int z = (int) Math.round(nodeZ(cx, cz));
            int[] col = column(radius, x, z);
            if (col != null && col[1] - col[0] >= 7) {
                return new BlockPos(x, col[0] + 1, z);
            }
        }
        return new BlockPos(0, 60, 0);
    }

    private CaveShape() {
    }
}
