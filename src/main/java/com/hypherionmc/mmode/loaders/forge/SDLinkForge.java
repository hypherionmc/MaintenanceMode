package com.hypherionmc.mmode.loaders.forge;

import com.hypherionmc.craterlib.core.event.CraterEventBus;
import com.hypherionmc.craterlib.core.platform.ModloaderEnvironment;
import com.hypherionmc.mmode.CommonClass;
import com.hypherionmc.mmode.ModConstants;
import net.minecraftforge.fml.common.Mod;

@Mod(ModConstants.MOD_ID)
public class SDLinkForge {

    public SDLinkForge() {
        if (ModloaderEnvironment.INSTANCE.getEnvironment().isServer()) {
            CraterEventBus.INSTANCE.registerEventListener(CommonClass.INSTANCE);
        }
    }
}