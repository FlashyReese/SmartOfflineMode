package me.flashyreese.mods.smartofflinemode.server.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.flashyreese.mods.smartofflinemode.server.SmartOfflineModeServerMod;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

public class LogoutCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> getCommand() {
        return CommandManager.literal("logout").requires(serverCommandSource -> {
            try {
                return SmartOfflineModeServerMod.getAuthHandler().getAccount(serverCommandSource.getPlayer().getGameProfile()) != null
                        && SmartOfflineModeServerMod.getAuthHandler().isLoggedIn(serverCommandSource.getPlayer().getGameProfile());
            } catch (CommandSyntaxException exception) {
                exception.printStackTrace();
            }
            return false;
        }).executes(context -> logout(context.getSource()));
    }

    private static int logout(ServerCommandSource serverCommandSource) throws CommandSyntaxException {
        ServerPlayerEntity player = serverCommandSource.getPlayer();
        if (SmartOfflineModeServerMod.getAuthHandler().isLoggedIn(player.getGameProfile())) {
            if (SmartOfflineModeServerMod.getAuthHandler().deauthenticateAccount(player.getGameProfile(), true)) {
                SmartOfflineModeServerMod.getAuthHandler().getPlayerStateManager().trackState(player);
                SmartOfflineModeServerMod.getAuthHandler().getPlayerStateManager().isolateState(player);
                serverCommandSource.sendFeedback(new LiteralText("Logged out!"), false);
            } else {
                serverCommandSource.sendFeedback(new LiteralText("Something went wrong! :(s"), false);
            }
        } else {
            serverCommandSource.sendFeedback(new LiteralText("Already logged out!"), false);
        }
        return Command.SINGLE_SUCCESS;
    }
}
