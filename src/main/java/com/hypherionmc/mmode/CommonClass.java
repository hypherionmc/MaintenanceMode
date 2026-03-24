package com.hypherionmc.mmode;

import com.google.common.base.Preconditions;
import com.hypherionmc.craterlib.api.compat.LuckPermsCompat;
import com.hypherionmc.craterlib.api.events.server.CraterRegisterCommandEvent;
import com.hypherionmc.craterlib.api.events.server.CraterServerLifecycleEvent;
import com.hypherionmc.craterlib.api.events.server.PlayerPreLoginEvent;
import com.hypherionmc.craterlib.api.events.server.ServerStatusEvent;
import com.hypherionmc.craterlib.api.game.authlib.CraterGameProfile;
import com.hypherionmc.craterlib.api.game.network.protocol.status.CraterServerStatus;
import com.hypherionmc.craterlib.api.game.server.CraterGameServer;
import com.hypherionmc.craterlib.api.game.text.Text;
import com.hypherionmc.craterlib.api.loader.CraterLoader;
import com.hypherionmc.craterlib.core.event.annot.CraterEventListener;
import com.hypherionmc.craterlib.impl.api.network.protocol.status.WrappedServerStatus;
import com.hypherionmc.mmode.commands.MaintenanceModeCommand;
import com.hypherionmc.mmode.config.MaintenanceModeConfig;
import com.hypherionmc.mmode.schedule.MaintenanceSchedule;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommonClass {

    public static final CommonClass INSTANCE = new CommonClass();

    public AtomicBoolean isDirty = new AtomicBoolean(false);
    private CraterGameServer mcServer;
    private Optional<CraterServerStatus.CraterFavIcon> favicon = Optional.empty();
    private Optional<CraterServerStatus.CraterFavIcon> backupIcon = Optional.empty();
    public boolean resetOnStartup = false;
    public static final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @CraterEventListener
    public void serverStartedEvent(CraterServerLifecycleEvent.Started event) {
        new MaintenanceModeConfig();
        mcServer = event.getServer();
        MaintenanceSchedule.INSTANCE.initScheduler();

        loadIcons();

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
        try {
            // Check if maintenance mode is enabled and kick the player
            if (MaintenanceModeConfig.INSTANCE.isEnabled()) {
                if (isNotAllowedToJoin(event.getGameProfile())) {
                    String message = MaintenanceModeConfig.INSTANCE.getMessage();
                    if (message == null || message.isEmpty())
                        message = "Server is currently undergoing maintenance. Please try connecting again later";

                    event.setMessage(Text.formatted(message));
                }
            }
        } catch (Exception e) {
            if (MaintenanceModeConfig.INSTANCE.isDebug())
                ModConstants.LOG.error("Failed to check if player is allowed to join", e);
        }
    }

    private boolean isNotAllowedToJoin(CraterGameProfile player) {
        if (!MaintenanceModeConfig.INSTANCE.getAllowedLuckpermsGroups().isEmpty() && CraterLoader.isModLoaded("luckperms")) {
            for (String group : MaintenanceModeConfig.INSTANCE.getAllowedLuckpermsGroups()) {
                if (LuckPermsCompat.getInstance().hasGroup(player.getId(), group))
                    return false;
            }
        }

        return MaintenanceModeConfig.INSTANCE.getAllowedUsers().stream().noneMatch(allowedUser -> allowedUser.getUuid().equals(player.getId().toString()));
    }

    @CraterEventListener
    public void requestFavIconEvent(ServerStatusEvent.FaviconRequestEvent event) {
        try {
            if (!MaintenanceModeConfig.INSTANCE.isEnabled() && CraterLoader.isModLoaded("minimotd")) {
                return;
            }

            if (!MaintenanceModeConfig.INSTANCE.isEnabled() && backupIcon.isPresent())
                event.setNewIcon(backupIcon);

            if (MaintenanceModeConfig.INSTANCE.isEnabled() && favicon.isPresent())
                event.setNewIcon(favicon);
        } catch (Exception e) {
            if (MaintenanceModeConfig.INSTANCE.isDebug())
                ModConstants.LOG.error("Failed to update server icon", e);
        }
    }

    @CraterEventListener
    public void requestServerStatus(ServerStatusEvent.StatusRequestEvent event) {
       try {
           if (MaintenanceModeConfig.INSTANCE.isEnabled()) {
               String message = MaintenanceModeConfig.INSTANCE.getMotd();
               if (message != null && !message.isEmpty())
                   event.setNewStatus(Text.formatted(message));
           }
       } catch (Exception e) {
           if (MaintenanceModeConfig.INSTANCE.isDebug())
               ModConstants.LOG.error("Failed to update server status", e);
       }
    }

    @CraterEventListener
    public void serverShutdownEvent(CraterServerLifecycleEvent.Stopped event) {
        executor.shutdownNow();
        if (resetOnStartup && MaintenanceModeConfig.INSTANCE.isEnabled()) {
            MaintenanceModeConfig.INSTANCE.setEnabled(false);
            MaintenanceModeConfig.INSTANCE.saveConfig(MaintenanceModeConfig.INSTANCE);
        }

        MaintenanceSchedule.INSTANCE.shutDown();
    }

    public void loadIcons() {
        if (MaintenanceModeConfig.INSTANCE.getMaintenanceIcon() != null && !MaintenanceModeConfig.INSTANCE.getMaintenanceIcon().isEmpty()) {
            File file = new File(MaintenanceModeConfig.INSTANCE.getMaintenanceIcon());
            if (!file.exists())
                return;

            favicon = loadIcon(file);
        }

        if (!new File("server-icon.png").exists()) {
            backupIcon = loadIcon(Thread.currentThread().getContextClassLoader().getResourceAsStream("mmicon.png"));
        }
    }

    public void kickAllPlayers(String message) {
        if (MaintenanceModeConfig.INSTANCE != null && !MaintenanceModeConfig.INSTANCE.isKickOnlinePlayers())
            return;

        if (mcServer != null) {
            mcServer.getPlayers().forEach(serverPlayer -> {
                if (isNotAllowedToJoin(serverPlayer.getGameProfile())) {
                    serverPlayer.disconnect(Text.formatted(message));
                }
            });
        }
    }

    public void broadcastMessage(String message) {
        if (mcServer != null) {
            mcServer.broadcastSystemMessage(Text.formatted(message), false);
        }
    }

    private Optional<CraterServerStatus.CraterFavIcon> loadIcon(File file) {
        try {
            return loadIcon(new FileInputStream(file));
        } catch (Exception e) {
            ModConstants.LOG.error("Failed to load icon", e);
        }

        return Optional.empty();
    }

    private Optional<CraterServerStatus.CraterFavIcon> loadIcon(InputStream inputStream) {
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
