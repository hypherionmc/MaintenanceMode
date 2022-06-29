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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

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
            BackupUtil.createBackup();
        } catch (Exception e) {
            ModConstants.LOG.error("Failed to create server backup: {}", e.getMessage());
            source.sendFailure(new TextComponent("Failed to create Server backup. Please check your server log"));
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

        stack.sendSuccess(new TextComponent("Do Backups Mode: " + enabled), true);

        return 1;
    }

    private static int checkStatus(CommandSourceStack stack) {
        if (CommonClass.config != null) {
            stack.sendSuccess(new TextComponent("Maintenance Mode: " + CommonClass.config.isEnabled()), true);
        } else {
            stack.sendFailure(new TextComponent("Maintenance Mode: Failed to load config"));
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
            stack.sendSuccess(new TranslatableComponent("No users are allowed to join"), false);
        } else {
            stack.sendSuccess(new TranslatableComponent("Allowed Users:", names.length, String.join(", ", names)), false);
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
                CommonClass.kickAllPlayers();

                try {
                    if (config.isDoBackup()) {
                        BackupUtil.createBackup();
                    }
                } catch (Exception e) {
                    ModConstants.LOG.error("Failed to save backup: {}", e.getMessage());
                }

            }
        } catch (Exception e) {
            stack.sendFailure(new TextComponent("Failed to save config. Please see server log"));
            ModConstants.LOG.error("Failed to save config: {}", e.getMessage());
        }

        stack.sendSuccess(new TextComponent("Maintenance mode: " + enabled), true);

        return 1;
    }

    private static int reload(CommandSourceStack stack) {
        CommonClass.config = ConfigController.loadConfig();
        stack.sendSuccess(new TextComponent("Config Reloaded"), true);
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
                throw new SimpleCommandExceptionType(new TextComponent("User already in allowed list")).create();
            }
        }

        config.setAllowedUsers(allowedUsers);

        saveConfig(config, stack);
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
                throw new SimpleCommandExceptionType(new TextComponent("User not found in allowed list")).create();
            }
        }

        config.setAllowedUsers(allowedUsers);
        saveConfig(config, stack);

        return 1;
    }

    private static void saveConfig(MaintenanceModeConfig config, CommandSourceStack stack) {
        try {
            ConfigController.saveConfig(config);
        } catch (Exception e) {
            stack.sendFailure(new TextComponent("Failed to save config. Please see server log"));
            ModConstants.LOG.error("Failed to save config: {}", e.getMessage());
        }

        CommonClass.config = ConfigController.loadConfig();
        stack.sendSuccess(new TextComponent("Updated config"), true);
    }

    private static int setMessage(CommandSourceStack stack, String message) {
        MaintenanceModeConfig config = CommonClass.config;
        if (config == null) {
            config = new MaintenanceModeConfig();
        }

        config.setMessage(message);
        saveConfig(config, stack);
        return 1;
    }

    private static int setMotd(CommandSourceStack stack, String message) {
        MaintenanceModeConfig config = CommonClass.config;
        if (config == null) {
            config = new MaintenanceModeConfig();
        }

        config.setMotd(message);
        saveConfig(config, stack);
        return 1;
    }

}
