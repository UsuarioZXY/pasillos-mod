package com.pasillos;

import com.pasillos.event.AsechadorDirector;
import com.pasillos.registry.ModDimensions;
import com.pasillos.worldgen.CaveShape;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Logica de viaje entre el mundo normal y los Pasillos. */
public final class DreamTravel {

    private static final String RETURN_TAG = "pasillos_return";

    public static boolean isInside(ServerPlayer player) {
        return ModDimensions.isMod(player.level().dimension());
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

        BlockPos spawn = findSurface(target, player.getBlockX(), player.getBlockZ());
        player.teleportTo(target, spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, player.getYRot(), 0.0F);

        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false));
        player.displayClientMessage(Component.translatable("message.pasillos.arrive"), true);

        // el Asechador tarda de 30 a 60 segundos en aparecer
        AsechadorDirector.delayFor(player, 600 + player.getRandom().nextInt(600));
    }

    /** Lleva al jugador al Nivel -1 (la cueva gigante), recordando de donde venia si aun estaba en el mundo normal. */
    public static void enterDepth(ServerPlayer player) {
        MinecraftServer server = player.server;
        ServerLevel target = server.getLevel(ModDimensions.NIVEL_MENOS_1);
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
        BlockPos spawn = CaveShape.findSpawn(new java.util.Random(player.getRandom().nextLong()));
        target.getChunk(spawn.getX() >> 4, spawn.getZ() >> 4);
        player.teleportTo(target, spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, player.getYRot(), 0.0F);
        player.fallDistance = 0.0F;
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 140, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, false));
        AsechadorDirector.delayFor(player, 900 + player.getRandom().nextInt(600));
    }

    /** Busca suelo firme de hierba o arena cerca de unas coordenadas, generando los chunks necesarios. */
    private static BlockPos findSurface(ServerLevel level, int x, int z) {
        for (int r = 0; r <= 24; r += 2) {
            int points = r == 0 ? 1 : 8;
            for (int i = 0; i < points; i++) {
                double angle = i * Math.PI / 4.0D;
                int px = x + (int) Math.round(Math.cos(angle) * r);
                int pz = z + (int) Math.round(Math.sin(angle) * r);
                level.getChunk(px >> 4, pz >> 4); // fuerza a generar el chunk antes de aparecer
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, px, pz);
                BlockPos feet = new BlockPos(px, y, pz);
                BlockState below = level.getBlockState(feet.below());
                if ((below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.SAND))
                        && level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()) {
                    return feet;
                }
            }
        }
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1, z);
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
        if (ModDimensions.isMod(player.getRespawnDimension())) {
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
