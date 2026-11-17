package media.hofferbert.astralvigil.command;

import java.util.List;
import media.hofferbert.astralvigil.crafting.AstralForgeBlockEntity;
import media.hofferbert.astralvigil.crafting.ConvergenceAltarBlockEntity;
import media.hofferbert.astralvigil.crafting.PedestalBlockEntity;
import media.hofferbert.astralvigil.registry.AVBlocks;
import media.hofferbert.astralvigil.registry.AVItems;
import media.hofferbert.astralvigil.starlight.Tint;
import media.hofferbert.astralvigil.starlight.network.StarlightBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * {@code /astralvigil showcase <scene>}: builds a small, ready-to-film scene east of the player (+X) for screenshots,
 * trailers and manual testing. Op-only like every debug command; never used by gameplay.
 */
final class Showcase {
    static final List<String> SCENES = List.of("network", "ritual", "forge", "armory");

    private Showcase() {}

    /** Builds {@code scene} with its origin at {@code o}; returns false for an unknown scene. */
    static boolean build(ServerLevel level, BlockPos o, String scene) {
        switch (scene) {
            case "network" -> network(level, o);
            case "ritual" -> ritual(level, o);
            case "forge" -> forge(level, o);
            case "armory" -> armory(level, o);
            default -> {
                return false;
            }
        }
        return true;
    }

    private static void floor(ServerLevel level, BlockPos o, int x0, int z0, int x1, int z1, Block block) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                level.setBlockAndUpdate(o.offset(x, -1, z), block.defaultBlockState());
                for (int y = 0; y < 4; y++) {
                    level.setBlockAndUpdate(o.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T place(ServerLevel level, BlockPos pos, Block block, Direction facing) {
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        } else if (state.hasProperty(BlockStateProperties.FACING)) {
            state = state.setValue(BlockStateProperties.FACING, facing);
        }
        level.setBlockAndUpdate(pos, state);
        return (T) level.getBlockEntity(pos);
    }

    /** Collector -> lens -> prism splitting into three reservoirs. */
    private static void network(ServerLevel level, BlockPos o) {
        floor(level, o, -1, -5, 12, 5, Blocks.POLISHED_DEEPSLATE);
        StarlightBlockEntity collector = place(level, o, AVBlocks.STARLIGHT_COLLECTOR.get(), Direction.EAST);
        collector.buffer().insert(Tint.PURE, 4000, false);
        StarlightBlockEntity side = place(level, o.offset(0, 0, 3), AVBlocks.STARLIGHT_COLLECTOR.get(), Direction.EAST);
        side.buffer().insert(Tint.PURE, 4000, false);
        place(level, o.offset(3, 0, 0), AVBlocks.LENS.get(), Direction.EAST);
        place(level, o.offset(7, 0, 0), AVBlocks.PRISM.get(), Direction.EAST);
        place(level, o.offset(11, 0, 0), AVBlocks.STARLIGHT_RESERVOIR.get(), Direction.NORTH);
        place(level, o.offset(7, 0, -4), AVBlocks.STARLIGHT_RESERVOIR.get(), Direction.NORTH);
        place(level, o.offset(7, 0, 4), AVBlocks.STARLIGHT_RESERVOIR.get(), Direction.NORTH);
        place(level, o.offset(11, 0, 3), AVBlocks.STARLIGHT_RESERVOIR.get(), Direction.NORTH);
        place(level, o.offset(5, 0, 3), AVBlocks.MIRROR.get(), Direction.EAST);
    }

    /** Altar with four pedestals and amethyst for stability, ritual already running. */
    private static void ritual(ServerLevel level, BlockPos o) {
        BlockPos c = o.offset(4, 0, 0);
        floor(level, c, -4, -4, 4, 4, Blocks.CALCITE);
        for (int[] d : new int[][] {{-3, -3}, {3, 3}, {-3, 3}, {3, -3}, {-1, -1}, {1, 1}, {-1, 1}, {1, -1}}) {
            level.setBlockAndUpdate(c.offset(d[0], -1, d[1]), Blocks.AMETHYST_BLOCK.defaultBlockState());
        }
        ConvergenceAltarBlockEntity altar = place(level, c, AVBlocks.CONVERGENCE_ALTAR.get(), Direction.NORTH);
        int[][] spots = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};
        ItemStack[] offerings = {new ItemStack(Items.GOLD_NUGGET), new ItemStack(Items.GOLD_NUGGET),
                new ItemStack(Items.AMETHYST_SHARD), new ItemStack(Items.AMETHYST_SHARD)};
        for (int i = 0; i < spots.length; i++) {
            PedestalBlockEntity p = place(level, c.offset(spots[i][0], 0, spots[i][1]), AVBlocks.PEDESTAL.get(), Direction.NORTH);
            p.setItem(offerings[i]);
        }
        altar.setCatalyst(new ItemStack(AVItems.STARMETAL_INGOT.get()));
        altar.buffer().insert(Tint.STELLA, 4000, false);
        altar.tryStart(level);
    }

