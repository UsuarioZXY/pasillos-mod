package com.pasillos.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pasillos.PasillosMod;
import net.minecraft.ChatFormatting;
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

    private static final ResourceLocation SANITY_ICON = new ResourceLocation(PasillosMod.MODID, "textures/gui/icon_sanity.png");
    private static final ResourceLocation ANXIETY_ICON = new ResourceLocation(PasillosMod.MODID, "textures/gui/icon_anxiety.png");
    private static final int BAR_WIDTH = 100;
    private static final ResourceLocation RED = new ResourceLocation(PasillosMod.MODID, "textures/gui/vignette_red.png");

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.VIGNETTE.id(), "darkness", HudOverlays::renderDarkness);
        event.registerAboveAll("sanity_bars", HudOverlays::renderBars);
        event.registerAboveAll("presence_message", HudOverlays::renderPresence);
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

        // cordura: parpadea si esta muy baja y el icono late en rojo si te miran
        float sanity = SanityState.sanity;
        int sColor = 0xFFB8D4FF;
        boolean blink = sanity < 25.0F && Mth.sin(time * 0.8F) > 0.0F;
        if (blink) {
            sColor = 0xFF5F7AA8;
        }
        int sFlash = SanityState.sanityHeal > 0 ? 0xFF9FFFE0 : SanityState.hurtFlash > 0 ? 0xFFFF7070 : 0;
        boolean iconOff = sanity < 25.0F && Mth.sin(time * 1.9F) > 0.7F;
        float iconR = 1.0F;
        float iconG = 1.0F;
        float iconB = 1.0F;
        if (SanityState.watched) {
            float p = 0.5F + 0.5F * Mth.sin(time * 0.6F);
            iconG = 1.0F - 0.6F * p;
            iconB = 1.0F - 0.6F * p;
        }
        drawEntry(g, mc, x, y, SANITY_ICON, iconOff, iconR, iconG, iconB, "hud.pasillos.sanity", 0xFFE8EEFF,
                stateWord("hud.pasillos.sanity.state.", sanity, true), sanity, SanityState.sanityGhost,
                SanityState.sanityTrend, true, sColor, sFlash, 0);

        // ansiedad: la barra tiembla con mucha y pulsa en rojo en el panico
        float anxiety = SanityState.anxiety;
        int shake = anxiety > 70.0F ? (int) (Mth.sin(time * 2.1F) * 1.5F) : 0;
        int aColor = 0xFFD83A3A;
        if (anxiety >= 90.0F && Mth.sin(time * 0.9F) > 0.0F) {
            aColor = 0xFFFF6A5A;
        }
        int aFlash = SanityState.anxietyHeal > 0 ? 0xFF9FFFE0 : 0;
        drawEntry(g, mc, x + shake, y + 36, ANXIETY_ICON, false, 1.0F, 1.0F, 1.0F, "hud.pasillos.anxiety", 0xFFFFC8C8,
                stateWord("hud.pasillos.anxiety.state.", anxiety, false), anxiety, SanityState.anxietyGhost,
                SanityState.anxietyTrend, false, aColor, aFlash, 0);

        drawNoise(g, mc, x, y + 72, SanityState.noiseShown);
    }

    /** Medidor de ruido: cuanto se te oye ahora mismo (el Asechador te oye a unos 0,9 x ruido bloques). */
    private static void drawNoise(GuiGraphics g, Minecraft mc, int x, int y, float noise) {
        int width = BAR_WIDTH;
        g.fill(x - 3, y - 3, x + width + 4, y + 22, 0x66000000);
        g.drawString(mc.font, Component.translatable("hud.pasillos.noise"), x, y + 1, 0xFFD8E8D0, true);
        int idx = noise < 4.0F ? 0 : noise < 20.0F ? 1 : noise < 40.0F ? 2 : 3;
        Component word = Component.translatable("hud.pasillos.noise.state." + idx);
        g.drawString(mc.font, word, x + width - mc.font.width(word), y + 1, 0xFFB0B0B0, false);
        int by = y + 11;
        g.fill(x - 1, by - 1, x + width + 1, by + 6, 0xFF000000);
        g.fill(x, by, x + width, by + 5, 0xFF262626);
        int filled = (int) (width * Mth.clamp(noise / 60.0F, 0.0F, 1.0F));
        int color = noise < 20.0F ? 0xFF6FD07A : noise < 40.0F ? 0xFFE0C050 : 0xFFE05A4A;
        if (filled > 0) {
            g.fill(x, by, x + filled, by + 5, color);
            g.fill(x, by, x + filled, by + 1, 0x55FFFFFF);
        }
    }

    /** Palabra de estado segun el valor. Para la cordura, menos es peor; para la ansiedad, mas es peor. */
    private static Component stateWord(String key, float value, boolean goodHigh) {
        float bad = goodHigh ? 100.0F - value : value;
        int idx = bad >= 75.0F ? 3 : bad >= 50.0F ? 2 : bad >= 25.0F ? 1 : 0;
        return Component.translatable(key + idx);
    }

    private static void drawEntry(GuiGraphics g, Minecraft mc, int x, int y, ResourceLocation icon, boolean iconOff,
                                  float ir, float ig, float ib, String labelKey, int labelColor, Component state,
                                  float value, float ghost, float trend, boolean goodHigh, int color, int flash,
                                  int unused) {
        int width = BAR_WIDTH;
        // panel de fondo
        g.fill(x - 3, y - 3, x + width + 4, y + 30, 0x66000000);

        // icono + nombre + estado
        if (!iconOff) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            g.setColor(ir, ig, ib, 1.0F);
            g.blit(icon, x, y, 0.0F, 0.0F, 9, 9, 9, 9);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        g.drawString(mc.font, Component.translatable(labelKey), x + 12, y + 1, labelColor, true);
        int pct = Math.round(value);
        String pctText = pct + "%";
        g.drawString(mc.font, pctText, x + width - mc.font.width(pctText), y + 1, 0xFFFFFFFF, true);

        // flecha de tendencia (verde si mejora, roja si empeora)
        float good = goodHigh ? trend : -trend;
        int arrowX = x + width - mc.font.width(pctText) - 10;
        if (good > 0.25F) {
            g.drawString(mc.font, "\u25B2", arrowX, y + 1, 0xFF7CFFB0, false);
        } else if (good < -0.25F) {
            g.drawString(mc.font, "\u25BC", arrowX, y + 1, 0xFFFF6B6B, false);
        }

        // barra
        int by = y + 12;
        g.fill(x - 1, by - 1, x + width + 1, by + 8, 0xFF000000);
        g.fill(x, by, x + width, by + 7, 0xFF262626);
        int filled = (int) (width * Mth.clamp(value / 100.0F, 0.0F, 1.0F));
        int ghostPx = (int) (width * Mth.clamp(ghost / 100.0F, 0.0F, 1.0F));
        // estela: lo que acabas de perder (o ganar) se desvanece detras
        if (ghostPx > filled) {
            g.fill(x + filled, by, x + ghostPx, by + 7, 0x88FFFFFF);
        }
        if (filled > 0) {
            g.fill(x, by, x + filled, by + 7, color);
            g.fill(x, by, x + filled, by + 1, 0x55FFFFFF);        // brillo arriba
            g.fill(x, by + 6, x + filled, by + 7, 0x44000000);    // sombra abajo
            g.fill(x + filled - 1, by, x + filled, by + 7, 0xAAFFFFFF); // marca del final
        }
        if (flash != 0) {
            g.fill(x, by, x + width, by + 7, (flash & 0x00FFFFFF) | 0x55000000);
        }
        // marcas en 25, 50 y 75
        for (int q = 1; q <= 3; q++) {
            int tx = x + width * q / 4;
            g.fill(tx, by + 5, tx + 1, by + 7, 0x99000000);
        }

        // palabra de estado
        g.drawString(mc.font, state, x, y + 21, 0xFFB0B0B0, false);
    }

    /** Mensaje "Hay una presencia extraña al alrededor..." encima de la barra de objetos. */
    private static void renderPresence(ForgeGui gui, GuiGraphics g, float partialTick, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (!SanityState.inside || mc.options.hideGui || SanityState.thoughtTicks <= 0) {
            return;
        }
        int total = SanityState.THOUGHT_TOTAL;
        float t = total - SanityState.thoughtTicks + partialTick; // ticks desde que empezo
        float alpha = Math.min(1.0F, t / 14.0F);
        if (SanityState.thoughtTicks < 40) {
            alpha = Math.min(alpha, SanityState.thoughtTicks / 40.0F);
        }
        if (alpha < 0.06F) {
            return; // la fuente trata alfa muy bajo como opaco
        }
        float jitter = t < 20.0F ? (20.0F - t) / 20.0F : 0.0F;
        float time = mc.level != null ? mc.level.getGameTime() + partialTick : 0.0F;
        float ox = Mth.sin(time * 3.1F) * 1.5F * jitter;
        float oy = Mth.cos(time * 2.3F) * 1.0F * jitter;

        Component text = Component.translatable(SanityState.thoughtKey).withStyle(ChatFormatting.ITALIC);
        int a = Mth.clamp((int) (alpha * 255.0F), 4, 255);
        float scale = 1.15F;
        int tw = mc.font.width(text);
        g.pose().pushPose();
        g.pose().translate(w / 2.0F + ox, h - 70.0F + oy, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(mc.font, text, -tw / 2, 0, (a << 24) | 0xC8C8D8, true);
        g.pose().popPose();
    }

    private HudOverlays() {
    }
}
