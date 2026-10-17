package com.pasillos.event;

import com.pasillos.PasillosMod;
import com.pasillos.entity.Asechador;
import com.pasillos.registry.ModDimensions;
import com.pasillos.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Decide cuando aparece el Asechador: de noche, como mucho uno por jugador, a 36-44 bloques y con
 * preferencia por sitios tapados. Tras desvanecerse hay una pausa antes de que vuelva.
 */
@Mod.EventBusSubscriber(modid = PasillosMod.MODID)
public final class AsechadorDirector {

    private static final Map<UUID, Long> NEXT_SPAWN = new HashMap<>();

    /** Pausa de 20 a 60 segundos tras desvanecerse, para el jugador mas cercano. */
    public static void scheduleRespawn(ServerLevel level, Vec3 where) {
        Player player = level.getNearestPlayer(where.x, where.y, where.z, 128.0D, false);
        if (player != null) {
            NEXT_SPAWN.put(player.getUUID(), level.getGameTime() + 400L + level.random.nextInt(800));
        }
    }

    /** Pausa inicial al entrar en la dimension. */
    public static void delayFor(ServerPlayer player, int ticks) {
        NEXT_SPAWN.put(player.getUUID(), player.serverLevel().getGameTime() + ticks);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        if (!level.dimension().equals(ModDimensions.NIVEL_0) || level.getGameTime() % 40L != 0L) {
            return;
        }
        if (!level.isNight() || level.getDifficulty() == Difficulty.PEACEFUL
                || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            return;
        }
        long now = level.getGameTime();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || now < NEXT_SPAWN.getOrDefault(player.getUUID(), 0L)) {
                continue;
            }
            if (!level.getEntitiesOfClass(Asechador.class, player.getBoundingBox().inflate(100.0D)).isEmpty()) {
                continue;
            }
            trySpawn(level, player);
        }
    }

    private static void trySpawn(ServerLevel level, ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        BlockPos best = null;

        for (int i = 0; i < 10; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double dist = 36.0D + level.random.nextDouble() * 8.0D;
            int x = Mth.floor(player.getX() + Math.cos(angle) * dist);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * dist);
            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos feet = new BlockPos(x, y, z);
            if (!level.getBlockState(feet.below()).is(Blocks.GRASS_BLOCK)
                    && !level.getBlockState(feet.below()).is(Blocks.COARSE_DIRT)) {
                continue;
            }
            Vec3 head = new Vec3(x + 0.5D, y + 3.0D, z + 0.5D);
            HitResult hit = level.clip(new ClipContext(eye, head, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.MISS) {
                best = feet; // tapado por un arbol: perfecto
                break;
            }
            if (best == null) {
                best = feet;
            }
        }
        if (best == null) {
            return;
        }

        Asechador stalker = ModEntities.ASECHADOR.get().create(level);
        if (stalker == null) {
            return;
        }
        stalker.moveTo(best.getX() + 0.5D, best.getY(), best.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        if (!level.noCollision(stalker)) {
            return; // sin hueco para 4 bloques de alto; se reintenta en 2 segundos
        }
        level.addFreshEntity(stalker);
        level.sendParticles(ParticleTypes.SMOKE, stalker.getX(), stalker.getY() + 1.0D, stalker.getZ(),
                10, 0.3D, 0.8D, 0.3D, 0.01D);
    }

    private AsechadorDirector() {
    }
}
