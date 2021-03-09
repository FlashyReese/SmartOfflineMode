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
                return SmartOfflineModeServerMod.getAuthHandler().getAccount(serverCommandSource.getPlayer().getGameProfile()) != null;
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
}
