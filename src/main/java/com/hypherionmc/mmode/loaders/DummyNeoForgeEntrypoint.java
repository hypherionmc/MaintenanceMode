package com.hypherionmc.mmode.loaders;

import com.hypherionmc.mmode.MaintenanceMode;
import com.hypherionmc.mmode.ModConstants;
import lombok.NoArgsConstructor;
import net.minecraftforge.fml.common.Mod;

/**
 * @author HypherionSA
 * Dummy entrypoint for NeoForge. This is required, otherwise NeoForge crashes. Actual loading is handled by {@link MaintenanceMode}
 */
@Mod(ModConstants.MOD_ID)
@NoArgsConstructor
public final class DummyNeoForgeEntrypoint {}
