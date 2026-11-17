package media.hofferbert.astralvigil.block;

import java.util.Locale;
import media.hofferbert.astralvigil.research.Constellations;
import media.hofferbert.astralvigil.research.ResearchManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Seals a vault in Fallen Observatories. Unbreakable; dissolves (with its connected lock blocks) for a player who has
 * learned the constellation engraved on it.
 */
public class ConstellationLockBlock extends Block {
    public enum Sign implements StringRepresentable {
        HALO,
        CRESCENT,
        ANVIL,
        QUILL;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Sign> SIGN = EnumProperty.create("constellation", Sign.class);

    public ConstellationLockBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SIGN, Sign.HALO));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SIGN);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        String id = state.getValue(SIGN).getSerializedName();
        Component name = Component.translatable(Constellations.byId(id).map(c -> c.translationKey()).orElse(id));
        if (!ResearchManager.get(sp).hasConstellation(id)) {
            sp.sendSystemMessage(Component.translatable("message.astralvigil.lock.sealed", name).withStyle(ChatFormatting.GRAY));
            server.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0f, 0.8f);
            return InteractionResult.SUCCESS;
        }
        dissolve(server, pos, state.getBlock(), 0);
        sp.sendSystemMessage(Component.translatable("message.astralvigil.lock.opened", name).withStyle(ChatFormatting.AQUA));
        server.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0f, 1.6f);
        return InteractionResult.SUCCESS;
    }

    /** Removes this lock block and touching lock blocks (a lock may be several blocks tall). */
    private static void dissolve(ServerLevel level, BlockPos pos, Block lock, int depth) {
        if (depth > 16 || !level.getBlockState(pos).is(lock)) {
            return;
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15, 0.3, 0.3, 0.3, 0.05);
        for (BlockPos n : new BlockPos[] {pos.above(), pos.below(), pos.north(), pos.south(), pos.east(), pos.west()}) {
            dissolve(level, n, lock, depth + 1);
        }
    }
}
