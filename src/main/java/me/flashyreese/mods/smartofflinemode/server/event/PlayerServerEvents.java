package me.flashyreese.mods.smartofflinemode.server.event;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.net.SocketAddress;

public class PlayerServerEvents {

    public static final Event<PreJoin> PRE_JOIN = EventFactory.createArrayBacked(PreJoin.class, listeners -> (socketAddress, profile, manager) -> {
        for (PreJoin event : listeners) {
            Text returnText = event.checkCanJoin(socketAddress, profile, manager);
            if (returnText != null) {
                return returnText;
            }
        }
        return null;
    });

    public static final Event<Join> JOIN = EventFactory.createArrayBacked(Join.class, listeners -> (player) -> {
        for (Join callback : listeners) {
            callback.onPlayerJoin(player);
        }
    });

    public static final Event<Leave> LEAVE = EventFactory.createArrayBacked(Leave.class, listeners -> (player) -> {
        for (Leave callback : listeners) {
            callback.onPlayerLeave(player);
        }
    });

    public interface PreJoin {
        Text checkCanJoin(SocketAddress socketAddress, GameProfile profile, PlayerManager manager);
    }

    public interface Join {
        void onPlayerJoin(ServerPlayerEntity player);
    }

    public interface Leave {
        void onPlayerLeave(ServerPlayerEntity player);
    }
}
