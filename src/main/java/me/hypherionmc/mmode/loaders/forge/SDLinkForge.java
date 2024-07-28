package me.hypherionmc.mmode.loaders.forge;

import com.hypherionmc.craterlib.core.event.CraterEventBus;
import com.hypherionmc.craterlib.core.platform.ModloaderEnvironment;
import me.hypherionmc.mmode.CommonClass;
import me.hypherionmc.mmode.ModConstants;
import net.minecraftforge.fml.common.Mod;

@Mod(ModConstants.MOD_ID)
public class SDLinkForge {

    public SDLinkForge() {
        if (ModloaderEnvironment.INSTANCE.getEnvironment().isServer()) {
            CraterEventBus.INSTANCE.registerEventListener(CommonClass.INSTANCE);
        }
    }
}