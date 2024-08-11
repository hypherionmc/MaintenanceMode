package me.hypherionmc.mmode.commands;

import com.hypherionmc.craterlib.api.commands.CraterCommand;
import com.hypherionmc.craterlib.api.events.server.CraterRegisterCommandEvent;
import com.hypherionmc.craterlib.nojang.authlib.BridgedGameProfile;
import com.hypherionmc.craterlib.nojang.commands.BridgedCommandSourceStack;
import me.hypherionmc.mmode.CommonClass;
import me.hypherionmc.mmode.ModConstants;
import me.hypherionmc.mmode.config.MaintenanceModeConfig;
import me.hypherionmc.mmode.schedule.MaintenanceSchedule;
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
                .then(CraterCommand.literal("status").requiresPermission(3).withNode("maintenance.status").execute(MaintenanceModeCommand::checkStatus))
                .then(CraterCommand.literal("on").requiresPermission(3).withNode("maintenance.enable").execute(stack -> changeStatus(stack, true)))
                .then(CraterCommand.literal("off").requiresPermission(3).withNode("maintenance.disable").execute(ctx -> changeStatus(ctx, false)))
                .then(CraterCommand.literal("list").requiresPermission(3).withNode("maintenance.list").execute(MaintenanceModeCommand::listAllowedUsers))
                .then(CraterCommand.literal("reload").requiresPermission(3).withNode("maintenance.reload").execute(MaintenanceModeCommand::reload))
                .then(CraterCommand.literal("backup").requiresPermission(3).withNode("maintenance.backup").execute(MaintenanceModeCommand::doBackup))
                .then(CraterCommand.literal("doBackups").requiresPermission(3).withNode("maintenance.setbackups").withBoolArgument("value", (player, value, ctx) -> changeBackups(ctx, value)))
                .then(CraterCommand.literal("setMessage").requiresPermission(3).withNode("maintenance.setmessage").withPhraseArgument("value", (player, value, ctx) -> setMessage(ctx, value)))
                .then(CraterCommand.literal("setMotd").requiresPermission(3).withNode("maintenance.setmotd").withPhraseArgument("value", (player, value, ctx) -> setMotd(ctx, value)))
                .then(CraterCommand.literal("addAllowed").requiresPermission(3).withNode("maintenance.adduser").withGameProfilesArgument("targets", (player, gameProfiles, ctx) -> addAllowedPlayer(ctx, gameProfiles)))
                .then(CraterCommand.literal("schedule").requiresPermission(3).withNode("maintenance.schedule")
                        .then(CraterCommand.literal("disableOnRestart").requiresPermission(3).withNode("maintenance.changerestart").withBoolArgument("untilRestart", (ctx, value, stack) -> {
                            MaintenanceModeConfig.INSTANCE.getSchedule().setDisableOnRestart(value);
                            MaintenanceModeCommand.saveConfig(MaintenanceModeConfig.INSTANCE, stack);
                            return 1;
                        }))
                        .then(CraterCommand.literal("untilRestart").requiresPermission(3).withNode("maintenance.untilrestart").execute(MaintenanceModeCommand::scheduleTillRestart))
                        .then(CraterCommand.literal("repeating")
                                        .then(CraterCommand.literal("start").requiresPermission(3).withNode("maintenance.startschedule").withPhraseArgument("cron", (ctx, value, stack) -> scheduleStart(stack, value)))
                                        .then(CraterCommand.literal("end").requiresPermission(3).withNode("maintenance.endschedule").withPhraseArgument("cron", (ctx, value, stack) -> scheduleEnd(stack, value)))
                        )
                )
                .then(CraterCommand.literal("removeAllowed").requiresPermission(3).withNode("maintenance.removeuser").withGameProfilesArgument("targets", (player, gameProfiles, ctx) -> removeAllowedPlayer(ctx, gameProfiles)));

        event.registerCommand(cmd);
    }

    private static int scheduleTillRestart(BridgedCommandSourceStack stack) {
        CommonClass.INSTANCE.resetOnStartup = true;
        changeStatus(stack, true);
        stack.sendSuccess(() -> Component.text("Maintenance is enabled until restart"), false);
        return 1;
    }

    private static int scheduleStart(BridgedCommandSourceStack stack, String value) {
        MaintenanceModeConfig.INSTANCE.getSchedule().setStartTime(value);
        CommonClass.INSTANCE.isDirty.set(true);
        MaintenanceSchedule.INSTANCE.initScheduler();

        try {
            MaintenanceModeConfig.INSTANCE.saveConfig(MaintenanceModeConfig.INSTANCE);
        } catch (Exception e) {
            ModConstants.LOG.error("Failed to save config", e);
        }

        stack.sendSuccess(() -> Component.text("Maintenance start schedule is set to " + value), false);
        return 1;
    }

    private static int scheduleEnd(BridgedCommandSourceStack stack, String value) {
        MaintenanceModeConfig.INSTANCE.getSchedule().setEndTime(value);
        CommonClass.INSTANCE.isDirty.set(true);
        MaintenanceSchedule.INSTANCE.initScheduler();

        try {
            MaintenanceModeConfig.INSTANCE.saveConfig(MaintenanceModeConfig.INSTANCE);
        } catch (Exception e) {
            ModConstants.LOG.error("Failed to save config", e);
        }

        stack.sendSuccess(() -> Component.text("Maintenance end schedule is set to " + value), false);
        return 1;
    }

    private static int doBackup(BridgedCommandSourceStack source) {
        try {
            source.sendSuccess(() -> Component.text("Starting Maintenance Mode Backup"), false);
            BackupUtil.createBackup();
        } catch (Exception e) {
            ModConstants.LOG.error("Failed to create server backup: {}", e.getMessage());
            source.sendFailure(Component.text("Failed to create Server backup. Please check your server log"));
        }
        return 1;
    }

    private static int changeBackups(BridgedCommandSourceStack stack, boolean enabled) {
        if (MaintenanceModeConfig.INSTANCE == null) {
            new MaintenanceModeConfig();
        }

        MaintenanceModeConfig.INSTANCE.setDoBackup(enabled);
        saveConfig(MaintenanceModeConfig.INSTANCE, stack);

        stack.sendSuccess(() -> Component.text("Maintenance Mode Backups: ").append(Component.text((enabled ? "Enabled" : "Disabled")).color(NamedTextColor.YELLOW)), false);
        CommonClass.INSTANCE.isDirty.set(true);
        return 1;
    }

    private static int checkStatus(BridgedCommandSourceStack stack) {
        if (MaintenanceModeConfig.INSTANCE != null) {
            stack.sendSuccess(() -> Component.text("Maintenance Mode: ").append(Component.text((MaintenanceModeConfig.INSTANCE.isEnabled() ? "Enabled" : "Disabled")).color(NamedTextColor.YELLOW)), false);
        } else {
            stack.sendFailure(Component.text("Maintenance Mode: Failed to load config"));
        }
        return 1;
    }

    public static int listAllowedUsers(BridgedCommandSourceStack stack) {
        if (MaintenanceModeConfig.INSTANCE == null) {
            new MaintenanceModeConfig();
        }

        String[] names = MaintenanceModeConfig.INSTANCE.getAllowedUsers().stream().map(MaintenanceModeConfig.AllowedUser::getName).toArray(String[]::new);

        if (names.length == 0) {
            stack.sendSuccess(() -> Component.text("No users are allowed to join"), false);
        } else {
            stack.sendSuccess(() -> Component.text(String.format("There are %s allowed player(s): %s", names.length, String.join(", ", names))), false);
        }

        return 1;
    }

    private static int changeStatus(BridgedCommandSourceStack stack, boolean enabled) {
        if (MaintenanceModeConfig.INSTANCE == null) {
            new MaintenanceModeConfig();
        }

        MaintenanceModeConfig.INSTANCE.setEnabled(enabled);
        try {
            MaintenanceModeConfig.INSTANCE.saveConfig(MaintenanceModeConfig.INSTANCE);
            if (enabled) {
                CommonClass.INSTANCE.kickAllPlayers(MaintenanceModeConfig.INSTANCE.getMessage());

                try {
                    if (MaintenanceModeConfig.INSTANCE.isDoBackup()) {
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

        stack.sendSuccess(() -> Component.text("Maintenance Mode: ").append(Component.text((MaintenanceModeConfig.INSTANCE.isEnabled() ? "Enabled" : "Disabled")).color(NamedTextColor.YELLOW)), false);
        CommonClass.INSTANCE.isDirty.set(true);
        return 1;
    }

    private static int reload(BridgedCommandSourceStack stack) {
        new MaintenanceModeConfig(true);
        stack.sendSuccess(() -> Component.text("Config Reloaded"), false);
        CommonClass.INSTANCE.isDirty.set(true);
        MaintenanceSchedule.INSTANCE.initScheduler();
        return 1;
    }

    private static int addAllowedPlayer(BridgedCommandSourceStack stack, Collection<BridgedGameProfile> gameProfiles)  {
        if (MaintenanceModeConfig.INSTANCE == null) {
            new MaintenanceModeConfig();
        }

        List<MaintenanceModeConfig.AllowedUser> allowedUsers = MaintenanceModeConfig.INSTANCE.getAllowedUsers().isEmpty() ? new ArrayList<>() : MaintenanceModeConfig.INSTANCE.getAllowedUsers();

        for (BridgedGameProfile profile : gameProfiles) {
            if (allowedUsers.stream().noneMatch(allowedUser -> allowedUser.getUuid().equals(profile.getId().toString()))) {
                MaintenanceModeConfig.AllowedUser allowedUser = new MaintenanceModeConfig.AllowedUser(profile.getName(), profile.getId().toString());
                allowedUsers.add(allowedUser);
            } else {
                stack.sendFailure(Component.text("User already in allowed list"));
                
            }
        }

        MaintenanceModeConfig.INSTANCE.setAllowedUsers(allowedUsers);

        saveConfig(MaintenanceModeConfig.INSTANCE, stack);
        CommonClass.INSTANCE.isDirty.set(true);
        return 1;
    }

    private static int removeAllowedPlayer(BridgedCommandSourceStack stack, Collection<BridgedGameProfile> gameProfiles) {
        if (MaintenanceModeConfig.INSTANCE == null) {
            new MaintenanceModeConfig();
        }

        List<MaintenanceModeConfig.AllowedUser> allowedUsers = MaintenanceModeConfig.INSTANCE.getAllowedUsers().isEmpty() ? new ArrayList<>() : MaintenanceModeConfig.INSTANCE.getAllowedUsers();

        for (BridgedGameProfile profile : gameProfiles) {
            Optional<MaintenanceModeConfig.AllowedUser> allowedUserOptional = allowedUsers.stream().filter(allowedUser -> allowedUser.getUuid().equals(profile.getId().toString())).findFirst();

            if (allowedUserOptional.isPresent()) {
                allowedUsers.remove(allowedUserOptional.get());
            } else {
                stack.sendFailure(Component.text("User not found in allowed list"));
                return 1;
            }
        }

        MaintenanceModeConfig.INSTANCE.setAllowedUsers(allowedUsers);
        saveConfig(MaintenanceModeConfig.INSTANCE, stack);
        CommonClass.INSTANCE.isDirty.set(true);
        return 1;
    }

    private static void saveConfig(MaintenanceModeConfig config, BridgedCommandSourceStack stack) {
        try {
            MaintenanceModeConfig.INSTANCE.saveConfig(config);
        } catch (Exception e) {
            stack.sendFailure(Component.text("Failed to save config. Please see server log"));
            ModConstants.LOG.error("Failed to save config: {}", e.getMessage());
        }

        stack.sendSuccess(() -> Component.text("Updated config"), false);
        CommonClass.INSTANCE.isDirty.set(true);
    }

    private static int setMessage(BridgedCommandSourceStack stack, String message) {
        if (MaintenanceModeConfig.INSTANCE == null) {
            new MaintenanceModeConfig();
        }

        MaintenanceModeConfig.INSTANCE.setMessage(message);
        saveConfig(MaintenanceModeConfig.INSTANCE, stack);
        CommonClass.INSTANCE.isDirty.set(true);
        return 1;
    }

    private static int setMotd(BridgedCommandSourceStack stack, String message) {
        if (MaintenanceModeConfig.INSTANCE == null) {
            new MaintenanceModeConfig();
        }

        MaintenanceModeConfig.INSTANCE.setMotd(message);
        saveConfig(MaintenanceModeConfig.INSTANCE, stack);
        CommonClass.INSTANCE.isDirty.set(true);
        return 1;
    }

}
