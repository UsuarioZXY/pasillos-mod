package com.pasillos.client;

/** Estado del lado del cliente: cordura y ansiedad. Solo se usa dentro de la dimension. */
public final class SanityState {

    /** Cuantos ticks dura cada pensamiento / mensaje en pantalla. */
    public static final int THOUGHT_TOTAL = 140;

    /** True mientras el jugador esta en la dimension. */
    public static boolean inside = false;
    /** 0 a 100. Baja con la oscuridad, la cercania del Asechador y al verlo. */
    public static float sanity = 100.0F;
    /** 0 a 100. Sube con el Asechador cerca, la noche y la poca cordura. */
    public static float anxiety = 0.0F;
    /** 0 a 1. Cuanto se ha hecho de noche. */
    public static float night = 0.0F;

    /** Estela de las barras: se queda atras y alcanza al valor real poco a poco. */
    public static float sanityGhost = 100.0F;
    public static float anxietyGhost = 0.0F;
    /** Tendencia suave (puntos por segundo): negativa = bajando. */
    public static float sanityTrend = 0.0F;
    public static float anxietyTrend = 0.0F;
    /** El Asechador te esta mirando (hay linea de vista) y esta cerca. */
    public static boolean watched = false;
    /** Lo estas viendo de frente. */
    public static boolean seen = false;
    /** Destellos al curarte (ticks restantes). */
    public static int sanityHeal = 0;
    public static int anxietyHeal = 0;
    /** Destello rojo cuando la cordura cae rapido. */
    public static int hurtFlash = 0;
    /** Ruido que haces: ultimo valor del servidor y valor mostrado (sube rapido, baja suave). */
    public static float noiseTarget = 0.0F;
    public static float noiseShown = 0.0F;

    /** Pensamiento o mensaje que se muestra ahora (clave de traduccion) y ticks que le quedan. */
    public static String thoughtKey = "";
    public static int thoughtTicks = 0;
    private static final java.util.ArrayDeque<String> THOUGHT_QUEUE = new java.util.ArrayDeque<>();

    /** Muestra un pensamiento; si ya hay uno en pantalla, se pone en cola. */
    public static void say(String key) {
        if (thoughtTicks <= 0) {
            thoughtKey = key;
            thoughtTicks = THOUGHT_TOTAL;
        } else if (THOUGHT_QUEUE.size() < 4 && !thoughtKey.equals(key) && !THOUGHT_QUEUE.contains(key)) {
            THOUGHT_QUEUE.add(key);
        }
    }

    /** Avanza el pensamiento actual; cuando termina saca el siguiente de la cola. */
    public static void tickThought() {
        if (thoughtTicks > 0) {
            thoughtTicks--;
        } else if (!THOUGHT_QUEUE.isEmpty()) {
            thoughtKey = THOUGHT_QUEUE.poll();
            thoughtTicks = THOUGHT_TOTAL;
        }
    }

    public static void reset() {
        inside = false;
        sanity = 100.0F;
        anxiety = 0.0F;
        night = 0.0F;
        sanityGhost = 100.0F;
        anxietyGhost = 0.0F;
        sanityTrend = 0.0F;
        anxietyTrend = 0.0F;
        watched = false;
        seen = false;
        sanityHeal = 0;
        anxietyHeal = 0;
        hurtFlash = 0;
        noiseTarget = 0.0F;
        noiseShown = 0.0F;
        thoughtKey = "";
        thoughtTicks = 0;
        THOUGHT_QUEUE.clear();
    }

    private SanityState() {
    }
}
