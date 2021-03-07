package me.flashyreese.smartofflinemode.server;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import me.flashyreese.smartofflinemode.api.PlayerResolver;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkEncryptionUtils;
import net.minecraft.network.encryption.NetworkEncryptionException;
import net.minecraft.network.listener.ServerLoginPacketListener;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginKeyC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginQueryResponseC2SPacket;
import net.minecraft.network.packet.s2c.login.LoginCompressionS2CPacket;
import net.minecraft.network.packet.s2c.login.LoginDisconnectS2CPacket;
import net.minecraft.network.packet.s2c.login.LoginHelloS2CPacket;
import net.minecraft.network.packet.s2c.login.LoginSuccessS2CPacket;
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

/**
 * Vanilla Copy with some modified changes.
 */
public class SOMServerLoginNetworkHandler implements ServerLoginPacketListener {
    private static final AtomicInteger authenticatorThreadId = new AtomicInteger(0);
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random RANDOM = new Random();
    private final byte[] nonce = new byte[4];
    private final MinecraftServer server;
    public final ClientConnection connection;
    private SOMServerLoginNetworkHandler.State state;
    private int loginTicks;
    private GameProfile profile;
    private ServerPlayerEntity player;

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
                this.server.getPlayerManager().onPlayerConnect(this.connection, this.player);
                this.player = null;
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
            this.profile = PlayerResolver.getGameProfile(this.profile.getName());
        }

        Text text = this.server.getPlayerManager().checkCanJoin(this.connection.getAddress(), this.profile);
        if (text != null) {
            this.disconnect(text);
        } else {
            this.state = SOMServerLoginNetworkHandler.State.ACCEPTED;
            if (this.server.getNetworkCompressionThreshold() >= 0 && !this.connection.isLocal()) {
                this.connection.send(new LoginCompressionS2CPacket(this.server.getNetworkCompressionThreshold()), (channelFuture) -> this.connection.setCompressionThreshold(this.server.getNetworkCompressionThreshold()));
            }

            this.connection.send(new LoginSuccessS2CPacket(this.profile));
            ServerPlayerEntity serverPlayerEntity = this.server.getPlayerManager().getPlayer(this.profile.getId());
            if (serverPlayerEntity != null) {
                this.state = SOMServerLoginNetworkHandler.State.DELAY_ACCEPT;
                this.player = this.server.getPlayerManager().createPlayer(this.profile);
            } else {
                this.server.getPlayerManager().onPlayerConnect(this.connection, this.server.getPlayerManager().createPlayer(this.profile));
            }
        }

    }

    public void onDisconnected(Text reason) {
        LOGGER.info("{} lost connection: {}", this.getConnectionInfo(), reason.getString());
    }

    public String getConnectionInfo() {
        return this.profile != null ? this.profile + " (" + this.connection.getAddress() + ")" : String.valueOf(this.connection.getAddress());
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
        } catch (NetworkEncryptionException var6) {
            throw new IllegalStateException("Protocol error", var6);
        }

        Thread thread = new Thread("User Authenticator #" + authenticatorThreadId.incrementAndGet()) {
            public void run() {
                GameProfile gameProfile = SOMServerLoginNetworkHandler.this.profile;

                try {
                    SOMServerLoginNetworkHandler.this.profile = SOMServerLoginNetworkHandler.this.server.getSessionService().hasJoinedServer(new GameProfile(null, gameProfile.getName()), string2, this.getClientAddress());
                    if (SOMServerLoginNetworkHandler.this.profile != null) {
                        SOMServerLoginNetworkHandler.LOGGER.info("UUID of player {} is {}", SOMServerLoginNetworkHandler.this.profile.getName(), SOMServerLoginNetworkHandler.this.profile.getId());
                        SOMServerLoginNetworkHandler.this.state = SOMServerLoginNetworkHandler.State.READY_TO_ACCEPT;
                    } else if (SOMServerLoginNetworkHandler.this.server.isSinglePlayer()) {
                        SOMServerLoginNetworkHandler.LOGGER.warn("Failed to verify username but will let them in anyway!");
                        SOMServerLoginNetworkHandler.this.profile = SOMServerLoginNetworkHandler.this.toOfflineProfile(gameProfile);
                        SOMServerLoginNetworkHandler.this.state = SOMServerLoginNetworkHandler.State.READY_TO_ACCEPT;
                    } else {
                        // Todo: allow entry verify if offline fake user or offline real user, if offline fake user let them in but lock them in login/register state else if offline real user kick with reason to relog properly with online token
                        // Additionally: We could create a dedicated login/register screen and send packets to open and log into that screen like `this#disconnect`, this unnecessary since we can login/register with chat while in the on the server but might be fun.
                        if (PlayerResolver.isOnlineAccount(gameProfile)) {
                            SOMServerLoginNetworkHandler.this.disconnect(new LiteralText("This server is using Smart Offline Mode, please log into the account you are using or please choice an account name not used by Mojang"));
                            SOMServerLoginNetworkHandler.LOGGER.error("Username '{}' tried to join with an invalid session", gameProfile.getName());
                        } else {
                            SOMServerLoginNetworkHandler.this.profile = SOMServerLoginNetworkHandler.this.toOfflineProfile(gameProfile);
                            SOMServerLoginNetworkHandler.this.state = SOMServerLoginNetworkHandler.State.READY_TO_ACCEPT;
                            SmartOfflineModeServerMod.getAuthHandler().getUnauthenticated().add(SOMServerLoginNetworkHandler.this.toOfflineProfile(gameProfile));
                        }
                    }
                } catch (AuthenticationUnavailableException var3) {
                    if (SOMServerLoginNetworkHandler.this.server.isSinglePlayer()) {
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

