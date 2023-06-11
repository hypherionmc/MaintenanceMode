package me.hypherionmc.mmode.mixin;

import me.hypherionmc.mmode.CommonClass;
import me.hypherionmc.mmode.config.objects.MaintenanceModeConfig;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.network.status.ServerStatusNetHandler;
import net.minecraft.network.status.client.CServerQueryPacket;
import net.minecraft.network.status.server.SServerInfoPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerStatusNetHandler.class)
public class ServerStatusPacketListenerMixin {

    @Shadow @Final private NetworkManager connection;

    @Shadow @Final private MinecraftServer server;

    @Inject(method = "handleStatusRequest",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/network/status/ServerStatusNetHandler;hasRequestedStatus:Z",
                    ordinal = 1,
                    shift = At.Shift.AFTER),
            cancellable = true
    )
    private void injectHandleStatusRequest(CServerQueryPacket p_147312_1_, CallbackInfo ci) {
        MaintenanceModeConfig config = CommonClass.config;
        if (config == null) config = new MaintenanceModeConfig();

        if (config.isEnabled()) {
            ci.cancel();

            ServerStatusResponse status = this.server.getStatus();

            String message = config.getMotd();
            if (message == null || message.isEmpty()) {
                status.getDescription();
                message = status.getDescription().getString();
            }

            status.setDescription(new StringTextComponent(message));
            status.setFavicon(CommonClass.favicon.orElse(null));
            this.connection.send(new SServerInfoPacket(status));
        }
    }

}
