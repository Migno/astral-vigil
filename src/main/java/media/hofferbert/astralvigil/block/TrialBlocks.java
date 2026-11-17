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
            BlockPos centre = media.hofferbert.astralvigil.firmament.SanctumBuilder.centre(Watcher.VAEL);
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

        @Override
        protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
            if (!(level instanceof ServerLevel server) || !(projectile.getOwner() instanceof ServerPlayer player)
                    || state.getValue(LIT) || !"vael".equals(TrialManager.activeTrial(player))) {
                return;
            }
            BlockPos pos = hit.getBlockPos();
            server.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
            server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.5f, 1.5f);
            int total = 0;
            int lit = 0;
            for (BlockPos p : arena(pos)) {
                BlockState s = server.getBlockState(p);
                if (s.getBlock() instanceof MoonTarget) {
                    total++;
                    if (s.getValue(LIT)) {
                        lit++;
                    }
                }
            }
            if (total > 0 && lit >= total && TrialManager.complete(player, Watcher.VAEL)) {
                for (BlockPos p : arena(pos)) {
                    BlockState s = server.getBlockState(p);
                    if (s.getBlock() instanceof MoonTarget) {
                        server.setBlock(p.immutable(), s.setValue(LIT, false), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }
}
