package net.matteo.force_animation.utils;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

public class Values {

    public static Map<ServerPlayer, Integer> player = new HashMap<>();
    public static Map<ServerPlayer, Boolean> playing_emotecraft_animation = new HashMap<>();

    public static boolean emotecraft_loaded = false;
}
