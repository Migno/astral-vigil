package media.hofferbert.astralvigil.block;

import com.mojang.serialization.MapCodec;
import media.hofferbert.astralvigil.crafting.AstralForgeBlockEntity;
import media.hofferbert.astralvigil.crafting.ConvergenceAltarBlockEntity;
import media.hofferbert.astralvigil.crafting.PedestalBlockEntity;
import media.hofferbert.astralvigil.starlight.network.StarlightBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
//? if <26.1 {
/*import net.minecraft.world.ItemInteractionResult;
*///?}

/**
 * Pedestal, Convergence Altar and Astral Forge. Use with an item to place one; empty hand to take it back. On the
 * altar, an empty hand starts the ritual (sneak + empty hand takes the catalyst/result).
 */
public class RitualBlock extends BaseEntityBlock {
    public enum Kind {
        PEDESTAL,
        ALTAR,
        FORGE
    }

    //? if <26.3 {
    public static final MapCodec<RitualBlock> PEDESTAL_CODEC = simpleCodec(p -> new RitualBlock(p, Kind.PEDESTAL));
    public static final MapCodec<RitualBlock> ALTAR_CODEC = simpleCodec(p -> new RitualBlock(p, Kind.ALTAR));
    public static final MapCodec<RitualBlock> FORGE_CODEC = simpleCodec(p -> new RitualBlock(p, Kind.FORGE));
    //?}

    private final Kind kind;

    public RitualBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public static RitualBlock pedestal(Properties p) {
        return new RitualBlock(p, Kind.PEDESTAL);
    }

    public static RitualBlock altar(Properties p) {
        return new RitualBlock(p, Kind.ALTAR);
    }

    public static RitualBlock forge(Properties p) {
        return new RitualBlock(p, Kind.FORGE);
    }

    public Kind kind() {
        return kind;
    }

    //? if <26.3 {
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return switch (kind) {
            case PEDESTAL -> PEDESTAL_CODEC;
            case ALTAR -> ALTAR_CODEC;
            case FORGE -> FORGE_CODEC;
        };
    }
    //?}

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (kind) {
            case PEDESTAL -> new PedestalBlockEntity(pos, state);
            case ALTAR -> new ConvergenceAltarBlockEntity(pos, state);
            case FORGE -> new AstralForgeBlockEntity(pos, state);
        };
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || kind == Kind.PEDESTAL) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof StarlightBlockEntity device) {
                StarlightBlockEntity.serverTick(lvl, pos, st, device);
            }
        };
    }

    // ------------------------------------------------------------------ interaction (version-neutral core)

    /** Tries to put one item from {@code held} into the block. */
    boolean insert(Level level, BlockPos pos, Player player, ItemStack held) {
        if (held.isEmpty() || level.isClientSide()) {
            return !held.isEmpty();
        }
        BlockEntity be = level.getBlockEntity(pos);
        boolean inserted = false;
        if (be instanceof PedestalBlockEntity pedestal && pedestal.item().isEmpty()) {
            pedestal.setItem(held.copyWithCount(1));
            inserted = true;
        } else if (be instanceof ConvergenceAltarBlockEntity altar && altar.catalyst().isEmpty() && !altar.isCrafting()) {
            altar.setCatalyst(held.copyWithCount(1));
            inserted = true;
        } else if (be instanceof AstralForgeBlockEntity forge) {
            inserted = forge.insert(held.copyWithCount(1));
        }
        if (inserted) {
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS, 0.8f, 1.4f);
        }
        return inserted;
    }

    /** Empty-hand use: take items out, or start the altar ritual. */
    void useEmpty(Level level, BlockPos pos, Player player) {
        // The off-hand pass after a main-hand insert also lands here: only a truly empty main hand takes/starts.
        if (level.isClientSide() || !player.getMainHandItem().isEmpty()) {
            return;
        }
        BlockEntity be = level.getBlockEntity(pos);
        ItemStack taken = ItemStack.EMPTY;
        if (be instanceof PedestalBlockEntity pedestal) {
            taken = pedestal.item();
            pedestal.setItem(ItemStack.EMPTY);
        } else if (be instanceof ConvergenceAltarBlockEntity altar) {
            if (!player.isShiftKeyDown() && altar.tryStart((ServerLevel) level)) {
                return;
            }
            if (!altar.isCrafting()) {
                taken = altar.catalyst();
                altar.setCatalyst(ItemStack.EMPTY);
            } else {
                player.sendSystemMessage(Component.translatable("message.astralvigil.altar.busy"));
            }
        } else if (be instanceof AstralForgeBlockEntity forge) {
            taken = forge.takeLast();
        }
        if (!taken.isEmpty() && !player.getInventory().add(taken)) {
            player.drop(taken, false);
        }
    }

    //? if >=26.1 {
    @Override
    protected InteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return insert(level, pos, player, stack) ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
    }
    //?} else {
    /*@Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return insert(level, pos, player, stack)
                ? ItemInteractionResult.sidedSuccess(level.isClientSide())
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    *///?}

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        useEmpty(level, pos, player);
        return InteractionResult.SUCCESS;
    }

    //? if <26.1 {
    /*@Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            dropContents(level, pos, level.getBlockEntity(pos));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
    *///?}

    /** Drops held items when broken (both targets route through this helper). */
    public static void dropContents(Level level, BlockPos pos, BlockEntity be) {
        if (be instanceof PedestalBlockEntity pedestal && !pedestal.item().isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), pedestal.item());
        } else if (be instanceof ConvergenceAltarBlockEntity altar && !altar.catalyst().isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), altar.catalyst());
        } else if (be instanceof AstralForgeBlockEntity forge) {
            forge.items().forEach(s -> Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), s));
        }
    }
}
