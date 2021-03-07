package me.flashyreese.mods.smartofflinemode.mixin.handshake;

import me.flashyreese.mods.smartofflinemode.server.SOMServerLoginNetworkHandler;
import net.minecraft.SharedConstants;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkState;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.s2c.login.LoginDisconnectS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerHandshakeNetworkHandler;
import net.minecraft.server.network.ServerQueryNetworkHandler;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerHandshakeNetworkHandler.class)
public class MixinServerHandshakeNetworkHandler {

    private static final Text IGNORING_STATUS_REQUEST_MESSAGE = new LiteralText("Ignoring status request");

    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow
    @Final
    private ClientConnection connection;

    /**
     * @author FlashyReese
     */
    @Overwrite
    public void onHandshake(HandshakeC2SPacket packet) {
        switch(packet.getIntendedState()) {
            case LOGIN:
                this.connection.setState(NetworkState.LOGIN);
                if (packet.getProtocolVersion() != SharedConstants.getGameVersion().getProtocolVersion()) {
                    TranslatableText text2;
                    if (packet.getProtocolVersion() < 754) {
                        text2 = new TranslatableText("multiplayer.disconnect.outdated_client", SharedConstants.getGameVersion().getName());
                    } else {
                        text2 = new TranslatableText("multiplayer.disconnect.incompatible", SharedConstants.getGameVersion().getName());
                    }

                    this.connection.send(new LoginDisconnectS2CPacket(text2));
                    this.connection.disconnect(text2);
                } else {
                    this.connection.setPacketListener(new SOMServerLoginNetworkHandler(this.server, this.connection));
                }
                break;
            case STATUS:
                if (this.server.acceptsStatusQuery()) {
                    this.connection.setState(NetworkState.STATUS);
                    this.connection.setPacketListener(new ServerQueryNetworkHandler(this.server, this.connection));
                } else {
                    this.connection.disconnect(IGNORING_STATUS_REQUEST_MESSAGE);
                }
                break;
            default:
                throw new UnsupportedOperationException("Invalid intention " + packet.getIntendedState());
        }

    }
}
