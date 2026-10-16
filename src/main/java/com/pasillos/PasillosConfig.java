package com.pasillos;

import net.minecraftforge.common.ForgeConfigSpec;

public final class PasillosConfig {

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;

    // --- servidor / mundo ---
    public static final ForgeConfigSpec.DoubleValue SLEEP_CHANCE;
    public static final ForgeConfigSpec.IntValue SLEEP_DELAY_TICKS;

    // --- cliente / niebla ---
    public static final ForgeConfigSpec.DoubleValue DAY_FOG_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue NIGHT_FOG_DISTANCE;

    static {
        ForgeConfigSpec.Builder common = new ForgeConfigSpec.Builder();
        common.push("sueno");
        SLEEP_CHANCE = common
                .comment("Probabilidad (0.0 a 1.0) de que al dormir en una cama del Overworld despiertes en los Pasillos.")
                .defineInRange("probabilidad_al_dormir", 0.5D, 0.0D, 1.0D);
        SLEEP_DELAY_TICKS = common
                .comment("Ticks que debes llevar dormido antes de que ocurra el cambio (20 ticks = 1 segundo, maximo 99).")
                .defineInRange("ticks_hasta_el_salto", 60, 10, 99);
        common.pop();
        COMMON_SPEC = common.build();

        ForgeConfigSpec.Builder client = new ForgeConfigSpec.Builder();
        client.push("niebla");
        DAY_FOG_DISTANCE = client
                .comment("Distancia de visibilidad (en bloques) de dia dentro de los Pasillos.")
                .defineInRange("distancia_dia", 40.0D, 4.0D, 256.0D);
        NIGHT_FOG_DISTANCE = client
                .comment("Distancia de visibilidad (en bloques) de noche dentro de los Pasillos. Menos = mas niebla.")
                .defineInRange("distancia_noche", 6.0D, 2.0D, 64.0D);
        client.pop();
        CLIENT_SPEC = client.build();
    }

    private PasillosConfig() {
    }
}
