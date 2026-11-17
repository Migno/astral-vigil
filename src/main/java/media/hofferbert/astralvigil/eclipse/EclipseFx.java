package media.hofferbert.astralvigil.eclipse;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

/** Version-neutral helpers for the Eclipse fight: coloured dust, action-bar cues and on-screen titles. */
public final class EclipseFx {
    public static final int VIOLET = 0xB57CFF;
    public static final int PALE = 0xE0C8FF;
    public static final int DEEP = 0x5B2A86;

    private EclipseFx() {}

    public static DustParticleOptions dust(int rgb, float scale) {
        //? if >=26.1 {
        return new DustParticleOptions(rgb, scale);
        //?} else {
        /*return new DustParticleOptions(new org.joml.Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), scale);
        *///?}
    }

    /** Short cue above the hotbar (no chat spam). */
    public static void actionBar(ServerPlayer player, Component message) {
        //? if >=26.1 {
        player.sendSystemMessage(message, true);
        //?} else {
        /*player.displayClientMessage(message, true);
        *///?}
    }

    /** Big on-screen title (phase changes, Umbral Nova). */
    public static void title(ServerPlayer player, Component title, Component subtitle) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 40, 15));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }
}
