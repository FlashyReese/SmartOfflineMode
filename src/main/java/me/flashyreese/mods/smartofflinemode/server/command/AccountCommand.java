package me.flashyreese.mods.smartofflinemode.server.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.flashyreese.mods.smartofflinemode.server.SmartOfflineModeServerMod;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

public class AccountCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> getCommand() {
        return CommandManager.literal("account").requires(serverCommandSource -> {
            try {
                return SmartOfflineModeServerMod.getAuthHandler().getAccount(serverCommandSource.getPlayer().getGameProfile()) != null && SmartOfflineModeServerMod.getAuthHandler().isLoggedIn(serverCommandSource.getPlayer().getGameProfile());
            } catch (CommandSyntaxException exception) {
                exception.printStackTrace();
            }
            return false;
        }).then(CommandManager.literal("changePassword")
                .then(CommandManager.argument("oldPassword", StringArgumentType.word())
                        .then(CommandManager.argument("newPassword", StringArgumentType.word())
                                .then(CommandManager.argument("confirmNewPassword", StringArgumentType.word())
                                        .executes(context -> changePassword(context.getSource(), StringArgumentType.getString(context, "oldPassword"), StringArgumentType.getString(context, "newPassword"), StringArgumentType.getString(context, "confirmNewPassword")))
                                )
                        )
                )
        ).then(CommandManager.literal("deleteAccount")
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .then(CommandManager.argument("password", StringArgumentType.word())
                                .executes(context -> deleteAccount(context.getSource(), StringArgumentType.getString(context, "player"), StringArgumentType.getString(context, "password")))
                        )
                )
        );
    }

    private static int changePassword(ServerCommandSource source, String oldPass, String newPass, String confirmNewPass) throws CommandSyntaxException {
        ServerPlayerEntity playerEntity = source.getPlayer();
        if (SmartOfflineModeServerMod.getAuthHandler().isRegistered(playerEntity.getGameProfile())) {
            if (SmartOfflineModeServerMod.getAuthHandler().isValidPassword(playerEntity.getGameProfile(), oldPass)) {
                if (newPass.equals(confirmNewPass)) {
                    SmartOfflineModeServerMod.getAuthHandler().changeAccountPassword(playerEntity.getGameProfile(), newPass);
                    source.sendFeedback(new LiteralText("Successfully changed password!"), false);
                } else {
                    source.sendFeedback(new LiteralText("The new password does not match!"), false);
                }
            } else {
                source.sendFeedback(new LiteralText("The old password does not match!"), false);
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int deleteAccount(ServerCommandSource source, String player, String newPass) throws CommandSyntaxException {
        ServerPlayerEntity playerEntity = source.getPlayer();
        if (SmartOfflineModeServerMod.getAuthHandler().isRegistered(playerEntity.getGameProfile())) {
            if (playerEntity.getGameProfile().getName().equals(player)) {
                if (SmartOfflineModeServerMod.getAuthHandler().isValidPassword(playerEntity.getGameProfile(), newPass)) {
                    if (SmartOfflineModeServerMod.getAuthHandler().unregisterAccount(playerEntity.getGameProfile())) {
                        //Todo: Backup Offline Player Data, Delete Player Data Live
                        //Fixme: trackState is a boolean put if statement on all it's location
                        SmartOfflineModeServerMod.getAuthHandler().getPlayerStateManager().trackState(playerEntity);
                        SmartOfflineModeServerMod.getAuthHandler().getPlayerStateManager().isolateState(playerEntity);
                        SmartOfflineModeServerMod.getAuthHandler().addUnauthenticated(playerEntity.getGameProfile());

                        source.sendFeedback(new LiteralText("Account has been deleted!"), false);
                        // Update command tree
                        source.getMinecraftServer().getPlayerManager().sendCommandTree(playerEntity);
                    } else {
                        source.sendFeedback(new LiteralText("Something went wrong!"), false);
                    }
                } else {
                    source.sendFeedback(new LiteralText("The password does not match!"), false);
                }
            } else {
                source.sendFeedback(new LiteralText("The player name does not match!"), false);
            }
        }
        return Command.SINGLE_SUCCESS;
    }
}
