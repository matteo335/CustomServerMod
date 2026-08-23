package net.matteo.networklogger.mixins;

import net.matteo.networklogger.utils.values.ModValues;
import net.matteo.networklogger.Main;

import net.minecraft.network.Connection;

import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.channel.Channel;

@Mixin(Connection.class)
public abstract class ConnectionMixin {

    @Shadow
    private float averageSentPackets;

    @Shadow
    public abstract Channel channel();

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V", at = @At("TAIL"))
    public void send$networklogger(Packet<?> pPacket, PacketSendListener pSendListener, CallbackInfo ci) {
        if (!ModValues.enabled) return;
        try {
            if (channel() == null) return;
            if (channel().attr(ModValues.channelPlayer).get() == null) return;
            ModValues.averageSentPackets.put(channel().attr(ModValues.channelPlayer).get().getName().getString(), (int) averageSentPackets);
        } catch (Throwable exception) {
            Main.logger.error("NetworkLogger errored in ConnectionMixin send$networklogger. Please report to the mod author!");
            //noinspection CallToPrintStackTrace
            exception.printStackTrace();
        }
    }
}
