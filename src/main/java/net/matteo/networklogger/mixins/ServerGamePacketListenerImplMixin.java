package net.matteo.networklogger.mixins;

import static net.matteo.networklogger.utils.values.ConfigValues.*;
import static net.matteo.networklogger.utils.values.ModValues.*;
import net.matteo.networklogger.utils.HandleFiles;
import net.matteo.networklogger.Main;

import net.minecraft.network.Connection;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    public abstract ServerPlayer getPlayer();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init$networklogger(MinecraftServer pServer, Connection pConnection, ServerPlayer pPlayer, CallbackInfo ci) {
        try {
            //noinspection ConstantConditions
            if (!valueIsModEnabled || pConnection.channel() == null) return;
            pConnection.channel().attr(channelPlayer).set(pPlayer);
        } catch (Throwable exception) {
            Main.logger.error("Error in ServerGamePacketListenerImplMixin init$networklogger. Please report it to author of NetworkLogger.", exception);
        }
    }

    @Inject(method = "sendPlayerChatMessage", at = @At("TAIL"))
    private void sendPlayerChatMessage$networklogger(PlayerChatMessage pChatMessage, ChatType.Bound pBoundType, CallbackInfo ci) {
        //noinspection ConstantValue
        if (!valueIsModEnabled || pChatMessage.sender() != getPlayer().getUUID() || getPlayer().connection == null) return;
        String message = pChatMessage.decoratedContent().getString();

        if ((message.contains(" ping") || message.contains(" lag")) && valueWriteIfContains) HandleFiles.writeGlobal(getPlayer(), message);
    }
}
