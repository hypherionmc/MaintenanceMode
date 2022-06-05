package me.hypherionmc.mmode;

import com.mojang.brigadier.CommandDispatcher;
import me.hypherionmc.mmode.commands.MaintenanceModeCommand;
import net.minecraft.command.CommandSource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ServerEvents {

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();
        MaintenanceModeCommand.register(dispatcher);
    }

    @SubscribeEvent
    public void serverStartedEvent(FMLServerAboutToStartEvent event) {
        CommonClass.init(event.getServer());
    }
}
