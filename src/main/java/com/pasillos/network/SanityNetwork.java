package com.pasillos.network;

import com.pasillos.PasillosMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Canal de red del mod. De momento solo lleva los cambios de cordura y ansiedad hacia el cliente. */
public final class SanityNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(PasillosMod.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    /** Se llama una vez al arrancar (FMLCommonSetupEvent). */
    public static void register() {
        CHANNEL.registerMessage(0, SanityMessage.class,
                SanityMessage::encode, SanityMessage::decode, SanityMessage::handle);
        CHANNEL.registerMessage(1, PanicMessage.class,
                PanicMessage::encode, PanicMessage::decode, PanicMessage::handle);
    }

    /** Cambia la cordura y la ansiedad del jugador (valores positivos suben, negativos bajan). */
    public static void send(ServerPlayer player, float sanityDelta, float anxietyDelta) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SanityMessage(sanityDelta, anxietyDelta));
    }

    /** Del cliente al servidor: avisa de que el jugador sufre un ataque de panico, mareo o temblor. */
    public static void sendPanic(int kind) {
        CHANNEL.sendToServer(new PanicMessage(kind));
    }

    private SanityNetwork() {
    }
}
