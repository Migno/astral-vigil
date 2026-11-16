package media.hofferbert.astralvigil.starlight.network;

import media.hofferbert.astralvigil.registry.AVTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Straight-line beam tracing: stops at the first opaque block or starlight device. */
public final class BeamTracer {
    private BeamTracer() {}

    /**
     * @param receiver the device hit, or {@code null}
     * @param lengthTenths visible beam length in tenths of a block, measured from the emitter's centre
     */
    public record Hit(StarlightBlockEntity receiver, int lengthTenths) {}

    public static Hit trace(ServerLevel level, BlockPos from, Direction direction, int range) {
        BlockPos.MutableBlockPos p = from.mutable();
        for (int i = 1; i <= range; i++) {
            p.move(direction);
            if (!level.isLoaded(p)) {
                return new Hit(null, i * 10 - 5);
            }
            BlockState state = level.getBlockState(p);
            if (state.hasBlockEntity()) {
                BlockEntity be = level.getBlockEntity(p);
                if (be instanceof StarlightBlockEntity device) {
                    return new Hit(device, i * 10 - 3);
                }
            }
            if (state.isAir() || state.is(AVTags.Blocks.BEAM_TRANSPARENT)) {
                continue;
            }
            return new Hit(null, i * 10 - 5); // stops at the face of the blocking block
        }
        return new Hit(null, range * 10);
    }
}
