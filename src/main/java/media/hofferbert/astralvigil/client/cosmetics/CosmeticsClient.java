package media.hofferbert.astralvigil.client.cosmetics;

import com.mojang.blaze3d.platform.InputConstants;
import media.hofferbert.astralvigil.client.GearClient;
import media.hofferbert.astralvigil.config.AVClientConfig;
import media.hofferbert.astralvigil.cosmetics.Cosmetic;
import media.hofferbert.astralvigil.cosmetics.Cosmetic.Slot;
import media.hofferbert.astralvigil.cosmetics.Loadout;
import media.hofferbert.astralvigil.registry.AVAttachments;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/**
 * Client glue for developer cosmetics: the Cosmetics key (J; K is Iris' shader toggle), render layers and the particle auras - orbiting
 * starlight motes, rising solar embers and a crackling void storm that throws small lightning arcs.
 * Particles are purely client-side and scale with the client particle-density setting.
 */
public final class CosmeticsClient {
    static final KeyMapping OPEN =
            new KeyMapping("key.astralvigil.cosmetics", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, GearClient.CATEGORY);
    private static final double RANGE_SQR = 48 * 48;

    private CosmeticsClient() {}

    public static void register(IEventBus modBus) {
        CosmeticLayer.register(modBus);
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(OPEN));
        NeoForge.EVENT_BUS.addListener(CosmeticsClient::onClientTick);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        while (OPEN.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new CosmeticsScreen());
            }
        }
        if (mc.isPaused()) {
            return;
        }
        double density = AVClientConfig.particleFactor();
        if (density <= 0) {
            return;
        }
        for (Player player : mc.level.players()) {
            if (player.isInvisible() || player.distanceToSqr(mc.player) > RANGE_SQR) {
                continue;
            }
            // no aura in your own face: skip yourself while the camera is in your head
            if (player == mc.player && !mc.gameRenderer.getMainCamera().isDetached()) {
                continue;
            }
            Loadout loadout = player.getData(AVAttachments.COSMETICS);
            loadout.get(Slot.AURA).ifPresent(c -> aura(mc.level, player, c, density));
            loadout.get(Slot.HEAD).ifPresent(c -> crown(mc.level, player, c, density));
        }
    }

    static void aura(ClientLevel level, Player p, Cosmetic c, double density) {
        RandomSource r = level.getRandom();
        float t = p.tickCount;
        switch (c) {
            case STARLIGHT_MOTES -> {
                for (int i = 0; i < 2; i++) {
                    double a = t * 0.15 + i * Math.PI;
                    double y = 0.25 + (Mth.sin(t * 0.05f + i * 1.7f) * 0.5 + 0.5) * 1.6;
                    add(level, density, dust(0x8FEFFF, 0.8f), p.getX() + Math.cos(a) * 0.8, p.getY() + y, p.getZ() + Math.sin(a) * 0.8, 0, 0, 0);
                }
                if (r.nextInt(6) == 0) {
                    add(level, density, ParticleTypes.END_ROD, p.getX() + r.nextGaussian() * 0.4, p.getY() + 0.2 + r.nextDouble() * 1.6,
                            p.getZ() + r.nextGaussian() * 0.4, 0, 0.015, 0);
                }
            }
            case SOLAR_EMBERS -> {
                double a = r.nextDouble() * Math.PI * 2;
                add(level, density, dust(r.nextBoolean() ? 0xF2C94C : 0xFFF4C0, 0.9f), p.getX() + Math.cos(a) * 0.55, p.getY() + 0.1,
                        p.getZ() + Math.sin(a) * 0.55, 0, 0.06, 0);
                if (r.nextInt(5) == 0) {
                    add(level, density, ParticleTypes.SMALL_FLAME, p.getX() + Math.cos(a) * 0.5, p.getY() + 0.05, p.getZ() + Math.sin(a) * 0.5,
                            0, 0.02, 0);
                }
            }
            case VOID_STORM -> {
                for (int i = 0; i < 3; i++) {
                    double a = -t * 0.3 + i * Math.PI * 2 / 3;
                    double y = 0.2 + i * 0.7 + Mth.sin(t * 0.2f + i) * 0.15;
                    add(level, density, dust(i == 1 ? 0xE0C8FF : 0x5B2A86, 1.0f), p.getX() + Math.cos(a) * 0.9, p.getY() + y,
                            p.getZ() + Math.sin(a) * 0.9, 0, 0, 0);
                }
                if (r.nextInt(4) == 0) {
                    add(level, density, ParticleTypes.ELECTRIC_SPARK, p.getX() + r.nextGaussian() * 0.5, p.getY() + r.nextDouble() * 2,
                            p.getZ() + r.nextGaussian() * 0.5, 0, 0, 0);
                }
                if ((p.tickCount + p.getId() * 7) % 24 == 0 && r.nextDouble() < density) {
                    lightning(level, p, r);
                }
            }
            default -> {}
        }
    }

    /** Head extras: the Eclipse Crown crackles, the halo sheds a mote now and then. */
    static void crown(ClientLevel level, Player p, Cosmetic c, double density) {
        RandomSource r = level.getRandom();
        double top = p.getY() + p.getBbHeight() + 0.15;
        if (c == Cosmetic.ECLIPSE_CROWN && r.nextInt(10) == 0) {
            add(level, density, ParticleTypes.ELECTRIC_SPARK, p.getX() + r.nextGaussian() * 0.25, top, p.getZ() + r.nextGaussian() * 0.25, 0, 0, 0);
        } else if (c == Cosmetic.SERAPH_HALO && r.nextInt(14) == 0) {
            add(level, density, ParticleTypes.END_ROD, p.getX() + r.nextGaussian() * 0.2, top + 0.2, p.getZ() + r.nextGaussian() * 0.2, 0, -0.01, 0);
        }
    }

    /**
     * A small forked lightning arc: a jagged polyline of bright dust from above the head down to the ground beside the
     * player, with sparks where it lands.
     */
    static void lightning(ClientLevel level, Player p, RandomSource r) {
        double angle = r.nextDouble() * Math.PI * 2;
        Vec3 from = new Vec3(p.getX() + Math.cos(angle) * 0.2, p.getY() + p.getBbHeight() + 0.9, p.getZ() + Math.sin(angle) * 0.2);
        Vec3 to = new Vec3(p.getX() + Math.cos(angle) * 1.4, p.getY() + 0.05, p.getZ() + Math.sin(angle) * 1.4);
        Vec3 fork = bolt(level, r, from, to, 8, 0.28, true);
        // a shorter side branch from the middle of the bolt
        double side = angle + (r.nextBoolean() ? 1 : -1) * (0.8 + r.nextDouble() * 0.6);
        bolt(level, r, fork, new Vec3(p.getX() + Math.cos(side) * 1.6, p.getY() + 0.4 + r.nextDouble() * 0.5, p.getZ() + Math.sin(side) * 1.6), 4, 0.2, false);
        for (int i = 0; i < 10; i++) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, to.x, to.y + 0.05, to.z, r.nextGaussian() * 0.15, 0.12, r.nextGaussian() * 0.15);
        }
    }

    /** Draws one jagged bolt (bright white core inside a violet glow); returns the point halfway down for a branch. */
    private static Vec3 bolt(ClientLevel level, RandomSource r, Vec3 from, Vec3 to, int segments, double jitter, boolean thick) {
        Vec3 prev = from;
        Vec3 middle = from.lerp(to, 0.5);
        for (int i = 1; i <= segments; i++) {
            Vec3 next = from.lerp(to, i / (double) segments);
            if (i < segments) {
                next = next.add(r.nextGaussian() * jitter, r.nextGaussian() * jitter * 0.3, r.nextGaussian() * jitter);
            }
            if (i == segments / 2) {
                middle = next;
            }
            int steps = Math.max(2, (int) (prev.distanceTo(next) / 0.06));
            for (int s = 0; s < steps; s++) {
                Vec3 q = prev.lerp(next, s / (double) steps);
                // glowing (full-bright) spark core, so the bolt reads as light even at night
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, q.x, q.y, q.z, 0, 0, 0);
                if (thick && s % 2 == 0) {
                    level.addParticle(dust(0xB57CFF, 0.7f), q.x, q.y, q.z, 0, 0, 0);
                }
            }
            prev = next;
        }
        return middle;
    }

    private static void add(ClientLevel level, double density, ParticleOptions o, double x, double y, double z, double dx, double dy, double dz) {
        if (density >= 1 || level.getRandom().nextDouble() < density) {
            level.addParticle(o, x, y, z, dx, dy, dz);
        }
    }

    static DustParticleOptions dust(int rgb, float scale) {
        return media.hofferbert.astralvigil.compat.Particles.dust(rgb, scale);
    }
}
