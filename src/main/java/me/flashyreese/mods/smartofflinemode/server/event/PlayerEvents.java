package me.flashyreese.mods.smartofflinemode.server.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.filter.TextStream;
import net.minecraft.util.ActionResult;

public class PlayerEvents {

    public static final Event<Chat> CHAT = EventFactory.createArrayBacked(Chat.class, listeners -> (player, message) -> {
        for (Chat event : listeners) {
            ActionResult result = event.onPlayerChat(player, message);

            if (result != ActionResult.PASS) {
                return result;
            }
        }
        return ActionResult.PASS;
    });

    public static final Event<Move> MOVE = EventFactory.createArrayBacked(Move.class, listeners -> (player) -> {
        for (Move event : listeners) {
            ActionResult result = event.onPlayerMove(player);

            if (result != ActionResult.PASS) {
                return result;
            }
        }
        return ActionResult.PASS;
    });

    public interface Chat {
        ActionResult onPlayerChat(PlayerEntity player, TextStream.Message message);
    }

    public interface Move {
        ActionResult onPlayerMove(PlayerEntity player);
    }
}
