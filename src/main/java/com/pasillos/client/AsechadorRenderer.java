package com.pasillos.client;

import com.pasillos.PasillosMod;
import com.pasillos.entity.Asechador;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

public class AsechadorRenderer extends MobRenderer<Asechador, AsechadorModel> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(PasillosMod.MODID, "textures/entity/asechador.png");
    private static final ResourceLocation EYES =
            new ResourceLocation(PasillosMod.MODID, "textures/entity/asechador_eyes.png");

    public AsechadorRenderer(EntityRendererProvider.Context context) {
        super(context, new AsechadorModel(context.bakeLayer(AsechadorModel.LAYER)), 0.0F);
        this.addLayer(new EyesGlow(this));
    }

    @Override
    public ResourceLocation getTextureLocation(Asechador entity) {
        return TEXTURE;
    }

    /** Los ojos brillan con su propia luz, incluso en plena niebla negra. */
    private static class EyesGlow extends EyesLayer<Asechador, AsechadorModel> {
        EyesGlow(RenderLayerParent<Asechador, AsechadorModel> parent) {
            super(parent);
        }

        @Override
        public RenderType renderType() {
            return RenderType.eyes(EYES);
        }
    }
}
