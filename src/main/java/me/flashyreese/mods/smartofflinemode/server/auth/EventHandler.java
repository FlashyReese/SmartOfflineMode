package me.flashyreese.mods.smartofflinemode.server.auth;

import com.mojang.authlib.GameProfile;
import me.flashyreese.mods.smartofflinemode.api.PlayerResolver;
import me.flashyreese.mods.smartofflinemode.server.command.LoginCommand;
import me.flashyreese.mods.smartofflinemode.server.command.RegisterCommand;
import me.flashyreese.mods.smartofflinemode.server.event.PlayerEvents;
import me.flashyreese.mods.smartofflinemode.server.event.PlayerServerEvents;
import me.flashyreese.mods.smartofflinemode.server.event.item.DropItemCallback;
import me.flashyreese.mods.smartofflinemode.server.event.item.TakeItemCallback;
import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.net.SocketAddress;

public final class EventHandler implements PlayerServerEvents.PreJoin, PlayerServerEvents.Join, PlayerServerEvents.Leave,
        PlayerEvents.Chat, PlayerEvents.Move, DropItemCallback, TakeItemCallback, UseEntityCallback, AttackEntityCallback, UseItemCallback,
        UseBlockCallback, PlayerBlockBreakEvents.Before {

    private final AuthHandler authHandler;

    public EventHandler(AuthHandler authHandler) {
        this.authHandler = authHandler;
    }

    @Override
    public ActionResult onPlayerChat(PlayerEntity player, String message) {
        if (!this.authHandler.isLoggedIn(player.getGameProfile()) && !message.startsWith("/" + LoginCommand.getCommand().getLiteral()) && !message.startsWith("/" + RegisterCommand.getCommand().getLiteral())) {
            player.sendMessage(new LiteralText("Please login :>"), false);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    @Override
    public void onPlayerJoin(ServerPlayerEntity player) {
        if (!PlayerResolver.isOnlineAccount(player.getGameProfile())) {

            this.authHandler.getPlayerStateManager().trackState(player);
            this.authHandler.getPlayerStateManager().isolateState(player);

            if (this.authHandler.isLastIP(player.getGameProfile(), player.getIp())) {
                this.authHandler.authenticateProfile(player.getGameProfile());
                this.authHandler.getPlayerStateManager().restoreState(player);
                player.sendMessage(new LiteralText("Logged in with last IP"), false);

                player.getServer().getPlayerManager().sendCommandTree(player);
            } else if (!this.authHandler.isRegistered(player.getGameProfile())) {
                player.sendMessage(new LiteralText("Please register using /register"), false);
            } else {
                if (!this.authHandler.isLoggedIn(player.getGameProfile())) {
                    player.sendMessage(new LiteralText("Please login using /login"), false);
                }
            }
        }
    }

    @Override
    public void onPlayerLeave(ServerPlayerEntity player) {
        if (!this.authHandler.isLoggedIn(player.getGameProfile())) {
            this.authHandler.getPlayerStateManager().restoreState(player);
        }
    }

    @Override
    public ActionResult onPlayerMove(PlayerEntity player) {
        if (!this.authHandler.isLoggedIn(player.getGameProfile())) {
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    @Override
    public LiteralText checkCanJoin(SocketAddress socketAddress, GameProfile profile, PlayerManager manager) {
        /*InetSocketAddress sockaddr = (InetSocketAddress)socketAddress;
        if (this.authHandler.isLastIP(profile, sockaddr.getAddress().getHostAddress())) {
            return new LiteralText("Logged in with last IP");
        }*/
        return null;
    }

    @Override
    public ActionResult onDropItem(PlayerEntity player) {
        if (!this.authHandler.isLoggedIn(player.getGameProfile())) {
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    @Override
    public ActionResult onTakeItem(PlayerEntity player) {
        if (!this.authHandler.isLoggedIn(player.getGameProfile())) {
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    @Override
    public boolean beforeBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        return this.authHandler.isLoggedIn(player.getGameProfile());
    }

    @Override
    public ActionResult interact(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        if (!this.authHandler.isLoggedIn(player.getGameProfile())) {
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    @Override
    public ActionResult interact(PlayerEntity player, World world, Hand hand, Entity entity, @Nullable EntityHitResult hitResult) {
        if (!this.authHandler.isLoggedIn(player.getGameProfile())) {
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    @Override
    public TypedActionResult<ItemStack> interact(PlayerEntity player, World world, Hand hand) {
        if (!this.authHandler.isLoggedIn(player.getGameProfile())) {
            return TypedActionResult.fail(ItemStack.EMPTY);
        }
        return TypedActionResult.pass(ItemStack.EMPTY);
    }
}
