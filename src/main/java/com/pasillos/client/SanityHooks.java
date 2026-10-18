package com.pasillos.client;

import net.minecraft.util.Mth;

/** Punto de entrada del cliente para los cambios que manda el servidor (ver SanityMessage). */
public final class SanityHooks {

    public static void apply(float sanityDelta, float anxietyDelta) {
        if (!SanityState.inside) {
            return;
        }
        SanityState.sanity = Mth.clamp(SanityState.sanity + sanityDelta, 0.0F, 100.0F);
        SanityState.anxiety = Mth.clamp(SanityState.anxiety + anxietyDelta, 0.0F, 100.0F);
        if (sanityDelta > 0.0F) {
            SanityState.sanityGhost = SanityState.sanity;
            SanityState.sanityHeal = 24;
        }
        if (anxietyDelta < 0.0F) {
            SanityState.anxietyGhost = Math.max(SanityState.anxietyGhost, SanityState.anxiety);
            SanityState.anxietyHeal = 24;
        }
    }

    private SanityHooks() {
    }
}
