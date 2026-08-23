package net.matteo.networklogger.packets;

import static net.matteo.networklogger.utils.values.ConfigValues.*;
import static net.matteo.networklogger.utils.values.ModValues.*;
import static net.matteo.networklogger.mappings.MappingService.*;

import net.matteo.networklogger.Main;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.io.IOException;
import java.io.BufferedWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class PacketProfiler {
    public static Path profilerFile = Main.networkloggerfolder.resolve("profiler").resolve("profiler" + numberOfProfilerFiles + ".json");

    public static final ExecutorService thread = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "networklogger-profiler");
        thread.setDaemon(true);
        return thread;
    });

    public static void writePacketThread(Packet<?> packet, Level level, Integer size) {
        thread.execute(() -> {
            try {
                writePacket(packet, level, size);
            } catch (Throwable exception) {
                Main.logger.error("Error in writePacket thread", exception);
            }
        });
    }

    public static void writePacket(Packet<?> packet, Level level, Integer size) {

        while (Files.exists(profilerFile) && !writing && profiling) {
            numberOfProfilerFiles++;
            profilerFile = Main.networkloggerfolder.resolve("profiler").resolve("profiler" + numberOfProfilerFiles + ".json");
        }

        if (!Files.exists(profilerFile)) try {
            Files.createDirectories(profilerFile.getParent());
            Files.createFile(profilerFile);
            writing = true;
        } catch (Throwable exception) {
            Main.logger.error("Caught exception while creating the profiler file", exception);
        }

        if (profilerFile.toFile().length() > 100_000_000) {
            profilingTime = -35;
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(profilerFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            String packetName = packet instanceof ClientboundCustomPayloadPacket custom ? custom.getIdentifier().toString() : packet.getClass().getSimpleName();

            writer.write(String.format("\"Time in seconds\": %1$f \n\"Packet number\": %2$d \n\"Size\": %3$d bytes\n %4$s ",
                    profilingTime, numberOfPackets, size, packetName));
            dumpObject(packet, writer, 0, level);
            numberOfPackets++;
        } catch (Throwable exception) {
            Main.logger.error("Caught exception while the writing of the profiler file", exception);
        }

    }

    public static void dumpObject(Object object, BufferedWriter writer, int depth, Level level) {
        if (valueDeleteFileOnExit) profilerFile.toFile().deleteOnExit();

        try {
            if (object == null) {
                writer.write("null");
                return;
            }

            if (!isSimpleType(object.getClass()) && depth > 50) {
                writer.write("likely circular reference, cutting out this " + object.getClass().getSimpleName());
                return;
            }

            if (object instanceof FriendlyByteBuf buf) {
                writer.write("FriendlyByteBuf {");
                indent(writer, depth + 1);
                writer.write("\nreaderIndex=" + buf.readerIndex());

                indent(writer, depth + 1);
                writer.write("\nwriterIndex=" + buf.writerIndex());
                indent(writer, depth + 1);

                writer.write("\nreadableBytes=" + buf.readableBytes());
                indent(writer, depth + 1);
                writer.write("\ncapacity=" + buf.capacity());

                indent(writer, depth + 1);
                writer.write("\npreview=" + hexPreview(buf));
                indent(writer, depth + 1);

                writer.write("\n}");
                return;
            }

            if (object instanceof ByteBuf buf) {
                writer.write("ByteBuf {");
                indent(writer, depth + 1);
                writer.write("\nreaderIndex=" + buf.readerIndex());
                indent(writer, depth + 1);

                writer.write("\nwriterIndex=" + buf.writerIndex());
                indent(writer, depth + 1);
                writer.write("\nreadableBytes=" + buf.readableBytes());

                indent(writer, depth + 1);
                writer.write("\ncapacity=" + buf.capacity());
                indent(writer, depth + 1);

                writer.write("\n}");
                return;
            }

            if (object instanceof ByteBufAllocator allocator) {
                writer.write("\nByteBufAllocator {\n"
                        + allocator.getClass().getName()
                        + "\n}");
            }

            if (object instanceof Entity entity) {
                writer.write(entity + " {");

                indent(writer, depth + 1);
                writer.write("\nid = " + entity.getId());

                indent(writer, depth + 1);
                writer.write("\nuuid = " + entity.getUUID());

                indent(writer, depth + 1);
                writer.write("\npos = " + entity.position());

                indent(writer, depth);
                writer.write("\n}");

                return;
            }

            if (object instanceof BitSet bitSet) {
                writer.write("BitSet { cardinality=" + bitSet.cardinality() + ", length=" + bitSet.length() + ", setBits=" + bitSet + "}\n");
                return;
            }

            // Handle Level before reflection
            if (object instanceof Level objectLevel) {
                writer.write("Level { dimension=" + objectLevel.dimension().location() + "}\n");
                return;
            }

            Class<?> clazz = object.getClass();

            if (isSimpleType(clazz)) {
                writer.write(String.valueOf(object));
                return;
            }

            if (clazz.isArray()) {
                writer.write("[");

                int length = Array.getLength(object);

                for (int i = 0; i < Math.min(length, 50); i++) {
                    if (i > 0) {
                        writer.write(", ");
                    }

                    dumpObject(Array.get(object, i), writer, depth + 1, level);
                }

                writer.write("]\n");
                return;
            }

            if (object instanceof Collection<?> collection) {
                writer.write("[\n");

                for (Object value : collection) {
                    indent(writer, depth + 1);
                    dumpObject(value, writer, depth + 1, level);
                    writer.write("\n");
                }

                indent(writer, depth);
                writer.write("]\n");
                return;
            }

            if (object instanceof Map<?, ?> map) {
                writer.write("{\n");

                for (Map.Entry<?, ?> entry : map.entrySet()) {

                    indent(writer, depth + 1);

                    dumpObject(entry.getKey(), writer, depth + 1, level);

                    writer.write(" = ");

                    dumpObject(entry.getValue(), writer, depth + 1, level);

                    writer.write("\n");
                }

                indent(writer, depth);
                writer.write("}\n");
                return;
            }

            writer.write("{\n");

            while (clazz != null && clazz != Object.class) {

                for (Field field : clazz.getDeclaredFields()) {
                    try {
                        String fieldName = get().table().lookup(field.getName()) != null ? get().table().lookup(field.getName()) : field.getName();
                        if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;

                        try {
                            field.setAccessible(true);
                        } catch (Throwable ignored) {}
                        Object value = field.get(object);

                        indent(writer, depth + 1);
                        writer.write(fieldName + " = ");

                        if (fieldName.equals("entityId") && value instanceof Integer entityId) {
                            Entity entity = level.getEntity(entityId);

                            if (entity != null) {
                                //noinspection deprecation
                                writer.write(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()) + " {\n");


                                indent(writer, depth + 2);
                                writer.write("id = " + entity.getId() + "\n");

                                indent(writer, depth + 2);
                                writer.write("uuid = " + entity.getUUID() + "\n");

                                indent(writer, depth + 2);
                                writer.write("pos = " + entity.position() + "\n");

                                indent(writer, depth + 1);
                                writer.write("}\n");
                            } else {
                                writer.write("Entity not found");
                            }
                        } else {
                            dumpObject(value, writer, depth + 1, level);
                        }

                        writer.write("\n");

                    } catch (Throwable exception) {
                        indent(writer, depth + 1);
                        writer.write(field.getName() + " = <error: " + exception.getClass().getSimpleName() + ">\n");
                    }
                }

                clazz = clazz.getSuperclass();
            }

            indent(writer, depth);
            writer.write("}\n");
        } catch (Throwable exception) {
            Main.logger.error("Exception during the profiler writer logic", exception);
        }
    }

    public static void indent(BufferedWriter writer, int depth) throws IOException {
        writer.write("    ".repeat(depth));
    }

    public static boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive()
                || clazz.isEnum()
                || Number.class.isAssignableFrom(clazz)
                || CharSequence.class.isAssignableFrom(clazz)
                || Boolean.class == clazz
                || Character.class == clazz
                || clazz == UUID.class;
    }

    public static String hexPreview(ByteBuf buf) {
        int len = buf.readableBytes();
        if (len == 0) return "(empty)";
        byte[] bytes = new byte[len];
        buf.getBytes(buf.readerIndex(), bytes); // absolute read, doesn't touch readerIndex
        StringBuilder stringBuilder = new StringBuilder(len * 2);
        for (byte b : bytes) stringBuilder.append(String.format("%02X", b));
        return stringBuilder.toString();
    }
}
