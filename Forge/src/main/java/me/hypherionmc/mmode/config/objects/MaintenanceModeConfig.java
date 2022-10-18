package me.hypherionmc.mmode.config.objects;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MaintenanceModeConfig {

    private boolean enabled = false;

    private boolean doBackup = false;
    private String message = "This server is currently in maintenance mode! Please check back later";

    private String motd = "This server is currently undergoing maintenance!";
    private List<AllowedUser> allowedUsers = new ArrayList<>();
    private String maintenanceIcon = "server-icon.png";

    public MaintenanceModeConfig() {}

    public MaintenanceModeConfig(boolean enabled, boolean doBackup, String message, String motd, String maintenanceIcon, List<AllowedUser> allowedUsers) {
        this.enabled = enabled;
        this.message = message;
        this.motd = motd;
        this.maintenanceIcon = maintenanceIcon;
        this.doBackup = doBackup;
        this.allowedUsers = allowedUsers;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getMessage() {
        return message;
    }

    public List<AllowedUser> getAllowedUsers() {
        return allowedUsers;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setAllowedUsers(List<AllowedUser> allowedUsers) {
        this.allowedUsers = allowedUsers;
    }

    public void setDoBackup(boolean doBackup) {
        this.doBackup = doBackup;
    }

    public boolean isDoBackup() {
        return doBackup;
    }

    public String getMotd() {
        return motd;
    }

    public void setMotd(String motd) {
        this.motd = motd;
    }

    public void setMaintenanceIcon(String maintenanceIcon) {
        this.maintenanceIcon = maintenanceIcon;
    }

    public String getMaintenanceIcon() {
        return maintenanceIcon;
    }

    public static class AllowedUser {
        private String name;
        private UUID uuid;

        public AllowedUser(String username, UUID uuid) {
            this.name = username;
            this.uuid = uuid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public UUID getUuid() {
            return uuid;
        }

        public void setUuid(UUID uuid) {
            this.uuid = uuid;
        }
    }

}
