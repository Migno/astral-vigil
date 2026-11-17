package media.hofferbert.astralvigil.eclipse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import media.hofferbert.astralvigil.config.AVServerConfig;
import media.hofferbert.astralvigil.research.AnchorPatterns;
import media.hofferbert.astralvigil.research.ResearchData;
import media.hofferbert.astralvigil.research.ResearchManager;
import media.hofferbert.astralvigil.starlight.Tint;
import media.hofferbert.astralvigil.starlight.network.Devices;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Star Anchor ritual: anchors laid out in a learned constellation, each holding starlight, open sky above. Using an
 * anchor with an empty hand consumes the light, eases the Eclipse Gaze and grants the constellation's blessing to
 * players nearby. Larger layouts (scale 2–3) last longer and soothe more Gaze.
 */
public final class AnchorRitual {
    public static final int SEARCH_RADIUS = 18;

    private AnchorRitual() {}

    public static void tryActivate(ServerLevel level, BlockPos origin, ServerPlayer player) {
        if (!(level.getBlockEntity(origin) instanceof Devices.Anchor first)) {
            return;
        }
        if (level.getGameTime() < first.readyAt()) {
            player.sendSystemMessage(Component.translatable("message.astralvigil.ritual.resting").withStyle(ChatFormatting.GRAY));
            return;
        }
        Set<AnchorPatterns.P> points = new HashSet<>();
        for (BlockPos p : BlockPos.betweenClosed(
                origin.offset(-SEARCH_RADIUS, 0, -SEARCH_RADIUS), origin.offset(SEARCH_RADIUS, 0, SEARCH_RADIUS))) {
            if (level.getBlockEntity(p) instanceof Devices.Anchor) {
                points.add(new AnchorPatterns.P(p.getX(), p.getZ()));
            }
        }
        ResearchData research = ResearchManager.get(player);
        Optional<AnchorPatterns.Match> match =
                AnchorPatterns.find(points, new AnchorPatterns.P(origin.getX(), origin.getZ()), research.constellations());
        if (match.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.astralvigil.ritual.no_pattern").withStyle(ChatFormatting.GRAY));
            return;
        }
        int cost = AVServerConfig.RITUAL_STARLIGHT.get();
        List<Devices.Anchor> anchors = new ArrayList<>();
        for (AnchorPatterns.P p : match.get().anchors()) {
            BlockPos pos = new BlockPos(p.x(), origin.getY(), p.z());
            if (!(level.getBlockEntity(pos) instanceof Devices.Anchor anchor)) {
                return;
            }
            if (!level.canSeeSky(pos.above())) {
                player.sendSystemMessage(Component.translatable("message.astralvigil.ritual.no_sky").withStyle(ChatFormatting.GRAY));
                return;
            }
            if (anchor.buffer().total() < cost) {
                player.sendSystemMessage(Component.translatable("message.astralvigil.ritual.no_light", cost).withStyle(ChatFormatting.GRAY));
                return;
            }
            anchors.add(anchor);
        }

        // pay
        for (Devices.Anchor a : anchors) {
            int left = cost;
            for (Tint t : Tint.values()) {
                left -= a.buffer().extract(t, left, false);
            }
            a.setReadyAt(level.getGameTime() + AVServerConfig.RITUAL_COOLDOWN.get());
            a.syncNow();
            Vec3 c = Vec3.atCenterOf(a.getBlockPos()).add(0, 0.8, 0);
            level.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 30, 0.1, 2.0, 0.1, 0.05);
        }

        int scale = match.get().scale();
        String id = match.get().constellation().id();
        int ticks = 20 * AVServerConfig.RITUAL_BASE_SECONDS.get() * scale;
        for (ServerPlayer p : level.players()) {
            if (p.blockPosition().closerThan(origin, 16 + 4 * scale)) {
                bless(p, id, ticks);
                GazeManager.add(p, -AVServerConfig.RITUAL_GAZE_RELIEF.get() * scale);
                p.sendSystemMessage(Component.translatable(
                                "message.astralvigil.ritual.complete",
                                Component.translatable(match.get().constellation().translationKey()))
                        .withStyle(ChatFormatting.GOLD));
            }
        }
        level.playSound(null, origin, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.5f, 1.3f);
    }

    /** One blessing per Watcher; no passive permanent buffs – always timed. */
    static void bless(ServerPlayer player, String constellation, int ticks) {
        switch (constellation) {
            case "halo" -> {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ticks, 0));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Math.min(ticks, 400), 0));
            }
            case "crescent" -> {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, ticks, 0));
                //? if >=26.1 {
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, 0));
                //?} else {
                /*player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 0));
                *///?}
            }
            case "anvil" -> {
                //? if >=26.1 {
                player.addEffect(new MobEffectInstance(MobEffects.HASTE, ticks, 0));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, Math.min(ticks, 1200), 0));
                //?} else {
                /*player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, ticks, 0));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Math.min(ticks, 1200), 0));
                *///?}
            }
            default -> {
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, ticks, 0));
                player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, ticks, 0));
            }
        }
    }
}
