package me.flashyreese.mods.smartofflinemode.server.mixin.callback;

import com.mojang.authlib.GameProfile;
import me.flashyreese.mods.smartofflinemode.server.event.PlayerServerEvents;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

@Mixin(PlayerManager.class)
public abstract class MixinPlayerManager {
    @Inject(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At("RETURN"))
    private void onPlayerConnect(ClientConnection clientConnection, ServerPlayerEntity serverPlayerEntity, CallbackInfo ci) {
        PlayerServerEvents.JOIN.invoker().onPlayerJoin(serverPlayerEntity);
    }

    @Inject(method = "remove(Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At("HEAD"))
    private void onPlayerLeave(ServerPlayerEntity serverPlayerEntity, CallbackInfo ci) {
        PlayerServerEvents.LEAVE.invoker().onPlayerLeave(serverPlayerEntity);
    }

    @Inject(method = "checkCanJoin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/text/Text;", at = @At("HEAD"), cancellable = true)
    private void checkCanJoin(SocketAddress socketAddress, GameProfile profile, CallbackInfoReturnable<Text> cir) {
        PlayerManager manager = (PlayerManager) (Object) this;

        LiteralText returnText = PlayerServerEvents.PRE_JOIN.invoker().checkCanJoin(socketAddress, profile, manager);
        if(returnText != null) {
            cir.setReturnValue(returnText);
        }
    }
}
