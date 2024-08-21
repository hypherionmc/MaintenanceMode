package com.hypherionmc.mmode;

import com.google.common.base.Preconditions;
import com.hypherionmc.craterlib.api.events.server.CraterRegisterCommandEvent;
import com.hypherionmc.craterlib.api.events.server.CraterServerLifecycleEvent;
import com.hypherionmc.craterlib.api.events.server.PlayerPreLoginEvent;
import com.hypherionmc.craterlib.api.events.server.ServerStatusEvent;
import com.hypherionmc.craterlib.core.event.annot.CraterEventListener;
import com.hypherionmc.craterlib.nojang.network.protocol.status.WrappedServerStatus;
import com.hypherionmc.craterlib.nojang.server.BridgedMinecraftServer;
import com.hypherionmc.craterlib.utils.ChatUtils;
import com.hypherionmc.mmode.commands.MaintenanceModeCommand;
import com.hypherionmc.mmode.config.MaintenanceModeConfig;
import com.hypherionmc.mmode.schedule.MaintenanceSchedule;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommonClass {

    public static final CommonClass INSTANCE = new CommonClass();

    public AtomicBoolean isDirty = new AtomicBoolean(false);
    private BridgedMinecraftServer mcServer;
    private Optional<WrappedServerStatus.WrappedFavicon> favicon = Optional.empty();
    private Optional<WrappedServerStatus.WrappedFavicon> backupIcon = Optional.empty();
    public boolean resetOnStartup = false;

    @CraterEventListener
    public void serverStartedEvent(CraterServerLifecycleEvent.Started event) {
        new MaintenanceModeConfig();
        mcServer = event.getServer();
        MaintenanceSchedule.INSTANCE.initScheduler();

        if (MaintenanceModeConfig.INSTANCE.getMaintenanceIcon() != null && !MaintenanceModeConfig.INSTANCE.getMaintenanceIcon().isEmpty()) {
            File file = new File(MaintenanceModeConfig.INSTANCE.getMaintenanceIcon());
            if (!file.exists())
                return;

            favicon = loadIcon(file);
        }

        if (!new File("server-icon.png").exists()) {
            backupIcon = loadIcon(Thread.currentThread().getContextClassLoader().getResourceAsStream("mmicon.png"));
        }

        if (MaintenanceModeConfig.INSTANCE != null) {
            ModConstants.LOG.info(MaintenanceModeConfig.INSTANCE.isEnabled() ? "Maintenance mode is active!" : "Maintenance mode is off");
        }
    }

    @CraterEventListener
    public void registerCommandEvent(CraterRegisterCommandEvent event) {
        MaintenanceModeCommand.register(event);
    }

    @CraterEventListener
    public void playerPreLoginEvent(PlayerPreLoginEvent event) {
        // Check if maintenance mode is enabled and kick the player
        if (MaintenanceModeConfig.INSTANCE.isEnabled()) {
            if (MaintenanceModeConfig.INSTANCE.getAllowedUsers().stream().noneMatch(allowedUser -> allowedUser.getUuid().equals(event.getGameProfile().getId().toString()))) {
                String message = MaintenanceModeConfig.INSTANCE.getMessage();
                if (message == null || message.isEmpty())
                    message = "Server is currently undergoing maintenance. Please try connecting again later";

                event.setMessage(ChatUtils.format(message));
            }
        }
    }

    @CraterEventListener
    public void requestFavIconEvent(ServerStatusEvent.FaviconRequestEvent event) {
        if (!MaintenanceModeConfig.INSTANCE.isEnabled() && backupIcon.isPresent())
            event.setNewIcon(backupIcon);

        if (MaintenanceModeConfig.INSTANCE.isEnabled() && favicon.isPresent())
            event.setNewIcon(favicon);
    }

    @CraterEventListener
    public void requestServerStatus(ServerStatusEvent.StatusRequestEvent event) {
        if (MaintenanceModeConfig.INSTANCE.isEnabled()) {
            String message = MaintenanceModeConfig.INSTANCE.getMotd();
            if (message != null && !message.isEmpty())
                event.setNewStatus(ChatUtils.format(message));
        }
    }

    @CraterEventListener
    public void serverShutdownEvent(CraterServerLifecycleEvent.Stopped event) {
        if (resetOnStartup && MaintenanceModeConfig.INSTANCE.isEnabled()) {
            MaintenanceModeConfig.INSTANCE.setEnabled(false);
            MaintenanceModeConfig.INSTANCE.saveConfig(MaintenanceModeConfig.INSTANCE);
        }

        MaintenanceSchedule.INSTANCE.shutDown();
    }

    public void kickAllPlayers(String message) {
        if (mcServer != null && MaintenanceModeConfig.INSTANCE != null) {
            mcServer.getPlayers().forEach(serverPlayer -> {
                if (MaintenanceModeConfig.INSTANCE.getAllowedUsers().stream().noneMatch(allowedUser -> allowedUser.getUuid().equals(serverPlayer.getStringUUID()))) {
                    serverPlayer.disconnect(ChatUtils.format(message));
                }
            });
        }
    }

    public void broadcastMessage(String message) {
        if (mcServer != null) {
            mcServer.broadcastSystemMessage(ChatUtils.format(message), false);
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
