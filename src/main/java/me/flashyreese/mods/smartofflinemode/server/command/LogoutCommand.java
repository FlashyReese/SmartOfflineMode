package me.flashyreese.mods.smartofflinemode.server.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.flashyreese.mods.smartofflinemode.server.SmartOfflineModeServerMod;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class LogoutCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> getCommand() {
        return CommandManager.literal("logout").requires(serverCommandSource -> SmartOfflineModeServerMod.getAuthHandler().getAccount(serverCommandSource.getPlayer().getGameProfile()) != null
                && SmartOfflineModeServerMod.getAuthHandler().isLoggedIn(serverCommandSource.getPlayer().getGameProfile())).executes(context -> logout(context.getSource()));
    }

    private static int logout(ServerCommandSource serverCommandSource) throws CommandSyntaxException {
        ServerPlayerEntity player = serverCommandSource.getPlayer();
        if (SmartOfflineModeServerMod.getAuthHandler().isLoggedIn(player.getGameProfile())) {
            if (SmartOfflineModeServerMod.getAuthHandler().deauthenticateAccount(player.getGameProfile(), true)) {
                SmartOfflineModeServerMod.getAuthHandler().getPlayerStateManager().trackState(player);
                SmartOfflineModeServerMod.getAuthHandler().getPlayerStateManager().isolateState(player);
                serverCommandSource.sendFeedback(Text.literal("Logged out!"), false);

                // Update command tree
                serverCommandSource.getServer().getPlayerManager().sendCommandTree(player);
            } else {
                serverCommandSource.sendFeedback(Text.literal("Something went wrong! :(s"), false);
            }
        } else {
            serverCommandSource.sendFeedback(Text.literal("Already logged out!"), false);
        }
        return Command.SINGLE_SUCCESS;
    }
}
