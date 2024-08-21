package com.hypherionmc.mmode.config;

import com.hypherionmc.craterlib.core.config.AbstractConfig;
import com.hypherionmc.craterlib.core.config.ConfigController;
import com.hypherionmc.craterlib.core.config.annotations.NoConfigScreen;
import com.hypherionmc.craterlib.core.config.formats.JsonConfigFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.hypherionmc.mmode.ModConstants;
import org.apache.commons.io.FileUtils;
import shadow.hypherionmc.moonconfig.core.conversion.ObjectConverter;
import shadow.hypherionmc.moonconfig.core.file.FileConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoConfigScreen
public final class MaintenanceModeConfig extends AbstractConfig<MaintenanceModeConfig> {

    // DO NOT REMOVE TRANSIENT HERE... OTHERWISE, THE STUPID CONFIG LIBRARY
    // WILL TRY TO WRITE THESE TO THE CONFIG
    public transient static MaintenanceModeConfig INSTANCE;
    public transient static int configVer = 2;
    public transient static boolean hasConfigLoaded = false;
    public transient static boolean wasReload = false;

    private boolean enabled = false;
    private boolean doBackup = false;
    private String message = "This server is currently in maintenance mode! Please check back later";
    private String motd = "This server is currently undergoing maintenance!";
    private String maintenanceIcon = "";
    private Schedule schedule = new Schedule();
    private int configVersion = 2;
    private List<AllowedUser> allowedUsers = new ArrayList<>();

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public final static class AllowedUser {
        private String name;
        private String uuid;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public final static class Schedule {
        private String startTime = "";
        private String endTime = "";
        private boolean disableOnRestart = false;
        private String maintanceNotifyMessage = "Maintenance is starting in %s minute(s)";
    }

    // Config Methods
    public MaintenanceModeConfig(boolean wasReload) {
        super(ModConstants.MOD_ID, "mmode.json");
        MaintenanceModeConfig.wasReload = wasReload;
        registerAndSetup(this);
    }

    public MaintenanceModeConfig() {
        this(false);
    }

    @Override
    public void registerAndSetup(MaintenanceModeConfig config) {
        if (this.getConfigPath().exists() && this.getConfigPath().length() >= 2L) {
            this.migrateConfig(config);
        } else {
            this.saveConfig(config);
        }

        if (!wasReload) {
            ConfigController.register_config(this);
        }
        this.configReloaded();
    }

    @Override
    public void migrateConfig(MaintenanceModeConfig conf) {
        FileConfig config = FileConfig.builder(getConfigPath()).build();
        FileConfig newConfig = FileConfig.builder(getConfigPath()).sync().build();
        config.load();

        if (config.getOrElse("configVersion", 0) == configVer) {
            newConfig.close();
            config.close();
            return;
        }

        new ObjectConverter().toConfig(conf, newConfig);
        if (getConfigFormat() instanceof JsonConfigFormat<MaintenanceModeConfig> json) {
            json.updateConfigValues(config, newConfig, newConfig, "");
        }
        newConfig.set("configVersion", configVer);

        try {
            FileUtils.copyFile(getConfigPath(), new File(getConfigPath().getAbsolutePath().replace(".json", ".old")));
        } catch (IOException e) {
            ModConstants.LOG.warn("Failed to create config backup.", e);
        }
        newConfig.save();

        newConfig.close();
        config.close();
    }

    @Override
    public void configReloaded() {
        INSTANCE = readConfig(this);
        hasConfigLoaded = true;
    }
}
