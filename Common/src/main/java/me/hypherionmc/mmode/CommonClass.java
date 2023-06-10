package me.hypherionmc.mmode;

import com.google.common.base.Preconditions;
import me.hypherionmc.mmode.config.ConfigController;
import me.hypherionmc.mmode.config.objects.MaintenanceModeConfig;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

public class CommonClass {

    public static MaintenanceModeConfig config;
    private static MinecraftServer mcServer;
    public static Optional<String> favicon = Optional.empty();
    public static Optional<String> backupIcon = Optional.empty();

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
                    serverPlayer.connection.disconnect(new TextComponent(message));
                }
            });
        }
    }

    private static Optional<String> loadIcon(File file) {
        try {
            BufferedImage bufferedImage = ImageIO.read(file);
            return readIcon(bufferedImage);
        } catch (Exception e) {
            ModConstants.LOG.error("Failed to load icon", e);
            return Optional.empty();
        }
    }

    private static Optional<String> loadIcon(InputStream inputStream) {
        try {
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            return readIcon(bufferedImage);
        } catch (Exception e) {
            ModConstants.LOG.error("Failed to load icon", e);
            return Optional.empty();
        }
    }

    private static Optional<String> readIcon(BufferedImage image) {
        try {
            Preconditions.checkState(image.getWidth() == 64, "Must be 64 pixels wide");
            Preconditions.checkState(image.getHeight() == 64, "Must be 64 pixels high");
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", outputStream);
            byte[] encoded = Base64.getEncoder().encode(outputStream.toByteArray());
            return Optional.of("data:image/png;base64," + new String(encoded, StandardCharsets.UTF_8));
        } catch (Exception e) {
            ModConstants.LOG.error("Failed to load icon", e);
            return Optional.empty();
        }
    }

}
