package com.pasillos.command;

import com.mojang.brigadier.CommandDispatcher;
import com.pasillos.DreamTravel;
import com.pasillos.PasillosMod;
import com.pasillos.entity.Asechador;
import com.pasillos.registry.ModBlocks;
import com.pasillos.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * /pasillos asechador -> aparece un Asechador a unos 18 bloques
 * /pasillos entrar  -> te manda al Nivel 0
 * /pasillos profundo -> te manda al Nivel -1 (la cueva)
 * /pasillos volver  -> te saca del Nivel 0
 * /pasillos ciego   -> aparece un Ciego a 26-40 bloques
 * /pasillos saco    -> coloca un saco delante de ti
 * /pasillos noche   -> pone la noche en el mundo (para probar la niebla)
 * /pasillos dia     -> pone el dia
 */
@Mod.EventBusSubscriber(modid = PasillosMod.MODID)
public final class PasillosCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("pasillos")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("entrar").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    DreamTravel.enter(player);
                    return 1;
                }))
                .then(Commands.literal("profundo").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    DreamTravel.enterDepth(player);
                    return 1;
                }))
                .then(Commands.literal("volver").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (!DreamTravel.isInside(player)) {
                        ctx.getSource().sendFailure(Component.translatable("message.pasillos.not_inside"));
                        return 0;
                    }
                    DreamTravel.leave(player);
                    return 1;
                }))
                .then(Commands.literal("asechador").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ServerLevel level = player.serverLevel();
                    double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
                    int x = player.getBlockX() + (int) (Math.cos(angle) * 18.0D);
                    int z = player.getBlockZ() + (int) (Math.sin(angle) * 18.0D);
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    Asechador stalker = ModEntities.ASECHADOR.get().create(level);
                    if (stalker == null) {
                        return 0;
                    }
                    stalker.moveTo(x + 0.5D, y, z + 0.5D, 0.0F, 0.0F);
                    level.addFreshEntity(stalker);
                    return 1;
                }))
                .then(Commands.literal("ciego").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (!com.pasillos.event.CiegoDirector.trySpawn(player.serverLevel(), player)) {
                        ctx.getSource().sendFailure(Component.translatable("message.pasillos.no_space"));
                        return 0;
                    }
                    return 1;
                }))
                .then(Commands.literal("saco").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ServerLevel level = player.serverLevel();
                    BlockPos pos = BlockPos.containing(player.position().add(player.getLookAngle().multiply(2.0D, 0.0D, 2.0D)));
                    BlockState state = ModBlocks.SACK.get().defaultBlockState();
                    if (!level.getBlockState(pos).isAir() || !state.canSurvive(level, pos)) {
                        ctx.getSource().sendFailure(Component.translatable("message.pasillos.no_space"));
                        return 0;
                    }
                    level.setBlock(pos, state, 3);
                    return 1;
                }))
                .then(Commands.literal("noche").executes(ctx -> {
                    ctx.getSource().getServer().overworld().setDayTime(18000L);
                    return 1;
                }))
                .then(Commands.literal("dia").executes(ctx -> {
                    ctx.getSource().getServer().overworld().setDayTime(6000L);
                    return 1;
                })));
    }

    private PasillosCommand() {
    }
}
