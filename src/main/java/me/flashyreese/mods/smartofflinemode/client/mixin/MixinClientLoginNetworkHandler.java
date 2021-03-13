package me.flashyreese.mods.smartofflinemode.client.mixin;

import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLoginNetworkHandler.class)
public class MixinClientLoginNetworkHandler {
    @Inject(method = "joinServerSession", at = @At(value = "RETURN"), cancellable = true)
    public void preJoinServerSession(CallbackInfoReturnable<Text> cir){
        cir.setReturnValue(null);
    }
}
