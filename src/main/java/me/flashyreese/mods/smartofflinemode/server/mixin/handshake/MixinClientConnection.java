package me.flashyreese.mods.smartofflinemode.server.mixin.handshake;

import me.flashyreese.mods.smartofflinemode.server.SOMServerLoginNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public abstract class MixinClientConnection {

    @Shadow
    private PacketListener packetListener;

    @Shadow public abstract void tick();

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;sendQueuedPackets()V", shift = At.Shift.AFTER))
    public void tick(CallbackInfo ci){
        if (this.packetListener instanceof SOMServerLoginNetworkHandler){
            ((SOMServerLoginNetworkHandler)this.packetListener).tick();
        }
    }
}
