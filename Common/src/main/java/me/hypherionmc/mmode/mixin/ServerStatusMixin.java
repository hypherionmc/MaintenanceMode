package me.hypherionmc.mmode.mixin;

import me.hypherionmc.mmode.CommonClass;
import net.minecraft.network.protocol.status.ServerStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ServerStatus.class)
public class ServerStatusMixin {

    @Inject(method = "favicon", at = @At("RETURN"), cancellable = true)
    private void injectIconDefault(CallbackInfoReturnable<Optional<ServerStatus.Favicon>> cir) {
        if (!CommonClass.config.isEnabled() && CommonClass.backupIcon.isPresent())
            cir.setReturnValue(CommonClass.backupIcon);
    }

}
