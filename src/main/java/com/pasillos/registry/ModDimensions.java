package com.pasillos.registry;

import com.pasillos.PasillosMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class ModDimensions {

    /** Nivel 0: el mundo plano de los arboles identicos. */
    public static final ResourceKey<Level> NIVEL_0 = ResourceKey.create(
            Registries.DIMENSION, new ResourceLocation(PasillosMod.MODID, "nivel_0"));

    /** Nivel -1: una cueva gigante de piedra, a oscuras. */
    public static final ResourceKey<Level> NIVEL_MENOS_1 = ResourceKey.create(
            Registries.DIMENSION, new ResourceLocation(PasillosMod.MODID, "nivel_menos_1"));

    /** True si es cualquiera de los niveles del mod. */
    public static boolean isMod(ResourceKey<Level> key) {
        return NIVEL_0.equals(key) || NIVEL_MENOS_1.equals(key);
    }

    private ModDimensions() {
    }
}
