package me.hypherionmc.mmode;

import me.hypherionmc.mmode.commands.CommandMaintenanceMode;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

@Mod(modid = ModConstants.MOD_ID, serverSideOnly = true, acceptableRemoteVersions = "*")
public class MaintenanceMode {

    @Mod.EventHandler
    public void serverStartedEvent(FMLServerAboutToStartEvent event) {
        CommonClass.init(event.getServer());
    }

    @Mod.EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandMaintenanceMode());
    }
}
