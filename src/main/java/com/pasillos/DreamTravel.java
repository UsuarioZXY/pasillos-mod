package com.pasillos;

import com.pasillos.registry.ModDimensions;
import com.pasillos.worldgen.LiminalChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;

/** Logica de viaje entre el mundo normal y los Pasillos. */
public final class DreamTravel {

    private static final String RETURN_TAG = "pasillos_return";

    public static boolean isInside(ServerPlayer player) {
        return player.level().dimension().equals(ModDimensions.NIVEL_0);
    }

    /** Lleva al jugador al Nivel 0, recordando de donde venia. */
    public static void enter(ServerPlayer player) {
        MinecraftServer server = player.server;
        ServerLevel target = server.getLevel(ModDimensions.NIVEL_0);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.pasillos.no_dimension"), true);
            return;
        }

        if (!isInside(player)) {
            CompoundTag origin = new CompoundTag();
            origin.putString("dim", player.level().dimension().location().toString());
            origin.putDouble("x", player.getX());
            origin.putDouble("y", player.getY());
            origin.putDouble("z", player.getZ());
            origin.putFloat("yRot", player.getYRot());
            player.getPersistentData().put(RETURN_TAG, origin);
        }

        if (player.isSleeping()) {
            player.stopSleepInBed(true, true);
        }

        BlockPos spawn = LiminalChunkGenerator.safeSpawnNear(player.getBlockX(), player.getBlockZ());
        target.getChunkAt(spawn); // fuerza a generar el chunk antes de aparecer para no caer al vacio
        player.teleportTo(target, spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, player.getYRot(), 0.0F);

        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false));
        player.displayClientMessage(Component.translatable("message.pasillos.arrive"), true);
    }

    /** Devuelve al jugador al sitio donde se durmio (o al spawn del mundo si no se sabe). */
    public static void leave(ServerPlayer player) {
        MinecraftServer server = player.server;
        ServerLevel destination = server.overworld();
        double x;
        double y;
        double z;
        float yRot = player.getYRot();

        BlockPos worldSpawn = destination.getSharedSpawnPos();
        x = worldSpawn.getX() + 0.5D;
        y = worldSpawn.getY();
        z = worldSpawn.getZ() + 0.5D;

        CompoundTag data = player.getPersistentData();
        if (data.contains(RETURN_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag origin = data.getCompound(RETURN_TAG);
            ResourceLocation dimId = ResourceLocation.tryParse(origin.getString("dim"));
            if (dimId != null) {
                ServerLevel saved = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimId));
                if (saved != null) {
                    destination = saved;
                    x = origin.getDouble("x");
                    y = origin.getDouble("y");
                    z = origin.getDouble("z");
                    yRot = origin.getFloat("yRot");
                }
            }
            data.remove(RETURN_TAG);
        }

        // Si dormir dentro de los Pasillos cambio el punto de reaparicion, lo devolvemos al mundo normal
        if (ModDimensions.NIVEL_0.equals(player.getRespawnDimension())) {
            player.setRespawnPosition(Level.OVERWORLD, null, 0.0F, false, false);
        }

        if (player.isSleeping()) {
            player.stopSleepInBed(true, true);
        }

        destination.getChunkAt(BlockPos.containing(x, y, z));
        player.teleportTo(destination, x, y, z, yRot, 0.0F);
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, false));
        player.displayClientMessage(Component.translatable("message.pasillos.leave"), true);
    }

    private DreamTravel() {
    }
}
