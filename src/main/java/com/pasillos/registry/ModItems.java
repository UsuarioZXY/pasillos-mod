package com.pasillos.registry;

import com.pasillos.PasillosMod;
import com.pasillos.item.FlashlightItem;
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

    public static final RegistryObject<Item> FLASHLIGHT = ITEMS.register("flashlight",
            () -> new FlashlightItem(new Item.Properties()));

    private ModItems() {
    }
}
