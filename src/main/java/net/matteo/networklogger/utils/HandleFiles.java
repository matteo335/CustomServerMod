package net.matteo.networklogger.utils;

import static net.matteo.networklogger.utils.values.ConfigValues.*;
import static net.matteo.networklogger.utils.values.ModValues.*;

import net.matteo.networklogger.Main;
import net.minecraft.server.level.ServerPlayer;

import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.annotation.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@SuppressWarnings("CallToPrintStackTrace")
public class HandleFiles {

    public static Integer numberOfGlobalFiles = 0;
    public static Path configDir; //Set by the mod loader on initialization
    public static Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static File config; //Set by initialize()

    public static void initialize() {
        config = configDir.resolve("networklogger.json").toFile();
        if (!config.exists()) writeConfig();
        readConfig();
    }

    public static void writeConfig() {
        try (FileWriter writer = new FileWriter(config)) {
            JsonObject object = new JsonObject();

            object.addProperty(stringWriteIfContainsString, valueWriteIfContains);
            object.addProperty(stringClearIfPingExcess, valueClearIfPingExcess);
            object.addProperty(stringWriteAfterPingExcess, valueWriteAfterPingExcess);

            object.addProperty(stringOnlyCapturePlayerOver, valueOnlyCapturePlayerOver);
            object.addProperty(stringOnlyCaptureGlobalOver, valueOnlyCaptureGlobalOver);
            object.addProperty(stringMessageEnabled, valueMessageEnabled);

            object.addProperty(stringMessageValue, valuePingExcess);
            object.addProperty(stringSendMessage, valueSendMessage);
            object.addProperty(stringConnectionStable, valueConnectionStable / 60);

            object.addProperty(stringDeleteFileOnExit, valueDeleteFileOnExit);

            gson.toJson(object, writer);
        } catch (Exception exception) {
            throw new RuntimeException("NetworkLogger failed to write the config file", exception);
        }
    }

    public static void readConfig() {
        boolean missing = false;

        try (FileReader reader = new FileReader(config)) {
            JsonObject object = gson.fromJson(reader, JsonObject.class);

            if (object.has(stringWriteIfContainsString)) valueWriteIfContains = object.get(stringWriteIfContainsString).getAsBoolean(); else missing = true;
            if (object.has(stringClearIfPingExcess)) valueClearIfPingExcess = object.get(stringClearIfPingExcess).getAsInt(); else missing = true;
            if (object.has(stringWriteAfterPingExcess)) valueWriteAfterPingExcess = object.get(stringWriteAfterPingExcess).getAsBoolean(); else missing = true;

            if (object.has(stringOnlyCapturePlayerOver)) valueOnlyCapturePlayerOver = object.get(stringOnlyCapturePlayerOver).getAsInt(); else missing = true;
            if (object.has(stringOnlyCaptureGlobalOver)) valueOnlyCaptureGlobalOver = object.get(stringOnlyCaptureGlobalOver).getAsInt(); else missing = true;

            if (object.has(stringMessageEnabled)) valueMessageEnabled = object.get(stringMessageEnabled).getAsBoolean(); else missing = true;
            if (object.has(stringMessageValue)) valuePingExcess = object.get(stringMessageValue).getAsInt(); else missing = true;
            if (object.has(stringSendMessage)) valueSendMessage = object.get(stringSendMessage).getAsString(); else missing = true;

            if (object.has(stringConnectionStable)) valueConnectionStable = object.get(stringConnectionStable).getAsInt() * 60; else missing = true;
            if (object.has(stringDeleteFileOnExit)) valueDeleteFileOnExit = object.get(stringDeleteFileOnExit).getAsBoolean(); else missing = true;
        } catch (Throwable exception) {
            Main.logger.error("NetworkLogger failed to read the config file", exception);
        }

        if (missing) writeConfig();
    }

