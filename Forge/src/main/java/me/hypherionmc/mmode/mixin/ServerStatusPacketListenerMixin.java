package me.hypherionmc.mmode.mixin;

import me.hypherionmc.mmode.CommonClass;
import me.hypherionmc.mmode.config.objects.MaintenanceModeConfig;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;
import net.minecraft.server.network.ServerStatusPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerStatusPacketListenerImpl.class)
public class ServerStatusPacketListenerMixin {

    @Shadow @Final private ServerStatus status;

    @Shadow @Final private Connection connection;

    @Inject(method = "handleStatusRequest",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/server/network/ServerStatusPacketListenerImpl;hasRequestedStatus:Z",
                    shift = At.Shift.AFTER),
            cancellable = true
    )
    private void injectHandleStatusRequest(ServerboundStatusRequestPacket $$0, CallbackInfo ci) {
        MaintenanceModeConfig config = CommonClass.config;
        if (config == null) config = new MaintenanceModeConfig();

        if (config.isEnabled()) {
            ci.cancel();

            String message = config.getMotd();
            if (message == null || message.isEmpty())
                message = this.status.description().getString();

            ServerStatus modifiedStatus = new ServerStatus(Component.literal(message), this.status.players(), this.status.version(), CommonClass.favicon, this.status.enforcesSecureChat(), this.status.forgeData());
            this.connection.send(new ClientboundStatusResponsePacket(modifiedStatus));
        }
    }

}
