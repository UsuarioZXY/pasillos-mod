package com.pasillos.network;

import com.pasillos.client.SanityHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Del servidor al cliente: sube o baja la cordura y la ansiedad del jugador (por ejemplo al usar Sancor o Anx). */
public class SanityMessage {

    private final float sanityDelta;
    private final float anxietyDelta;

    public SanityMessage(float sanityDelta, float anxietyDelta) {
        this.sanityDelta = sanityDelta;
        this.anxietyDelta = anxietyDelta;
    }

    public static void encode(SanityMessage message, FriendlyByteBuf buffer) {
        buffer.writeFloat(message.sanityDelta);
        buffer.writeFloat(message.anxietyDelta);
    }

    public static SanityMessage decode(FriendlyByteBuf buffer) {
        return new SanityMessage(buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(SanityMessage message, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> SanityHooks.apply(message.sanityDelta, message.anxietyDelta)));
        ctx.setPacketHandled(true);
    }
}
