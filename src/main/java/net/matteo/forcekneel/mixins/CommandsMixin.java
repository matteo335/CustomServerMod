package net.matteo.forcekneel.mixins;

import net.matteo.forcekneel.utils.Values;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.Pose;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.commands.*;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

@Mixin(Commands.class)
public abstract class CommandsMixin {

    @Shadow
    public abstract CommandDispatcher<CommandSourceStack> getDispatcher();

    @Shadow
    public abstract int performPrefixedCommand(CommandSourceStack pSource, String pCommand);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init$forcekneel(Commands.CommandSelection pSelection, CommandBuildContext pContext, CallbackInfo ci) {
        getDispatcher().register(Commands.literal("forcekneel").requires(source -> source.hasPermission(4))

                .then(Commands.literal("clear").then(Commands.argument("players", StringArgumentType.greedyString()).suggests((command, builder) -> {
                    List<String> suggestions = new ArrayList<>();
                    suggestions.add("*");
                    suggestions.addAll(Values.player.keySet().stream().map(player -> player.getName().getString()).toList());
                    return SharedSuggestionProvider.suggest(suggestions.stream(), builder);
                }).executes(command -> {
                    List<String> string = Collections.singletonList(StringArgumentType.getString(command, "players"));
                    PlayerList playerList = command.getSource().getServer().getPlayerList();
                    List<ServerPlayer> players = string.stream().map(playerList::getPlayerByName).toList();

                    if (string.contains("*")) for (ServerPlayer player : playerList.getPlayers()) player.setForcedPose(null);
                    else for (ServerPlayer player : players) player.setForcedPose(null);

                    return 0;
                })))

                //forcekneel time
                .then(Commands.literal("time").then(Commands.argument("seconds", IntegerArgumentType.integer())

                        //forcekneel time distance
                        .then(Commands.literal("distance").then(Commands.argument("distance", IntegerArgumentType.integer()).executes(command -> {
                            int distance = IntegerArgumentType.getInteger(command, "distance");
                            ServerLevel level = command.getSource().getLevel();
                            Vec3 pos = command.getSource().getPosition();

                            List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class,
                                    new AABB(
                                            pos.x - distance, pos.y - distance, pos.z - distance,
                                            pos.x + distance, pos.y + distance, pos.z + distance
                                    ));

                            for (ServerPlayer player : players) {
                                if (player == command.getSource().getPlayer()) continue;

                                player.setForcedPose(Pose.CROUCHING);
                                performPrefixedCommand(command.getSource(), "/title " + player.getName().getString() + " title \"Kneel\"");
                                Values.player.put(player, IntegerArgumentType.getInteger(command, "seconds") * 20);
                            }

                            return 0;
                        })))

                        //forcekneel time players
                        .then(Commands.literal("players").then(Commands.argument("players", StringArgumentType.greedyString())
                                .suggests((command, builder) -> {
                                    List<String> suggestions = new ArrayList<>();
                                    List<ServerPlayer> players = command.getSource().getServer().getPlayerList().getPlayers();

                                    suggestions.add("*");
                                    suggestions.addAll(players.stream().map(player -> player.getName().getString()).toList());
                                    return SharedSuggestionProvider.suggest(suggestions.stream(), builder);
                                }).executes(command -> {
                                    List<String> string = Collections.singletonList(StringArgumentType.getString(command, "players"));
                                    PlayerList playerList = command.getSource().getServer().getPlayerList();
                                    List<ServerPlayer> players = string.stream().map(playerList::getPlayerByName).toList();

                                    if (string.contains("*")) for (ServerPlayer player : playerList.getPlayers()) {
                                        if (player == command.getSource().getPlayer()) continue;

                                        performPrefixedCommand(command.getSource(), "/title " + player.getName().getString() + " title \"Kneel\"");
                                        player.setForcedPose(Pose.CROUCHING);
                                        Values.player.put(player, IntegerArgumentType.getInteger(command, "seconds") * 20);
                                    } else for (ServerPlayer player : players) {
                                        performPrefixedCommand(command.getSource(), "/title " + player.getName().getString() + " title \"Kneel\"");
                                        player.setForcedPose(Pose.CROUCHING);
                                        Values.player.put(player, IntegerArgumentType.getInteger(command, "seconds") * 20);
                                    }

                                    return 0;
                                })))
                ))
        );
    }

}
