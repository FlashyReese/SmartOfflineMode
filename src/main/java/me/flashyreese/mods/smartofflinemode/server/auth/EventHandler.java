package me.flashyreese.mods.smartofflinemode.server.auth;

import com.mojang.authlib.GameProfile;
import me.flashyreese.mods.smartofflinemode.api.PlayerResolver;
import me.flashyreese.mods.smartofflinemode.server.command.LoginCommand;
import me.flashyreese.mods.smartofflinemode.server.command.RegisterCommand;
import me.flashyreese.mods.smartofflinemode.server.event.entity.player.*;
import me.flashyreese.mods.smartofflinemode.server.event.item.DropItemCallback;
import me.flashyreese.mods.smartofflinemode.server.event.item.TakeItemCallback;
import me.flashyreese.smartofflinemode.server.event.entity.player.*;
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

public final class EventHandler implements ChatCallback, PlayerJoinServerCallback, PlayerLeaveServerCallback, PlayerMoveCallback,
        PrePlayerJoinCallback, DropItemCallback, TakeItemCallback, UseEntityCallback, AttackEntityCallback, UseItemCallback,
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
            if (!this.authHandler.isRegistered(player.getGameProfile())) {
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

    }

    @Override
    public ActionResult onPlayerMove(PlayerEntity player) {
        if (!this.authHandler.isLoggedIn(player.getGameProfile())) {
            if (!player.isInvulnerable())
                player.setInvulnerable(true);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    @Override
    public LiteralText checkCanPlayerJoinServer(SocketAddress socketAddress, GameProfile profile, PlayerManager manager) {
        if (this.authHandler.isLastIP(profile, socketAddress.toString())) {
            this.authHandler.authenticateProfile(profile);
            return new LiteralText("Logged in with last IP");
        }
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
        if (!this.authHandler.isLoggedIn(player.getGameProfile())) {
            return false;
        }
        return true;
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
