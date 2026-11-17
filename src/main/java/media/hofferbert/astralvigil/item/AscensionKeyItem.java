package media.hofferbert.astralvigil.item;

import java.util.List;
import media.hofferbert.astralvigil.block.CelestialPortalBlock;
import media.hofferbert.astralvigil.firmament.GateShape;
import media.hofferbert.astralvigil.registry.AVBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ascension Key: set into an Astral Gate frame it lights the gate's opening with a Celestial Portal. The key itself no
 * longer carries anyone - the gate has to be built first.
 */
public class AscensionKeyItem extends Item {
    public AscensionKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (!level.getBlockState(pos).is(AVBlocks.ASTRAL_GATE.get())) {
            if (player != null && !level.isClientSide()) {
                player.sendSystemMessage(Component.translatable("message.astralvigil.gate.hint").withStyle(ChatFormatting.AQUA));
            }
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        int lit = ignite(level, pos);
        if (lit == 0) {
            if (player != null) {
                player.sendSystemMessage(Component.translatable("message.astralvigil.gate.incomplete").withStyle(ChatFormatting.GRAY));
            }
            return InteractionResult.FAIL;
        }
        level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.8f, 1.4f);
        if (player != null) {
            player.sendSystemMessage(Component.translatable("message.astralvigil.gate.lit").withStyle(ChatFormatting.GOLD));
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Lights the gate whose frame contains {@code frame}: tries the X plane, then the Z plane.
     *
     * @return number of portal blocks placed (0 = no complete gate)
     */
    public static int ignite(Level level, BlockPos frame) {
        for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
            boolean x = axis == Direction.Axis.X;
            int u0 = x ? frame.getX() : frame.getZ();
            List<int[]> cells = GateShape.opening(u0, frame.getY(),
                    (u, y) -> level.getBlockState(at(frame, x, u, y)).is(AVBlocks.ASTRAL_GATE.get()),
                    (u, y) -> {
                        BlockState s = level.getBlockState(at(frame, x, u, y));
                        return s.isAir() || s.is(AVBlocks.CELESTIAL_PORTAL.get()) || s.canBeReplaced();
                    });
            if (cells.isEmpty()) {
                continue;
            }
            BlockState portal = AVBlocks.CELESTIAL_PORTAL.get().defaultBlockState().setValue(CelestialPortalBlock.AXIS, axis);
            for (int[] c : cells) {
                BlockPos p = at(frame, x, c[0], c[1]);
                level.setBlock(p, portal, Block.UPDATE_CLIENTS);
                if (level instanceof ServerLevel server && level.getRandom().nextInt(3) == 0) {
                    server.sendParticles(ParticleTypes.END_ROD, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.02);
                }
            }
            return cells.size();
        }
        return 0;
    }

    private static BlockPos at(BlockPos frame, boolean xPlane, int u, int y) {
        return xPlane ? new BlockPos(u, y, frame.getZ()) : new BlockPos(frame.getX(), y, u);
    }
}
