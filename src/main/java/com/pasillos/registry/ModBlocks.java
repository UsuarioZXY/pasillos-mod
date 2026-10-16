package com.pasillos.registry;

import com.pasillos.PasillosMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, PasillosMod.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, PasillosMod.MODID);

    public static final RegistryObject<Block> WALLPAPER = BLOCKS.register("wallpaper",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(1.0F, 4.0F)
                    .sound(SoundType.WOOD)));

    public static final RegistryObject<Block> WALLPAPER_STAINED = BLOCKS.register("wallpaper_stained",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(1.0F, 4.0F)
                    .sound(SoundType.WOOD)));

    public static final RegistryObject<Block> CARPET = BLOCKS.register("carpet",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(0.8F, 3.0F)
                    .sound(SoundType.WOOL)));

    public static final RegistryObject<Block> CEILING_TILE = BLOCKS.register("ceiling_tile",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(0.8F, 3.0F)
                    .sound(SoundType.CALCITE)));

    public static final RegistryObject<Block> FLUORESCENT_LIGHT = BLOCKS.register("fluorescent_light",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .strength(0.5F, 1.0F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 13)));

    public static final RegistryObject<Item> WALLPAPER_ITEM = blockItem("wallpaper", WALLPAPER);
    public static final RegistryObject<Item> WALLPAPER_STAINED_ITEM = blockItem("wallpaper_stained", WALLPAPER_STAINED);
    public static final RegistryObject<Item> CARPET_ITEM = blockItem("carpet", CARPET);
    public static final RegistryObject<Item> CEILING_TILE_ITEM = blockItem("ceiling_tile", CEILING_TILE);
    public static final RegistryObject<Item> FLUORESCENT_LIGHT_ITEM = blockItem("fluorescent_light", FLUORESCENT_LIGHT);

    private static RegistryObject<Item> blockItem(String name, RegistryObject<Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private ModBlocks() {
    }
}
