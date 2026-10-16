package com.pasillos.event;

import com.pasillos.DreamTravel;
import com.pasillos.PasillosConfig;
import com.pasillos.PasillosMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Dormir en el mundo normal puede mandarte a los Pasillos.
 * Dormir dentro de los Pasillos (de noche, en una cama) te devuelve.
 */
@Mod.EventBusSubscriber(modid = PasillosMod.MODID)
public final class SleepHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player) || !player.isSleeping()) {
            return;
        }
        // getSleepTimer sube de 0 a 100 mientras duermes; actuamos una sola vez al llegar al valor configurado
        if (player.getSleepTimer() != PasillosConfig.SLEEP_DELAY_TICKS.get()) {
            return;
        }

        if (DreamTravel.isInside(player)) {
            DreamTravel.leave(player);
        } else if (player.level().dimension().equals(Level.OVERWORLD)
                && player.getRandom().nextDouble() < PasillosConfig.SLEEP_CHANCE.get()) {
            DreamTravel.enter(player);
        }
    }

    private SleepHandler() {
    }
}
