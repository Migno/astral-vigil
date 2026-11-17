package media.hofferbert.astralvigil.block;

import media.hofferbert.astralvigil.firmament.Firmament;
import media.hofferbert.astralvigil.registry.AVBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
//? if >=26.1 {
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.portal.TeleportTransition;
//?} else {
/*import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.portal.DimensionTransition;
*///?}

/**
 * The shimmering opening of an Astral Gate (frame lit with the Ascension Key). Stand in it for a few seconds to rise
 * into the Firmament - or, in the Firmament, to return to where you ascended from. Uses the vanilla portal machinery
 * (transition time, cooldown, confusion overlay); only players travel.
 */
public class CelestialPortalBlock extends Block implements Portal {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final VoxelShape X_SHAPE = Block.box(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);
    private static final VoxelShape Z_SHAPE = Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);
    /** Ticks a survival player must stand in the gate (creative players pass almost at once). */
    public static final int TRANSITION_TICKS = 60;

    private final media.hofferbert.astralvigil.firmament.GateKind kind;

    public CelestialPortalBlock(Properties properties, media.hofferbert.astralvigil.firmament.GateKind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? X_SHAPE : Z_SHAPE;
    }

    public media.hofferbert.astralvigil.firmament.GateKind kind() {
        return kind;
    }

    /** A gate side that is neither this portal nor its frame any more collapses the whole opening (cascades block by block). */
    boolean supported(BlockState state, Direction toNeighbour, BlockState neighbour) {
        Direction.Axis axis = toNeighbour.getAxis();
        boolean inPlane = axis == Direction.Axis.Y || axis == state.getValue(AXIS);
        return !inPlane || neighbour.is(this) || neighbour.is(AVBlocks.GATES.get(kind).get());
    }

    //? if >=26.1 {
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        return supported(state, direction, neighbour) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean precise) {
        if (entity instanceof ServerPlayer && entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    @Override
    public TeleportTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        return entity instanceof ServerPlayer player ? Firmament.gateDestination(player, kind) : null;
    }
    //?} else {
    /*@Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos,
            BlockPos neighbourPos) {
        return supported(state, direction, neighbour) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity instanceof ServerPlayer && entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    @Override
    public DimensionTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        return entity instanceof ServerPlayer player ? Firmament.gateDestination(player, kind) : null;
    }
    *///?}

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        return entity instanceof ServerPlayer p && p.getAbilities().invulnerable ? 1 : TRANSITION_TICKS;
    }

    @Override
    public Portal.Transition getLocalTransition() {
        return Portal.Transition.CONFUSION;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 2; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            if (i == 0) {
                level.addParticle(media.hofferbert.astralvigil.compat.Particles.dust(kind.colour(), 1.0f), x, y, z, 0, 0.02, 0);
            } else {
                level.addParticle(kind == media.hofferbert.astralvigil.firmament.GateKind.UMBRAL ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.END_ROD,
                        x, y, z, (random.nextDouble() - 0.5) * 0.02, 0.02, (random.nextDouble() - 0.5) * 0.02);
            }
        }
    }
}
