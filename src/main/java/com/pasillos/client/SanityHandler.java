package com.pasillos.client;

import com.pasillos.PasillosConfig;
import com.pasillos.PasillosMod;
import com.pasillos.entity.Asechador;
import com.pasillos.entity.Ground;
import com.pasillos.network.PanicMessage;
import com.pasillos.network.SanityNetwork;
import com.pasillos.registry.ModDimensions;
import com.pasillos.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Calcula cada tick la cordura y la ansiedad del jugador dentro de los niveles y aplica sus efectos:
 * latidos, sonidos que no existen, temblor de camara, pensamientos, alucinaciones y, si es demasiado,
 * ataques al corazon. Que el Asechador te mire te quita cordura y te sube la ansiedad.
 */
@Mod.EventBusSubscriber(modid = PasillosMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SanityHandler {

    /** Distancia a la que se ve al Asechador de noche; coincide con HideAndWatchGoal. */
    private static final double NIGHT_VISIBLE = 30.0D;
    /** Nivel de luz de bloque a partir del cual se considera que estas "a la luz". */
    private static final int LIT_LEVEL = 8;
    /** Distancia maxima a la que un Asechador que te mira te afecta. */
    private static final double WATCH_DIST = 48.0D;

    private static final String[] LOW_SANITY_THOUGHTS = {
            "thought.pasillos.low.0", "thought.pasillos.low.1", "thought.pasillos.low.2",
            "thought.pasillos.low.3", "thought.pasillos.low.4"};

    private static int heartbeatTimer;
    private static int presenceCooldown;
    private static int panicTicks;
    private static int noiseThoughtCooldown;
    private static int dogThoughtCooldown;
    private static int hallucinationTimer;
    private static int thoughtTimer;
    private static int nextFakeId = -3_000_000;
    private static ResourceKey<Level> lastDimension;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null || !ModDimensions.isMod(level.dimension())) {
            SanityState.reset();
            lastDimension = null;
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        if (!SanityState.inside) {
            SanityState.reset();
            SanityState.inside = true;
            heartbeatTimer = 0;
            panicTicks = 0;
            hallucinationTimer = 400;
            thoughtTimer = 600;
            lastDimension = null;
        }
        if (!level.dimension().equals(lastDimension)) {
            lastDimension = level.dimension();
            presenceCooldown = 300;
            if (level.dimension().equals(ModDimensions.NIVEL_MENOS_1)) {
                // acabas de caer a la cueva: te preguntas donde estas
                SanityState.say("thought.pasillos.depth.0");
                SanityState.say("thought.pasillos.depth.1");
                SanityState.say("thought.pasillos.depth.2");
            }
        }
        SanityState.tickThought();
        if (SanityState.noiseTarget >= SanityState.noiseShown) {
            SanityState.noiseShown = SanityState.noiseTarget;
        } else {
            SanityState.noiseShown = Math.max(SanityState.noiseTarget, SanityState.noiseShown - 1.5F);
        }
        if (noiseThoughtCooldown > 0) {
            noiseThoughtCooldown--;
        }

        float night = FogHandler.nightFactor(level, 1.0F);
        SanityState.night = night;
        boolean lit = level.getBrightness(LightLayer.BLOCK, player.blockPosition()) >= LIT_LEVEL;
        boolean darkness = night > 0.4F && !lit;

        // --- el Asechador (real) mas cercano y si lo estoy viendo o me esta mirando ---
        List<Asechador> stalkers = level.getEntitiesOfClass(Asechador.class, player.getBoundingBox().inflate(64.0D));
        stalkers.removeIf(Asechador::isHallucination);
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

        // --- el Ciego (perro sin ojos): cuanto mas cerca, mas miedo; mucho mas si te esta cazando ---
        float dogK = 0.0F;
        boolean dogHunting = false;
        for (com.pasillos.entity.Ciego dog : level.getEntitiesOfClass(com.pasillos.entity.Ciego.class,
                player.getBoundingBox().inflate(32.0D))) {
            dogK = Math.max(dogK, (float) (1.0D - dog.distanceTo(player) / 32.0D));
            dogHunting |= dog.isHunting();
        }
        if (dogThoughtCooldown > 0) {
            dogThoughtCooldown--;
        }
        if (dogK > 0.0F && dogThoughtCooldown <= 0) {
            SanityState.say("thought.pasillos.dog");
            dogThoughtCooldown = 3000;
        }
        SanityState.watched = watched;
        SanityState.seen = seen;
        float nearK = stalkerNear ? (float) (1.0D - Math.min(nearest, 48.0D) / 48.0D) : 0.0F;
        // con la mente rota, si de verdad hay algo cerca te afecta todavia mas
        float insane = SanityState.sanity < 30.0F ? 1.4F : 1.0F;
        float oldSanity = SanityState.sanity;
        float oldAnxiety = SanityState.anxiety;

        // --- mensaje de presencia ---
        if (presenceCooldown > 0) {
            presenceCooldown--;
        }
        if (nearest < 64.0D && presenceCooldown <= 0) {
            SanityState.say("message.pasillos.presence");
            presenceCooldown = 2400 + player.getRandom().nextInt(1200);
            level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_NEARBY_CLOSE,
                    SoundSource.AMBIENT, 0.5F, 0.6F, false);
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
            delta -= (0.035F + 0.04F * nearK) * insane;
        }
        if (seen) {
            delta -= 0.12F;
        }
        if (!darkness && !stalkerNear) {
            delta += night < 0.2F ? 0.03F : 0.015F;
        }
        delta -= 0.015F * dogK * (dogHunting ? 2.5F : 1.0F);
        SanityState.sanity = Mth.clamp(SanityState.sanity + delta, 0.0F, 100.0F);

        // hacer ruido con el Asechador cerca te pone nervioso
        if (stalkerNear && SanityState.noiseShown >= 30.0F) {
            if (noiseThoughtCooldown <= 0 && nearest < 55.0D) {
                SanityState.say("thought.pasillos.noise");
                noiseThoughtCooldown = 1800;
            }
        }

        // --- ansiedad: se acerca a un objetivo (sube rapido, baja despacio) ---
        float target = night * (lit ? 8.0F : 18.0F) + (100.0F - SanityState.sanity) * 0.35F;
        if (stalkerNear) {
            target += 50.0F * nearK2(nearest) * insane;
            if (seen) {
                target += 30.0F;
            }
        }
        if (watched) {
            target += (22.0F + 18.0F * nearK) * insane;
        }
        if (stalkerNear) {
            target += SanityState.noiseShown * 0.3F * nearK2(nearest);
        }
        target += 35.0F * dogK * (dogHunting ? 1.5F : 1.0F);
        target = Mth.clamp(target, 0.0F, 100.0F);
        float rate = target > SanityState.anxiety ? (watched ? 0.05F : 0.03F) : 0.01F;
        SanityState.anxiety = Mth.clamp(SanityState.anxiety + (target - SanityState.anxiety) * rate, 0.0F, 100.0F);

        handleHallucinations(level, player, view);
        handlePanic(player);

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

    private static float nearK2(double nearest) {
        return (float) (1.0D - Math.min(nearest, 64.0D) / 64.0D);
    }

    private static float approach(float value, float target, float step) {
        if (value < target) {
            return Math.min(target, value + step);
        }
        return Math.max(target, value - step);
    }

    // ------------------------------------------------------------------------------------------
    // Ataque al corazon, mareos y temblores
    // ------------------------------------------------------------------------------------------

    private static void handlePanic(LocalPlayer player) {
        float anxiety = SanityState.anxiety;
        float sanity = SanityState.sanity;

        // segundos seguidos en panico (ansiedad al 100 %)
        if (anxiety >= 99.0F) {
            panicTicks++;
        } else if (anxiety < 90.0F) {
            panicTicks = 0;
        } else if (panicTicks > 0) {
            panicTicks--;
        }
        if (panicTicks == 30) {
            SanityState.say("thought.pasillos.panic");
        }
        if (panicTicks > 0 && panicTicks % 20 == 0) {
            double chance = PasillosConfig.HEART_ATTACK_CHANCE.get() * (panicTicks / 20);
            if (chance > 0.0D && player.getRandom().nextDouble() < chance) {
                SanityNetwork.sendPanic(PanicMessage.HEART_ATTACK);
                SanityState.say("thought.pasillos.heart");
                SanityState.hurtFlash = 20;
                panicTicks = 0;
                return;
            }
        }

        // con muy poca cordura o mucha ansiedad pasan cosas malas de vez en cuando
        if (player.tickCount % 20 == 0) {
            if (sanity < 25.0F && player.getRandom().nextInt(45) == 0) {
                SanityNetwork.sendPanic(PanicMessage.BREAKDOWN);
            } else if (anxiety >= 75.0F && player.getRandom().nextInt(50) == 0) {
                SanityNetwork.sendPanic(PanicMessage.TREMOR);
                SanityState.say("thought.pasillos.tremor");
            }
        }

        // pensamientos sueltos cuando la mente flaquea
        if (sanity < 50.0F) {
            if (--thoughtTimer <= 0) {
                SanityState.say(LOW_SANITY_THOUGHTS[player.getRandom().nextInt(LOW_SANITY_THOUGHTS.length)]);
                thoughtTimer = (int) Mth.lerp(sanity / 50.0F, 500.0F, 1300.0F) + player.getRandom().nextInt(400);
            }
        } else {
            thoughtTimer = Math.max(thoughtTimer, 400);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Alucinaciones: un Asechador falso que solo ves tu y que se esfuma al mirarlo
    // ------------------------------------------------------------------------------------------

    private static void handleHallucinations(ClientLevel level, LocalPlayer player, Vec3 view) {
        List<Asechador> all = level.getEntitiesOfClass(Asechador.class, player.getBoundingBox().inflate(80.0D));
        int active = 0;
        for (Asechador a : all) {
            if (!a.isHallucination()) {
                continue;
            }
            active++;
            a.setHallucinationLife(a.getHallucinationLife() - 1);
            // siempre te mira
            double dx = player.getX() - a.getX();
            double dz = player.getZ() - a.getZ();
            float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
            a.setYRot(yaw);
            a.yRotO = yaw;
            a.setYBodyRot(yaw);
            a.setYHeadRot(yaw);

            Vec3 toIt = a.getEyePosition().subtract(player.getEyePosition()).normalize();
            boolean looked = view.dot(toIt) > 0.95D && player.hasLineOfSight(a);
            if (looked || a.getHallucinationLife() <= 0) {
                if (looked) {
                    // te asustas aunque no sea real
                    SanityState.anxiety = Mth.clamp(SanityState.anxiety + 9.0F, 0.0F, 100.0F);
                    SanityState.sanity = Mth.clamp(SanityState.sanity - 3.0F, 0.0F, 100.0F);
                    SanityState.hurtFlash = 10;
                    level.playLocalSound(a.getX(), a.getY(), a.getZ(), SoundEvents.ENDERMAN_STARE,
                            SoundSource.HOSTILE, 0.9F, 0.5F, false);
                }
                for (int i = 0; i < 18; i++) {
                    level.addParticle(ParticleTypes.LARGE_SMOKE, a.getX() + (level.random.nextDouble() - 0.5D),
                            a.getY() + level.random.nextDouble() * 3.5D, a.getZ() + (level.random.nextDouble() - 0.5D),
                            0.0D, 0.02D, 0.0D);
                }
                a.discard();
            }
        }

        if (SanityState.sanity >= 35.0F) {
            hallucinationTimer = Math.max(hallucinationTimer, 300);
            return;
        }
        if (active == 0 && --hallucinationTimer <= 0) {
            spawnHallucination(level, player);
            hallucinationTimer = (int) Mth.lerp(SanityState.sanity / 35.0F, 250.0F, 800.0F)
                    + player.getRandom().nextInt(300);
        }
    }

    /** Coloca un Asechador falso a 18-30 bloques, a tu espalda, para que lo veas al girarte. */
    private static void spawnHallucination(ClientLevel level, LocalPlayer player) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double back = Math.toRadians(player.getYRot() + 90.0F) + Math.PI; // direccion a la espalda
            double angle = back + (player.getRandom().nextDouble() - 0.5D) * Math.toRadians(150.0D);
            double dist = 18.0D + player.getRandom().nextDouble() * 12.0D;
            int x = Mth.floor(player.getX() + Math.cos(angle) * dist);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * dist);
            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            int y = Ground.standY(level, x, z, player.getBlockY());
            if (y == Ground.NONE) {
                continue;
            }
            Asechador fake = new Asechador(ModEntities.ASECHADOR.get(), level);
            fake.makeHallucination(160 + player.getRandom().nextInt(160));
            fake.moveTo(x + 0.5D, y, z + 0.5D, 0.0F, 0.0F);
            fake.setState(player.getRandom().nextInt(3) == 0 ? Asechador.STATE_CROUCH : Asechador.STATE_WATCH);
            level.putNonPlayerEntity(nextFakeId--, fake);
            return;
        }
    }

    // ------------------------------------------------------------------------------------------

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
