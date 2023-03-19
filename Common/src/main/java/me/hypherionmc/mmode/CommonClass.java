package me.hypherionmc.mmode;

import me.hypherionmc.mmode.config.ConfigController;
import me.hypherionmc.mmode.config.objects.MaintenanceModeConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommonClass {

    public static AtomicBoolean isDirty = new AtomicBoolean(false);

    public static MaintenanceModeConfig config;
    private static MinecraftServer mcServer;

    public static void init(MinecraftServer server) {
        config = ConfigController.initConfig();
        mcServer = server;

        // Workaround for icon not resetting, if server doesn't have an icon set
        File tmpIcon = new File("server-icon.png");
        if (!tmpIcon.exists()) {
            try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("mmicon.png")) {
                if (is != null) {
                    Files.copy(is, tmpIcon.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception ignored) {}
        }

        if (config != null) {
            ModConstants.LOG.info(config.isEnabled() ? "Maintenance mode is active!" : "Maintenance mode is off");
        }
    }

    public static void kickAllPlayers(String message) {
        if (mcServer != null && config != null) {
            mcServer.getPlayerList().getPlayers().forEach(serverPlayer -> {
                if (config.getAllowedUsers().stream().noneMatch(allowedUser -> allowedUser.getUuid().equals(serverPlayer.getUUID()))) {
                    serverPlayer.connection.disconnect(Component.literal(message));
                }
            });
        }
    }

}
