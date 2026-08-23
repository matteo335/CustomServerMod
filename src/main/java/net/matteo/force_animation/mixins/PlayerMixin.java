package net.matteo.force_animation.mixins;

import net.matteo.force_animation.utils.Values;

import io.github.kosmx.emotes.api.events.server.ServerEmoteAPI;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerMixin {

    @SuppressWarnings("SuspiciousMethodCalls")
    @Inject(method = "tick", at = @At("TAIL"))
    public void tick$force_animation(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (Values.player.getOrDefault(player, 0) > 0) Values.player.put((ServerPlayer) player, Values.player.get(player) - 1);
        if (Values.player.getOrDefault(player, -1) == 0) player.setForcedPose(null);

        if (Values.playing_emotecraft_animation.getOrDefault(player, false) && Values.player.getOrDefault(player, -1) == 0) {
            ServerEmoteAPI.forcePlayEmote(player.getUUID(), null);
        }
    }
}
