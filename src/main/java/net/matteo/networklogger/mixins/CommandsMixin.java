package net.matteo.networklogger.mixins;

import static net.matteo.networklogger.utils.values.ModValues.*;
import static net.matteo.networklogger.utils.values.ConfigValues.*;

import net.matteo.networklogger.Main;
import net.matteo.networklogger.utils.HandleFiles;

import static net.minecraft.network.chat.Component.literal;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Mixin(Commands.class)
public class CommandsMixin {

    @Shadow
    @Final
    private CommandDispatcher<CommandSourceStack> dispatcher;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init$networklogger(Commands.CommandSelection pSelection, CommandBuildContext pContext, CallbackInfo ci) {
        dispatcher.register(Commands.literal("networklogger")

                //networklogger global
                .then(Commands.literal("global").requires(source -> source.hasPermission(4))

                        //networklogger global profiler
                        .then(Commands.literal("profiler").requires(source -> source.hasPermission(4))
                                .then(Commands.argument("packet", StringArgumentType.greedyString()).suggests((command, builder) -> {
                                    List<String> suggestions = new ArrayList<>();
                                    suggestions.add("*");
                                    suggestions.addAll(counts.keySet());
                                    return SharedSuggestionProvider.suggest(suggestions.stream(), builder);
                                }).executes(command -> {
                                    String packet = StringArgumentType.getString(command, "packet");
                                    CommandSourceStack source = command.getSource();

                                    if (!profiling) {
                                        profilingPacket = packet;
                                        profilingPlayer = "*";
                                        profiling = true;
                                        player_that_started_the_profiler = command.getSource().getTextName();
                                        Main.logger.info("Global profiler started for 60 seconds profiling {} ", packet);
                                        source.sendSystemMessage(literal("Profiler started for 60 seconds profiling " + packet));
                                    } else {
                                        source.sendSystemMessage(literal(String.format("Profiler already started %1$d seconds ago by %2$s profiling %3$s",
                                                Math.round(profilingTime), player_that_started_the_profiler, profilingPacket)));
                                    }

                                    return 0;
                                })))

                        //networklogger global get
                        .then(Commands.literal("get")

                                //networklogger global get page(Integer)
                                .then(Commands.literal("page").then(Commands.argument("page", IntegerArgumentType.integer())

                                        //networklogger global get page(Integer) ping
                                        .then(Commands.literal("ping").executes(command -> {
                                            int page = IntegerArgumentType.getInteger(command, "page");
                                            command.getSource().sendSystemMessage(literal("§lPlayers Ping§r:\n" + command.getSource().getServer().getPlayerList().getPlayers()
                                                    .stream().sorted(Comparator.comparing((ServerPlayer player) -> player.latency).reversed()).skip((long) (page - 1) * 5)
                                                    .limit(5).map(entry -> entry.getName().getString() + " -> " + entry.latency + " ms").collect(Collectors.joining("\n"))
                                            ));
                                            return 0;
                                        }))

                                        //networklogger get global page(Integer) accumulated
                                        .then(Commands.literal("accumulated").executes(command -> {
                                            int page = IntegerArgumentType.getInteger(command, "page");
                                            command.getSource().sendSystemMessage(literal("§lAccumulated§r:\n" + accumulated.entrySet().stream()
                                                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed()).skip((long) (page - 1) * 5).limit(5)
                                                    .map(entry -> entry.getKey() + " -> " + String.format("%,d", entry.getValue()) + " bytes").collect(Collectors.joining("\n"))
                                            ));
                                            return 0;
                                        }))

                                        //networklogger get global page(Integer) highest
                                        .then(Commands.literal("highest").executes(command -> {
                                            int page = IntegerArgumentType.getInteger(command, "page");
                                            command.getSource().sendSystemMessage(literal("§lHighest§r:\n" + highest.entrySet().stream()
                                                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).skip((long) (page - 1) * 5).limit(5)
                                                    .map(entry -> entry.getKey() + " -> "
                                                            + String.format("%,d", entry.getValue()) + " heaviest packet in bytes").collect(Collectors.joining("\n"))
                                            ));
                                            return 0;
                                        }))

                                        //networklogger get global page(Integer) counts
                                        .then(Commands.literal("counts").executes(command -> {
                                            int page = IntegerArgumentType.getInteger(command, "page");
                                            command.getSource().sendSystemMessage(literal("§lCounts§r:\n" + counts.entrySet().stream()
                                                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).skip((long) (page - 1) * 5).limit(5)
                                                    .map(entry -> entry.getKey() + " -> " + String.format("%,d", entry.getValue()) + " packets")
                                                    .collect(Collectors.joining("\n"))
                                            ));
                                            return 0;
                                        }))

                                        //networklogger get global page(Integer) average
                                        .then(Commands.literal("average").executes(command -> {
                                            int page = IntegerArgumentType.getInteger(command, "page");
                                            command.getSource().sendSystemMessage(literal("§lAverage size over time§r:\n" + average.entrySet().stream()
                                                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).skip((long) (page - 1) * 5).limit(5)
                                                    .map(entry -> entry.getKey() + " -> " + String.format("%,d", entry.getValue()) + " bytes").collect(Collectors.joining("\n"))
                                            ));
                                            return 0;
                                        }))

                                        //networklogger get global page(Integer) packets_per_second
                                        .then(Commands.literal("packets_per_second").executes(command -> {
                                            int page = IntegerArgumentType.getInteger(command, "page");

                                            command.getSource().sendSystemMessage(literal("§lPackets per second§r:\n" + packets_per_second.entrySet().stream()
                                                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).skip((long) (page - 1) * 5).limit(5)
                                                    .map(entry -> entry.getKey() + " -> " + entry.getValue()).collect(Collectors.joining("\n"))
                                            ));
                                            return 0;
                                        }))

                                        //networklogger get page(Integer) chunk
                                        .then(Commands.literal("chunk")

                                                //networklogger get page(Integer) chunk help
                                                .then(Commands.literal("help").executes(command -> {
                                                    String link = "https://minecraft.tools/en/coordinate-calculator.php";
                                                    Style linkStyle = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link)).withUnderlined(true);
                                                    command.getSource().sendSystemMessage(literal("If you are not sure how to find the chunk, " +
                                                            "use this calculator and fill the first and last value of Chuck Section Information:\n")
                                                            .append(literal(link).withStyle(linkStyle).withStyle(ChatFormatting.BLUE)));
                                                    return 0;
                                                }))

