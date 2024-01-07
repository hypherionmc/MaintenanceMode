package me.hypherionmc.mmode.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.hypherionmc.mmode.ModConstants;
import me.hypherionmc.mmode.config.objects.MaintenanceModeConfig;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public final class ConfigController {

    private static File confFile;

    public static MaintenanceModeConfig initConfig() {
        File confDir = new File("config");
        confFile = new File( confDir + File.separator + "mmode.json");
        if (!confDir.exists()) {
            confDir.mkdirs();
        }

        if (!confFile.exists() || confFile.length() < 10) {
            try {
                saveConfig(new MaintenanceModeConfig());
            } catch (Exception e) {
                ModConstants.LOG.error("Failed to save config file: {}", e.getMessage());
            }
        }
        return loadConfig();
    }

    public static void saveConfig(MaintenanceModeConfig config) throws IOException {
        try (FileWriter writer = new FileWriter(confFile)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(config, writer);
            writer.flush();
        }
    }

    public static MaintenanceModeConfig loadConfig() {
        try (FileReader fileWriter = new FileReader(confFile)) {
            Gson gson = new Gson();
            try {
                return gson.fromJson(fileWriter, MaintenanceModeConfig.class);
            } catch (Exception e) {
                ModConstants.LOG.error("Failed to load config file: {}", e.getMessage());
            }
        } catch (Exception e) {
            ModConstants.LOG.error("Failed to load config file: {}", e.getMessage());
        }

        return new MaintenanceModeConfig();
    }

}
