package me.flashyreese.mods.smartofflinemode.server.mixin.callback;

import me.flashyreese.mods.smartofflinemode.server.event.item.DropItemCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class MixinPlayerEntity {

    // Player item dropping
    @Inject(method = "dropSelectedItem(Z)Z", at = @At("HEAD"), cancellable = true)
    private void dropSelectedItem(boolean dropEntireStack, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        ActionResult result = DropItemCallback.EVENT.invoker().onDropItem(player);

        if (result == ActionResult.FAIL) {
            cir.setReturnValue(false);
        }
    }
}