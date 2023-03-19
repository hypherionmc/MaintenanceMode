package me.hypherionmc.mmode.mixin;

import me.hypherionmc.mmode.CommonClass;
import me.hypherionmc.mmode.config.objects.MaintenanceModeConfig;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Optional;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Shadow @Nullable private ServerStatus status;

    @Shadow protected abstract ServerStatus buildServerStatus();

    @Shadow @Nullable private ServerStatus.Favicon statusIcon;

    @Shadow protected abstract Optional<ServerStatus.Favicon> loadStatusIcon();

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void injectTickServer(BooleanSupplier $$0, CallbackInfo ci) {
        if (CommonClass.isDirty.get()) {
            this.statusIcon = this.loadStatusIcon().orElse(null);
            status = buildServerStatus();
            CommonClass.isDirty.set(false);
        }
    }

    @ModifyArg(method = "buildServerStatus", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;nullToEmpty(Ljava/lang/String;)Lnet/minecraft/network/chat/Component;"))
    private String injectMotd(String $$0) {
        // Check if server is in maintenance mode and update MOTD
        MaintenanceModeConfig config = CommonClass.config;
        if (config == null) {
            config = new MaintenanceModeConfig();
        }

        String message = config.getMotd();

        // Use a "cache" to prevent unnecessary updates
        if (config.isEnabled() && (message != null && !message.isEmpty())) {
            return message;
        }
        return $$0;
    }

    @Redirect(method = "loadStatusIcon", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getFile(Ljava/lang/String;)Ljava/io/File;"))
    private File injectIcon(MinecraftServer instance, String $$0) {
        MaintenanceModeConfig config = CommonClass.config;
        if (config == null) {
            config = new MaintenanceModeConfig();
        }
        return config.isEnabled() ? new File(config.getMaintenanceIcon()) : new File($$0);
    }

}