                                                //networklogger get page(Integer) chunk  accumulated
                                                .then(Commands.literal("accumulated").executes(command -> {
                                                    int page = IntegerArgumentType.getInteger(command, "page");

                                                    command.getSource().sendSystemMessage(literal("§lregion accumulated§r:\n" + chunkAccumulated.entrySet().stream()
                                                            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).skip((long) (page - 1) * 5).limit(5)
                                                            .map(entry -> entry.getKey() + " -> " + String.format("%,d", entry.getValue()) + " bytes")
                                                            .collect(Collectors.joining("\n"))
                                                    ));
                                                    return 0;
                                                }))

                                                //networklogger get page(Integer) chunk counts
                                                .then(Commands.literal("counts").executes(command -> {
                                                    int page = IntegerArgumentType.getInteger(command, "page");

                                                    command.getSource().sendSystemMessage(literal("§lchunk counts§r:\n" + chunkCounts.entrySet().stream()
                                                            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).skip((long) (page - 1) * 5).limit(5)
                                                            .map(entry -> entry.getKey() + " -> " + String.format("%,d", entry.getValue()) + " packets")
                                                            .collect(Collectors.joining("\n"))
                                                    ));
                                                    return 0;
                                                }))

                                                //networklogger get page(Integer) chunk average
                                                .then(Commands.literal("average").executes(command -> {
                                                    int page = IntegerArgumentType.getInteger(command, "page");

                                                    command.getSource().sendSystemMessage(literal("§lchunk average§r:\n" + chunkAverage.entrySet().stream()
                                                            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).skip((long) (page - 1) * 5).limit(5)
                                                            .map(entry -> entry.getKey() + " -> " + String.format("%,d", entry.getValue()) + " bytes")
                                                            .collect(Collectors.joining("\n"))
                                                    ));
                                                    return 0;
                                                }))

                                                //networklogger get page(Integer) chunk highest
                                                .then(Commands.literal("highest").executes(command -> {
                                                    int page = IntegerArgumentType.getInteger(command, "page");
                                                    command.getSource().sendSystemMessage(literal("§lchunk heaviest packets§r:\n" + chunkHighest.entrySet().stream()
                                                            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).skip((long) (page - 1) * 5).limit(5)
                                                            .map(entry -> entry.getKey() + String.format(" -> %,d bytes", entry.getValue()))
                                                            .collect(Collectors.joining("\n"))
                                                    ));
                                                    return 0;
                                                }))

                                                //networklogger get page(Integer) chunk packets_per_second
                                                .then(Commands.literal("packets_per_second").executes(command -> {
                                                    int page = IntegerArgumentType.getInteger(command, "page");
                                                    command.getSource().sendSystemMessage(literal("§lchunk average bytes over time§r:\n" + chunk_packets_per_second.entrySet()
                                                            .stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).skip((long) (page - 1) * 5).limit(5)
                                                            .map(entry -> entry.getKey() + String.format(" -> %,d bytes", entry.getValue()))
                                                    ));
                                                    return 0;
                                                }))
                                        )
                                ))))

                //networklogger player(ServerPlayer)
                .then(Commands.literal("player").requires(source -> source.hasPermission(4)).then(Commands.argument("player", EntityArgument.player())

                        .then(Commands.literal("profiler").requires(source -> source.hasPermission(4))
                                .then(Commands.argument("packet", StringArgumentType.greedyString()).suggests((command, builder) -> {
                                    List<String> suggestions = new ArrayList<>();
                                    suggestions.add("*");
                                    suggestions.addAll(counts.keySet());
                                    return SharedSuggestionProvider.suggest(suggestions.stream(), builder);
                                }).executes(command -> {
                                    String packet = StringArgumentType.getString(command, "packet");
                                    ServerPlayer player = EntityArgument.getPlayer(command, "player");
                                    CommandSourceStack source = command.getSource();

                                    if (!profiling) {
                                        profilingPacket = packet;
                                        profilingPlayer = player.getName().getString();
                                        profiling = true;
                                        player_that_started_the_profiler = command.getSource().getTextName();
                                        Main.logger.info("{}'s Profiler started for 60 seconds profiling {} ", player.getName().getString(), packet);
                                        source.sendSystemMessage(literal("Profiler started for 60 seconds profiling " + packet));
                                    } else {
                                        source.sendSystemMessage(literal(String.format("Profiler already started %1$d seconds ago by %2$s profiling %3$s",
                                                Math.round(profilingTime), player_that_started_the_profiler, profilingPacket)));
                                    }

                                    return 0;
                                })))

                        //networklogger player(ServerPlayer) get
                        .then(Commands.literal("get")

                                //networklogger get player(ServerPlayer)
                                .then(Commands.literal("ping").executes(command -> {
                                    ServerPlayer player = EntityArgument.getPlayer(command, "player");
                                    command.getSource().sendSystemMessage(literal(player.getName().getString() + " ping is " + player.latency + " ms"));
                                    return 0;
                                }))

                                //networklogger get player(ServerPlayer) averageSentPackets
                                .then(Commands.literal("averageSentPackets").executes(command -> {
                                    String player = EntityArgument.getPlayer(command, "player").getName().getString();
                                    command.getSource().sendSystemMessage(literal("§l" + player + "§r averageSentPackets§r: " + averageSentPackets.get(player)));
                                    return 0;
                                }))

                                //networklogger get player(ServerPlayer) page(Integer)
                                .then(Commands.literal("page").then(Commands.argument("page", IntegerArgumentType.integer())

                                        //networklogger get player(ServerPlayer) page(integer) accumulated
                                        .then(Commands.literal("accumulated").executes(command -> {
                                            int page = IntegerArgumentType.getInteger(command, "page");
                                            String player = EntityArgument.getPlayer(command, "player").getName().getString();

                                            if (playerAccumulated.get(player).isEmpty()) {
                                                command.getSource().sendSystemMessage(literal(String.format("Empty! Check ping requirements!\n%1$s %2$d\n%3$s %4$d",
                                                        stringOnlyCaptureGlobalOver, valueOnlyCaptureGlobalOver, stringOnlyCapturePlayerOver, valueOnlyCapturePlayerOver)));
                                                return 1;
                                            }

                                            command.getSource().sendSystemMessage(literal("§l" + player + " accumulated§r:\n" +
                                                    playerAccumulated.get(player).entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue()
                                                                    .reversed()).skip((long) (page - 1) * 5)
                                                            .limit(5).map(entry -> entry.getKey() + " -> " + entry.getValue()).collect(Collectors.joining("\n"))
                                            ));
                                            return 0;
                                        }))

                                        //networklogger get player(ServerPlayer) page(Integer) counts
                                        .then(Commands.literal("counts").executes(command -> {
                                            int page = IntegerArgumentType.getInteger(command, "page");
                                            String player = EntityArgument.getPlayer(command, "player").getName().getString();

                                            if (playerCounts.get(player).isEmpty()) {
                                                command.getSource().sendSystemMessage(literal(String.format("Empty! Check ping requirements!\n%1$s %2$d\n%3$s %4$d",
                                                        stringOnlyCaptureGlobalOver, valueOnlyCaptureGlobalOver, stringOnlyCapturePlayerOver, valueOnlyCapturePlayerOver)));
                                                return 1;
                                            }

                                            command.getSource().sendSystemMessage(literal("§l" + player + " packet counts§r:\n" +
                                                    playerCounts.get(player).entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue()
                                                                    .reversed()).skip((long) (page - 1) * 5)
                                                            .limit(5).map(entry -> entry.getKey() + " -> " + entry.getValue()).collect(Collectors.joining("\n"))
                                            ));
                                            return 0;
                                        }))

                                        //networklogger get player(ServerPlayer) page(Integer) highest
                                        .then(Commands.literal("highest").executes(command -> {
                                            int page = IntegerArgumentType.getInteger(command, "page");
                                            String player = EntityArgument.getPlayer(command, "player").getName().getString();

                                            if (playerHighest.get(player).isEmpty()) {
                                                command.getSource().sendSystemMessage(literal(String.format("Empty! Check ping requirements!\n%1$s %2$d\n%3$s %4$d",
                                                        stringOnlyCaptureGlobalOver, valueOnlyCaptureGlobalOver, stringOnlyCapturePlayerOver, valueOnlyCapturePlayerOver)));
                                                return 1;
                                            }

                                            command.getSource().sendSystemMessage(literal("§l" + player + " highest packets§r:\n" +
                                                    playerHighest.get(player).entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue()
                                                                    .reversed()).skip((long) (page - 1) * 5)
                                                            .limit(5).map(entry -> entry.getKey() + " -> " + entry.getValue()).collect(Collectors.joining("\n"))
                                            ));
                                            return 0;
                                        }))

                                        //networklogger get player(ServerPlayer) page(integer) average
                                        .then(Commands.literal("average").executes(command -> {
                                            int page = IntegerArgumentType.getInteger(command, "page");
                                            String player = EntityArgument.getPlayer(command, "player").getName().getString();

                                            if (playerAverage.get(player).isEmpty()) {
                                                command.getSource().sendSystemMessage(literal(String.format("Empty! Check ping requirements!\n%1$s %2$d\n%3$s %4$d",
                                                        stringOnlyCaptureGlobalOver, valueOnlyCaptureGlobalOver, stringOnlyCapturePlayerOver, valueOnlyCapturePlayerOver)));
                                                return 1;
                                            }

                                            command.getSource().sendSystemMessage(literal("§l" + player + " average size over time§r:\n" +
                                                    playerAverage.get(player).entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue()
                                                                    .reversed()).skip((long) (page - 1) * 5).limit(5)
                                                            .map(entry -> entry.getKey() + " -> " + entry.getValue()).collect(Collectors.joining("\n"))
                                            ));
                                            return 0;
                                        }))

                                        //networklogger get player(ServerPlayer) page(integer) packets_per_second
                                        .then(Commands.literal("packets_per_second").executes(command -> {
                                            int page = IntegerArgumentType.getInteger(command, "page");
                                            String player = EntityArgument.getPlayer(command, "player").getName().getString();

                                            if (player_packets_per_second.get(player).isEmpty()) {
                                                command.getSource().sendSystemMessage(literal(String.format("Empty! Check ping requirements!\n%1$s %2$d\n%3$s %4$d",
                                                        stringOnlyCaptureGlobalOver, valueOnlyCaptureGlobalOver, stringOnlyCapturePlayerOver, valueOnlyCapturePlayerOver)));
                                                return 1;
                                            }

                                            command.getSource().sendSystemMessage(literal("§l" + player + " packets per second§r:\n" +
                                                    player_packets_per_second.get(player).entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue()
                                                                    .reversed()).skip((long) (page - 1) * 5).limit(5)
                                                            .map(entry -> entry.getKey() + " -> " + entry.getValue()).collect(Collectors.joining("\n"))
                                            ));
                                            return 0;
                                        }))
                                )))
                ))

                //networklogger config
                .then(Commands.literal("config").requires(source -> source.hasPermission(4))

                        //networklogger config message
                        .then(Commands.literal("message")

                                //networklogger config message enabled
                                .then(Commands.literal("enable").executes(command -> {
                                    valueMessageEnabled = true;
                                    HandleFiles.writeConfig();

                                    command.getSource().sendSystemMessage(literal(
                                            String.format("Message: %1$s\nPing over: %2$d\nEnabled: %3$b", valueSendMessage, valuePingExcess, valueMessageEnabled)));
                                    return 0;
                                }))

                                //networklogger config message disable
                                .then(Commands.literal("disable").executes(command -> {
                                    valueMessageEnabled = false;
                                    HandleFiles.writeConfig();

                                    command.getSource().sendSystemMessage(literal(
                                            String.format("Message: %1$s\nPing over: %2$d\nEnabled: %3$b", valueSendMessage, valuePingExcess, valueMessageEnabled)));
                                    return 0;
                                }))

                                //networklogger config message value(Integer)
                                .then(Commands.literal("value").then(Commands.argument("ping", IntegerArgumentType.integer()).executes(command -> {
                                    valuePingExcess = IntegerArgumentType.getInteger(command, "ping");
                                    HandleFiles.writeConfig();

                                    command.getSource().sendSystemMessage(literal(
                                            String.format("Message: %1$s\nPing over: %2$d\nEnabled %3$b", valueSendMessage, valuePingExcess, valueMessageEnabled)));
                                    return 0;
                                })))

                                //networklogger config message string(String)
                                .then(Commands.literal("string").then(Commands.argument("message", StringArgumentType.greedyString()).executes(command -> {
                                    valueSendMessage = StringArgumentType.getString(command, "message");
                                    HandleFiles.writeConfig();

                                    command.getSource().sendSystemMessage(literal(
                                            String.format("Message: %1$s\nPing over: %2$d\nEnabled %3$b", valueSendMessage, valuePingExcess, valueMessageEnabled)));
                                    return 0;
                                })))
                        )

                        //networklogger config onlyCapturePlayerOverValue(Integer)
                        .then(Commands.literal("onlyCapturePlayerOverValue")
                                .then(Commands.argument("ping", IntegerArgumentType.integer()).executes(command -> {
                                    valueOnlyCapturePlayerOver = IntegerArgumentType.getInteger(command, "ping");
                                    HandleFiles.writeConfig();

                                    command.getSource().sendSystemMessage(literal("onlyCapturePlayerOverValue set to " + valueOnlyCapturePlayerOver));
                                    return 0;
                                })))

                        //networklogger config onlyCaptureGlobalOverValue(Integer)
                        .then(Commands.literal("onlyCaptureGlobalOverValue")
                                .then(Commands.argument("ping", IntegerArgumentType.integer()).executes(command -> {
                                    valueOnlyCaptureGlobalOver = IntegerArgumentType.getInteger(command, "ping");
                                    HandleFiles.writeConfig();

                                    command.getSource().sendSystemMessage(literal("onlyCaptureGlobalOverValue set to " + valueOnlyCaptureGlobalOver));
                                    return 0;
                                })))

                        .then(Commands.literal("deleteFileOnExit").then(Commands.argument("true/false", BoolArgumentType.bool()).executes(command -> {
                            valueDeleteFileOnExit = BoolArgumentType.getBool(command, "true/false");
                            HandleFiles.writeConfig();

                            command.getSource().sendSystemMessage(literal("deleteFileOnExit set to " + valueDeleteFileOnExit));
                            return 0;
                        })))
                )

                //networklogger clear
                .then(Commands.literal("clear").requires(source -> source.hasPermission(4)).executes(command -> {
                    clear();
                    command.getSource().sendSystemMessage(literal("Cleared!"));
                    return 0;
                }))

                //networklogger write
                .then(Commands.literal("write").executes(command -> {
                    CommandSourceStack source = command.getSource();
                    HandleFiles.writeGlobal(source.getPlayer(), "write command");
                    if (command.getSource() == null) return 0;

                    if (command.getSource().hasPermission(4)) {
                        command.getSource().sendSystemMessage(literal("file written in " +
                                Main.networkloggerfolder.resolve("networklogger" + HandleFiles.numberOfGlobalFiles + ".txt").toAbsolutePath()));
                    } else {
                        command.getSource().sendSystemMessage(literal("file written!"));
                    }
                    return 0;
                }))


                //networklogger enable
                .then(Commands.literal("enable").requires(source -> source.hasPermission(4)).executes(command -> {
                    enabled = true;
                    command.getSource().sendSystemMessage(literal("NetworkLogger enabled!"));
                    return 0;
                }))


                //networklogger disable
                .then(Commands.literal("disable").requires(source -> source.hasPermission(4)).executes(command -> {
                    enabled = false;
                    command.getSource().sendSystemMessage(literal("NetworkLogger disabled!"));
                    return 0;
                }))
        );
    }
}
