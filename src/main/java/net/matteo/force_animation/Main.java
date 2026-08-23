package net.matteo.force_animation;

import net.matteo.force_animation.utils.Values;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

@Mod("force_animation")
public class Main {
    public static Logger logger = LogManager.getLogger("force_animation");

    public Main() {
        Values.emotecraft_loaded = ModList.get().isLoaded("emotecraft");
    }
}
