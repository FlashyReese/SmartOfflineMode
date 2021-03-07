package me.flashyreese.smartofflinemode.server.event.entity.player;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;

public interface ChatCallback {
    Event<ChatCallback> EVENT = EventFactory.createArrayBacked(ChatCallback.class, listeners -> (player, message) -> {
        for (ChatCallback event : listeners) {
            ActionResult result = event.onPlayerChat(player, message);

            if (result != ActionResult.PASS) {
                return result;
            }
        }
        return ActionResult.PASS;
    });

    ActionResult onPlayerChat(PlayerEntity player, String message);
}