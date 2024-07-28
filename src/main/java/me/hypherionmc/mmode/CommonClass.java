package me.hypherionmc.mmode;

import com.google.common.base.Preconditions;
import com.hypherionmc.craterlib.api.events.server.CraterRegisterCommandEvent;
import com.hypherionmc.craterlib.api.events.server.CraterServerLifecycleEvent;
import com.hypherionmc.craterlib.api.events.server.PlayerPreLoginEvent;
import com.hypherionmc.craterlib.api.events.server.ServerStatusEvent;
import com.hypherionmc.craterlib.core.event.annot.CraterEventListener;
import com.hypherionmc.craterlib.nojang.network.protocol.status.WrappedServerStatus;
import com.hypherionmc.craterlib.nojang.server.BridgedMinecraftServer;
import com.hypherionmc.craterlib.utils.ChatUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.hypherionmc.mmode.commands.MaintenanceModeCommand;
import me.hypherionmc.mmode.config.ConfigController;
import me.hypherionmc.mmode.config.objects.MaintenanceModeConfig;
import shadow.kyori.adventure.text.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommonClass {

    public static final CommonClass INSTANCE = new CommonClass();

    public AtomicBoolean isDirty = new AtomicBoolean(false);
    public MaintenanceModeConfig config;
    private BridgedMinecraftServer mcServer;
    private Optional<WrappedServerStatus.WrappedFavicon> favicon = Optional.empty();
    private Optional<WrappedServerStatus.WrappedFavicon> backupIcon = Optional.empty();

    @CraterEventListener
    public void serverStartedEvent(CraterServerLifecycleEvent.Started event) {
        config = ConfigController.initConfig();
        mcServer = event.getServer();

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

    @CraterEventListener
    public void registerCommandEvent(CraterRegisterCommandEvent event) {
        MaintenanceModeCommand.register(event);
    }

    @CraterEventListener
    public void playerPreLoginEvent(PlayerPreLoginEvent event) {
        // Check if maintenance mode is enabled and kick the player
        if (config.isEnabled()) {
            if (config.getAllowedUsers().stream().noneMatch(allowedUser -> allowedUser.getUuid().equals(event.getGameProfile().getId()))) {
                String message = config.getMessage();
                if (message == null || message.isEmpty())
                    message = "Server is currently undergoing maintenance. Please try connecting again later";

                event.setMessage(ChatUtils.format(message));
            }
        }
    }

    @CraterEventListener
    public void requestFavIconEvent(ServerStatusEvent.FaviconRequestEvent event) {
        if (!config.isEnabled() && backupIcon.isPresent())
            event.setNewIcon(backupIcon);

        if (config.isEnabled() && favicon.isPresent())
            event.setNewIcon(favicon);
    }

    @CraterEventListener
    public void requestServerStatus(ServerStatusEvent.StatusRequestEvent event) {
        if (config == null) config = new MaintenanceModeConfig();
        if (config.isEnabled()) {
            String message = config.getMotd();
            if (message != null && !message.isEmpty())
                event.setNewStatus(ChatUtils.format(message));
        }
    }

    public void kickAllPlayers(String message) {
        if (mcServer != null && config != null) {
            mcServer.getPlayers().forEach(serverPlayer -> {
                if (config.getAllowedUsers().stream().noneMatch(allowedUser -> allowedUser.getUuid().toString().equals(serverPlayer.getStringUUID()))) {
                    serverPlayer.disconnect(Component.text(message));
                }
            });
        }
    }

    private Optional<WrappedServerStatus.WrappedFavicon> loadIcon(File file) {
        try {
            return loadIcon(new FileInputStream(file));
        } catch (Exception e) {
            ModConstants.LOG.error("Failed to load icon", e);
        }

        return Optional.empty();
    }

    private Optional<WrappedServerStatus.WrappedFavicon> loadIcon(InputStream inputStream) {
        try {
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            Preconditions.checkState(bufferedImage.getWidth() == 64, "Must be 64 pixels wide");
            Preconditions.checkState(bufferedImage.getHeight() == 64, "Must be 64 pixels high");
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "PNG", outputStream);
            return Optional.of(new WrappedServerStatus.WrappedFavicon(outputStream.toByteArray()));
        } catch (Exception e) {
            ModConstants.LOG.error("Failed to load icon", e);
            return Optional.empty();
        }
    }

}
