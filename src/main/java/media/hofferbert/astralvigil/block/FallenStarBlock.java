package media.hofferbert.astralvigil.block;

import media.hofferbert.astralvigil.config.AVClientConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** The glowing core left by a starfall. Purely client-side sparkle; no ticking. */
public class FallenStarBlock extends Block {
    public FallenStarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() > 0.35f * AVClientConfig.particleFactor()) {
            return;
        }
        double x = pos.getX() + random.nextDouble();
        double y = pos.getY() + 1.05;
        double z = pos.getZ() + random.nextDouble();
        level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.02 + random.nextDouble() * 0.03, 0.0);
    }
}
