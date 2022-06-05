package me.hypherionmc.mmode.mixin;

import com.mojang.authlib.GameProfile;
import me.hypherionmc.mmode.CommonClass;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

@Mixin(PlayerList.class)
public class PlayerListMixin {

    @Inject(method = "allowUserToConnect", at = @At("RETURN"), cancellable = true)
    public void canPlayerLogin(SocketAddress p_206258_1_, GameProfile p_206258_2_, CallbackInfoReturnable<String> cir) {
        // Check if maintenance mode is enabled and kick the player
        if (CommonClass.config != null && CommonClass.config.isEnabled()) {
            if (CommonClass.config.getAllowedUsers().stream().noneMatch(allowedUser -> allowedUser.getUuid().equals(p_206258_2_.getId()))) {
                String message = CommonClass.config.getMessage();
                if (message == null || message.isEmpty()) {
                    message = "Server is currently undergoing maintenance. Please try connecting again later";
                }
                cir.setReturnValue(message);
            }
        }
    }

}
