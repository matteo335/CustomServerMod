package net.matteo.networklogger.mixins;

import net.matteo.networklogger.utils.values.ModValues;
import net.matteo.networklogger.utils.values.ConfigValues;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Inject(method = "tickServer", at = @At("TAIL"))
    public void tickServer$networklogger(BooleanSupplier pHasTimeLeft, CallbackInfo ci) {
        if (!ConfigValues.valueIsModEnabled) return;
        MinecraftServer server = (MinecraftServer) (Object) this;
        ModValues.time += 0.05;

        if (ModValues.pingExcess && ModValues.pingExcessTime < 60) {
            ModValues.pingExcessTime += 0.05;
        } else {
            ModValues.pingExcess = false;
            ModValues.endedPingExcess = true;
        }

        if (!ModValues.profiling) return;
        if (ModValues.profilingTime >= 60 || ModValues.profilingTime < 0) {
            ModValues.profiling = false;
            ModValues.writing = false;
            ModValues.profilingPacket = "";
            ServerPlayer player = server.getPlayerList().getPlayerByName(ModValues.player_that_started_the_profiler);

            if (player != null) {
                if (ModValues.profilingTime < 0) {
                    player.sendSystemMessage(Component.literal("The file is big! Created the file before the expected 60 seconds."));
                } else player.sendSystemMessage(Component.literal("File created!"));
            }
            ModValues.profilingTime = 0;
            ModValues.numberOfPackets = 0;
            return;
        }

        ModValues.profilingTime += 0.05;
    }
}
