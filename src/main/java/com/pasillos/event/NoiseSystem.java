package com.pasillos.event;

import com.pasillos.PasillosMod;
import com.pasillos.network.SanityNetwork;
import com.pasillos.registry.ModDimensions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * El ruido que haces. Cada jugador tiene un nivel de ruido (0 a 60) que sube al caminar, correr, saltar,
 * caer, romper o poner bloques, y baja rapido en silencio. Agacharse y caminar despacio hace muy poco ruido.
 * El Asechador te oye a (ruido x 0.9) bloques: se acerca, y si haces ruido sostenido aparece antes.
 */
@Mod.EventBusSubscriber(modid = PasillosMod.MODID)
public final class NoiseSystem {

    public static final float SNEAK = 6.0F;
    public static final float WALK = 18.0F;
    public static final float SPRINT = 38.0F;
    public static final float BREAK = 55.0F;
    public static final float PLACE = 40.0F;

    private static final class Data {
        float noise;
        float lastSent = -100.0F;
        double x;
        double z;
        boolean wasOnGround = true;
        float lastFall;
        boolean init;
    }

    private static final Map<UUID, Data> DATA = new HashMap<>();

    private static Data of(ServerPlayer player) {
        return DATA.computeIfAbsent(player.getUUID(), id -> new Data());
    }

    /** Nivel de ruido actual del jugador (0 si no hay datos). */
    public static float get(net.minecraft.world.entity.player.Player player) {
        Data d = DATA.get(player.getUUID());
        return d == null ? 0.0F : d.noise;
    }

    /** Un ruido puntual, como un golpe: sube el nivel de golpe si es mayor que el actual. */
    public static void pulse(ServerPlayer player, float level) {
        Data d = of(player);
        d.noise = Math.max(d.noise, level);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (!ModDimensions.isMod(player.level().dimension())) {
            DATA.remove(player.getUUID());
            return;
        }
        Data d = of(player);
        double dx = player.getX() - d.x;
        double dz = player.getZ() - d.z;
        double speed = d.init ? Math.sqrt(dx * dx + dz * dz) : 0.0D;
        d.x = player.getX();
        d.z = player.getZ();
        d.init = true;

        float target = 0.0F;
        boolean silent = player.isSpectator() || player.getAbilities().flying || player.isPassenger();
        if (!silent && speed > 0.03D) {
            if (player.isSprinting()) {
                target = SPRINT;
            } else if (player.isCrouching()) {
                target = SNEAK;
            } else {
                target = WALK;
            }
            if (player.isInWater()) {
                target *= 0.6F;
            }
        }
        // saltos y caidas
        boolean onGround = player.onGround();
        if (!silent) {
            if (d.wasOnGround && !onGround && player.getDeltaMovement().y > 0.1D) {
                target = Math.max(target, 24.0F);
            }
            if (!d.wasOnGround && onGround) {
                target = Math.max(target, Math.min(60.0F, 12.0F + d.lastFall * 7.0F));
            }
        }
        if (!onGround) {
            d.lastFall = player.fallDistance;
        }
        d.wasOnGround = onGround;

        d.noise = Math.max(target, d.noise - 2.0F);

        // avisa al cliente (para el medidor de ruido) cuando cambia
        if (Math.abs(d.noise - d.lastSent) >= 2.0F || (d.noise == 0.0F && d.lastSent != 0.0F)) {
            d.lastSent = d.noise;
            SanityNetwork.sendNoise(player, d.noise);
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && ModDimensions.isMod(player.level().dimension())) {
            pulse(player, BREAK);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && ModDimensions.isMod(player.level().dimension())) {
            pulse(player, PLACE);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DATA.remove(event.getEntity().getUUID());
    }

    private NoiseSystem() {
    }
}
