package com.pasillos.client;

import com.pasillos.PasillosMod;
import com.pasillos.entity.Ciego;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class CiegoRenderer extends MobRenderer<Ciego, CiegoModel> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(PasillosMod.MODID, "textures/entity/ciego.png");

    public CiegoRenderer(EntityRendererProvider.Context context) {
        super(context, new CiegoModel(context.bakeLayer(CiegoModel.LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(Ciego entity) {
        return TEXTURE;
    }
}
