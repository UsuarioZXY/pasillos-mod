package com.pasillos.client;

/** Estado del lado del cliente: cordura y ansiedad. Solo se usa dentro de la dimension. */
public final class SanityState {

    /** True mientras el jugador esta en la dimension. */
    public static boolean inside = false;
    /** 0 a 100. Baja con la oscuridad, la cercania del Asechador y al verlo. */
    public static float sanity = 100.0F;
    /** 0 a 100. Sube con el Asechador cerca, la noche y la poca cordura. */
    public static float anxiety = 0.0F;
    /** 0 a 1. Cuanto se ha hecho de noche. */
    public static float night = 0.0F;

    public static void reset() {
        inside = false;
        sanity = 100.0F;
        anxiety = 0.0F;
        night = 0.0F;
    }

    private SanityState() {
    }
}
