package com.pasillos.command;

import com.mojang.brigadier.CommandDispatcher;
import com.pasillos.DreamTravel;
import com.pasillos.PasillosMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * /pasillos entrar  -> te manda al Nivel 0
 * /pasillos volver  -> te saca del Nivel 0
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
                .then(Commands.literal("volver").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (!DreamTravel.isInside(player)) {
                        ctx.getSource().sendFailure(Component.translatable("message.pasillos.not_inside"));
                        return 0;
                    }
                    DreamTravel.leave(player);
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
