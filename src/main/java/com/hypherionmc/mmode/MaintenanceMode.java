package com.hypherionmc.mmode;

import com.hypherionmc.craterlib.api.loader.plugins.entrypoints.CraterServerPlugin;
import com.hypherionmc.craterlib.core.event.CraterEventBus;

public class MaintenanceMode implements CraterServerPlugin {

    @Override
    public void onLoadServer() {
        CraterEventBus.INSTANCE.registerEventListener(CommonClass.INSTANCE);
    }

    @Override
    public String getPluginId() {
        return ModConstants.MOD_ID;
    }
}
