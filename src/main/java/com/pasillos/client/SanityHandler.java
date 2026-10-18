package com.pasillos.client;

import com.pasillos.PasillosMod;
import com.pasillos.entity.Asechador;
import com.pasillos.registry.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Calcula cada tick la cordura y la ansiedad del jugador dentro de la dimension y aplica sus efectos:
 * latidos, sonidos que no existen y temblor de camara. La luz de bloques (antorchas, faroles...) te calma.
 */
@Mod.EventBusSubscriber(modid = PasillosMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SanityHandler {

    /** Distancia a la que se ve al Asechador de noche; coincide con HideAndWatchGoal. */
    private static final double NIGHT_VISIBLE = 30.0D;
    /** Nivel de luz de bloque a partir del cual se considera que estas "a la luz". */
    private static final int LIT_LEVEL = 8;

    /** Distancia maxima a la que un Asechador que te mira te afecta. */
    private static final double WATCH_DIST = 48.0D;

    private static int heartbeatTimer;
    private static int presenceCooldown;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null || !level.dimension().equals(ModDimensions.NIVEL_0)) {
            SanityState.reset();
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        if (!SanityState.inside) {
            SanityState.reset();
            SanityState.inside = true;
            heartbeatTimer = 0;
            presenceCooldown = 200;
        }

        float night = FogHandler.nightFactor(level, 1.0F);
        SanityState.night = night;
        boolean lit = level.getBrightness(LightLayer.BLOCK, player.blockPosition()) >= LIT_LEVEL;
        boolean darkness = night > 0.4F && !lit;

        // --- el Asechador mas cercano y si lo estoy viendo ---
        List<Asechador> stalkers = level.getEntitiesOfClass(Asechador.class, player.getBoundingBox().inflate(64.0D));
        double nearest = Double.MAX_VALUE;
        boolean seen = false;
        boolean watched = false;
        double visible = night > 0.4F ? NIGHT_VISIBLE : 64.0D;
        Vec3 view = player.getViewVector(1.0F).normalize();
        for (Asechador stalker : stalkers) {
            double dist = stalker.distanceTo(player);
            nearest = Math.min(nearest, dist);
            Vec3 toStalker = stalker.getEyePosition().subtract(player.getEyePosition()).normalize();
            if (dist <= WATCH_DIST && stalker.hasLineOfSight(player)) {
                watched = true;
            }
            if (dist <= visible && view.dot(toStalker) > 0.95D && player.hasLineOfSight(stalker)) {
                seen = true;
            }
        }
        boolean stalkerNear = nearest < 64.0D;
        SanityState.watched = watched;
        SanityState.seen = seen;
        float nearK = stalkerNear ? (float) (1.0D - Math.min(nearest, 48.0D) / 48.0D) : 0.0F;
        float oldSanity = SanityState.sanity;
        float oldAnxiety = SanityState.anxiety;

        // --- mensaje de presencia ---
        if (presenceCooldown > 0) {
            presenceCooldown--;
        }
        if (nearest < 64.0D && presenceCooldown <= 0) {
            SanityState.presenceTicks = SanityState.PRESENCE_TOTAL;
            presenceCooldown = 2400 + player.getRandom().nextInt(1200);
            level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_NEARBY_CLOSE,
                    SoundSource.AMBIENT, 0.5F, 0.6F, false);
        }
        if (SanityState.presenceTicks > 0) {
            SanityState.presenceTicks--;
        }

        // --- cordura ---
        float delta = 0.0F;
        if (darkness) {
            delta -= 0.007F * night;
        }
        if (stalkerNear) {
            delta -= 0.03F * (float) (1.0D - nearest / 64.0D);
        }
        if (watched) {
            delta -= 0.035F + 0.04F * nearK;
        }
        if (seen) {
            delta -= 0.12F;
        }
        if (!darkness && !stalkerNear) {
            delta += night < 0.2F ? 0.03F : 0.015F;
        }
        SanityState.sanity = Mth.clamp(SanityState.sanity + delta, 0.0F, 100.0F);

        // --- ansiedad: se acerca a un objetivo (sube rapido, baja despacio) ---
        float target = night * (lit ? 8.0F : 18.0F) + (100.0F - SanityState.sanity) * 0.35F;
        if (stalkerNear) {
            target += 50.0F * (float) (1.0D - nearest / 64.0D);
            if (seen) {
                target += 30.0F;
            }
        }
        if (watched) {
            target += 22.0F + 18.0F * nearK;
        }
        target = Mth.clamp(target, 0.0F, 100.0F);
        float rate = target > SanityState.anxiety ? (watched ? 0.05F : 0.03F) : 0.01F;
        SanityState.anxiety = Mth.clamp(SanityState.anxiety + (target - SanityState.anxiety) * rate, 0.0F, 100.0F);

        // --- tendencias, estela de las barras y destellos ---
        float dS = SanityState.sanity - oldSanity;
        float dA = SanityState.anxiety - oldAnxiety;
        SanityState.sanityTrend += (dS * 20.0F - SanityState.sanityTrend) * 0.05F;
        SanityState.anxietyTrend += (dA * 20.0F - SanityState.anxietyTrend) * 0.05F;
        SanityState.sanityGhost = approach(SanityState.sanityGhost, SanityState.sanity, 0.25F);
        SanityState.anxietyGhost = approach(SanityState.anxietyGhost, SanityState.anxiety, 0.25F);
        if (dS < -0.06F) {
            SanityState.hurtFlash = 8;
        } else if (SanityState.hurtFlash > 0) {
            SanityState.hurtFlash--;
        }
        if (SanityState.sanityHeal > 0) {
            SanityState.sanityHeal--;
        }
        if (SanityState.anxietyHeal > 0) {
            SanityState.anxietyHeal--;
        }

        applyEffects(level, player);
    }

    private static float approach(float value, float target, float step) {
        if (value < target) {
            return Math.min(target, value + step);
        }
        return Math.max(target, value - step);
    }

    private static void applyEffects(ClientLevel level, LocalPlayer player) {
        float anxiety = SanityState.anxiety;
        float sanity = SanityState.sanity;

        // latidos, mas rapidos y fuertes cuanto mas ansiedad
        if (anxiety > 45.0F) {
            if (--heartbeatTimer <= 0) {
                float k = (anxiety - 45.0F) / 55.0F;
                level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT,
                        SoundSource.PLAYERS, 0.4F + k * 0.6F, 0.9F + k * 0.3F, false);
                heartbeatTimer = (int) Mth.lerp(k, 44.0F, 16.0F);
            }
        } else {
            heartbeatTimer = 0;
        }

        // con poca cordura se oyen cosas que no estan
        if (sanity < 45.0F && player.getRandom().nextInt(400) == 0) {
            SoundEvent[] sounds = {SoundEvents.WARDEN_NEARBY_CLOSE, SoundEvents.ENDERMAN_STARE};
            SoundEvent sound = sounds[player.getRandom().nextInt(sounds.length)];
            double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
            level.playLocalSound(player.getX() + Math.cos(angle) * 8.0D, player.getY(),
                    player.getZ() + Math.sin(angle) * 8.0D, sound, SoundSource.AMBIENT,
                    0.5F, 0.7F + player.getRandom().nextFloat() * 0.3F, false);
        }
    }

    /** Temblor de camara con la ansiedad alta. */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!SanityState.inside || SanityState.anxiety < 40.0F) {
            return;
        }
        float k = (SanityState.anxiety - 40.0F) / 60.0F;
        k *= k;
        float t = (Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0L)
                + (float) event.getPartialTick();
        event.setRoll(event.getRoll() + Mth.sin(t * 0.9F) * 0.9F * k);
        event.setPitch(event.getPitch() + Mth.sin(t * 1.7F) * 0.35F * k);
        event.setYaw(event.getYaw() + Mth.sin(t * 1.3F) * 0.35F * k);
    }

    private SanityHandler() {
    }
}
