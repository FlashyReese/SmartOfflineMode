package me.flashyreese.mods.smartofflinemode.server;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import me.flashyreese.mods.smartofflinemode.server.util.PlayerResolver;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.encryption.NetworkEncryptionException;
import net.minecraft.network.encryption.NetworkEncryptionUtils;
import net.minecraft.network.listener.ServerLoginPacketListener;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginKeyC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginQueryResponseC2SPacket;
import net.minecraft.network.packet.s2c.login.LoginCompressionS2CPacket;
import net.minecraft.network.packet.s2c.login.LoginDisconnectS2CPacket;
import net.minecraft.network.packet.s2c.login.LoginHelloS2CPacket;
import net.minecraft.network.packet.s2c.login.LoginSuccessS2CPacket;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.logging.UncaughtExceptionLogger;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class SOMServerLoginNetworkHandler implements ServerLoginPacketListener {
    private static final AtomicInteger NEXT_AUTHENTICATOR_THREAD_ID = new AtomicInteger(0);
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random RANDOM = new Random();
    private final byte[] nonce = new byte[4];
    private final MinecraftServer server;
    public final ClientConnection connection;
    private SOMServerLoginNetworkHandler.State state;
    private int loginTicks;
    private GameProfile profile;
    private ServerPlayerEntity delayedPlayer;

    public SOMServerLoginNetworkHandler(MinecraftServer server, ClientConnection connection) {
        this.state = SOMServerLoginNetworkHandler.State.HELLO;
        this.server = server;
        this.connection = connection;
        RANDOM.nextBytes(this.nonce);
    }

    public void tick() {
        if (this.state == SOMServerLoginNetworkHandler.State.READY_TO_ACCEPT) {
            this.acceptPlayer();
        } else if (this.state == SOMServerLoginNetworkHandler.State.DELAY_ACCEPT) {
            ServerPlayerEntity serverPlayerEntity = this.server.getPlayerManager().getPlayer(this.profile.getId());
            if (serverPlayerEntity == null) {
                this.state = SOMServerLoginNetworkHandler.State.READY_TO_ACCEPT;
                this.addToServer(this.delayedPlayer);
                this.delayedPlayer = null;
            }
        }

        if (this.loginTicks++ == 600) {
            this.disconnect(new TranslatableText("multiplayer.disconnect.slow_login"));
        }

    }

    public ClientConnection getConnection() {
        return this.connection;
    }

    public void disconnect(Text reason) {
        try {
            LOGGER.info("Disconnecting {}: {}", this.getConnectionInfo(), reason.getString());
            this.connection.send(new LoginDisconnectS2CPacket(reason));
            this.connection.disconnect(reason);
        } catch (Exception var3) {
            LOGGER.error("Error whilst disconnecting player", var3);
        }

    }

    public void acceptPlayer() {
        if (!this.profile.isComplete()) {
            //this.profile = this.toOfflineProfile(this.profile);
            this.profile = PlayerResolver.getGameProfile(this.profile.getName());
        }

        Text text = this.server.getPlayerManager().checkCanJoin(this.connection.getAddress(), this.profile);
        if (text != null) {
            this.disconnect(text);
        } else {
            this.state = SOMServerLoginNetworkHandler.State.ACCEPTED;
            if (this.server.getNetworkCompressionThreshold() >= 0 && !this.connection.isLocal()) {
                this.connection.send(new LoginCompressionS2CPacket(this.server.getNetworkCompressionThreshold()), (channelFuture) -> this.connection.setCompressionThreshold(this.server.getNetworkCompressionThreshold(), true));
            }

            this.connection.send(new LoginSuccessS2CPacket(this.profile));
            ServerPlayerEntity serverPlayerEntity = this.server.getPlayerManager().getPlayer(this.profile.getId());

            try {
                ServerPlayerEntity serverPlayerEntity2 = this.server.getPlayerManager().createPlayer(this.profile);
                if (serverPlayerEntity != null) {
                    this.state = SOMServerLoginNetworkHandler.State.DELAY_ACCEPT;
                    this.delayedPlayer = serverPlayerEntity2;
                } else {
                    this.addToServer(serverPlayerEntity2);
                }
            } catch (Exception var5) {
                Text text2 = new TranslatableText("multiplayer.disconnect.invalid_player_data");
                this.connection.send(new DisconnectS2CPacket(text2));
                this.connection.disconnect(text2);
            }
        }

    }

    private void addToServer(ServerPlayerEntity player) {
        this.server.getPlayerManager().onPlayerConnect(this.connection, player);
    }

    public void onDisconnected(Text reason) {
        LOGGER.info("{} lost connection: {}", this.getConnectionInfo(), reason.getString());
    }

    public String getConnectionInfo() {
        if (this.profile != null) {
            GameProfile var10000 = this.profile;
            return var10000 + " (" + this.connection.getAddress() + ")";
        } else {
            return String.valueOf(this.connection.getAddress());
        }
    }

    public void onHello(LoginHelloC2SPacket packet) {
        Validate.validState(this.state == SOMServerLoginNetworkHandler.State.HELLO, "Unexpected hello packet");
        this.profile = packet.getProfile();
        if (this.server.isOnlineMode() && !this.connection.isLocal()) {
            this.state = SOMServerLoginNetworkHandler.State.KEY;
            this.connection.send(new LoginHelloS2CPacket("", this.server.getKeyPair().getPublic().getEncoded(), this.nonce));
        } else {
            this.state = SOMServerLoginNetworkHandler.State.READY_TO_ACCEPT;
        }

    }

    public void onKey(LoginKeyC2SPacket packet) {
        Validate.validState(this.state == SOMServerLoginNetworkHandler.State.KEY, "Unexpected key packet");
        PrivateKey privateKey = this.server.getKeyPair().getPrivate();

        final String string2;
        try {
            if (!Arrays.equals(this.nonce, packet.decryptNonce(privateKey))) {
                throw new IllegalStateException("Protocol error");
            }

            SecretKey secretKey = packet.decryptSecretKey(privateKey);
            Cipher cipher = NetworkEncryptionUtils.cipherFromKey(2, secretKey);
            Cipher cipher2 = NetworkEncryptionUtils.cipherFromKey(1, secretKey);
            string2 = (new BigInteger(NetworkEncryptionUtils.generateServerId("", this.server.getKeyPair().getPublic(), secretKey))).toString(16);
            this.state = SOMServerLoginNetworkHandler.State.AUTHENTICATING;
            this.connection.setupEncryption(cipher, cipher2);
        } catch (NetworkEncryptionException var7) {
            throw new IllegalStateException("Protocol error", var7);
        }

        Thread thread = new Thread("User Authenticator #" + NEXT_AUTHENTICATOR_THREAD_ID.incrementAndGet()) {
            public void run() {
                GameProfile gameProfile = SOMServerLoginNetworkHandler.this.profile;

                try {
                    SOMServerLoginNetworkHandler.this.profile = SOMServerLoginNetworkHandler.this.server.getSessionService().hasJoinedServer(new GameProfile(null, gameProfile.getName()), string2, this.getClientAddress());
                    if (SOMServerLoginNetworkHandler.this.profile != null) {
                        SOMServerLoginNetworkHandler.LOGGER.info("UUID of player {} is {}", SOMServerLoginNetworkHandler.this.profile.getName(), SOMServerLoginNetworkHandler.this.profile.getId());
                        SOMServerLoginNetworkHandler.this.state = SOMServerLoginNetworkHandler.State.READY_TO_ACCEPT;
                    } else if (SOMServerLoginNetworkHandler.this.server.isSingleplayer()) {
                        SOMServerLoginNetworkHandler.LOGGER.warn("Failed to verify username but will let them in anyway!");
                        SOMServerLoginNetworkHandler.this.profile = SOMServerLoginNetworkHandler.this.toOfflineProfile(gameProfile);
                        SOMServerLoginNetworkHandler.this.state = SOMServerLoginNetworkHandler.State.READY_TO_ACCEPT;
                    } else {
                        if (PlayerResolver.isOnlineAccount(gameProfile)) {
                            SOMServerLoginNetworkHandler.this.disconnect(new LiteralText("This server is using Smart Offline Mode, please log into the account you are using or please choice an account name not used by Mojang"));
                            SOMServerLoginNetworkHandler.LOGGER.error("Username '{}' tried to join with an invalid session", gameProfile.getName());
                        } else {
                            SOMServerLoginNetworkHandler.this.profile = SOMServerLoginNetworkHandler.this.toOfflineProfile(gameProfile);
                            SOMServerLoginNetworkHandler.this.state = SOMServerLoginNetworkHandler.State.READY_TO_ACCEPT;
                            SmartOfflineModeServerMod.getAuthHandler().addUnauthenticated(SOMServerLoginNetworkHandler.this.toOfflineProfile(gameProfile));
                        }
                    }
                } catch (AuthenticationUnavailableException var3) {
                    if (SOMServerLoginNetworkHandler.this.server.isSingleplayer()) {
                        SOMServerLoginNetworkHandler.LOGGER.warn("Authentication servers are down but will let them in anyway!");
                        SOMServerLoginNetworkHandler.this.profile = SOMServerLoginNetworkHandler.this.toOfflineProfile(gameProfile);
                        SOMServerLoginNetworkHandler.this.state = SOMServerLoginNetworkHandler.State.READY_TO_ACCEPT;
                    } else {
                        SOMServerLoginNetworkHandler.this.disconnect(new TranslatableText("multiplayer.disconnect.authservers_down"));
                        SOMServerLoginNetworkHandler.LOGGER.error("Couldn't verify username because servers are unavailable");
                    }
                }

            }

            @Nullable
            private InetAddress getClientAddress() {
                SocketAddress socketAddress = SOMServerLoginNetworkHandler.this.connection.getAddress();
                return SOMServerLoginNetworkHandler.this.server.shouldPreventProxyConnections() && socketAddress instanceof InetSocketAddress ? ((InetSocketAddress) socketAddress).getAddress() : null;
            }
        };
        thread.setUncaughtExceptionHandler(new UncaughtExceptionLogger(LOGGER));
        thread.start();
    }

    public void onQueryResponse(LoginQueryResponseC2SPacket packet) {
        this.disconnect(new TranslatableText("multiplayer.disconnect.unexpected_query_response"));
    }

    protected GameProfile toOfflineProfile(GameProfile profile) {
        UUID uUID = PlayerEntity.getOfflinePlayerUuid(profile.getName());
        return new GameProfile(uUID, profile.getName());
    }

    enum State {
        HELLO,
        KEY,
        AUTHENTICATING,
        READY_TO_ACCEPT,
        DELAY_ACCEPT,
        ACCEPTED
    }
}