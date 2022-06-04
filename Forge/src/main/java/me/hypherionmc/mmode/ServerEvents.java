package me.hypherionmc.mmode;

import com.mojang.brigadier.CommandDispatcher;
import me.hypherionmc.mmode.commands.MaintenanceModeCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
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
