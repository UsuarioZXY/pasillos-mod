package com.pasillos.event;

import com.pasillos.PasillosMod;
import com.pasillos.registry.ModDimensions;
import com.pasillos.worldgen.CaveShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * En los tuneles del Nivel -1 aparecen de vez en cuando vagonetas con cofre sobre un trocito de via, con
 * antorchas, pastillas Anx y, con suerte, Sancor (tabla pasillos:chests/vagoneta). El sitio de cada
 * vagoneta sale de CaveShape.cartSpot; si ya hay via en el sitio, no se vuelve a colocar.
 */
@Mod.EventBusSubscriber(modid = PasillosMod.MODID)
public final class MinecartSpawner {

    private static final class Pending {
        final ChunkPos pos;
        int tries;

        Pending(ChunkPos pos) {
            this.pos = pos;
        }
    }

    private static final ConcurrentLinkedQueue<Pending> QUEUE = new ConcurrentLinkedQueue<>();
    private static final ResourceLocation LOOT = new ResourceLocation(PasillosMod.MODID, "chests/vagoneta");

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension().equals(ModDimensions.NIVEL_MENOS_1)) {
            ChunkPos pos = event.getChunk().getPos();
            if (CaveShape.cartSpot(pos.x, pos.z) != null) {
                QUEUE.add(new Pending(pos));
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                || !level.dimension().equals(ModDimensions.NIVEL_MENOS_1) || QUEUE.isEmpty()) {
            return;
        }
        List<Pending> retry = new ArrayList<>();
        Pending p;
        int budget = 4;
        while (budget-- > 0 && (p = QUEUE.poll()) != null) {
            if (level.getChunkSource().getChunkNow(p.pos.x, p.pos.z) == null) {
                if (++p.tries < 60) {
                    retry.add(p);
                }
                continue;
            }
            place(level, p.pos);
        }
        QUEUE.addAll(retry);
    }

    private static void place(ServerLevel level, ChunkPos chunk) {
        BlockPos spot = CaveShape.cartSpot(chunk.x, chunk.z);
        if (spot == null || !level.getBlockState(spot).isAir()) {
            return; // ya hay via (ya se puso) o no cabe
        }
        if (!level.getBlockState(spot.below()).isFaceSturdy(level, spot.below(), Direction.UP)) {
            return;
        }
        level.setBlock(spot, Blocks.RAIL.defaultBlockState(), 3);
        // un par de tramos de via a los lados si caben
        for (Direction d : new Direction[]{Direction.WEST, Direction.EAST}) {
            BlockPos side = spot.relative(d);
            if (level.getBlockState(side).isAir()
                    && level.getBlockState(side.below()).isFaceSturdy(level, side.below(), Direction.UP)) {
                level.setBlock(side, Blocks.RAIL.defaultBlockState(), 3);
            }
        }
        MinecartChest cart = new MinecartChest(level, spot.getX() + 0.5D, spot.getY() + 0.0625D, spot.getZ() + 0.5D);
        cart.setLootTable(LOOT, level.random.nextLong());
        level.addFreshEntity(cart);
    }

    private MinecartSpawner() {
    }
}
