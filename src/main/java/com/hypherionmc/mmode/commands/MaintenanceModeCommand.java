package com.hypherionmc.mmode.commands;

import com.hypherionmc.craterlib.api.commands.CraterCommand;
import com.hypherionmc.craterlib.api.events.server.CraterRegisterCommandEvent;
import com.hypherionmc.craterlib.api.game.authlib.CraterGameProfile;
import com.hypherionmc.craterlib.api.game.commands.CraterCommandSourceStack;
import com.hypherionmc.craterlib.api.game.text.Text;
import com.hypherionmc.craterlib.core.event.CraterEventBus;
import com.hypherionmc.craterlib.libs.kyori.adventure.text.format.NamedTextColor;
import com.hypherionmc.mmode.api.events.MaintenanceModeEvent;
import com.hypherionmc.mmode.schedule.MaintenanceSchedule;
import com.hypherionmc.mmode.CommonClass;
import com.hypherionmc.mmode.ModConstants;
import com.hypherionmc.mmode.config.MaintenanceModeConfig;
import com.hypherionmc.mmode.util.BackupUtil;

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
                            MaintenanceModeCommand.saveConfig(stack);
                            return 1;
                        }))
                        .then(CraterCommand.literal("untilRestart").requiresPermission(3).withNode("maintenance.untilrestart").execute(MaintenanceModeCommand::scheduleTillRestart))
                        .then(CraterCommand.literal("repeating")
                                        .then(CraterCommand.literal("start").requiresPermission(3).withNode("maintenance.startschedule").withPhraseArgument("cron", (ctx, value, stack) -> scheduleStart(stack, value)))
                                        .then(CraterCommand.literal("end").requiresPermission(3).withNode("maintenance.endschedule").withPhraseArgument("cron", (ctx, value, stack) -> scheduleEnd(stack, value)))
                        )
                )
                .then(CraterCommand.literal("addGroup").requiresPermission(3).withNode("maintenance.adduser").withStringArgument("lpgroup", (player, group, ctx) -> addAllowedPlayer(ctx, group)))
                .then(CraterCommand.literal("removeGroup").requiresPermission(3).withNode("maintenance.removeuser").withStringArgument("withStringArgument", (player, group, ctx) -> removeAllowedPlayer(ctx, group)))
                .then(CraterCommand.literal("removeAllowed").requiresPermission(3).withNode("maintenance.removeuser").withGameProfilesArgument("targets", (player, gameProfiles, ctx) -> removeAllowedPlayer(ctx, gameProfiles)));

        event.registerCommand(cmd);
    }

    private static int scheduleTillRestart(CraterCommandSourceStack stack) {
        CommonClass.INSTANCE.resetOnStartup = true;
        changeStatus(stack, true);
        stack.sendSuccess(() -> Text.literal("Maintenance is enabled until restart"), false);
        return 1;
    }

    private static int scheduleStart(CraterCommandSourceStack stack, String value) {
        MaintenanceModeConfig.INSTANCE.getSchedule().setStartTime(value);
        CommonClass.INSTANCE.isDirty.set(true);
        MaintenanceSchedule.INSTANCE.initScheduler();

        try {
            MaintenanceModeConfig.INSTANCE.saveConfig(MaintenanceModeConfig.INSTANCE);
        } catch (Exception e) {
            ModConstants.LOG.error("Failed to save config", e);
        }

        stack.sendSuccess(() -> Text.literal("Maintenance start schedule is set to " + value), false);
        return 1;
    }

    private static int scheduleEnd(CraterCommandSourceStack stack, String value) {
        MaintenanceModeConfig.INSTANCE.getSchedule().setEndTime(value);
        CommonClass.INSTANCE.isDirty.set(true);
        MaintenanceSchedule.INSTANCE.initScheduler();

        try {
            MaintenanceModeConfig.INSTANCE.saveConfig(MaintenanceModeConfig.INSTANCE);
        } catch (Exception e) {
            ModConstants.LOG.error("Failed to save config", e);
        }

        stack.sendSuccess(() -> Text.literal("Maintenance end schedule is set to " + value), false);
        return 1;
    }

    private static int doBackup(CraterCommandSourceStack source) {
        try {
            source.sendSuccess(() -> Text.literal("Starting Maintenance Mode Backup"), false);
            BackupUtil.createBackup();
        } catch (Exception e) {
            ModConstants.LOG.error("Failed to create server backup: {}", e.getMessage());
            source.sendFailure(Text.literal("Failed to create Server backup. Please check your server log"));
        }
        return 1;
    }

    private static int changeBackups(CraterCommandSourceStack stack, boolean enabled) {
        if (MaintenanceModeConfig.INSTANCE == null) {
            new MaintenanceModeConfig();
        }

        MaintenanceModeConfig.INSTANCE.setDoBackup(enabled);
        saveConfig(stack);

        stack.sendSuccess(() -> Text.literal("Maintenance Mode Backups: ").append(Text.literal((enabled ? "Enabled" : "Disabled")).color(NamedTextColor.YELLOW)), false);
        CommonClass.INSTANCE.isDirty.set(true);
        return 1;
    }

    private static int checkStatus(CraterCommandSourceStack stack) {
        if (MaintenanceModeConfig.INSTANCE != null) {
            stack.sendSuccess(() -> Text.literal("Maintenance Mode: ").append(Text.literal((MaintenanceModeConfig.INSTANCE.isEnabled() ? "Enabled" : "Disabled")).color(NamedTextColor.YELLOW)), false);
        } else {
            stack.sendFailure(Text.literal("Maintenance Mode: Failed to load config"));
        }
        return 1;
    }

    public static int listAllowedUsers(CraterCommandSourceStack stack) {
        if (MaintenanceModeConfig.INSTANCE == null) {
            new MaintenanceModeConfig();
        }

        String[] names = MaintenanceModeConfig.INSTANCE.getAllowedUsers().stream().map(MaintenanceModeConfig.AllowedUser::getName).toArray(String[]::new);
        String[] lpGroups = MaintenanceModeConfig.INSTANCE.getAllowedLuckpermsGroups().toArray(String[]::new);

        String returnS = "No users and groups are allowed to join";

        if (names.length > 0) {
            returnS = String.format("There are %s allowed player(s): %s", names.length, String.join(", ", names));
        }

        if (lpGroups.length > 0) {
            if (names.length > 0) {
                returnS += "\n" + String.format("There are %s allowed luckperms groups(s): %s", lpGroups.length, String.join(", ", lpGroups));
            } else {
                returnS = String.format("There are %s allowed luckperms groups(s): %s", lpGroups.length, String.join(", ", lpGroups));
            }
        }

        String finalReturnS = returnS;
        stack.sendSuccess(() -> Text.literal(finalReturnS), false);

        return 1;
    }

    private static int changeStatus(CraterCommandSourceStack stack, boolean enabled) {
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
            stack.sendFailure(Text.literal("Failed to save config. Please see server log"));
            ModConstants.LOG.error("Failed to save config: {}", e.getMessage());
        }

        stack.sendSuccess(() -> Text.literal("Maintenance Mode: ").append(Text.literal((MaintenanceModeConfig.INSTANCE.isEnabled() ? "Enabled" : "Disabled")).color(NamedTextColor.YELLOW)), false);
        CommonClass.INSTANCE.isDirty.set(true);

        if (enabled) {
            CraterEventBus.INSTANCE.postEvent(new MaintenanceModeEvent.MaintenanceStart());
        } else {
            CraterEventBus.INSTANCE.postEvent(new MaintenanceModeEvent.MaintenanceEnd());
        }

        return 1;
    }

    private static int reload(CraterCommandSourceStack stack) {
        new MaintenanceModeConfig(true);
        stack.sendSuccess(() -> Text.literal("Config Reloaded"), false);
        CommonClass.INSTANCE.isDirty.set(true);
        MaintenanceSchedule.INSTANCE.initScheduler();
        return 1;
    }

    private static int addAllowedPlayer(CraterCommandSourceStack stack, Collection<? extends CraterGameProfile> gameProfiles)  {
        if (MaintenanceModeConfig.INSTANCE == null) {
            new MaintenanceModeConfig();
        }

        List<MaintenanceModeConfig.AllowedUser> allowedUsers = MaintenanceModeConfig.INSTANCE.getAllowedUsers().isEmpty() ? new ArrayList<>() : MaintenanceModeConfig.INSTANCE.getAllowedUsers();

        for (CraterGameProfile profile : gameProfiles) {
            if (allowedUsers.stream().noneMatch(allowedUser -> allowedUser.getUuid().equals(profile.getId().toString()))) {
                MaintenanceModeConfig.AllowedUser allowedUser = new MaintenanceModeConfig.AllowedUser(profile.getName(), profile.getId().toString());
                allowedUsers.add(allowedUser);
            } else {
                stack.sendFailure(Text.literal("User already in allowed list"));
                
            }
        }

        MaintenanceModeConfig.INSTANCE.setAllowedUsers(allowedUsers);
        stack.sendSuccess(() -> Text.literal("User added to allowed list"), false);

        saveConfig(stack);
        CommonClass.INSTANCE.isDirty.set(true);
        return 1;
    }

    private static int addAllowedPlayer(CraterCommandSourceStack stack, String lpGroup)  {
        if (MaintenanceModeConfig.INSTANCE == null) {
            new MaintenanceModeConfig();
        }

        List<String> allowedUsers = MaintenanceModeConfig.INSTANCE.getAllowedLuckpermsGroups().isEmpty() ? new ArrayList<>() : MaintenanceModeConfig.INSTANCE.getAllowedLuckpermsGroups();

        if (allowedUsers.stream().noneMatch(allowedUser -> allowedUser.equalsIgnoreCase(lpGroup))) {
            allowedUsers.add(lpGroup);
        } else {
            stack.sendFailure(Text.literal("Group already in allowed list"));

        }

        MaintenanceModeConfig.INSTANCE.setAllowedLuckpermsGroups(allowedUsers);
        stack.sendSuccess(() -> Text.literal("Group added to allowed list"), false);

        saveConfig(stack);
        CommonClass.INSTANCE.isDirty.set(true);
        return 1;
    }

    private static int removeAllowedPlayer(CraterCommandSourceStack stack, Collection<? extends CraterGameProfile> gameProfiles) {
        if (MaintenanceModeConfig.INSTANCE == null) {
            new MaintenanceModeConfig();
        }

        List<MaintenanceModeConfig.AllowedUser> allowedUsers = MaintenanceModeConfig.INSTANCE.getAllowedUsers().isEmpty() ? new ArrayList<>() : MaintenanceModeConfig.INSTANCE.getAllowedUsers();

        for (CraterGameProfile profile : gameProfiles) {
            Optional<MaintenanceModeConfig.AllowedUser> allowedUserOptional = allowedUsers.stream().filter(allowedUser -> allowedUser.getUuid().equals(profile.getId().toString())).findFirst();

            if (allowedUserOptional.isPresent()) {
                allowedUsers.remove(allowedUserOptional.get());
            } else {
                stack.sendFailure(Text.literal("User not found in allowed list"));
                return 1;
            }
        }

        MaintenanceModeConfig.INSTANCE.setAllowedUsers(allowedUsers);
        saveConfig(stack);
        CommonClass.INSTANCE.isDirty.set(true);
        stack.sendSuccess(() -> Text.literal("User removed from allowed list"), false);
        CommonClass.INSTANCE.kickAllPlayers(MaintenanceModeConfig.INSTANCE.getMessage());
        return 1;
    }

    private static int removeAllowedPlayer(CraterCommandSourceStack stack, String lpGroup) {
        if (MaintenanceModeConfig.INSTANCE == null) {
            new MaintenanceModeConfig();
        }

        List<String> allowedUsers = MaintenanceModeConfig.INSTANCE.getAllowedLuckpermsGroups().isEmpty() ? new ArrayList<>() : MaintenanceModeConfig.INSTANCE.getAllowedLuckpermsGroups();
        Optional<String> allowedUserOptional = allowedUsers.stream().filter(allowedUser -> allowedUser.equalsIgnoreCase(lpGroup)).findFirst();

        if (allowedUserOptional.isPresent()) {
            allowedUsers.remove(allowedUserOptional.get());
        } else {
            stack.sendFailure(Text.literal("Group not found in allowed list"));
            return 1;
        }

        MaintenanceModeConfig.INSTANCE.setAllowedLuckpermsGroups(allowedUsers);
        stack.sendSuccess(() -> Text.literal("Group removed from allowed list"), false);
        saveConfig(stack);
        CommonClass.INSTANCE.isDirty.set(true);
        CommonClass.INSTANCE.kickAllPlayers(MaintenanceModeConfig.INSTANCE.getMessage());
        return 1;
    }

    private static void saveConfig(CraterCommandSourceStack stack) {
        try {
            MaintenanceModeConfig.INSTANCE.saveConfig(MaintenanceModeConfig.INSTANCE);
        } catch (Exception e) {
            stack.sendFailure(Text.literal("Failed to save config. Please see server log"));
            ModConstants.LOG.error("Failed to save config: {}", e.getMessage());
        }

        stack.sendSuccess(() -> Text.literal("Updated config"), false);
        CommonClass.INSTANCE.isDirty.set(true);
    }

    private static int setMessage(CraterCommandSourceStack stack, String message) {
        if (MaintenanceModeConfig.INSTANCE == null) {
            new MaintenanceModeConfig();
        }

        MaintenanceModeConfig.INSTANCE.setMessage(message);
        saveConfig(stack);
        CommonClass.INSTANCE.isDirty.set(true);
        return 1;
    }

    private static int setMotd(CraterCommandSourceStack stack, String message) {
        if (MaintenanceModeConfig.INSTANCE == null) {
            new MaintenanceModeConfig();
        }

        MaintenanceModeConfig.INSTANCE.setMotd(message);
        saveConfig(stack);
        CommonClass.INSTANCE.isDirty.set(true);
        return 1;
    }

}
