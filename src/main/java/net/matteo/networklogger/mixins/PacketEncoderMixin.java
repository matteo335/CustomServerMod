package net.matteo.networklogger.mixins;

import net.matteo.networklogger.packets.PacketProfiler;
import net.matteo.networklogger.packets.UpdateData;
import net.matteo.networklogger.utils.values.ConfigValues;

import static net.matteo.networklogger.utils.values.ModValues.*;

import net.minecraft.network.PacketEncoder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.channel.ChannelHandlerContext;
import io.netty.buffer.ByteBuf;

@Mixin(value = PacketEncoder.class, priority = 1)
public class PacketEncoderMixin {

    @Unique
    private static final ThreadLocal<Integer> networklogger$size = new ThreadLocal<>();

    @Inject(method = "encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;Lio/netty/buffer/ByteBuf;)V", at = @At("TAIL"))
    public void encode$tail$networklogger(ChannelHandlerContext ctx, Packet<?> packet, ByteBuf byteBuffer, CallbackInfo ci) {
        if (ctx.channel() == null || !ConfigValues.valueIsModEnabled) return;
        UpdateData.updateDataThread(ctx.channel().attr(channelPlayer).get(), packet, byteBuffer.writerIndex());
    }

    @Inject(method = "encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;Lio/netty/buffer/ByteBuf;)V", at = @At("HEAD"))
    public void encode$head$networklogger(ChannelHandlerContext ctx, Packet<?> packet, ByteBuf byteBuffer, CallbackInfo ci) {
        if (!ConfigValues.valueIsModEnabled || ctx.channel() == null || !profiling) return;
        String id = packet instanceof ClientboundCustomPayloadPacket modded ? modded.getIdentifier().toString() : packet.getClass().getSimpleName();
        ServerPlayer Player = ctx.channel().attr(channelPlayer).get();
        String name = Player == null ? "null" : Player.getName().getString();

        if ((profilingPacket.equals(id) || profilingPacket.equals("*")) && (profilingPlayer.equals(name) || profilingPlayer.equals("*"))) {
            networklogger$size.set(byteBuffer.writerIndex());
        }
    }

    @Inject(method = "encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;Lio/netty/buffer/ByteBuf;)V", at = @At("RETURN"))
    public void encode$return$networklogger(ChannelHandlerContext ctx, Packet<?> packet, ByteBuf buf, CallbackInfo ci) {
        if (!ConfigValues.valueIsModEnabled || ctx.channel() == null || !profiling) return;
        String id = packet instanceof ClientboundCustomPayloadPacket modded ? modded.getIdentifier().toString() : packet.getClass().getSimpleName();
        ServerPlayer player = ctx.channel().attr(channelPlayer).get();
        String name = player != null ? player.getName().getString() : "null";

        if ((profilingPacket.equals(id) || profilingPacket.equals("*")) && (profilingPlayer.equals(name) || profilingPlayer.equals("*"))) {
            PacketProfiler.writePacketThread(packet, ctx.channel().attr(channelPlayer).get().level(), buf.writerIndex() - networklogger$size.get());
            networklogger$size.remove();
        }
    }
}
