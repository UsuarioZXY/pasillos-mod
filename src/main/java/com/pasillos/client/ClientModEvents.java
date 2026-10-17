package com.pasillos.client;

import com.pasillos.PasillosMod;
import com.pasillos.item.FlashlightItem;
import com.pasillos.registry.ModEntities;
import com.pasillos.registry.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = PasillosMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(AsechadorModel.LAYER, AsechadorModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ASECHADOR.get(), AsechadorRenderer::new);
    }

    /** Cambia la textura de la linterna segun este encendida o apagada. */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(ModItems.FLASHLIGHT.get(),
                new ResourceLocation(PasillosMod.MODID, "on"),
                (stack, level, entity, seed) -> FlashlightItem.isOn(stack) ? 1.0F : 0.0F));
    }

    private ClientModEvents() {
    }
}
