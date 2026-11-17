package media.hofferbert.astralvigil.block;

import com.mojang.serialization.MapCodec;
import java.util.function.BiFunction;
import media.hofferbert.astralvigil.starlight.network.Devices;
import media.hofferbert.astralvigil.starlight.network.StarlightBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Shared block for all starlight devices. Placement aims the device away from the player; using it with an empty hand
 * rotates it (sneak to rotate backwards).
 */
public class StarlightDeviceBlock extends BaseEntityBlock {
    public enum Kind {
        COLLECTOR(Facing.HORIZONTAL, Devices.Collector::new),
        LENS(Facing.ALL, Devices.Lens::new),
        MIRROR(Facing.ALL, Devices.Mirror::new),
        PRISM(Facing.HORIZONTAL, Devices.Prism::new),
        RESERVOIR(Facing.NONE, Devices.Reservoir::new),
        ANCHOR(Facing.NONE, Devices.Anchor::new);

        final Facing facing;
        final BiFunction<BlockPos, BlockState, StarlightBlockEntity> factory;

        Kind(Facing facing, BiFunction<BlockPos, BlockState, StarlightBlockEntity> factory) {
            this.facing = facing;
            this.factory = factory;
        }
    }

    enum Facing {
        NONE,
        HORIZONTAL,
        ALL
    }

    // one codec per kind (BaseEntityBlock requires a codec; properties are the only constructor input)
    public static final MapCodec<StarlightDeviceBlock> COLLECTOR_CODEC = simpleCodec(p -> new StarlightDeviceBlock(p, Kind.COLLECTOR));
    public static final MapCodec<StarlightDeviceBlock> LENS_CODEC = simpleCodec(p -> new StarlightDeviceBlock(p, Kind.LENS));
    public static final MapCodec<StarlightDeviceBlock> MIRROR_CODEC = simpleCodec(p -> new StarlightDeviceBlock(p, Kind.MIRROR));
    public static final MapCodec<StarlightDeviceBlock> PRISM_CODEC = simpleCodec(p -> new StarlightDeviceBlock(p, Kind.PRISM));
    public static final MapCodec<StarlightDeviceBlock> RESERVOIR_CODEC = simpleCodec(p -> new StarlightDeviceBlock(p, Kind.RESERVOIR));
    public static final MapCodec<StarlightDeviceBlock> ANCHOR_CODEC = simpleCodec(p -> new StarlightDeviceBlock(p, Kind.ANCHOR));

    /** Set before super() runs so createBlockStateDefinition knows the kind. */
    private static final ThreadLocal<Kind> CONSTRUCTING = new ThreadLocal<>();

    private final Kind kind;

    public StarlightDeviceBlock(Properties properties, Kind kind) {
        super(prepare(properties, kind));
        CONSTRUCTING.remove();
        this.kind = kind;
        BlockState state = stateDefinition.any();
        if (kind.facing == Facing.ALL) {
            state = state.setValue(BlockStateProperties.FACING, Direction.NORTH);
        } else if (kind.facing == Facing.HORIZONTAL) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        }
        registerDefaultState(state);
    }

    private static Properties prepare(Properties properties, Kind kind) {
        CONSTRUCTING.set(kind);
        return properties;
    }

    public static StarlightDeviceBlock collector(Properties p) {
        return new StarlightDeviceBlock(p, Kind.COLLECTOR);
    }

    public static StarlightDeviceBlock lens(Properties p) {
        return new StarlightDeviceBlock(p, Kind.LENS);
    }

    public static StarlightDeviceBlock mirror(Properties p) {
        return new StarlightDeviceBlock(p, Kind.MIRROR);
    }

    public static StarlightDeviceBlock prism(Properties p) {
        return new StarlightDeviceBlock(p, Kind.PRISM);
    }

    public static StarlightDeviceBlock anchor(Properties p) {
        return new StarlightDeviceBlock(p, Kind.ANCHOR);
    }

    public static StarlightDeviceBlock reservoir(Properties p) {
        return new StarlightDeviceBlock(p, Kind.RESERVOIR);
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return switch (kind) {
            case COLLECTOR -> COLLECTOR_CODEC;
            case LENS -> LENS_CODEC;
            case MIRROR -> MIRROR_CODEC;
            case PRISM -> PRISM_CODEC;
            case RESERVOIR -> RESERVOIR_CODEC;
            case ANCHOR -> ANCHOR_CODEC;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        Kind k = kind != null ? kind : CONSTRUCTING.get();
        if (k == null) {
            return;
        }
        if (k.facing == Facing.ALL) {
            builder.add(BlockStateProperties.FACING);
        } else if (k.facing == Facing.HORIZONTAL) {
            builder.add(BlockStateProperties.HORIZONTAL_FACING);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = defaultBlockState();
        return switch (kind.facing) {
            case ALL -> state.setValue(BlockStateProperties.FACING, ctx.getNearestLookingDirection());
            case HORIZONTAL -> state.setValue(BlockStateProperties.HORIZONTAL_FACING, ctx.getHorizontalDirection());
            case NONE -> state;
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (kind == Kind.ANCHOR) {
            if (level instanceof net.minecraft.server.level.ServerLevel server
                    && player instanceof net.minecraft.server.level.ServerPlayer sp) {
                media.hofferbert.astralvigil.eclipse.AnchorRitual.tryActivate(server, pos, sp);
            }
            return InteractionResult.SUCCESS;
        }
        if (kind.facing == Facing.NONE) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, rotateOnce(state, player.isShiftKeyDown()), Block.UPDATE_ALL);
        }
        return InteractionResult.SUCCESS;
    }

    private BlockState rotateOnce(BlockState state, boolean backwards) {
        if (kind.facing == Facing.HORIZONTAL) {
            Direction d = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            return state.setValue(BlockStateProperties.HORIZONTAL_FACING, backwards ? d.getCounterClockWise() : d.getClockWise());
        }
        Direction[] order = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN};
        Direction d = state.getValue(BlockStateProperties.FACING);
        int i = 0;
        while (order[i] != d) {
            i++;
        }
        int next = Math.floorMod(i + (backwards ? -1 : 1), order.length);
        return state.setValue(BlockStateProperties.FACING, order[next]);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.setValue(BlockStateProperties.FACING, rotation.rotate(state.getValue(BlockStateProperties.FACING)));
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.setValue(
                    BlockStateProperties.HORIZONTAL_FACING, rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
        }
        return state;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.rotate(mirror.getRotation(state.getValue(BlockStateProperties.FACING)));
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.rotate(mirror.getRotation(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
        }
        return state;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return kind.factory.apply(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof StarlightBlockEntity device) {
                StarlightBlockEntity.serverTick(lvl, pos, st, device);
            }
        };
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return kind == Kind.RESERVOIR;
    }

    //? if >=26.1 {
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof StarlightBlockEntity be ? be.buffer().signal() : 0;
    }
    //?} else {
    /*@Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof StarlightBlockEntity be ? be.buffer().signal() : 0;
    }
    *///?}
}
