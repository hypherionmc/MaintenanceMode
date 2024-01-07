package me.hypherionmc.mmode.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.hypherionmc.mmode.CommonClass;
import me.hypherionmc.mmode.ModConstants;
import me.hypherionmc.mmode.config.ConfigController;
import me.hypherionmc.mmode.config.objects.MaintenanceModeConfig;
import me.hypherionmc.mmode.util.BackupUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class MaintenanceModeCommand {

    public static void register(CommandDispatcher<CommandSourceStack> stack) {
        stack.register(Commands.literal("maintenance")
                .requires((perm) -> perm.hasPermission(3))
                .then(Commands.literal("status").executes((source) -> checkStatus(source.getSource())))
                .then(Commands.literal("on").executes((source) -> changeStatus(source.getSource(), true)))
                .then(Commands.literal("off").executes((source) -> changeStatus(source.getSource(), false)))
                .then(Commands.literal("list").executes((source) -> listAllowedUsers(source.getSource())))
                .then(Commands.literal("reload").executes((source) -> reload(source.getSource())))
                .then(Commands.literal("backup").executes((source) -> doBackup(source.getSource())))

                .then(
                        Commands.literal("doBackups")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes((source) -> changeBackups(source.getSource(), BoolArgumentType.getBool(source, "value")))))

                .then(
                        Commands.literal("setMessage")
                                .then(Commands.argument("message", StringArgumentType.string())
                                        .executes((source) -> setMessage(source.getSource(), StringArgumentType.getString(source, "message")))))
                .then(
                        Commands.literal("setMotd")
                                .then(Commands.argument("message", StringArgumentType.string())
                                        .executes((source) -> setMotd(source.getSource(), StringArgumentType.getString(source, "message")))))

                .then(
                        Commands.literal("addAllowed")
                                .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                                        .executes((source) -> addAllowedPlayer(source.getSource(), GameProfileArgument.getGameProfiles(source, "targets")))))
                .then(
                        Commands.literal("removeAllowed")
                                .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                                        .executes((source) -> removeAllowedPlayer(source.getSource(), GameProfileArgument.getGameProfiles(source, "targets"))))));
    }

    private static int doBackup(CommandSourceStack source) {
        try {
            source.sendSuccess(() -> Component.literal("Starting Maintenance Mode Backup"), true);
            BackupUtil.createBackup();
        } catch (Exception e) {
            ModConstants.LOG.error("Failed to create server backup: {}", e.getMessage());
            source.sendFailure(Component.literal("Failed to create Server backup. Please check your server log"));
        }
        return 1;
    }

    private static int changeBackups(CommandSourceStack stack, boolean enabled) {
        MaintenanceModeConfig config = CommonClass.config;
        if (config == null) {
            config = new MaintenanceModeConfig();
        }

        config.setDoBackup(enabled);
        saveConfig(config, stack);

        stack.sendSuccess(() -> Component.literal("Maintenance Mode Backups: " + ChatFormatting.YELLOW + (enabled ? "Enabled" : "Disabled")), true);
        CommonClass.isDirty.set(true);
        return 1;
    }

    private static int checkStatus(CommandSourceStack stack) {
        if (CommonClass.config != null) {
            stack.sendSuccess(() -> Component.literal("Maintenance Mode: " + ChatFormatting.YELLOW + (CommonClass.config.isEnabled() ? "Enabled" : "Disabled")), true);
        } else {
            stack.sendFailure(Component.literal("Maintenance Mode: Failed to load config"));
        }
        return 1;
    }

    public static int listAllowedUsers(CommandSourceStack stack) {
        MaintenanceModeConfig config = CommonClass.config;
        if (config == null) {
            config = new MaintenanceModeConfig();
        }

        String[] names = config.getAllowedUsers().stream().map(MaintenanceModeConfig.AllowedUser::getName).toArray(String[]::new);

        if (names.length == 0) {
            stack.sendSuccess(() -> Component.literal("No users are allowed to join"), false);
        } else {
            stack.sendSuccess(() -> Component.translatable("There are %s allowed player(s): %s", names.length, String.join(", ", names)), false);
        }

        return names.length;
    }

    private static int changeStatus(CommandSourceStack stack, boolean enabled) {
        MaintenanceModeConfig config = CommonClass.config;
        if (config == null) {
            config = new MaintenanceModeConfig();
        }

        config.setEnabled(enabled);
        try {
            ConfigController.saveConfig(config);
            CommonClass.config = ConfigController.loadConfig();
            if (enabled) {
                CommonClass.kickAllPlayers(config.getMessage());

                try {
                    if (config.isDoBackup()) {
                        BackupUtil.createBackup();
                    }
                } catch (Exception e) {
                    ModConstants.LOG.error("Failed to save backup: {}", e.getMessage());
                }

            }
        } catch (Exception e) {
            stack.sendFailure(Component.literal("Failed to save config. Please see server log"));
            ModConstants.LOG.error("Failed to save config: {}", e.getMessage());
        }

        stack.sendSuccess(() -> Component.literal("Maintenance Mode: " + ChatFormatting.YELLOW + (CommonClass.config.isEnabled() ? "Enabled" : "Disabled")), true);
        CommonClass.isDirty.set(true);
        return 1;
    }

    private static int reload(CommandSourceStack stack) {
        CommonClass.config = ConfigController.loadConfig();
        stack.sendSuccess(() -> Component.literal("Config Reloaded"), true);
        CommonClass.isDirty.set(true);
        return 1;
    }

    private static int addAllowedPlayer(CommandSourceStack stack, Collection<GameProfile> gameProfiles) throws CommandSyntaxException  {
        MaintenanceModeConfig config = CommonClass.config;
        if (config == null) {
            config = new MaintenanceModeConfig();
        }

        List<MaintenanceModeConfig.AllowedUser> allowedUsers = config.getAllowedUsers().isEmpty() ? new ArrayList<>() : config.getAllowedUsers();

        for (GameProfile profile : gameProfiles) {
            if (allowedUsers.stream().noneMatch(allowedUser -> allowedUser.getUuid().equals(profile.getId()))) {
                MaintenanceModeConfig.AllowedUser allowedUser = new MaintenanceModeConfig.AllowedUser(profile.getName(), profile.getId());
                allowedUsers.add(allowedUser);
            } else {
                throw new SimpleCommandExceptionType(Component.literal("User already in allowed list")).create();
            }
        }

        config.setAllowedUsers(allowedUsers);

        saveConfig(config, stack);
        CommonClass.isDirty.set(true);
        return 1;
    }

    private static int removeAllowedPlayer(CommandSourceStack stack, Collection<GameProfile> gameProfiles) throws CommandSyntaxException  {
        MaintenanceModeConfig config = CommonClass.config;
        if (config == null) {
            config = new MaintenanceModeConfig();
        }

        List<MaintenanceModeConfig.AllowedUser> allowedUsers = config.getAllowedUsers().isEmpty() ? new ArrayList<>() : config.getAllowedUsers();

        for (GameProfile profile : gameProfiles) {
            Optional<MaintenanceModeConfig.AllowedUser> allowedUserOptional = allowedUsers.stream().filter(allowedUser -> allowedUser.getUuid().equals(profile.getId())).findFirst();

            if (allowedUserOptional.isPresent()) {
                allowedUsers.remove(allowedUserOptional.get());
            } else {
                throw new SimpleCommandExceptionType(Component.literal("User not found in allowed list")).create();
            }
        }

        config.setAllowedUsers(allowedUsers);
        saveConfig(config, stack);
        CommonClass.isDirty.set(true);
        return 1;
    }

    private static void saveConfig(MaintenanceModeConfig config, CommandSourceStack stack) {
        try {
            ConfigController.saveConfig(config);
        } catch (Exception e) {
            stack.sendFailure(Component.literal("Failed to save config. Please see server log"));
            ModConstants.LOG.error("Failed to save config: {}", e.getMessage());
        }

        CommonClass.config = ConfigController.loadConfig();
        stack.sendSuccess(() -> Component.literal("Updated config"), true);
        CommonClass.isDirty.set(true);
    }

    private static int setMessage(CommandSourceStack stack, String message) {
        MaintenanceModeConfig config = CommonClass.config;
        if (config == null) {
            config = new MaintenanceModeConfig();
        }

        config.setMessage(message);
        saveConfig(config, stack);
        CommonClass.isDirty.set(true);
        return 1;
    }

    private static int setMotd(CommandSourceStack stack, String message) {
        MaintenanceModeConfig config = CommonClass.config;
        if (config == null) {
            config = new MaintenanceModeConfig();
        }

        config.setMotd(message);
        saveConfig(config, stack);
        CommonClass.isDirty.set(true);
        return 1;
    }

}
