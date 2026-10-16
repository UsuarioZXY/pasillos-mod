package com.pasillos.registry;

import com.pasillos.PasillosMod;
import com.pasillos.entity.Asechador;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, PasillosMod.MODID);

    public static final RegistryObject<EntityType<Asechador>> ASECHADOR = ENTITIES.register("asechador",
            () -> EntityType.Builder.of(Asechador::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(PasillosMod.MODID, "asechador").toString()));

    private ModEntities() {
    }
}