    /** Astral Forge smelting Sunsteel, fed by a reservoir. */
    private static void forge(ServerLevel level, BlockPos o) {
        BlockPos c = o.offset(3, 0, 0);
        floor(level, c, -2, -2, 2, 2, Blocks.POLISHED_BLACKSTONE_BRICKS);
        AstralForgeBlockEntity forge = place(level, c, AVBlocks.ASTRAL_FORGE.get(), Direction.WEST);
        forge.insert(new ItemStack(Items.GOLD_INGOT));
        forge.insert(new ItemStack(AVItems.ASTRAL_SHARD.get()));
        forge.insert(new ItemStack(AVItems.ASTRAL_SHARD.get()));
        forge.buffer().insert(Tint.SOL, 2000, false);
    }

    /** Armour stands wearing the three celestial sets with the living weapons. */
    private static void armory(ServerLevel level, BlockPos o) {
        floor(level, o, 1, -3, 4, 3, Blocks.SMOOTH_QUARTZ);
        ItemStack[][] hands = {
            {new ItemStack(AVItems.DAWNBREAKER.get()), new ItemStack(AVItems.STARLIGHT_AEGIS.get())},
            {new ItemStack(AVItems.HALO_EDGE.get()), ItemStack.EMPTY},
            {new ItemStack(AVItems.CRESCENT.get()), new ItemStack(AVItems.TIDECALLER.get())}
        };
        for (int i = 0; i < 3; i++) {
            //? if >=26.1 {
            ArmorStand stand = EntityType.ARMOR_STAND.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            //?} else {
            /*ArmorStand stand = EntityType.ARMOR_STAND.create(level);
            *///?}
            if (stand == null) {
                return;
            }
            stand.setPos(o.getX() + 3.5, o.getY(), o.getZ() + 0.5 + (i - 1) * 2);
            stand.setYRot(90f);
            stand.setYBodyRot(90f);
            stand.setYHeadRot(90f);
            // left: Solar Regalia, centre: Seraph Plate, right: Lunar Raiment
            var set = new media.hofferbert.astralvigil.armor.ArmorSet[] {
                media.hofferbert.astralvigil.armor.ArmorSet.SOLAR, media.hofferbert.astralvigil.armor.ArmorSet.SERAPH,
                media.hofferbert.astralvigil.armor.ArmorSet.LUNAR}[i];
            for (EquipmentSlot slot : media.hofferbert.astralvigil.armor.ArmorSets.SLOTS) {
                stand.setItemSlot(slot, new ItemStack(media.hofferbert.astralvigil.armor.ArmorSets.piece(set, slot)));
            }
            stand.setItemSlot(EquipmentSlot.MAINHAND, hands[i][0]);
            stand.setItemSlot(EquipmentSlot.OFFHAND, hands[i][1]);
            stand.setShowArms(true);
            level.addFreshEntity(stand);
        }
    }
}
