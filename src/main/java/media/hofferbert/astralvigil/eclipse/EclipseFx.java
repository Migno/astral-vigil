package media.hofferbert.astralvigil.eclipse;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Version-neutral helpers for the Eclipse fight: coloured dust, weapon bursts, action-bar cues and on-screen titles. */
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

    /**
     * Client: a summoned weapon materialises ({@code appear}: motes rush in, a flash, a resonant chime) or dissolves
     * (shards burst outward with a crystalline break) at {@code at}. Cheap: a dozen particles scaled by {@code density}.
     */
    public static void weaponBurst(Level level, Vec3 at, EclipseWeapon weapon, boolean appear, double density, RandomSource random) {
        int n = density <= 0 ? 0 : Math.max(3, (int) (14 * density));
        for (int i = 0; i < n; i++) {
            Vec3 d = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).normalize();
            if (appear) {
                Vec3 p = at.add(d.scale(1.1));
                level.addParticle(dust(i % 3 == 0 ? PALE : VIOLET, 1.1f), p.x, p.y, p.z, 0, 0, 0);
                level.addParticle(ParticleTypes.REVERSE_PORTAL, p.x, p.y, p.z, -d.x * 0.12, -d.y * 0.12, -d.z * 0.12);
            } else {
                level.addParticle(dust(i % 2 == 0 ? DEEP : VIOLET, 1.3f), at.x, at.y, at.z, 0, 0, 0);
                level.addParticle(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, d.x * 0.18, d.y * 0.18, d.z * 0.18);
            }
        }
        if (n > 0) {
            level.addParticle(ParticleTypes.END_ROD, at.x, at.y, at.z, 0, appear ? 0.02 : 0.06, 0);
        }
        if (appear) {
            level.playLocalSound(at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1.6f, weapon.pitch(), false);
            level.playLocalSound(at.x, at.y, at.z, SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.HOSTILE, 0.7f, weapon.pitch() * 1.2f, false);
        } else {
            level.playLocalSound(at.x, at.y, at.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1.4f, weapon.pitch() * 0.8f, false);
        }
    }

    /** Big on-screen title (phase changes, Umbral Nova). */
    public static void title(ServerPlayer player, Component title, Component subtitle) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 40, 15));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }
}
