package me.flashyreese.mods.smartofflinemode.server.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.flashyreese.mods.smartofflinemode.server.SmartOfflineModeServerMod;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class RegisterCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> getCommand() {
        return CommandManager.literal("register").requires(serverCommandSource -> SmartOfflineModeServerMod.getAuthHandler().getAccount(serverCommandSource.getPlayer().getGameProfile()) == null && SmartOfflineModeServerMod.getAuthHandler().getUnauthenticated().contains(serverCommandSource.getPlayer().getGameProfile())).then(CommandManager.argument("password", StringArgumentType.word())
                .then(CommandManager.argument("confirmPassword", StringArgumentType.word())
                        .executes(context -> register(context.getSource(), StringArgumentType.getString(context, "password"), StringArgumentType.getString(context, "confirmPassword")))
                )
        ).executes(context -> {
            context.getSource().sendFeedback(Text.literal("Please enter password!"), false);
            return Command.SINGLE_SUCCESS;
        });
    }

    private static int register(ServerCommandSource source, String pass1, String pass2) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        if (SmartOfflineModeServerMod.getAuthHandler().isRegistered(player.getGameProfile())) {
            source.sendFeedback(Text.literal("Already registered!"), false);
        } else {
            if (pass1.equals(pass2)) {
                if (SmartOfflineModeServerMod.getAuthHandler().registerAccount(player.getGameProfile(), pass1, player.getIp())) {
                    SmartOfflineModeServerMod.getAuthHandler().getPlayerStateManager().restoreState(player);
                    source.sendFeedback(Text.literal("Registered and logged in!"), false);

                    // Update command tree
                    source.getServer().getPlayerManager().sendCommandTree(player);
                    return 0;
                } else {
                    source.sendFeedback(Text.literal("Already registered!"), false);
                }
            } else {
                source.sendFeedback(Text.literal("Passwords don't match"), false);
            }
        }
        return Command.SINGLE_SUCCESS;
    }
}
