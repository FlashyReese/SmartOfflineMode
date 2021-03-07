package me.flashyreese.smartofflinemode.server.event.entity.player;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;

public interface PlayerJoinServerCallback {
    Event<PlayerJoinServerCallback> EVENT = EventFactory.createArrayBacked(PlayerJoinServerCallback.class, listeners -> (player) -> {
        for (PlayerJoinServerCallback callback : listeners) {
            callback.onPlayerJoin(player);
        }
    });
    void onPlayerJoin(ServerPlayerEntity player);
}