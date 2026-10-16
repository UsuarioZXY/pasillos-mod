package com.pasillos.registry;

import com.mojang.serialization.Codec;
import com.pasillos.PasillosMod;
import com.pasillos.worldgen.LiminalChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModChunkGenerators {

    public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, PasillosMod.MODID);

    public static final RegistryObject<Codec<LiminalChunkGenerator>> LIMINAL =
            CHUNK_GENERATORS.register("liminal", () -> LiminalChunkGenerator.CODEC);

    private ModChunkGenerators() {
    }
}
