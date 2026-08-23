package net.matteo.networklogger.mixins;

import net.matteo.networklogger.utils.values.ModValues;
import net.matteo.networklogger.utils.values.ConfigValues;

import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerPlayer.class, priority = 1)
public class ServerPlayerMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    public void test(CallbackInfo ci) {
        if (!ModValues.enabled) return;
        String player = ((ServerPlayer) (Object) this).getName().getString();

        if (ModValues.playTime.getOrDefault(player,0.0) < ConfigValues.valueConnectionStable) ModValues.playTime.merge(player, 0.05, Double::sum);
    }
}
