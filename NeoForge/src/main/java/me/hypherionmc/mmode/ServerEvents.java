package me.hypherionmc.mmode;

import com.mojang.brigadier.CommandDispatcher;
import me.hypherionmc.mmode.commands.MaintenanceModeCommand;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

public class ServerEvents {

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        MaintenanceModeCommand.register(dispatcher);
    }

    @SubscribeEvent
    public void serverStartedEvent(ServerAboutToStartEvent event) {
        CommonClass.init(event.getServer());
    }
}
