package com.pasillos.network;

import com.pasillos.client.SanityHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Del servidor al cliente: cuanto ruido estas haciendo ahora (para el medidor de ruido). */
public class NoiseMessage {

    private final float noise;

    public NoiseMessage(float noise) {
        this.noise = noise;
    }

    public static void encode(NoiseMessage message, FriendlyByteBuf buffer) {
        buffer.writeFloat(message.noise);
    }

    public static NoiseMessage decode(FriendlyByteBuf buffer) {
        return new NoiseMessage(buffer.readFloat());
    }

    public static void handle(NoiseMessage message, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> SanityHooks.setNoise(message.noise)));
        ctx.setPacketHandled(true);
    }
}
