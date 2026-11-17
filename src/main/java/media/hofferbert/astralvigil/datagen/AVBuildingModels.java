package media.hofferbert.astralvigil.datagen;

import java.util.List;
import media.hofferbert.astralvigil.registry.AVBuilding;
import media.hofferbert.astralvigil.registry.AVBuilding.Entry;
import media.hofferbert.astralvigil.registry.AVBuilding.Shape;
import net.minecraft.data.BlockFamily;
import net.minecraft.world.level.block.Block;
//? if >=26.1 {
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.TexturedModel;
//?} else {
/*import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
*///?}

/** Block families, models and item models of the celestial building blocks ({@link AVBuilding}). */
public final class AVBuildingModels {
    private AVBuildingModels() {}

    /** Block families (shared by model and recipe datagen). Built lazily: the blocks must be registered. */
    public static List<BlockFamily> families() {
        return List.of(
                new BlockFamily.Builder(AVBuilding.STARWOOD_PLANKS.get())
                        .stairs(AVBuilding.STARWOOD_STAIRS.get()).slab(AVBuilding.STARWOOD_SLAB.get())
                        .fence(AVBuilding.STARWOOD_FENCE.get()).fenceGate(AVBuilding.STARWOOD_FENCE_GATE.get())
                        .door(AVBuilding.STARWOOD_DOOR.get()).trapdoor(AVBuilding.STARWOOD_TRAPDOOR.get())
                        .button(AVBuilding.STARWOOD_BUTTON.get()).pressurePlate(AVBuilding.STARWOOD_PRESSURE_PLATE.get())
                        .recipeGroupPrefix("wooden").recipeUnlockedBy("has_planks").getFamily(),
                new BlockFamily.Builder(AVBuilding.CELESTIAL_MARBLE.get())
                        .stairs(AVBuilding.CELESTIAL_MARBLE_STAIRS.get()).slab(AVBuilding.CELESTIAL_MARBLE_SLAB.get())
                        .wall(AVBuilding.CELESTIAL_MARBLE_WALL.get()).chiseled(AVBuilding.CHISELED_CELESTIAL_MARBLE.get()).getFamily(),
                new BlockFamily.Builder(AVBuilding.CELESTIAL_BRICKS.get())
                        .stairs(AVBuilding.CELESTIAL_BRICK_STAIRS.get()).slab(AVBuilding.CELESTIAL_BRICK_SLAB.get())
                        .wall(AVBuilding.CELESTIAL_BRICK_WALL.get()).getFamily(),
                new BlockFamily.Builder(AVBuilding.SUNSTONE_BRICKS.get())
                        .stairs(AVBuilding.SUNSTONE_BRICK_STAIRS.get()).slab(AVBuilding.SUNSTONE_BRICK_SLAB.get())
                        .wall(AVBuilding.SUNSTONE_BRICK_WALL.get()).getFamily(),
                new BlockFamily.Builder(AVBuilding.MOONSTONE_BRICKS.get())
                        .stairs(AVBuilding.MOONSTONE_BRICK_STAIRS.get()).slab(AVBuilding.MOONSTONE_BRICK_SLAB.get())
                        .wall(AVBuilding.MOONSTONE_BRICK_WALL.get()).getFamily(),
                new BlockFamily.Builder(AVBuilding.UMBRAL_BRICKS.get())
                        .stairs(AVBuilding.UMBRAL_BRICK_STAIRS.get()).slab(AVBuilding.UMBRAL_BRICK_SLAB.get())
                        .wall(AVBuilding.UMBRAL_BRICK_WALL.get()).chiseled(AVBuilding.CHISELED_UMBRAL_BRICKS.get()).getFamily());
    }

    /** True if the block is generated as part of a family (base or variant). */
    static boolean inFamily(Block block) {
        for (BlockFamily f : families()) {
            if (f.getBaseBlock() == block || f.getVariants().containsValue(block)) {
                return true;
            }
        }
        return false;
    }

