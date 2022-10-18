package me.hypherionmc.mmode.mixin;

import me.hypherionmc.mmode.CommonClass;
import me.hypherionmc.mmode.config.objects.MaintenanceModeConfig;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
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
    @Shadow private ServerStatusResponse statusResponse;

    @Shadow private String motd;

    @Shadow public abstract void applyServerIconToResponse(ServerStatusResponse response);

    @Inject(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getCurrentTimeMillis()J"))
    public void runServer(CallbackInfo ci) {
        // Check if server is in maintenance mode and update MOTD
        MaintenanceModeConfig config = CommonClass.config;
        if (config == null) {
            config = new MaintenanceModeConfig();
        }

        String message = config.getMotd();

        // Use a "cache" to prevent unnecessary updates
        if (config.isEnabled()) {
            statusResponse.setServerDescription(new TextComponentString(message));
            this.applyServerIconToResponse(statusResponse);
        } else if (this.motd != null) {
            statusResponse.setServerDescription(new TextComponentString(this.motd));
            this.applyServerIconToResponse(statusResponse);
        }
    }

    @Redirect(method = "applyServerIconToResponse", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getFile(Ljava/lang/String;)Ljava/io/File;"))
    private File injectIcon(MinecraftServer instance, String $$0) {
        MaintenanceModeConfig config = CommonClass.config;
        if (config == null) {
            config = new MaintenanceModeConfig();
        }
        return config.isEnabled() ? new File(config.getMaintenanceIcon()) : new File($$0);
    }
}