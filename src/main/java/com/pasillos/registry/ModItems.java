package com.pasillos.registry;

import com.pasillos.PasillosMod;
import com.pasillos.item.AnxItem;
import com.pasillos.item.SancorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, PasillosMod.MODID);

    public static final RegistryObject<Item> ASECHADOR_SPAWN_EGG = ITEMS.register("asechador_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ASECHADOR, 0x0B0B0B, 0xE8E8E8, new Item.Properties()));

    /** Bebida que devuelve cordura. */
    public static final RegistryObject<Item> SANCOR = ITEMS.register("sancor",
            () -> new SancorItem(new Item.Properties()));

    /** Pastillas que bajan la ansiedad. */
    public static final RegistryObject<Item> ANX = ITEMS.register("anx",
            () -> new AnxItem(new Item.Properties()));

    /** El saco pequeno como objeto (para el modo creativo y para colocarlo a mano). */
    public static final RegistryObject<Item> SACK = ITEMS.register("saco",
            () -> new BlockItem(ModBlocks.SACK.get(), new Item.Properties()));

    private ModItems() {
    }
}
