package me.hypherionmc.mmode;

import me.hypherionmc.mmode.config.ConfigController;
import me.hypherionmc.mmode.config.objects.MaintenanceModeConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public class CommonClass {

    public static MaintenanceModeConfig config;
    private static MinecraftServer mcServer;

    public static void init(MinecraftServer server) {
        config = ConfigController.initConfig();
        mcServer = server;

        if (config != null) {
            ModConstants.LOG.info(config.isEnabled() ? "Maintenance mode is active!" : "Maintenance mode is off");
        }
    }

    public static void kickAllPlayers() {
        if (mcServer != null && config != null) {
            mcServer.getPlayerList().getPlayers().forEach(serverPlayer -> {
                if (config.getAllowedUsers().stream().noneMatch(allowedUser -> allowedUser.getUuid().equals(serverPlayer.getPersistentID()))) {
                    serverPlayer.connection.disconnect(new TextComponentString("Server is currently undergoing maintenance"));
                }
            });
        }
    }

}
