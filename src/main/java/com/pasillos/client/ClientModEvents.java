package com.pasillos.client;

import com.pasillos.PasillosMod;
import com.pasillos.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PasillosMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(AsechadorModel.LAYER, AsechadorModel::createBodyLayer);
        event.registerLayerDefinition(CiegoModel.LAYER, CiegoModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ASECHADOR.get(), AsechadorRenderer::new);
        event.registerEntityRenderer(ModEntities.CIEGO.get(), CiegoRenderer::new);
    }

    private ClientModEvents() {
    }
}
