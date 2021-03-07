package me.flashyreese.mods.smartofflinemode.server.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.flashyreese.mods.smartofflinemode.api.PlayerResolver;
import me.flashyreese.mods.smartofflinemode.server.SmartOfflineModeServerMod;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

public class LoginCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> getCommand() {
        return CommandManager.literal("login").requires(serverCommandSource -> {
            try {
                return !PlayerResolver.isOnlineAccount(serverCommandSource.getPlayer().getGameProfile());
            } catch (CommandSyntaxException exception) {
                exception.printStackTrace();
            }
            return false;
        }).then(CommandManager.argument("password", StringArgumentType.word())
                .executes(context -> login(context.getSource(), StringArgumentType.getString(context, "password")))
        ).executes(context -> {
            context.getSource().sendFeedback(new LiteralText("Please enter a password"), false);
            return Command.SINGLE_SUCCESS;
        });
    }

    private static int login(ServerCommandSource source, String pass) throws CommandSyntaxException {
        ServerPlayerEntity playerEntity = source.getPlayer();
        if (SmartOfflineModeServerMod.getAuthHandler().isRegistered(playerEntity.getGameProfile())) {
            if (SmartOfflineModeServerMod.getAuthHandler().authenticateAccount(playerEntity.getGameProfile(), pass, playerEntity.getIp())) {
                playerEntity.setInvulnerable(false);
                source.sendFeedback(new LiteralText("Logged in!"), false);
            } else {
                source.sendFeedback(new LiteralText("Invalid password!"), false);
            }
        } else {
            source.sendFeedback(new LiteralText("This account is not registered yet!"), false);
        }
        return Command.SINGLE_SUCCESS;
    }
}
