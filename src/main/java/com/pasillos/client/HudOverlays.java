package com.pasillos.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pasillos.PasillosMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Dos capas de pantalla, solo dentro de la dimension:
 *  - "darkness": de noche oscurece todo. Tambien dibuja el borde rojo pulsante de la ansiedad.
 *  - "sanity_bars": las barras de cordura y ansiedad.
 */
@Mod.EventBusSubscriber(modid = PasillosMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HudOverlays {

    private static final ResourceLocation RED = new ResourceLocation(PasillosMod.MODID, "textures/gui/vignette_red.png");

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.VIGNETTE.id(), "darkness", HudOverlays::renderDarkness);
        event.registerAboveAll("sanity_bars", HudOverlays::renderBars);
    }

    private static void renderDarkness(ForgeGui gui, GuiGraphics g, float partialTick, int w, int h) {
        if (!SanityState.inside) {
            return;
        }
        float night = SanityState.night;

        // 1) oscuridad pareja de noche
        float base = night * 0.42F;
        if (base > 0.01F) {
            g.fill(0, 0, w, h, ((int) (base * 255.0F) << 24));
        }

        // 2) borde rojo que late con la ansiedad
        if (SanityState.anxiety > 45.0F) {
            float k = (SanityState.anxiety - 45.0F) / 55.0F;
            float time = Minecraft.getInstance().level != null
                    ? Minecraft.getInstance().level.getGameTime() + partialTick : 0.0F;
            float pulse = 0.5F + 0.5F * Mth.sin(time * (0.35F + k * 0.25F));
            int size = Math.max(w, h);
            drawTexture(g, RED, w / 2 - size / 2, h / 2 - size / 2, size, 1.0F, 1.0F, 1.0F, k * (0.3F + 0.5F * pulse));
        }
    }

    private static void drawTexture(GuiGraphics g, ResourceLocation texture, int x, int y, int size,
                                    float r, float gr, float b, float alpha) {
        if (alpha <= 0.005F) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(r, gr, b, Mth.clamp(alpha, 0.0F, 1.0F));
        g.blit(texture, x, y, size, size, 0.0F, 0.0F, 256, 256, 256, 256);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderBars(ForgeGui gui, GuiGraphics g, float partialTick, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (!SanityState.inside || mc.options.hideGui) {
            return;
        }
        float time = mc.level != null ? mc.level.getGameTime() + partialTick : 0.0F;

        int x = 10;
        int y = 10;
        int width = 100;

        // cordura: se apaga a destellos cuando esta muy baja
        float sanity = SanityState.sanity;
        int sanityColor = 0xFFB8D4FF;
        if (sanity < 25.0F && Mth.sin(time * 0.8F) > 0.0F) {
            sanityColor = 0xFF5F7AA8;
        }
        g.drawString(mc.font, Component.translatable("hud.pasillos.sanity"), x, y, 0xFFE8EEFF, true);
        drawBar(g, x, y + 11, width, sanity / 100.0F, sanityColor);

        // ansiedad: la barra tiembla cuando es muy alta
        float anxiety = SanityState.anxiety;
        int shake = anxiety > 70.0F ? (int) (Mth.sin(time * 2.1F) * 1.5F) : 0;
        g.drawString(mc.font, Component.translatable("hud.pasillos.anxiety"), x + shake, y + 26, 0xFFFFC8C8, true);
        drawBar(g, x + shake, y + 37, width, anxiety / 100.0F, 0xFFD83A3A);
    }

    private static void drawBar(GuiGraphics g, int x, int y, int width, float fraction, int color) {
        g.fill(x - 1, y - 1, x + width + 1, y + 8, 0xFF000000);
        g.fill(x, y, x + width, y + 7, 0xFF2A2A2A);
        int filled = (int) (width * Mth.clamp(fraction, 0.0F, 1.0F));
        if (filled > 0) {
            g.fill(x, y, x + filled, y + 7, color);
        }
    }

    private HudOverlays() {
    }
}
