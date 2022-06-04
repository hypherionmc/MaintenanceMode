package me.hypherionmc.mmode.mixin;

import me.hypherionmc.mmode.CommonClass;
import me.hypherionmc.mmode.config.objects.MaintenanceModeConfig;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Final
    @Shadow private ServerStatus status;

    @Shadow private String motd;
    private String lastMessage = "";

    @Inject(method = "runServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;startMetricsRecordingTick()V"))
    public void runServer(CallbackInfo ci) {
        // Check if server is in maintenance mode and update MOTD
        MaintenanceModeConfig config = CommonClass.config;
        if (config == null) {
            config = new MaintenanceModeConfig();
        }

        String message = "This server is currently undergoing maintenance!";

        // Use a "cache" to prevent unnecessary updates
        if (config.isEnabled()) {
            if (!this.lastMessage.equals(message)) {
                status.setDescription(new TextComponent(message));
                this.lastMessage = message;
            }
        } else if (this.motd != null && !lastMessage.equals(this.motd)) {
            status.setDescription(new TextComponent(this.motd));
            this.lastMessage = this.motd;
        }
    }

}
