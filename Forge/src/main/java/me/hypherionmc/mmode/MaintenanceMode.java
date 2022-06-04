package me.hypherionmc.mmode;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(ModConstants.MOD_ID)
public class MaintenanceMode {

    public MaintenanceMode() {
        DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> () -> {
            MinecraftForge.EVENT_BUS.register(new ServerEvents());
        });
    }
}
