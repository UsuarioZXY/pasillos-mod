package com.pasillos.network;

import com.pasillos.event.PanicHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Del cliente al servidor: el jugador sufre algo por su estado mental (ver PanicHandler). */
public class PanicMessage {

    /** Ataque al corazon (ansiedad al 100 % durante demasiado tiempo). */
    public static final int HEART_ATTACK = 0;
    /** Mareo y vision que se apaga (cordura muy baja). */
    public static final int BREAKDOWN = 1;
    /** Temblor y debilidad (ansiedad alta). */
    public static final int TREMOR = 2;

    private final int kind;

    public PanicMessage(int kind) {
        this.kind = kind;
    }

    public static void encode(PanicMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.kind);
    }

    public static PanicMessage decode(FriendlyByteBuf buffer) {
        return new PanicMessage(buffer.readVarInt());
    }

    public static void handle(PanicMessage message, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ServerPlayer sender = ctx.getSender();
        ctx.enqueueWork(() -> {
            if (sender != null) {
                PanicHandler.apply(sender, message.kind);
            }
        });
        ctx.setPacketHandled(true);
    }
}
