package me.hypherionmc.mmode.commands;

import com.hypherionmc.craterlib.api.commands.CraterCommand;
import com.hypherionmc.craterlib.api.events.server.CraterRegisterCommandEvent;
import com.hypherionmc.craterlib.nojang.authlib.BridgedGameProfile;
import com.hypherionmc.craterlib.nojang.commands.BridgedCommandSourceStack;
import me.hypherionmc.mmode.CommonClass;
import me.hypherionmc.mmode.ModConstants;
import me.hypherionmc.mmode.config.ConfigController;
import me.hypherionmc.mmode.config.objects.MaintenanceModeConfig;
import me.hypherionmc.mmode.util.BackupUtil;
import shadow.kyori.adventure.text.Component;
import shadow.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class MaintenanceModeCommand {

    public static void register(CraterRegisterCommandEvent event) {
        CraterCommand cmd = CraterCommand.literal("maintenance")
                .requiresPermission(3)
                .withChild(CraterCommand.literal("status").executes(MaintenanceModeCommand::checkStatus))
                .withChild(CraterCommand.literal("on").executes(ctx -> changeStatus(ctx, true)))
                .withChild(CraterCommand.literal("off").executes(ctx -> changeStatus(ctx, false)))
                .withChild(CraterCommand.literal("list").executes(MaintenanceModeCommand::listAllowedUsers))
                .withChild(CraterCommand.literal("reload").executes(MaintenanceModeCommand::reload))
                .withChild(CraterCommand.literal("backup").executes(MaintenanceModeCommand::doBackup))
                .withChild(CraterCommand.literal("doBackups").withBoolArgument("value", (player, value, ctx) -> changeBackups(ctx, value)))
                .withChild(CraterCommand.literal("setMessage").withStringArgument("value", (player, value, ctx) -> setMessage(ctx, value)))
                .withChild(CraterCommand.literal("setMotd").withStringArgument("value", (player, value, ctx) -> setMotd(ctx, value)))
                .withChild(CraterCommand.literal("addAllowed").withGameProfileArgument("targets", (player, gameProfiles, ctx) -> addAllowedPlayer(ctx, gameProfiles)))
                .withChild(CraterCommand.literal("removeAllowed").withGameProfileArgument("targets", (player, gameProfiles, ctx) -> removeAllowedPlayer(ctx, gameProfiles)));

        event.registerCommand(cmd);
    }

    private static void doBackup(BridgedCommandSourceStack source) {
        try {
            source.sendSuccess(() -> Component.text("Starting Maintenance Mode Backup"), true);
            BackupUtil.createBackup();
        } catch (Exception e) {
            ModConstants.LOG.error("Failed to create server backup: {}", e.getMessage());
            source.sendFailure(Component.text("Failed to create Server backup. Please check your server log"));
        }
    }

    private static void changeBackups(BridgedCommandSourceStack stack, boolean enabled) {
        MaintenanceModeConfig config = CommonClass.INSTANCE.config;
        if (config == null) {
            config = new MaintenanceModeConfig();
        }

        config.setDoBackup(enabled);
        saveConfig(config, stack);

        stack.sendSuccess(() -> Component.text("Maintenance Mode Backups: ").append(Component.text((enabled ? "Enabled" : "Disabled")).color(NamedTextColor.YELLOW)), true);
        CommonClass.INSTANCE.isDirty.set(true);
    }

    private static void checkStatus(BridgedCommandSourceStack stack) {
        if (CommonClass.INSTANCE.config != null) {
            stack.sendSuccess(() -> Component.text("Maintenance Mode: ").append(Component.text((CommonClass.INSTANCE.config.isEnabled() ? "Enabled" : "Disabled")).color(NamedTextColor.YELLOW)), true);
        } else {
            stack.sendFailure(Component.text("Maintenance Mode: Failed to load config"));
        }
    }

    public static void listAllowedUsers(BridgedCommandSourceStack stack) {
        MaintenanceModeConfig config = CommonClass.INSTANCE.config;
        if (config == null) {
            config = new MaintenanceModeConfig();
        }

        String[] names = config.getAllowedUsers().stream().map(MaintenanceModeConfig.AllowedUser::getName).toArray(String[]::new);

        if (names.length == 0) {
            stack.sendSuccess(() -> Component.text("No users are allowed to join"), false);
        } else {
            stack.sendSuccess(() -> Component.text(String.format("There are %s allowed player(s): %s", names.length, String.join(", ", names))), false);
        }

    }

    private static void changeStatus(BridgedCommandSourceStack stack, boolean enabled) {
        MaintenanceModeConfig config = CommonClass.INSTANCE.config;
        if (config == null) {
            config = new MaintenanceModeConfig();
        }

        config.setEnabled(enabled);
        try {
            ConfigController.saveConfig(config);
            CommonClass.INSTANCE.config = ConfigController.loadConfig();
            if (enabled) {
                CommonClass.INSTANCE.kickAllPlayers(config.getMessage());

                try {
                    if (config.isDoBackup()) {
                        BackupUtil.createBackup();
                    }
                } catch (Exception e) {
                    ModConstants.LOG.error("Failed to save backup: {}", e.getMessage());
                }

            }
        } catch (Exception e) {
            stack.sendFailure(Component.text("Failed to save config. Please see server log"));
            ModConstants.LOG.error("Failed to save config: {}", e.getMessage());
        }

        stack.sendSuccess(() -> Component.text("Maintenance Mode: ").append(Component.text((CommonClass.INSTANCE.config.isEnabled() ? "Enabled" : "Disabled")).color(NamedTextColor.YELLOW)), true);
        CommonClass.INSTANCE.isDirty.set(true);
    }

    private static void reload(BridgedCommandSourceStack stack) {
        CommonClass.INSTANCE.config = ConfigController.loadConfig();
        stack.sendSuccess(() -> Component.text("Config Reloaded"), true);
        CommonClass.INSTANCE.isDirty.set(true);
    }

    private static void addAllowedPlayer(BridgedCommandSourceStack stack, Collection<BridgedGameProfile> gameProfiles)  {
        MaintenanceModeConfig config = CommonClass.INSTANCE.config;
        if (config == null) {
            config = new MaintenanceModeConfig();
        }

        List<MaintenanceModeConfig.AllowedUser> allowedUsers = config.getAllowedUsers().isEmpty() ? new ArrayList<>() : config.getAllowedUsers();

        for (BridgedGameProfile profile : gameProfiles) {
            if (allowedUsers.stream().noneMatch(allowedUser -> allowedUser.getUuid().equals(profile.getId()))) {
                MaintenanceModeConfig.AllowedUser allowedUser = new MaintenanceModeConfig.AllowedUser(profile.getName(), profile.getId());
                allowedUsers.add(allowedUser);
            } else {
                stack.sendFailure(Component.text("User already in allowed list"));
                
            }
        }

        config.setAllowedUsers(allowedUsers);

        saveConfig(config, stack);
        CommonClass.INSTANCE.isDirty.set(true);
    }

    private static void removeAllowedPlayer(BridgedCommandSourceStack stack, Collection<BridgedGameProfile> gameProfiles) {
        MaintenanceModeConfig config = CommonClass.INSTANCE.config;
        if (config == null) {
            config = new MaintenanceModeConfig();
        }

        List<MaintenanceModeConfig.AllowedUser> allowedUsers = config.getAllowedUsers().isEmpty() ? new ArrayList<>() : config.getAllowedUsers();

        for (BridgedGameProfile profile : gameProfiles) {
            Optional<MaintenanceModeConfig.AllowedUser> allowedUserOptional = allowedUsers.stream().filter(allowedUser -> allowedUser.getUuid().equals(profile.getId())).findFirst();

            if (allowedUserOptional.isPresent()) {
                allowedUsers.remove(allowedUserOptional.get());
            } else {
                stack.sendFailure(Component.text("User not found in allowed list"));
                return;
            }
        }

        config.setAllowedUsers(allowedUsers);
        saveConfig(config, stack);
        CommonClass.INSTANCE.isDirty.set(true);
    }

    private static void saveConfig(MaintenanceModeConfig config, BridgedCommandSourceStack stack) {
        try {
            ConfigController.saveConfig(config);
        } catch (Exception e) {
            stack.sendFailure(Component.text("Failed to save config. Please see server log"));
            ModConstants.LOG.error("Failed to save config: {}", e.getMessage());
        }

        CommonClass.INSTANCE.config = ConfigController.loadConfig();
        stack.sendSuccess(() -> Component.text("Updated config"), true);
        CommonClass.INSTANCE.isDirty.set(true);
    }

    private static void setMessage(BridgedCommandSourceStack stack, String message) {
        MaintenanceModeConfig config = CommonClass.INSTANCE.config;
        if (config == null) {
            config = new MaintenanceModeConfig();
        }

        config.setMessage(message);
        saveConfig(config, stack);
        CommonClass.INSTANCE.isDirty.set(true);
    }

    private static void setMotd(BridgedCommandSourceStack stack, String message) {
        MaintenanceModeConfig config = CommonClass.INSTANCE.config;
        if (config == null) {
            config = new MaintenanceModeConfig();
        }

        config.setMotd(message);
        saveConfig(config, stack);
        CommonClass.INSTANCE.isDirty.set(true);
    }

}
