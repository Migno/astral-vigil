package media.hofferbert.astralvigil.eclipse;

import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.compat.Celestial;
import media.hofferbert.astralvigil.config.AVServerConfig;
import media.hofferbert.astralvigil.registry.AVAttachments;
import media.hofferbert.astralvigil.registry.AVBlocks;
import media.hofferbert.astralvigil.starlight.StarlightMath;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Eclipse Gaze: rises with heavy use of celestial power (abilities, rituals), falls only through Star Anchor rituals.
 * Stages: 200 whispers · 400 Eclipse Echoes at night · 600 void crystallisation · 800 eclipse surges.
 */
public final class GazeManager {
    public static final int[] STAGES = {200, 400, 600, 800};

    private GazeManager() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(GazeManager::onPlayerTick);
    }

    public static int get(ServerPlayer player) {
        return player.getData(AVAttachments.GAZE);
    }

    public static void set(ServerPlayer player, int value) {
        int clamped = Math.max(0, Math.min(AVServerConfig.GAZE_MAX.get(), value));
        int before = get(player);
        player.setData(AVAttachments.GAZE, clamped);
        player.syncData(AVAttachments.GAZE);
        int stageBefore = stage(before);
        int stageAfter = stage(clamped);
        if (stageAfter > stageBefore) {
            player.sendSystemMessage(Component.translatable("message.astralvigil.gaze.stage." + stageAfter)
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        }
    }

    public static void add(ServerPlayer player, int amount) {
        if (AVServerConfig.GAZE_ENABLED.get() && amount != 0) {
            if (amount > 0 && media.hofferbert.astralvigil.gear.GearManager.hasHalo(player)) {
                amount = (int) Math.round(amount * (1.0 - AVServerConfig.HALO_GAZE_REDUCTION.get()));
            }
            set(player, get(player) + amount);
        }
    }

    /** 0 = calm, 1..4 = stages. */
    public static int stage(int gaze) {
        int s = 0;
        for (int t : STAGES) {
            if (gaze >= t) {
                s++;
            }
        }
        return s;
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 200 != 0) {
            return;
        }
        if (!AVServerConfig.GAZE_ENABLED.get() || player.isSpectator()) {
            return;
        }
        int stage = stage(get(player));
        if (stage == 0) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        RandomSource random = level.getRandom();
        if (random.nextDouble() > AVServerConfig.GAZE_EVENT_CHANCE.get() * stage) {
            return;
        }
        int pick = random.nextInt(stage);
        switch (pick) {
            case 0 -> whisper(player, random);
            case 1 -> echo(level, player, random);
            case 2 -> crystallize(level, player, random);
            default -> surge(level, player, random);
        }
    }

    static void whisper(ServerPlayer player, RandomSource random) {
        player.sendSystemMessage(Component.translatable("message.astralvigil.gaze.whisper." + (1 + random.nextInt(5)))
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        player.level().playSound(null, player.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.AMBIENT, 0.3f, 0.5f);
    }

    /** An Eclipse Echo that hunts the player for 30 seconds (night only; whispers by day). */
    static void echo(ServerLevel level, ServerPlayer player, RandomSource random) {
        if (!StarlightMath.isNight(Celestial.dayTime(level))) {
            whisper(player, random);
            return;
        }
        BlockPos at = player.blockPosition().offset(random.nextInt(9) - 4, 2, random.nextInt(9) - 4);
        EclipseEcho echo = EclipseEcho.spawn(level, net.minecraft.world.phys.Vec3.atBottomCenterOf(at), 20 * 30);
        if (echo != null) {
            echo.setTarget(player);
        }
    }

    static void crystallize(ServerLevel level, ServerPlayer player, RandomSource random) {
        if (!AVServerConfig.GAZE_CRYSTALLIZE_BLOCKS.get()) {
            whisper(player, random);
            return;
        }
        for (int i = 0; i < 6; i++) {
            BlockPos p = player.blockPosition().offset(random.nextInt(13) - 6, random.nextInt(5) - 3, random.nextInt(13) - 6);
            var state = level.getBlockState(p);
            if ((state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.DIRT)) && !state.hasBlockEntity()) {
                level.setBlock(p, AVBlocks.VOID_CRYSTAL.get().defaultBlockState(), Block.UPDATE_ALL);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
                level.playSound(null, p, SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.BLOCKS, 0.8f, 0.5f);
                return;
            }
        }
    }

    static void surge(ServerLevel level, ServerPlayer player, RandomSource random) {
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0));
        player.sendSystemMessage(Component.translatable("message.astralvigil.gaze.surge")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.0f, 0.6f);
        echo(level, player, random);
        AstralVigil.LOGGER.debug("Eclipse surge on {}", player.getName().getString());
    }
}
