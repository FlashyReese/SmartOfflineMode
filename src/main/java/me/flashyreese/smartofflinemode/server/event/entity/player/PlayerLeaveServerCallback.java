package me.flashyreese.smartofflinemode.server.event.entity.player;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;

public interface PlayerLeaveServerCallback {

    Event<PlayerLeaveServerCallback> EVENT = EventFactory.createArrayBacked(PlayerLeaveServerCallback.class, listeners -> (player) -> {
        for (PlayerLeaveServerCallback callback : listeners) {
            callback.onPlayerLeave(player);
        }
    });
    void onPlayerLeave(ServerPlayerEntity player);
}