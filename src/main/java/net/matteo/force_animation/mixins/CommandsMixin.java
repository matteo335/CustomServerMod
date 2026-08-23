package net.matteo.force_animation.mixins;

import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import net.matteo.force_animation.utils.Values;

import io.github.kosmx.emotes.api.events.server.ServerEmoteAPI;
import io.github.kosmx.emotes.executor.EmoteInstance;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.Pose;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.commands.*;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.stream.Stream;

@Mixin(Commands.class)
public abstract class CommandsMixin {

    @Shadow
    public abstract CommandDispatcher<CommandSourceStack> getDispatcher();

    @Shadow
    public abstract int performPrefixedCommand(CommandSourceStack pSource, String pCommand);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init$force_animation(Commands.CommandSelection pSelection, CommandBuildContext pContext, CallbackInfo ci) {
        getDispatcher().register(Commands.literal("force_animation").requires(source -> source.hasPermission(4))

                .then(Commands.literal("clear").then(Commands.argument("players", StringArgumentType.greedyString()).suggests((command, builder) -> {
                    List<String> suggestions = new ArrayList<>();
                    suggestions.add("*");
                    suggestions.addAll(Values.player.keySet().stream().map(player -> player.getName().getString()).toList());
                    return SharedSuggestionProvider.suggest(suggestions.stream(), builder);
                }).executes(command -> {
                    List<String> string = Collections.singletonList(StringArgumentType.getString(command, "players"));
                    PlayerList playerList = command.getSource().getServer().getPlayerList();
                    List<ServerPlayer> players = string.stream().map(playerList::getPlayerByName).toList();

                    if (string.contains("*")) for (ServerPlayer player : playerList.getPlayers()) {
                        player.setForcedPose(null);

                        if (Values.playing_emotecraft_animation.getOrDefault(player, false)) ServerEmoteAPI.forcePlayEmote(player.getUUID(), null);
                    } else for (ServerPlayer player : players) {
                        player.setForcedPose(null);

                        if (Values.playing_emotecraft_animation.getOrDefault(player, false)) ServerEmoteAPI.forcePlayEmote(player.getUUID(), null);
                    }

                    return 0;
                })))

                .then(Commands.literal("pose").then(Commands.argument("pose", StringArgumentType.string()).suggests((command, builder) -> {
                    List<String> suggestions = new ArrayList<>(Stream.of("swim", "crouch", "stand").toList());

                    if (Values.emotecraft_loaded) {
                        suggestions.addAll(ServerEmoteAPI.getLoadedEmotes().values().stream().
                                map(emote -> "\"" + EmoteInstance.instance.getDefaults().fromJson(emote.extraData.get("name")).append("\"").getString()).toList());
                    }

                    return SharedSuggestionProvider.suggest(suggestions.stream(), builder);
                })


                //force_animation time
                .then(Commands.literal("time").then(Commands.argument("seconds", IntegerArgumentType.integer())

                        //force_animation time distance
                        .then(Commands.literal("distance").then(Commands.argument("distance", IntegerArgumentType.integer()).executes(command -> {
                            int distance = IntegerArgumentType.getInteger(command, "distance");
                            ServerLevel level = command.getSource().getLevel();
                            Vec3 pos = command.getSource().getPosition();
                            String pose = StringArgumentType.getString(command, "pose");

                            List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class,
                                    new AABB(
                                            pos.x - distance, pos.y - distance, pos.z - distance,
                                            pos.x + distance, pos.y + distance, pos.z + distance
                                    ));


                            for (ServerPlayer player : players) {
                                if (player == command.getSource().getPlayer()) continue;

                                if (Values.emotecraft_loaded) {

                                    Optional<KeyframeAnimation> matchedEmote = ServerEmoteAPI.getLoadedEmotes().values().stream()
                                            .filter(emote -> pose.equals(EmoteInstance.instance.getDefaults().fromJson(emote.extraData.get("name"))
                                                    .getString())).findFirst();

                                    if (matchedEmote.isPresent()) {
                                        ServerEmoteAPI.forcePlayEmote(player.getUUID(), matchedEmote.get().copy());
                                        Values.playing_emotecraft_animation.put(player, true);
                                    }
                                }

                                switch (pose) {
                                    case "swim" -> {
                                        player.setForcedPose(Pose.SWIMMING);
                                        performPrefixedCommand(command.getSource(), "/title " + player.getName().getString() + " title \"Swim\"");
                                    }

                                    case "crouch" -> {
                                        player.setForcedPose(Pose.CROUCHING);
                                        performPrefixedCommand(command.getSource(), "/title " + player.getName().getString() + " title \"Kneel\"");
                                    }

                                    case "stand" -> {
                                        player.setForcedPose(Pose.STANDING);
                                        performPrefixedCommand(command.getSource(), "/title " + player.getName().getString() + " title \"Stand\"");
                                    }
                                }

                                Values.player.put(player, IntegerArgumentType.getInteger(command, "seconds") * 20);
                            }

                            return 0;
                        })))

                        //force_animation time players
                        .then(Commands.literal("players").then(Commands.argument("players", StringArgumentType.greedyString())
                                .suggests((command, builder) -> {
                                    String input = builder.getInput();
                                    String remaining = input.substring(builder.getStart());
                                    List<ServerPlayer> players = command.getSource().getServer().getPlayerList().getPlayers();

                                    int start = remaining.lastIndexOf(' ') == -1 ? builder.getStart() : builder.getStart() + remaining.lastIndexOf(' ') + 1;
                                    List<String> current = Arrays.stream(remaining.split(" ")).map(String::trim).filter(string -> !string.isEmpty()).toList();
                                    List<String> past = (!remaining.endsWith(" ") && !current.isEmpty()) ? current.subList(0, current.size() - 1) : current;

                                    while (start < input.length() && input.charAt(start) == ' ') start++;

                                    List<String> suggestions = new ArrayList<>();
                                    if (past.isEmpty()) suggestions.add("*");
                                    SuggestionsBuilder offsetBuilder = builder.createOffset(start);

                                    List<String> alreadyTyped = Arrays.stream(remaining.split(" ")).map(String::trim)
                                            .filter(string -> !string.isEmpty() && !string.contains("*")).toList();

                                    players.stream().map(player -> player.getName().getString())
                                            .filter(name -> !alreadyTyped.contains(name) && !alreadyTyped.contains("*")).forEach(suggestions::add);

                                    return SharedSuggestionProvider.suggest(suggestions.stream(), offsetBuilder);
                                }).executes(command -> {
                                    List<String> string = Collections.singletonList(StringArgumentType.getString(command, "players"));
                                    PlayerList playerList = command.getSource().getServer().getPlayerList();
                                    List<ServerPlayer> players = string.stream().map(playerList::getPlayerByName).toList();
                                    String pose = StringArgumentType.getString(command, "pose");

                                    if (string.contains("*")) for (ServerPlayer player : playerList.getPlayers()) {
                                        if (player == command.getSource().getPlayer()) continue;

                                        if (Values.emotecraft_loaded) {
                                            Optional<KeyframeAnimation> matchedEmote = ServerEmoteAPI.getLoadedEmotes().values().stream()
                                                    .filter(emote -> pose.equals(EmoteInstance.instance.getDefaults().fromJson(emote.extraData.get("name"))
                                                            .getString())).findFirst();

                                            if (matchedEmote.isPresent()) {
                                                ServerEmoteAPI.forcePlayEmote(player.getUUID(), matchedEmote.get().copy());
                                                Values.playing_emotecraft_animation.put(player, true);
                                            }
                                        }

                                        switch (pose) {
                                            case "swim" -> {
                                                player.setForcedPose(Pose.SWIMMING);
                                                performPrefixedCommand(command.getSource(), "/title " + player.getName().getString() + " title \"Swim\"");
                                            }

                                            case "crouch" -> {
                                                player.setForcedPose(Pose.CROUCHING);
                                                performPrefixedCommand(command.getSource(), "/title " + player.getName().getString() + " title \"Kneel\"");
                                            }

                                            case "stand" -> {
                                                player.setForcedPose(Pose.STANDING);
                                                performPrefixedCommand(command.getSource(), "/title " + player.getName().getString() + " title \"Stand\"");
                                            }
                                        }

                                        Values.player.put(player, IntegerArgumentType.getInteger(command, "seconds") * 20);
                                    } else for (ServerPlayer player : players) {

                                        if (Values.emotecraft_loaded) {

                                            Optional<KeyframeAnimation> matchedEmote = ServerEmoteAPI.getLoadedEmotes().values().stream()
                                                    .filter(emote -> pose.equals(EmoteInstance.instance.getDefaults().fromJson(emote.extraData.get("name"))
                                                            .getString())).findFirst();

                                            if (matchedEmote.isPresent()) {

                                                ServerEmoteAPI.forcePlayEmote(player.getUUID(), matchedEmote.get().copy());
                                                Values.playing_emotecraft_animation.put(player, true);
                                            }
                                        }

                                        switch (StringArgumentType.getString(command,"pose")) {
                                            case "swim" -> {
                                                player.setForcedPose(Pose.SWIMMING);
                                                performPrefixedCommand(command.getSource(), "/title " + player.getName().getString() + " title \"Swim\"");
                                            }

                                            case "crouch" -> {
                                                player.setForcedPose(Pose.CROUCHING);
                                                performPrefixedCommand(command.getSource(), "/title " + player.getName().getString() + " title \"Kneel\"");
                                            }

                                            case "stand" -> {
                                                player.setForcedPose(Pose.STANDING);
                                                performPrefixedCommand(command.getSource(), "/title " + player.getName().getString() + " title \"Stand\"");
                                            }
                                        }

                                        Values.player.put(player, IntegerArgumentType.getInteger(command, "seconds") * 20);
                                    }

                                    return 0;
                                })))
                ))
        )));
    }

}
