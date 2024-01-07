package me.hypherionmc.mmode;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;

@Mod(ModConstants.MOD_ID)
public class MaintenanceMode {

    public MaintenanceMode(IEventBus eventBus) {
        if (FMLLoader.getDist().isDedicatedServer()) {
            NeoForge.EVENT_BUS.register(new ServerEvents());
        }
    }
}
