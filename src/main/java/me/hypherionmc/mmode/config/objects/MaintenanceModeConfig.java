package me.hypherionmc.mmode.config.objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public final class MaintenanceModeConfig {

    private boolean enabled = false;

    private boolean doBackup = false;
    private String message = "This server is currently in maintenance mode! Please check back later";

    private String motd = "This server is currently undergoing maintenance!";

    private String maintenanceIcon = "";

    private List<AllowedUser> allowedUsers = new ArrayList<>();


    @Getter
    @Setter
    @AllArgsConstructor
    public final static class AllowedUser {
        private String name;
        private UUID uuid;
    }

}
