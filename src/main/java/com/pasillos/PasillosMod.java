package com.pasillos;

import com.pasillos.registry.ModChunkGenerators;
import com.pasillos.registry.ModEntities;
import com.pasillos.registry.ModItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(PasillosMod.MODID)
public class PasillosMod {

    public static final String MODID = "pasillos";

    public PasillosMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModChunkGenerators.CHUNK_GENERATORS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PasillosConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, PasillosConfig.CLIENT_SPEC);

        MinecraftForge.EVENT_BUS.register(this);
    }
}
