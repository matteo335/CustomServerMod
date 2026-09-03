package net.matteo.networklogger.packets;

import static net.matteo.networklogger.utils.values.ConfigValues.*;

import net.matteo.networklogger.utils.HandleFiles;
import net.matteo.networklogger.Main;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static net.matteo.networklogger.utils.values.ModValues.*;

public class UpdateData {

    public static boolean test = false;

    public static final ExecutorService updateExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "networklogger-updater");
        thread.setDaemon(true);
        return thread;
    });

    public static void updateDataThread(@Nullable ServerPlayer player, Packet<?> packet, Integer size) {
        updateExecutor.execute(() -> {
            try {
                updateData(player, packet, size);
            } catch (Throwable throwable) {
                Main.logger.error("Error in NetworkLogger updateData thread", throwable);
            }
        });
    }

    @SuppressWarnings("DataFlowIssue")
    public static void updateData(@Nullable ServerPlayer player, Packet<?> packet, Integer size) {
        String ps = player != null ? player.getName().getString() : null;

        try {
            try {
                if (player != null && (player.latency < valueOnlyCaptureGlobalOver) && playTime.getOrDefault(ps, 0.0) >= valueConnectionStable)
                    return;
            } catch (Throwable exception) {
                Main.logger.error("onlyCaptureGlobalOverValue errored", exception);
            }

            String id = packet instanceof ClientboundCustomPayloadPacket custom ? custom.getIdentifier().toString() : packet.getClass().getSimpleName();

            try {
                accumulated.put(id, accumulated.getOrDefault(id, (long) 0) + size);
            } catch (Throwable exception) {
                Main.logger.error("accumulated.put() errored", exception);
            }

            try {
                counts.put(id, counts.getOrDefault(id, 0) + 1);
            } catch (Throwable exception) {
                Main.logger.error("counts.put() errored", exception);
            }

            try {
                average.put(id, (int) (accumulated.get(id) / counts.get(id)));
            } catch (Throwable exception) {
                Main.logger.error("average.put() errored", exception);
            }

            try {
                counts.forEach((key, value) -> packets_per_second.put(key, (int) (value / time)));
            } catch (Throwable exception) {
                Main.logger.error("packets_per_second.put() errored", exception);
            }

            try {
                if (player == null || player.latency < valueOnlyCapturePlayerOver) return;
            } catch (Throwable exception) {
                Main.logger.error("onlyCapturePlayerOverValue errored", exception);
            }

            String chunkPosition = player.chunkPosition().toString();

            try {
                chunkAccumulated.merge(chunkPosition, size, Integer::sum);
            } catch (Throwable exception) {
                Main.logger.error("regionAccumulated errored", exception);
            }

            try {
                chunkCounts.merge(chunkPosition, 1, Integer::sum);
            } catch (Throwable exception) {
                Main.logger.error("regionCount errored", exception);
            }

            try {
                chunkAverage.put(chunkPosition, chunkAccumulated.getOrDefault(chunkPosition, 0) / chunkCounts.getOrDefault(chunkPosition, 0));
            } catch (Throwable exception) {
                Main.logger.error("regionAverage errored", exception);
            }

            try {
                chunkCounts.forEach((chunk, value) -> chunk_packets_per_second.put(chunk, (int) (value / time)));
            } catch (Throwable exception) {
                Main.logger.error("chunk_packets_per_second errored", exception);
            }

            try {
                playerAccumulated.computeIfAbsent(ps, serverPlayer -> new ConcurrentHashMap<>()).merge(id, (long) size, Long::sum);
            } catch (Throwable exception) {
                Main.logger.error("playerAccumulated errored", exception);
            }

            try {
                playerCounts.computeIfAbsent(ps, severPlayer -> new ConcurrentHashMap<>()).merge(id, 1, Integer::sum);
            } catch (Throwable exception) {
                Main.logger.error("playerCounts errored", exception);
            }

            try {
                playerAverage.computeIfAbsent(ps, ServerPlayer -> new ConcurrentHashMap<>())
                        .put(id, (int) (playerAccumulated.computeIfAbsent(ps, Player -> new ConcurrentHashMap<>()).get(id) / playerCounts.get(ps).get(id)));
            } catch (Throwable exception) {
                Main.logger.error("playerAverage errored", exception);
            }

            try {
                playerCounts.forEach((Player, map) -> map.forEach((string, count) ->
                        player_packets_per_second.computeIfAbsent(Player, serverPlayer -> new ConcurrentHashMap<>()).put(string, (int) (count / time))));
            } catch (Throwable exception) {
                Main.logger.error("player_packets_per_second errored", exception);
            }

            try {
                if (size > chunkHighest.getOrDefault(chunkPosition, 0)) chunkHighest.put(chunkPosition, size);
            } catch (Throwable exception) {
                Main.logger.error("chunkHighest errored", exception);
            }

            if (size > highest.getOrDefault(id, 0)) {
                try {
                    highest.put(id, size);
                } catch (Throwable exception) {
                    Main.logger.error("highest.put(id, size); errored", exception);
                }

                try {
                    playerHighest.computeIfAbsent(ps, serverPlayer -> new ConcurrentHashMap<>()).put(id, size);
                } catch (Throwable exception) {
                    Main.logger.error("playerHighest.computeIfAbsent 1 errored", exception);
                }
            } else if (size > playerHighest.computeIfAbsent(ps, serverPlayer -> new ConcurrentHashMap<>()).getOrDefault(id, 0)) {
                try {
                    playerHighest.computeIfAbsent(ps, serverPlayer -> new ConcurrentHashMap<>()).put(id, size);
                } catch (Throwable exception) {
                    Main.logger.error("playerHighest.computeIfAbsent 2 errored", exception);
                }
            }

            if (player.latency > valuePingExcess && valueMessageEnabled && playTime.get(ps) > valueConnectionStable && pingExcessTime > 300 && !test) {
                startedPingExcess = false;
                pingExcess = false;
                test = true;
                String message = valueSendMessage.replace("(player)", ps).replace("(ping)", String.valueOf(player.latency));
                player.getServer().getCommands().performPrefixedCommand(player.getServer().createCommandSourceStack(), "say " + message);
            }

            if (player.latency > valueClearIfPingExcess && valueWriteAfterPingExcess && endedPingExcess) {
                endedPingExcess = false;
                HandleFiles.writeGlobal(player, "pingExcess lasted for " + pingExcessTime);

                if (player.latency > valueClearIfPingExcess && !pingExcess) {
                    pingExcessTime = 0;
                    startedPingExcess = true;
                    clear();
                    pingExcess = true;
                }
            }

        } catch (Throwable exception) {
            Main.logger.error("NetworkLogger errored in PacketProfiler, please report to the author!", exception);
        }
    }
}
