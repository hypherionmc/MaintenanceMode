package me.hypherionmc.mmode;

import com.google.common.base.Preconditions;
import me.hypherionmc.mmode.config.ConfigController;
import me.hypherionmc.mmode.config.objects.MaintenanceModeConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CommonClass {

    public static AtomicBoolean isDirty = new AtomicBoolean(false);
    public static MaintenanceModeConfig config;
    private static MinecraftServer mcServer;
    public static Optional<ServerStatus.Favicon> favicon = Optional.empty();
    public static Optional<ServerStatus.Favicon> backupIcon = Optional.empty();

    public static void init(MinecraftServer server) {
        config = ConfigController.initConfig();
        mcServer = server;

        if (config.getMaintenanceIcon() != null && !config.getMaintenanceIcon().isEmpty()) {
            File file = new File(config.getMaintenanceIcon());
            if (!file.exists())
                return;

            favicon = loadIcon(file);
        }

        if (!new File("server-icon.png").exists()) {
            backupIcon = loadIcon(Thread.currentThread().getContextClassLoader().getResourceAsStream("mmicon.png"));
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

    private static Optional<ServerStatus.Favicon> loadIcon(File file) {
        try {
            return loadIcon(new FileInputStream(file));
        } catch (Exception e) {
            ModConstants.LOG.error("Failed to load icon", e);
        }

        return Optional.empty();
    }

    private static Optional<ServerStatus.Favicon> loadIcon(InputStream inputStream) {
        try {
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            Preconditions.checkState(bufferedImage.getWidth() == 64, "Must be 64 pixels wide");
            Preconditions.checkState(bufferedImage.getHeight() == 64, "Must be 64 pixels high");
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "PNG", outputStream);
            return Optional.of(new ServerStatus.Favicon(outputStream.toByteArray()));
        } catch (Exception e) {
            ModConstants.LOG.error("Failed to load icon", e);
            return Optional.empty();
        }
    }

}
