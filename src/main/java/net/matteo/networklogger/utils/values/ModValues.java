package net.matteo.networklogger.utils.values;

import io.netty.util.AttributeKey;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ModValues {
    public static boolean enabled = true;

    // Ping Excess
    public static boolean pingExcess = false;
    public static boolean startedPingExcess = false;
    public static boolean endedPingExcess = false;

    public static double pingExcessTime = 0;

    // Profiler
    public static boolean profiling = false;
    public static boolean writing = false;
    public static double profilingTime = 0;

    public static String profilingPacket = "";
    public static String profilingPlayer = "";
    public static String player_that_started_the_profiler;

    public static byte numberOfProfilerFiles = 0;
    public static short numberOfPackets = 0;

    //Updater
    public static final AttributeKey<ServerPlayer> channelPlayer = AttributeKey.valueOf("networklogger:player");
    public static ConcurrentMap<String, Double> playTime = new ConcurrentHashMap<>();
    public static double time = 0.05;

    public static ConcurrentMap<String, Long> accumulated = new ConcurrentHashMap<>();
    public static ConcurrentMap<String, Integer> highest = new ConcurrentHashMap<>();
    public static ConcurrentMap<String, Integer> counts = new ConcurrentHashMap<>();
    public static ConcurrentMap<String, Integer> average = new ConcurrentHashMap<>();
    public static ConcurrentMap<String, Integer> packets_per_second = new ConcurrentHashMap<>();

    public static ConcurrentMap<String, Integer> chunkAccumulated = new ConcurrentHashMap<>();
    public static ConcurrentMap<String, Integer> chunkCounts = new ConcurrentHashMap<>();
    public static ConcurrentMap<String, Integer> chunkAverage = new ConcurrentHashMap<>();
    public static ConcurrentMap<String, Integer> chunk_packets_per_second = new ConcurrentHashMap<>();
    public static ConcurrentMap<String, Integer> chunkHighest = new ConcurrentHashMap<>();

    public static ConcurrentMap<String, Integer> averageSentPackets = new ConcurrentHashMap<>();
    public static ConcurrentMap<String, ConcurrentMap<String, Long>> playerAccumulated = new ConcurrentHashMap<>();
    public static ConcurrentMap<String, ConcurrentMap<String, Integer>> playerHighest = new ConcurrentHashMap<>();
    public static ConcurrentMap<String, ConcurrentMap<String, Integer>> playerCounts = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, ConcurrentMap<String, Integer>> playerAverage = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, ConcurrentMap<String, Integer>> player_packets_per_second = new ConcurrentHashMap<>();

    public static void clear() {
        highest.clear();
        playerHighest.clear();

        chunkAverage.clear();
        playerAverage.clear();
        average.clear();

        accumulated.clear();
        chunkAccumulated.clear();
        playerAccumulated.clear();
        chunk_packets_per_second.clear();

        counts.clear();
        playerCounts.clear();
        chunkCounts.clear();

        packets_per_second.clear();
        player_packets_per_second.clear();

        playTime.clear();
        time = 0.05;
    }
}
