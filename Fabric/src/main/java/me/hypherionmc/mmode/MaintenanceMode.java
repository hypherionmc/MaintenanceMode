package me.hypherionmc.mmode;

import me.hypherionmc.mmode.commands.MaintenanceModeCommand;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class MaintenanceMode implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        CommandRegistrationCallback.EVENT.register(((dispatcher, dedicated) -> {
            MaintenanceModeCommand.register(dispatcher);
        }));

        ServerLifecycleEvents.SERVER_STARTED.register(CommonClass::init);
    }
}
