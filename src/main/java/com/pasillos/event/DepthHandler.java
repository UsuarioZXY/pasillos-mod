package com.pasillos.event;

import com.pasillos.DreamTravel;
import com.pasillos.PasillosConfig;
import com.pasillos.PasillosMod;
import com.pasillos.registry.ModDimensions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Si bajas hasta el fondo del mundo (Overworld) o hasta el suelo de bedrock del Nivel 0, caes al Nivel -1. */
@Mod.EventBusSubscriber(modid = PasillosMod.MODID)
public final class DepthHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 10 != 0 || player.isSpectator() || player.isSleeping()) {
            return;
        }
        Level level = player.level();
        if (level.dimension().equals(Level.OVERWORLD)) {
            if (player.getY() <= PasillosConfig.DEPTH_TRIGGER_OVERWORLD.get()) {
                DreamTravel.enterDepth(player);
            }
        } else if (level.dimension().equals(ModDimensions.NIVEL_0)) {
            if (player.getY() <= PasillosConfig.DEPTH_TRIGGER_NIVEL0.get()) {
                DreamTravel.enterDepth(player);
            }
        }
    }

    private DepthHandler() {
    }
}
