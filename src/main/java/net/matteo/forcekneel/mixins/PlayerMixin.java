package net.matteo.forcekneel.mixins;

import net.matteo.forcekneel.utils.Values;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    public void tick$forcekneel(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (Values.player.getOrDefault(player, 0) > 0) Values.player.put((ServerPlayer) player, Values.player.get(player) - 1);
        if (Values.player.getOrDefault(player, -1) == 0) player.setForcedPose(null);
    }
}
