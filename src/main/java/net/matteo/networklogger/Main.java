package net.matteo.networklogger;

import net.matteo.networklogger.utils.HandleFiles;
import net.matteo.networklogger.mappings.MappingDownloader;
import net.matteo.networklogger.mappings.MappingService;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.FMLEnvironment;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.nio.file.Path;

@Mod("networklogger")
public class Main {
    public static Logger logger = LogManager.getLogger("networklogger");
    public static Path networkloggerfolder; //Set by the mod loader on initialization;
    public static Path gamedir;

    public Main() {
        if (FMLEnvironment.dist.isClient()) {
            logger.error("Networklogger only work inside a dedicated server!");
            return;
        }

        HandleFiles.configDir = FMLPaths.CONFIGDIR.get();
        networkloggerfolder = FMLPaths.GAMEDIR.get().resolve("networklogger");
        gamedir = FMLPaths.GAMEDIR.get();
        HandleFiles.initialize();
        //noinspection InstantiationOfUtilityClass
        new MappingDownloader();
        MappingService.get().initAsync();
    }
}