package me.flashyreese.mods.smartofflinemode.server;

import me.flashyreese.mods.smartofflinemode.server.auth.AuthHandler;
import me.flashyreese.mods.smartofflinemode.server.command.AccountCommand;
import me.flashyreese.mods.smartofflinemode.server.command.LoginCommand;
import me.flashyreese.mods.smartofflinemode.server.command.LogoutCommand;
import me.flashyreese.mods.smartofflinemode.server.command.RegisterCommand;
import me.flashyreese.mods.smartofflinemode.server.event.PlayerEvents;
import me.flashyreese.mods.smartofflinemode.server.event.PlayerServerEvents;
import me.flashyreese.mods.smartofflinemode.server.event.item.DropItemCallback;
import me.flashyreese.mods.smartofflinemode.server.event.item.TakeItemCallback;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.loader.api.FabricLoader;

public class SmartOfflineModeServerMod implements DedicatedServerModInitializer {

    private static AuthHandler authHandler;

    @Override
    public void onInitializeServer() {
        // Registering the commands
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            if (dedicated) {
                dispatcher.register(AccountCommand.getCommand());
                dispatcher.register(RegisterCommand.getCommand());
                dispatcher.register(LoginCommand.getCommand());
                dispatcher.register(LogoutCommand.getCommand());
            }
        });

        // Registering the events
        PlayerEvents.CHAT.register(getAuthHandler().getEventHandler());
        PlayerEvents.MOVE.register(getAuthHandler().getEventHandler());
        PlayerServerEvents.PRE_JOIN.register(getAuthHandler().getEventHandler());
        PlayerServerEvents.JOIN.register(getAuthHandler().getEventHandler());
        PlayerServerEvents.LEAVE.register(getAuthHandler().getEventHandler());
        DropItemCallback.EVENT.register(getAuthHandler().getEventHandler());
        TakeItemCallback.EVENT.register(getAuthHandler().getEventHandler());

        // From Fabric API
        PlayerBlockBreakEvents.BEFORE.register(getAuthHandler().getEventHandler());
        UseBlockCallback.EVENT.register(getAuthHandler().getEventHandler());
        UseItemCallback.EVENT.register(getAuthHandler().getEventHandler());
        AttackEntityCallback.EVENT.register(getAuthHandler().getEventHandler());
        UseEntityCallback.EVENT.register(getAuthHandler().getEventHandler());
    }

    public static AuthHandler getAuthHandler() {
        if (authHandler == null) authHandler = new AuthHandler(FabricLoader.getInstance().getConfigDir().resolve("smart-offline-mode-credentials.json").toFile());
        return authHandler;
    }
}
