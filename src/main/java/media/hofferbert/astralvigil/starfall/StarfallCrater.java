package media.hofferbert.astralvigil.starfall;

import media.hofferbert.astralvigil.registry.AVBlocks;
import media.hofferbert.astralvigil.registry.AVTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Carves the bowl-shaped impact crater. Never touches block entities, unbreakable blocks, fluids or blocks tagged
 * {@code #astralvigil:starfall_immune}. Blocks are removed without drops (the meteor vaporises them).
 */
public final class StarfallCrater {
    /** Vertical squash of the crater ellipsoid. */
    static final double DEPTH_FACTOR = 0.7;

    private StarfallCrater() {}

    /**
     * @param surface the first air block above the ground at the impact column
     * @return the position of the placed Fallen Star
     */
    public static BlockPos impact(ServerLevel level, BlockPos surface, int radius, boolean terrainDamage, RandomSource random) {
        if (!terrainDamage) {
            BlockPos star = surface;
            if (canReplace(level, star)) {
                level.setBlock(star, AVBlocks.FALLEN_STAR.get().defaultBlockState(), Block.UPDATE_ALL);
            }
            return star;
        }

        double ry = Math.max(1.0, radius * DEPTH_FACTOR);
        BlockPos center = surface.below(); // ellipsoid centre = top ground block -> bowl ~ry+1 deep
        int r = radius + 1;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, 0, r))) {
            double dx = (p.getX() - center.getX()) / (double) radius;
            double dy = (p.getY() - center.getY()) / ry;
            double dz = (p.getZ() - center.getZ()) / (double) radius;
            double d = dx * dx + dy * dy + dz * dz;
            if (d <= 1.0) {
                if (canReplace(level, p)) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            } else if (d <= 1.6 && p.getY() < center.getY()) {
                // scorched lining
                BlockState state = level.getBlockState(p);
                if (!state.isAir() && canReplace(level, p) && random.nextFloat() < 0.7f) {
                    level.setBlock(p, AVBlocks.METEORIC_STONE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }

        // Fallen Star at the bottom of the bowl, resting on solid lining
        int depth = (int) Math.floor(ry);
        BlockPos star = center.below(depth);
        while (star.getY() < center.getY() && !level.getBlockState(star.below()).isSolid()) {
            star = star.below();
            if (level.isOutsideBuildHeight(star.below())) {
                break;
            }
        }
        if (canReplace(level, star) || level.getBlockState(star).isAir()) {
            level.setBlock(star, AVBlocks.FALLEN_STAR.get().defaultBlockState(), Block.UPDATE_ALL);
        }
        BlockPos below = star.below();
        if (canReplace(level, below)) {
            level.setBlock(below, AVBlocks.METEORIC_STONE.get().defaultBlockState(), Block.UPDATE_ALL);
        }
        return star;
    }

    static boolean canReplace(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.hasBlockEntity() || !state.getFluidState().isEmpty()) {
            return false;
        }
        if (state.getDestroySpeed(level, pos) < 0) {
            return false;
        }
        return !state.is(AVTags.Blocks.STARFALL_IMMUNE);
    }
}
