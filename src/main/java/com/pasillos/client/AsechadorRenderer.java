package com.pasillos.client;

import com.pasillos.PasillosMod;
import com.pasillos.entity.Asechador;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class AsechadorRenderer extends HumanoidMobRenderer<Asechador, HumanoidModel<Asechador>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(PasillosMod.MODID, "textures/entity/asechador.png");

    public AsechadorRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(Asechador entity) {
        return TEXTURE;
    }
}
