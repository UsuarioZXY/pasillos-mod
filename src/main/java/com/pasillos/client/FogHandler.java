package com.pasillos.client;

import com.mojang.blaze3d.shaders.FogShape;
import com.pasillos.PasillosConfig;
import com.pasillos.PasillosMod;
import com.pasillos.registry.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Niebla de los Pasillos: de dia es una bruma amarillenta; segun cae la noche se cierra hasta
 * dejarte ver apenas unos bloques a tu alrededor.
 */
@Mod.EventBusSubscriber(modid = PasillosMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FogHandler {

    // color de la niebla de dia / de noche (RGB 0..1)
    private static final float[] DAY_COLOR = {0.62F, 0.57F, 0.34F};
    private static final float[] NIGHT_COLOR = {0.045F, 0.045F, 0.035F};

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        ClientLevel level = insideLevel();
        if (level == null || event.getType() != FogType.NONE || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) {
            return;
        }

        float partial = (float) event.getPartialTick();
        float night = nightFactor(level, partial);

        float dayFar = PasillosConfig.DAY_FOG_DISTANCE.get().floatValue();
        float nightFar = PasillosConfig.NIGHT_FOG_DISTANCE.get().floatValue();

        // la niebla "respira": avanza y retrocede un poco, solo de noche
        float breath = Mth.sin((level.getGameTime() + partial) * 0.035F) * 0.9F * night;

        float far = Math.max(2.0F, Mth.lerp(night, dayFar, nightFar) + breath);
        float near = Mth.lerp(night, Math.min(8.0F, far * 0.2F), 0.0F);

        event.setNearPlaneDistance(near);
        event.setFarPlaneDistance(far);
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true); // sin esto Forge ignora los cambios
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        ClientLevel level = insideLevel();
        if (level == null) {
            return;
        }
        float night = nightFactor(level, (float) event.getPartialTick());
        event.setRed(Mth.lerp(night, DAY_COLOR[0], NIGHT_COLOR[0]));
        event.setGreen(Mth.lerp(night, DAY_COLOR[1], NIGHT_COLOR[1]));
        event.setBlue(Mth.lerp(night, DAY_COLOR[2], NIGHT_COLOR[2]));
    }

    private static ClientLevel insideLevel() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && level.dimension().equals(ModDimensions.NIVEL_0)) {
            return level;
        }
        return null;
    }

    /** 0 = pleno dia, 1 = noche cerrada, con transicion suave al atardecer y al amanecer. */
    private static float nightFactor(ClientLevel level, float partial) {
        float sunHeight = Mth.cos(level.getTimeOfDay(partial) * Mth.TWO_PI);
        float n = Mth.clamp((0.15F - sunHeight) / 0.45F, 0.0F, 1.0F);
        return n * n * (3.0F - 2.0F * n);
    }

    private FogHandler() {
    }
}
