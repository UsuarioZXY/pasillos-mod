package com.pasillos.registry;

import com.pasillos.PasillosMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class ModDimensions {

    /** Nivel 0: pasillos amarillos. Los siguientes niveles se anaden aqui con su propio JSON de dimension. */
    public static final ResourceKey<Level> NIVEL_0 = ResourceKey.create(
            Registries.DIMENSION, new ResourceLocation(PasillosMod.MODID, "nivel_0"));

    private ModDimensions() {
    }
}
