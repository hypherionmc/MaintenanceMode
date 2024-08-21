package com.hypherionmc.mmode.loaders.fabric;

import com.hypherionmc.craterlib.core.event.CraterEventBus;
import com.hypherionmc.mmode.CommonClass;
import net.fabricmc.api.DedicatedServerModInitializer;

public class MaintenanceModeFabric implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        CommonClass commonClass = CommonClass.INSTANCE;
        CraterEventBus.INSTANCE.registerEventListener(commonClass);
    }
}
