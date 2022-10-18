package me.hypherionmc.mmode.mixin;

import me.hypherionmc.mmode.CommonClass;
import me.hypherionmc.mmode.config.objects.MaintenanceModeConfig;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Final
    @Shadow private ServerStatusResponse status;

    @Shadow private String motd;

    @Shadow protected abstract void updateStatusIcon(ServerStatusResponse par1);

    @Inject(method = "runServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/IProfiler;startTick()V"))
    public void runServer(CallbackInfo ci) {
        // Check if server is in maintenance mode and update MOTD
        MaintenanceModeConfig config = CommonClass.config;
        if (config == null) {
            config = new MaintenanceModeConfig();
        }

        String message = config.getMotd();

        // Use a "cache" to prevent unnecessary updates
        if (config.isEnabled()) {
            status.setDescription(new StringTextComponent(message));
            this.updateStatusIcon(status);
        } else if (this.motd != null) {
            status.setDescription(new StringTextComponent(this.motd));
            this.updateStatusIcon(status);
        }
    }

    @Redirect(method = "updateStatusIcon", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getFile(Ljava/lang/String;)Ljava/io/File;"))
    private File injectIcon(MinecraftServer instance, String $$0) {
        MaintenanceModeConfig config = CommonClass.config;
        if (config == null) {
            config = new MaintenanceModeConfig();
        }
        return config.isEnabled() ? new File(config.getMaintenanceIcon()) : new File($$0);
    }
}
