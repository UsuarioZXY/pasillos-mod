package com.pasillos;

import com.pasillos.registry.ModBlocks;
import com.pasillos.registry.ModChunkGenerators;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
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

        ModBlocks.BLOCKS.register(modBus);
        ModBlocks.ITEMS.register(modBus);
        ModChunkGenerators.CHUNK_GENERATORS.register(modBus);

        modBus.addListener(this::addCreative);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PasillosConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, PasillosConfig.CLIENT_SPEC);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModBlocks.WALLPAPER_ITEM);
            event.accept(ModBlocks.WALLPAPER_STAINED_ITEM);
            event.accept(ModBlocks.CARPET_ITEM);
            event.accept(ModBlocks.CEILING_TILE_ITEM);
            event.accept(ModBlocks.FLUORESCENT_LIGHT_ITEM);
        }
    }
}