    @SuppressWarnings("ConstantConditions")
    public static void writeGlobal(ServerPlayer player, @Nullable String message) {
        File file = Main.networkloggerfolder.resolve("networklogger" + numberOfGlobalFiles + ".txt").toFile();
        if (file.exists()) {
            if (numberOfGlobalFiles > 10000) {
                Main.logger.error("More than 10 000 networklogger files detected, please delete them!");
                return;
            }

            numberOfGlobalFiles++;
            writeGlobal(player, message);
            return;
        }

        try (FileWriter writer = new FileWriter(file)) {
            final Map<String, Long> Accumulated = new HashMap<>(accumulated);
            final Map<String, Integer> Counts = new HashMap<>(counts);
            final Map<String, Integer> Highest = new HashMap<>(highest);
            final Map<String, Integer> Average = new HashMap<>(average);
            final Map<String, Integer> Packets_per_second = new HashMap<>(packets_per_second);
            final Map<String, Double> PlayTime = new HashMap<>(playTime);

            final Map<String, Integer> AverageSentPackets = new HashMap<>(averageSentPackets);
            final Map<String, Map<String, Long>> PlayerAccumulated = new HashMap<>(playerAccumulated);
            final Map<String, Map<String, Integer>> PlayerCounts = new HashMap<>(playerCounts);
            final Map<String, Map<String, Integer>> PlayerAverage = new HashMap<>(playerAverage);
            final Map<String, Map<String, Integer>> PlayerHighest = new HashMap<>(playerHighest);
            final Map<String, Map<String, Integer>> Player_packets_per_second = new HashMap<>(player_packets_per_second);

            final Comparator<Map.Entry<String, Integer>> sort = Map.Entry.<String, Integer>comparingByValue().reversed();
            final Comparator<Map.Entry<String, Long>> sortLong = Map.Entry.<String, Long>comparingByValue().reversed();

            final Collector<Map.Entry<String, Integer>, ?, LinkedHashMap<String, Integer>> collector =
                    Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new);
            final Collector<Map.Entry<String, Long>, ?, LinkedHashMap<String, Long>> collectorLong =
                    Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new);

            writer.write(
                    player + "\nPing:" + player.latency + "\n" + message +

                            "\n\nPLAYERS PING\n" + player.getServer().getPlayerList().getPlayers().stream()
                            .map(Player -> Player.getName().getString() + "=" + Player.latency).toList() +

                            "\n\nACCUMULATED BYTES\n" + Accumulated.entrySet().stream().sorted(sortLong).toList() +

                            "\n\nPACKET COUNTS\n" + Counts.entrySet().stream().sorted(sort).toList() +

                            "\n\nHEAVIEST PACKET OF EACH CLASSES\n" + Highest.entrySet().stream().sorted(sort).toList() +

                            "\n\nAVERAGE BYTES PER PACKET\n" + Average.entrySet().stream().sorted(sort).toList() +

                            "\n\nPACKETS PER SECOND\n" + Packets_per_second.entrySet().stream().sorted(sort).toList() +

                            "\n\nPLAYERS ACCUMULATED\n" + PlayerAccumulated.entrySet().stream().map(entry -> entry.getKey() + "=" +
                            entry.getValue().entrySet().stream().sorted(sortLong).collect(collectorLong)).toList() +

                            "\n\nPLAYERS COUNT\n" + PlayerCounts.entrySet().stream().map(entry -> entry.getKey() + "=" +
                            entry.getValue().entrySet().stream().sorted(sort).collect(collector)).toList() +

                            "\n\nPLAYERS AVERAGE\n" + PlayerAverage.entrySet().stream().map(entry -> entry.getKey() + "=" +
                            entry.getValue().entrySet().stream().sorted(sort).collect(collector)).toList() +

                            "\n\nPLAYERS HEAVIEST PACKETS OF EACH CLASSES\n" + PlayerHighest.entrySet().stream().map(entry -> entry.getKey() +
                            "=" + entry.getValue().entrySet().stream().sorted(sort).collect(collector)).toList() +

                            "\n\nPLAYERS PACKETS PER SECOND\n" + Player_packets_per_second.entrySet().stream().map(entry -> entry.getKey() +
                            "=" + entry.getValue().entrySet().stream().sorted(sort).collect(collector)).toList() +

                            "\n\nPLAYERS AVERAGE SENT PACKETS\n" + AverageSentPackets.entrySet().stream().sorted(Map.Entry.comparingByValue()).toList() +

                            "\n\n PLAYERS PLAY TIME IN SECOND\n" + PlayTime.entrySet().stream().sorted(Map.Entry.comparingByValue()).toList()
            );

            Main.logger.info("file written in {}", file);
        } catch (Throwable exception) {
            Main.logger.error("Networklogger failed to write global file");
            exception.printStackTrace();
        }
    }
}
