package media.hofferbert.astralvigil.block;

import java.util.Locale;
import media.hofferbert.astralvigil.firmament.TrialManager;
import media.hofferbert.astralvigil.firmament.Watcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Blocks used by the sanctum trials. */
public final class TrialBlocks {
    private TrialBlocks() {}

    public enum WatcherProp implements StringRepresentable {
        SOLENNE,
        VAEL,
        ORRIN,
        ILYRA;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Watcher watcher() {
            return Watcher.valueOf(name());
        }
    }

    /** Goal of the parkour (Solenne) and maze (Ilyra) trials: touch or step on it while the trial runs. */
    public static class Goal extends Block {
        public static final EnumProperty<WatcherProp> WATCHER = EnumProperty.create("watcher", WatcherProp.class);
        /** Gold pads on the Sun Spire (one every 16 steps, see gen_realms.py). */
        public static final int SUN_CHECKPOINTS = 4;

        public Goal(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(WATCHER, WatcherProp.SOLENNE));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(WATCHER);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            reach(state, level, pos, player);
            return InteractionResult.SUCCESS;
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            if (entity instanceof Player player) {
                reach(state, level, pos, player);
            }
            super.stepOn(level, pos, state, entity);
        }

        static void reach(BlockState state, Level level, BlockPos pos, Player player) {
            if (state.getValue(WATCHER) == WatcherProp.SOLENNE && player instanceof ServerPlayer sp0
                    && "solenne".equals(TrialManager.activeTrial(sp0))
                    && media.hofferbert.astralvigil.firmament.Firmament.checkpointsReached(player) < SUN_CHECKPOINTS) {
                // the crown only counts for a climber who passed every gold pad on the way up
                media.hofferbert.astralvigil.firmament.Firmament.actionBar(player, net.minecraft.network.chat.Component.translatable("message.astralvigil.trial.skipped",
                        media.hofferbert.astralvigil.firmament.Firmament.checkpointsReached(player), SUN_CHECKPOINTS)
                        .withStyle(net.minecraft.ChatFormatting.GOLD));
                return;
            }
            if (level instanceof ServerLevel server && player instanceof ServerPlayer sp
                    && TrialManager.complete(sp, state.getValue(WATCHER).watcher())) {
                server.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 60, 0.4, 1.5, 0.4, 0.08);
            }
        }
    }

    /** Vael's archery target: lights up when hit by a player running Vael's trial; all six lit completes it. */
    public static class MoonTarget extends Block {
        public static final BooleanProperty LIT = BooleanProperty.create("lit");
        public static final int RADIUS = 16;

        /** Every block of the Moon Arena (Vael's sanctum incl. its far islets); falls back to a box around the target. */
        static Iterable<BlockPos> arena(BlockPos hit) {
            BlockPos centre = media.hofferbert.astralvigil.firmament.Realm.MOONWELL.trialCentre();
            if (hit.distSqr(centre) > 60 * 60) {
                return BlockPos.betweenClosed(hit.offset(-RADIUS * 2, -8, -RADIUS * 2), hit.offset(RADIUS * 2, 8, RADIUS * 2));
            }
            return BlockPos.betweenClosed(centre.offset(-40, 0, -40), centre.offset(40, 30, 40));
        }

        public MoonTarget(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(LIT, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(LIT);
        }

        /** Target positions of an arena, scanned once per level (the template never changes). */
        private static final java.util.Map<net.minecraft.resources.ResourceKey<Level>, java.util.Set<BlockPos>> TARGETS = new java.util.HashMap<>();
        static final String HITS_KEY = "astralvigil_moon_hits";

        static java.util.Set<BlockPos> targets(ServerLevel level, BlockPos near) {
            return TARGETS.computeIfAbsent(level.dimension(), k -> {
                java.util.Set<BlockPos> set = new java.util.HashSet<>();
                for (BlockPos p : arena(near)) {
                    if (level.getBlockState(p).getBlock() instanceof MoonTarget) {
                        set.add(p.immutable());
                    }
                }
                return set;
            });
        }

        /** Targets this player has lit in the running trial (per player - other archers see their own progress). */
        public static java.util.Set<BlockPos> hits(Player player) {
            java.util.Set<BlockPos> set = new java.util.HashSet<>();
            //? if >=26.1 {
            long[] packed = player.getPersistentData().getLongArray(HITS_KEY).orElse(new long[0]);
            //?} else {
            /*long[] packed = player.getPersistentData().getLongArray(HITS_KEY);
            *///?}
            for (long l : packed) {
                set.add(BlockPos.of(l));
            }
            return set;
        }

        static void saveHits(Player player, java.util.Set<BlockPos> hits) {
            player.getPersistentData().putLongArray(HITS_KEY, hits.stream().mapToLong(BlockPos::asLong).toArray());
        }

        /** Forgets a player's lit targets and shows them dark again (trial start, completion). */
        public static void resetHits(ServerPlayer player) {
            for (BlockPos p : hits(player)) {
                BlockState s = player.level().getBlockState(p);
                if (s.getBlock() instanceof MoonTarget) {
                    player.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(p, s.setValue(LIT, false)));
                }
            }
            player.getPersistentData().remove(HITS_KEY);
        }

        @Override
        protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
            if (!(level instanceof ServerLevel server) || !(projectile.getOwner() instanceof ServerPlayer player)
                    || !"vael".equals(TrialManager.activeTrial(player))) {
                return;
            }
            BlockPos pos = hit.getBlockPos().immutable();
            java.util.Set<BlockPos> hits = hits(player);
            if (!hits.add(pos)) {
                return;
            }
            saveHits(player, hits);
            // the world keeps the target dark; only this archer sees it light up
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(pos, state.setValue(LIT, true)));
            server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.5f, 1.5f);
            java.util.Set<BlockPos> all = targets(server, pos);
            hits.retainAll(all);
            media.hofferbert.astralvigil.firmament.Firmament.actionBar(player, net.minecraft.network.chat.Component.translatable(
                    "message.astralvigil.trial.moon_progress", hits.size(), all.size()).withStyle(net.minecraft.ChatFormatting.AQUA));
            if (!all.isEmpty() && hits.containsAll(all) && TrialManager.complete(player, Watcher.VAEL)) {
                resetHits(player);
            }
        }
    }
}
