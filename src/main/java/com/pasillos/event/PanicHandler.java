package com.pasillos.event;

import com.pasillos.network.PanicMessage;
import com.pasillos.network.SanityNetwork;
import com.pasillos.registry.ModDimensions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Lado del servidor de las cosas malas que pasan con la cordura baja o la ansiedad alta. */
public final class PanicHandler {

    private static final Map<UUID, Long> LAST = new HashMap<>();

    public static void apply(ServerPlayer player, int kind) {
        if (!ModDimensions.isMod(player.level().dimension()) || player.isCreative() || player.isSpectator()) {
            return;
        }
        // evita abusos: cada tipo tiene su propia espera minima
        long now = player.level().getGameTime();
        long key = kind;
        long minGap = kind == PanicMessage.HEART_ATTACK ? 600L : 400L;
        Long last = LAST.get(new UUID(player.getUUID().getMostSignificantBits() ^ key, player.getUUID().getLeastSignificantBits()));
        if (last != null && now - last < minGap) {
            return;
        }
        LAST.put(new UUID(player.getUUID().getMostSignificantBits() ^ key, player.getUUID().getLeastSignificantBits()), now);

        switch (kind) {
            case PanicMessage.HEART_ATTACK -> {
                // un golpe fuerte que nunca mata: te deja como minimo con medio corazon
                float damage = Math.min(14.0F, player.getHealth() - 1.0F);
                if (damage > 0.0F) {
                    player.hurt(player.damageSources().generic(), damage);
                }
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 220, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 1, false, false));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.5F, 0.6F);
                // tras el colapso la ansiedad baja, pero la mente queda tocada
                SanityNetwork.send(player, -20.0F, -45.0F);
            }
            case PanicMessage.BREAKDOWN -> {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 140, 0, false, false));
            }
            case PanicMessage.TREMOR -> {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 240, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 240, 0, false, false));
            }
            default -> {
            }
        }
    }

    private PanicHandler() {
    }
}
