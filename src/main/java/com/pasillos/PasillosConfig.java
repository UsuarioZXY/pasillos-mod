package com.pasillos;

import net.minecraftforge.common.ForgeConfigSpec;

public final class PasillosConfig {

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;

    // --- servidor / mundo ---
    public static final ForgeConfigSpec.DoubleValue SLEEP_CHANCE;
    public static final ForgeConfigSpec.IntValue SLEEP_DELAY_TICKS;

    public static final ForgeConfigSpec.IntValue CAVE_RADIUS;
    public static final ForgeConfigSpec.IntValue DEPTH_TRIGGER_OVERWORLD;
    public static final ForgeConfigSpec.IntValue DEPTH_TRIGGER_NIVEL0;

    // --- cliente / cordura ---
    public static final ForgeConfigSpec.DoubleValue HEART_ATTACK_CHANCE;

    // --- cliente / niebla ---
    public static final ForgeConfigSpec.DoubleValue DAY_FOG_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue NIGHT_FOG_DISTANCE;

    static {
        ForgeConfigSpec.Builder common = new ForgeConfigSpec.Builder();
        common.push("sueno");
        SLEEP_CHANCE = common
                .comment("Probabilidad (0.0 a 1.0) de que al dormir en una cama del Overworld despiertes en el mundo olvidado.")
                .defineInRange("probabilidad_al_dormir", 0.5D, 0.0D, 1.0D);
        SLEEP_DELAY_TICKS = common
                .comment("Ticks que debes llevar dormido antes de que ocurra el cambio (20 ticks = 1 segundo, maximo 99).")
                .defineInRange("ticks_hasta_el_salto", 60, 10, 99);
        common.pop();
        common.push("nivel_menos_1");
        CAVE_RADIUS = common
                .comment("Radio (bloques) de la red de tuneles del Nivel -1. Menos = menos tuneles. Solo afecta a chunks nuevos.")
                .defineInRange("radio_cueva", 140, 40, 800);
        DEPTH_TRIGGER_OVERWORLD = common
                .comment("Altura (Y) por debajo de la cual, en el Overworld, caes al Nivel -1. La base del mundo esta en -64.")
                .defineInRange("altura_overworld", -60, -64, 300);
        DEPTH_TRIGGER_NIVEL0 = common
                .comment("Altura (Y) por debajo de la cual, en el Nivel 0, caes al Nivel -1 (el suelo de bedrock esta en 0).")
                .defineInRange("altura_nivel_0", 3, 0, 30);
        common.pop();
        COMMON_SPEC = common.build();

        ForgeConfigSpec.Builder client = new ForgeConfigSpec.Builder();
        client.push("cordura");
        HEART_ATTACK_CHANCE = client
                .comment("Con la ansiedad al 100 %, probabilidad por segundo de ataque al corazon MULTIPLICADA por los segundos que llevas en panico (0 lo desactiva).")
                .defineInRange("ataque_corazon_por_segundo", 0.015D, 0.0D, 1.0D);
        client.pop();
        client.push("niebla");
        DAY_FOG_DISTANCE = client
                .comment("Distancia de visibilidad (en bloques) de dia dentro de la dimension.")
                .defineInRange("distancia_dia", 70.0D, 4.0D, 256.0D);
        NIGHT_FOG_DISTANCE = client
                .comment("Distancia de visibilidad (en bloques) de noche dentro de la dimension. Menos = mas niebla.")
                .defineInRange("distancia_noche", 30.0D, 2.0D, 64.0D);
        client.pop();
        CLIENT_SPEC = client.build();
    }

    private PasillosConfig() {
    }
}