    //? if >=26.1 {
    public static void generate(BlockModelGenerators blocks, ItemModelGenerators items) {
        blocks.woodProvider(AVBuilding.STARWOOD_LOG.get()).logWithHorizontal(AVBuilding.STARWOOD_LOG.get()).wood(AVBuilding.STARWOOD.get());
        blocks.woodProvider(AVBuilding.STRIPPED_STARWOOD_LOG.get()).logWithHorizontal(AVBuilding.STRIPPED_STARWOOD_LOG.get())
                .wood(AVBuilding.STRIPPED_STARWOOD.get());
        families().forEach(f -> blocks.family(f.getBaseBlock()).generateFor(f));
        for (Entry e : AVBuilding.ALL) {
            Block b = e.block().get();
            if (e.shape() == Shape.LOG || inFamily(b)) {
                continue;
            }
            switch (e.shape()) {
                case LEAVES -> blocks.createTrivialBlock(b, TexturedModel.LEAVES);
                case SAPLING -> blocks.createCrossBlockWithDefaultItem(b, BlockModelGenerators.PlantType.NOT_TINTED);
                case LANTERN -> blocks.createLantern(b);
                case PILLAR -> blocks.createAxisAlignedPillarBlock(b, TexturedModel.COLUMN_ALT);
                default -> blocks.createTrivialCube(b);
            }
        }
    }
    //?} else {
    /*public static void blockStates(BlockStateProvider p) {
        for (Entry e : AVBuilding.ALL) {
            Block b = e.block().get();
            String id = e.id();
            ResourceLocation own = p.modLoc("block/" + id);
            ResourceLocation base = e.base() == null ? own : p.modLoc("block/" + e.base());
            var items = p.itemModels();
            switch (e.shape()) {
                case LOG -> {
                    if (e.base() == null) {
                        p.logBlock((RotatedPillarBlock) b);
                    } else {
                        p.axisBlock((RotatedPillarBlock) b, base, base);
                    }
                    items.withExistingParent(id, own);
                }
                case PILLAR -> {
                    p.axisBlock((RotatedPillarBlock) b, own, p.modLoc("block/" + id + "_top"));
                    items.withExistingParent(id, own);
                }
                case STAIRS -> {
                    p.stairsBlock((StairBlock) b, base);
                    items.withExistingParent(id, own);
                }
                case SLAB -> {
                    p.slabBlock((SlabBlock) b, base, base);
                    items.withExistingParent(id, own);
                }
                case WALL -> {
                    p.wallBlock((WallBlock) b, base);
                    items.wallInventory(id, base);
                }
                case FENCE -> {
                    p.fenceBlock((FenceBlock) b, base);
                    items.fenceInventory(id, base);
                }
                case FENCE_GATE -> {
                    p.fenceGateBlock((FenceGateBlock) b, base);
                    items.withExistingParent(id, own);
                }
                case DOOR -> {
                    p.doorBlockWithRenderType((DoorBlock) b, p.modLoc("block/" + id + "_bottom"), p.modLoc("block/" + id + "_top"), "cutout");
                    items.basicItem(b.asItem());
                }
                case TRAPDOOR -> {
                    p.trapdoorBlockWithRenderType((TrapDoorBlock) b, own, true, "cutout");
                    items.withExistingParent(id, p.modLoc("block/" + id + "_bottom"));
                }
                case BUTTON -> {
                    p.buttonBlock((ButtonBlock) b, base);
                    items.buttonInventory(id, base);
                }
                case PLATE -> {
                    p.pressurePlateBlock((PressurePlateBlock) b, base);
                    items.withExistingParent(id, own);
                }
                case LEAVES -> {
                    p.simpleBlock(b, p.models().leaves(id, own).renderType("cutout_mipped"));
                    items.withExistingParent(id, own);
                }
                case SAPLING -> {
                    p.simpleBlock(b, p.models().cross(id, own).renderType("cutout"));
                    items.withExistingParent(id, "item/generated").texture("layer0", own);
                }
                case LANTERN -> {
                    ModelFile ground = p.models().withExistingParent(id, p.mcLoc("block/template_lantern")).texture("lantern", own).renderType("cutout");
                    ModelFile hanging = p.models().withExistingParent(id + "_hanging", p.mcLoc("block/template_hanging_lantern"))
                            .texture("lantern", own).renderType("cutout");
                    p.getVariantBuilder(b).forAllStates(s -> ConfiguredModel.builder()
                            .modelFile(s.getValue(LanternBlock.HANGING) ? hanging : ground).build());
                    items.basicItem(b.asItem());
                }
                case GLASS -> {
                    p.simpleBlock(b, p.models().cubeAll(id, own).renderType("translucent"));
                    items.withExistingParent(id, own);
                }
                default -> {
                    p.simpleBlock(b);
                    items.withExistingParent(id, own);
                }
            }
        }
    }
    *///?}
}
