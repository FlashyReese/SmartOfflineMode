package me.flashyreese.smartofflinemode.mixin.handshake;

import me.flashyreese.smartofflinemode.server.SOMServerLoginNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.IntegratedServerHandshakeNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(IntegratedServerHandshakeNetworkHandler.class)
public class MixinIntegratedServerHandshakeNetworkHandler {

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
        this.connection.setState(packet.getIntendedState());
        this.connection.setPacketListener(new SOMServerLoginNetworkHandler(this.server, this.connection));
    }
}
