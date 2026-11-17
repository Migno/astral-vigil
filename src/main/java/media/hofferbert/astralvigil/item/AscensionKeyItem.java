package media.hofferbert.astralvigil.item;

import java.util.List;
import media.hofferbert.astralvigil.block.CelestialPortalBlock;
import media.hofferbert.astralvigil.firmament.GateKind;
import media.hofferbert.astralvigil.firmament.GateShape;
import media.hofferbert.astralvigil.firmament.Watcher;
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
 * Ascension Key: set into a gate frame it lights the opening with that gate's portal. Each gate kind has its own frame
 * block and leads to its own place (hub, one of the four trial islands, the Umbral Throne). The key itself carries no
 * one - the gates have to be built first; the Umbral Gate only opens for someone carrying all four Marks.
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
        GateKind kind = frameKind(level.getBlockState(pos));
        if (kind == null) {
            if (player != null && !level.isClientSide()) {
                player.sendSystemMessage(Component.translatable("message.astralvigil.gate.hint").withStyle(ChatFormatting.AQUA));
            }
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (kind == GateKind.UMBRAL && player != null && !carriesAllMarks(player)) {
            player.sendSystemMessage(Component.translatable("message.astralvigil.gate.umbral_marks").withStyle(ChatFormatting.DARK_PURPLE));
            return InteractionResult.FAIL;
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
            player.sendSystemMessage(Component.translatable("message.astralvigil.gate.lit." + kind.id()).withStyle(ChatFormatting.GOLD));
        }
        return InteractionResult.SUCCESS;
    }

    /** The gate kind whose frame block this is, or null. */
    public static GateKind frameKind(BlockState state) {
        for (var e : AVBlocks.GATES.entrySet()) {
            if (state.is(e.getValue().get())) {
                return e.getKey();
            }
        }
        return null;
    }

    static boolean carriesAllMarks(Player player) {
        for (Watcher w : Watcher.values()) {
            if (!player.getInventory().contains(new net.minecraft.world.item.ItemStack(media.hofferbert.astralvigil.registry.AVItems.MARKS.get(w).get()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Lights the gate whose frame contains {@code frame}: tries the X plane, then the Z plane. The frame must be made
     * entirely of one gate kind; the portal takes that kind.
     *
     * @return number of portal blocks placed (0 = no complete gate)
     */
    public static int ignite(Level level, BlockPos frame) {
        GateKind kind = frameKind(level.getBlockState(frame));
        if (kind == null) {
            return 0;
        }
        Block frameBlock = AVBlocks.GATES.get(kind).get();
        Block portalBlock = AVBlocks.PORTALS.get(kind).get();
        for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
            boolean x = axis == Direction.Axis.X;
            int u0 = x ? frame.getX() : frame.getZ();
            List<int[]> cells = GateShape.opening(u0, frame.getY(),
                    (u, y) -> level.getBlockState(at(frame, x, u, y)).is(frameBlock),
                    (u, y) -> {
                        BlockState s = level.getBlockState(at(frame, x, u, y));
                        return s.isAir() || s.is(portalBlock) || s.canBeReplaced();
                    });
            if (cells.isEmpty()) {
                continue;
            }
            BlockState portal = portalBlock.defaultBlockState().setValue(CelestialPortalBlock.AXIS, axis);
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
