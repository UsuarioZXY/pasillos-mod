package com.pasillos.event;

import com.pasillos.PasillosMod;
import com.pasillos.entity.Ciego;
import com.pasillos.entity.Ground;
import com.pasillos.registry.ModDimensions;
import com.pasillos.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Decide cuando aparece el Ciego: lo atrae el ruido sostenido (unos 8 segundos haciendo ruido alto) y,
 * en el Nivel -1, de vez en cuando aparece solo. Como mucho uno por jugador, con pausas largas.
 */
@Mod.EventBusSubscriber(modid = PasillosMod.MODID)
public final class CiegoDirector {

    private static final Map<UUID, Integer> LOUD = new HashMap<>();
    private static final Map<UUID, Long> NEXT = new HashMap<>();

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        if (!ModDimensions.isMod(level.dimension()) || level.getGameTime() % 40L != 0L
                || level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        long now = level.getGameTime();
        boolean cave = level.dimension().equals(ModDimensions.NIVEL_MENOS_1);
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            UUID id = player.getUUID();
            int loud = LOUD.getOrDefault(id, 0);
            loud = NoiseSystem.get(player) >= 30.0F ? loud + 1 : Math.max(0, loud - 1);
            LOUD.put(id, loud);
            if (now < NEXT.getOrDefault(id, now + 1200L)) {
                NEXT.putIfAbsent(id, now + 1200L); // primera pausa tras llegar
                continue;
            }
            if (!level.getEntitiesOfClass(Ciego.class, player.getBoundingBox().inflate(100.0D)).isEmpty()) {
                continue;
            }
            boolean attracted = loud >= 4;
            boolean wandering = cave && level.random.nextInt(90) == 0;
            if ((attracted || wandering) && trySpawn(level, player)) {
                NEXT.put(id, now + 2400L + level.random.nextInt(2400));
                LOUD.put(id, 0);
            }
        }
    }

    public static boolean trySpawn(ServerLevel level, ServerPlayer player) {
        for (int i = 0; i < 12; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double dist = 26.0D + level.random.nextDouble() * 14.0D;
            int x = Mth.floor(player.getX() + Math.cos(angle) * dist);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * dist);
            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            int y = Ground.standY(level, x, z, player.getBlockY());
            if (y == Ground.NONE) {
                continue;
            }
            Ciego dog = ModEntities.CIEGO.get().create(level);
            if (dog == null) {
                return false;
            }
            dog.moveTo(x + 0.5D, y, z + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(dog)) {
                continue;
            }
            level.addFreshEntity(dog);
            return true;
        }
        return false;
    }

    private CiegoDirector() {
    }
}
