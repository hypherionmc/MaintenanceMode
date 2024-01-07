package me.hypherionmc.mmode.mixin;

import com.mojang.authlib.GameProfile;
import me.hypherionmc.mmode.CommonClass;
import me.hypherionmc.mmode.util.FormattingUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

@Mixin(PlayerList.class)
public class PlayerListMixin {

    @Inject(method = "canPlayerLogin", at = @At("RETURN"), cancellable = true)
    public void canPlayerLogin(SocketAddress socketAddress, GameProfile gameProfile, CallbackInfoReturnable<Component> cir) {
        // Check if maintenance mode is enabled and kick the player
        if (CommonClass.config.isEnabled()) {
            if (CommonClass.config.getAllowedUsers().stream().noneMatch(allowedUser -> allowedUser.getUuid().equals(gameProfile.getId()))) {
                String message = CommonClass.config.getMessage();
                if (message == null || message.isEmpty())
                    message = "Server is currently undergoing maintenance. Please try connecting again later";

                cir.setReturnValue(FormattingUtils.format(message));
            }
        }
    }

}
