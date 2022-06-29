package me.hypherionmc.mmode.commands;

import com.mojang.authlib.GameProfile;
import me.hypherionmc.mmode.CommonClass;
import me.hypherionmc.mmode.ModConstants;
import me.hypherionmc.mmode.config.ConfigController;
import me.hypherionmc.mmode.config.objects.MaintenanceModeConfig;
import me.hypherionmc.mmode.util.BackupUtil;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.*;

public class CommandMaintenanceMode extends CommandBase {

    @Override
    public String getName() {
        return "maintenance";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 3;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/maintenance <on|off|status|list|addAllowed|removeAllowed|doBackups|setMessage|setMotd|reload>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            throw new WrongUsageException("/maintenance <on|off|status|list|addAllowed|removeAllowed|doBackups|setMessage|setMotd|reload>", new Object[0]);
        } else {
            if ("status".equals(args[0])) {
                checkStatus(sender);
            }
            if ("on".equals(args[0])) {
               changeStatus(sender, true);
            }
            if ("off".equals(args[0])) {
                changeStatus(sender, false);
            }
            if ("list".equals(args[0])) {
                listAllowedUsers(sender);
            }
            if ("reload".equals(args[0])) {
                reload(sender);
            }
            if ("backup".equals(args[0])) {
                doBackup(sender);
            }

            if ("doBackups".equals(args[0])) {
                if (args.length < 2) {
                    throw new WrongUsageException("true or false required");
                }
                changeBackups(sender, args[1].equalsIgnoreCase("true"));
            }

            if ("setMessage".equals(args[0])) {
                if (args.length < 2) {
                    throw new WrongUsageException("A message is required");
                }
                StringBuilder builder = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    builder.append(args[i]).append(" ");
                }
                setMessage(sender, builder.toString());
            }

            if ("setMotd".equals(args[0])) {
                if (args.length < 2) {
                    throw new WrongUsageException("A message is required");
                }
                StringBuilder builder = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    builder.append(args[i]).append(" ");
                }
                setMotd(sender, builder.toString());
            }

            if ("addAllowed".equals(args[0])) {
                if (args.length < 2) {
                    throw new WrongUsageException("A username is required");
                }
                GameProfile gameprofile = server.getPlayerProfileCache().getGameProfileForUsername(args[1]);

                if (gameprofile == null)
                {
                    throw new CommandException("Could not add " + args[1] + " to the allowed players list");
                }

                addAllowedPlayer(sender, Collections.singleton(gameprofile));
            }

            if ("removeAllowed".equals(args[0])) {
                if (args.length < 2) {
                    throw new WrongUsageException("A username is required");
                }
                GameProfile gameprofile = server.getPlayerProfileCache().getGameProfileForUsername(args[1]);

                if (gameprofile == null)
                {
                    throw new CommandException("Could not remove " + args[1] + " from the allowed players list");
                }

                removeAllowedPlayer(sender, Collections.singleton(gameprofile));
            }
        }
    }

    private void doBackup(ICommandSender source) {
        try {
            BackupUtil.createBackup();
        } catch (Exception e) {
            ModConstants.LOG.error("Failed to create server backup: {}", e.getMessage());
            source.sendMessage(new TextComponentString("Failed to create Server backup. Please check your server log"));
        }
    }

    private void changeBackups(ICommandSender stack, boolean enabled) {
        MaintenanceModeConfig config = CommonClass.config;
        if (config == null) {
            config = new MaintenanceModeConfig();
        }

        config.setDoBackup(enabled);
        saveConfig(config, stack);

        stack.sendMessage(new TextComponentString("Do Backups Mode: " + enabled));
    }

    private void checkStatus(ICommandSender stack) {
        if (CommonClass.config != null) {
            stack.sendMessage(new TextComponentString("Maintenance Mode: " + CommonClass.config.isEnabled()));
        } else {
            stack.sendMessage(new TextComponentString("Maintenance Mode: Failed to load config"));
        }
    }

    public void listAllowedUsers(ICommandSender stack) {
        MaintenanceModeConfig config = CommonClass.config;
        if (config == null) {
            config = new MaintenanceModeConfig();
        }

        String[] names = config.getAllowedUsers().stream().map(MaintenanceModeConfig.AllowedUser::getName).toArray(String[]::new);

        if (names.length == 0) {
            stack.sendMessage(new TextComponentString("No users are allowed to join"));
        } else {
            stack.sendMessage(new TextComponentString("Allowed Players: \r\n" + joinNiceString(names)));
        }
    }

    private void changeStatus(ICommandSender stack, boolean enabled) {
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
            stack.sendMessage(new TextComponentString("Failed to save config. Please see server log"));
            ModConstants.LOG.error("Failed to save config: {}", e.getMessage());
        }

        stack.sendMessage(new TextComponentString("Maintenance mode: " + enabled));
    }

    private void reload(ICommandSender stack) {
        CommonClass.config = ConfigController.loadConfig();
        stack.sendMessage(new TextComponentString("Config Reloaded"));
    }

    private void addAllowedPlayer(ICommandSender stack, Collection<GameProfile> gameProfiles) throws WrongUsageException  {
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
                throw new WrongUsageException("User already in allowed list");
            }
        }

        config.setAllowedUsers(allowedUsers);

        saveConfig(config, stack);
    }

    private void removeAllowedPlayer(ICommandSender stack, Collection<GameProfile> gameProfiles) throws WrongUsageException  {
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
                throw new WrongUsageException("User not found in allowed list");
            }
        }

        config.setAllowedUsers(allowedUsers);
        saveConfig(config, stack);
    }

    private void saveConfig(MaintenanceModeConfig config, ICommandSender stack) {
        try {
            ConfigController.saveConfig(config);
        } catch (Exception e) {
            stack.sendMessage(new TextComponentString("Failed to save config. Please see server log"));
            ModConstants.LOG.error("Failed to save config: {}", e.getMessage());
        }

        CommonClass.config = ConfigController.loadConfig();
        stack.sendMessage(new TextComponentString("Updated config"));
    }

    private void setMessage(ICommandSender stack, String message) {
        MaintenanceModeConfig config = CommonClass.config;
        if (config == null) {
            config = new MaintenanceModeConfig();
        }

        config.setMessage(message);
        saveConfig(config, stack);
    }

    private void setMotd(ICommandSender stack, String message) {
        MaintenanceModeConfig config = CommonClass.config;
        if (config == null) {
            config = new MaintenanceModeConfig();
        }

        config.setMotd(message);
        saveConfig(config, stack);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        return getListOfStringsMatchingLastWord(args, new String[] {"on", "off", "status", "list", "addAllowed", "removeAllowed", "doBackups", "setMessage", "setMotd", "reload"});
    }
}
