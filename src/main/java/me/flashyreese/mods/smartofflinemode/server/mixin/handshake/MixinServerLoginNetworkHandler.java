package me.flashyreese.mods.smartofflinemode.server.mixin.handshake;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import me.flashyreese.mods.smartofflinemode.server.SmartOfflineModeServerMod;
import me.flashyreese.mods.smartofflinemode.server.util.PlayerResolver;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.login.LoginKeyC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.logging.UncaughtExceptionLogger;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.PrivateKey;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(ServerLoginNetworkHandler.class)
public abstract class MixinServerLoginNetworkHandler {

    @Shadow
    @Final
    static Logger LOGGER;

    @Shadow
    @Final
    private static AtomicInteger NEXT_AUTHENTICATOR_THREAD_ID;

    @Shadow
    @Final
    public ClientConnection connection;

    @Shadow
    @Nullable GameProfile profile;

    @Shadow
    ServerLoginNetworkHandler.State state;

    @Shadow
    @Final
    MinecraftServer server;

    @Shadow
    @Final
    private byte[] nonce;

    @Shadow
    public abstract void disconnect(Text reason);

    @Shadow
    protected abstract GameProfile toOfflineProfile(GameProfile profile);

    @Redirect(method = "acceptPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerLoginNetworkHandler;toOfflineProfile(Lcom/mojang/authlib/GameProfile;)Lcom/mojang/authlib/GameProfile;"))
    public GameProfile redirectGameProfile(ServerLoginNetworkHandler instance, GameProfile profile) {
        return PlayerResolver.getGameProfile(profile.getName());
    }

    @Inject(
            method = "onKey",
            at = @At(value = "INVOKE", target = "Ljava/lang/Thread;setUncaughtExceptionHandler(Ljava/lang/Thread$UncaughtExceptionHandler;)V", shift = At.Shift.BEFORE),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION,
            cancellable = true
    )
    private void createNewThread(LoginKeyC2SPacket packet, CallbackInfo ci, PrivateKey privateKey, String string, Thread thread) {
        Thread thread2 = new Thread("User Authenticator #" + NEXT_AUTHENTICATOR_THREAD_ID.incrementAndGet()) {

            @Override
            public void run() {
                GameProfile gameProfile = MixinServerLoginNetworkHandler.this.profile;
                try {
                    MixinServerLoginNetworkHandler.this.profile = MixinServerLoginNetworkHandler.this.server.getSessionService().hasJoinedServer(new GameProfile(null, gameProfile.getName()), string, this.getClientAddress());
                    if (MixinServerLoginNetworkHandler.this.profile != null) {
                        LOGGER.info("UUID of player {} is {}", MixinServerLoginNetworkHandler.this.profile.getName(), MixinServerLoginNetworkHandler.this.profile.getId());
                        MixinServerLoginNetworkHandler.this.state = ServerLoginNetworkHandler.State.READY_TO_ACCEPT;
                    } else if (MixinServerLoginNetworkHandler.this.server.isSingleplayer()) {
                        LOGGER.warn("Failed to verify username but will let them in anyway!");
                        MixinServerLoginNetworkHandler.this.profile = MixinServerLoginNetworkHandler.this.toOfflineProfile(gameProfile);
                        MixinServerLoginNetworkHandler.this.state = ServerLoginNetworkHandler.State.READY_TO_ACCEPT;
                    } else {
                        if (PlayerResolver.isOnlineAccount(gameProfile)) {
                            MixinServerLoginNetworkHandler.this.disconnect(new LiteralText("This server is using Smart Offline Mode, please log into the account you are using or please choice an account name not used by Mojang"));
                            LOGGER.error("Username '{}' tried to join with an invalid session", gameProfile.getName());
                        } else {
                            MixinServerLoginNetworkHandler.this.profile = MixinServerLoginNetworkHandler.this.toOfflineProfile(gameProfile);
                            MixinServerLoginNetworkHandler.this.state = ServerLoginNetworkHandler.State.READY_TO_ACCEPT;
                            SmartOfflineModeServerMod.getAuthHandler().addUnauthenticated(MixinServerLoginNetworkHandler.this.toOfflineProfile(gameProfile));
                        }
                    }
                } catch (AuthenticationUnavailableException authenticationUnavailableException) {
                    if (MixinServerLoginNetworkHandler.this.server.isSingleplayer()) {
                        LOGGER.warn("Authentication servers are down but will let them in anyway!");
                        MixinServerLoginNetworkHandler.this.profile = MixinServerLoginNetworkHandler.this.toOfflineProfile(gameProfile);
                        MixinServerLoginNetworkHandler.this.state = ServerLoginNetworkHandler.State.READY_TO_ACCEPT;
                    }
                    MixinServerLoginNetworkHandler.this.disconnect(new TranslatableText("multiplayer.disconnect.authservers_down"));
                    LOGGER.error("Couldn't verify username because servers are unavailable");
                }
            }

            @Nullable
            private InetAddress getClientAddress() {
                SocketAddress socketAddress = MixinServerLoginNetworkHandler.this.connection.getAddress();
                return MixinServerLoginNetworkHandler.this.server.shouldPreventProxyConnections() && socketAddress instanceof InetSocketAddress ? ((InetSocketAddress) socketAddress).getAddress() : null;
            }
        };
        thread2.setUncaughtExceptionHandler(new UncaughtExceptionLogger(LOGGER));
        thread2.start();
        ci.cancel();
    }
}
