package com.hypherionmc.mmode.loaders.neoforge;

import com.hypherionmc.craterlib.core.event.CraterEventBus;
import com.hypherionmc.craterlib.core.platform.ModloaderEnvironment;
import com.hypherionmc.mmode.CommonClass;
import com.hypherionmc.mmode.ModConstants;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(ModConstants.MOD_ID)
public class MaintenanceMode {

    public MaintenanceMode(IEventBus eventBus) {
        if (ModloaderEnvironment.INSTANCE.getEnvironment().isServer()) {
            CraterEventBus.INSTANCE.registerEventListener(CommonClass.INSTANCE);
        }
    }
}