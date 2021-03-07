package me.flashyreese.smartofflinemode.server;

import me.flashyreese.smartofflinemode.server.auth.AuthHandler;
import me.flashyreese.smartofflinemode.server.command.LoginCommand;
import me.flashyreese.smartofflinemode.server.command.LogoutCommand;
import me.flashyreese.smartofflinemode.server.command.RegisterCommand;
import me.flashyreese.smartofflinemode.server.event.entity.player.ChatCallback;
import me.flashyreese.smartofflinemode.server.event.entity.player.PlayerJoinServerCallback;
import me.flashyreese.smartofflinemode.server.event.entity.player.PlayerMoveCallback;
import me.flashyreese.smartofflinemode.server.event.entity.player.PrePlayerJoinCallback;
import me.flashyreese.smartofflinemode.server.event.item.DropItemCallback;
import me.flashyreese.smartofflinemode.server.event.item.TakeItemCallback;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.*;

import java.io.File;

public class SmartOfflineModeServerMod implements DedicatedServerModInitializer {

    private static AuthHandler authHandler;

    @Override
    public void onInitializeServer() {

        // Registering the commands
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(RegisterCommand.getCommand());
            dispatcher.register(LoginCommand.getCommand());
            dispatcher.register(LogoutCommand.getCommand());
        });

        // Registering the events
        PrePlayerJoinCallback.EVENT.register(getAuthHandler().getEventHandler());
        PlayerJoinServerCallback.EVENT.register(getAuthHandler().getEventHandler());
        //PlayerLeaveServerCallback.EVENT.register(authHandler::onPlayerLeave);
        DropItemCallback.EVENT.register(getAuthHandler().getEventHandler());
        TakeItemCallback.EVENT.register(getAuthHandler().getEventHandler());
        ChatCallback.EVENT.register(getAuthHandler().getEventHandler());
        PlayerMoveCallback.EVENT.register(getAuthHandler().getEventHandler());

        // From Fabric API
        PlayerBlockBreakEvents.BEFORE.register(getAuthHandler().getEventHandler());
        UseBlockCallback.EVENT.register(getAuthHandler().getEventHandler());
        UseItemCallback.EVENT.register(getAuthHandler().getEventHandler());
        AttackEntityCallback.EVENT.register(getAuthHandler().getEventHandler());
        UseEntityCallback.EVENT.register(getAuthHandler().getEventHandler());
    }

    public static AuthHandler getAuthHandler() {
        if (authHandler == null) authHandler = new AuthHandler(new File("config/smart-offline-mode-credentials.json"));
        return authHandler;
    }
}
