package media.hofferbert.astralvigil.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import media.hofferbert.astralvigil.AstralVigil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Celestial building blocks: the Starwood set (a tree of the Firmament), Celestial Marble (the Watchers' masonry),
 * Sunstone and Moonstone (Solenne's and Vael's realms), Umbral Stone (the Eclipse) and starlit lights.
 *
 * <p>Everything here is described by an {@link Entry} list so datagen (models, loot, tags, lang, recipes) can walk it.
 * Textures come from {@code tools/dev/gen_building.py}.
 */
public final class AVBuilding {
    public enum Shape { CUBE, PILLAR, LOG, STAIRS, SLAB, WALL, FENCE, FENCE_GATE, DOOR, TRAPDOOR, BUTTON, PLATE, LEAVES, SAPLING, LANTERN, GLASS }

    public enum Material { WOOD, STONE, GLASS, LIGHT, PLANT }

    /**
     * One building block. {@code base} is the block whose texture a shaped variant (stairs, slab, wall, fence…) uses;
     * for {@link Shape#LOG} wood blocks it is the log whose side texture covers all six faces.
     */
    public record Entry(String id, Shape shape, Material material, DeferredBlock<? extends Block> block, DeferredItem<BlockItem> item,
            String base, String en, String de) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final java.util.Map<DeferredBlock<?>, UnaryOperator<BlockBehaviour.Properties>> PROPS = new java.util.HashMap<>();
    public static final List<Entry> ALL = Collections.unmodifiableList(ENTRIES);

    public static final ResourceKey<ConfiguredFeature<?, ?>> STARWOOD_TREE =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, AstralVigil.id("starwood_tree"));
    static final TreeGrower STARWOOD_GROWER = new TreeGrower(AstralVigil.MOD_ID + ":starwood", Optional.empty(), Optional.of(STARWOOD_TREE), Optional.empty());

    // ------------------------------------------------------------------ Starwood
    public static final DeferredBlock<Starlog> STRIPPED_STARWOOD_LOG = add("stripped_starwood_log", Shape.LOG, Material.WOOD, null,
            "Stripped Starwood Log", "Entrindeter Sternholzstamm", p -> new Starlog(p, null), p -> wood(p, MapColor.TERRACOTTA_WHITE));
    public static final DeferredBlock<Starlog> STARWOOD_LOG = add("starwood_log", Shape.LOG, Material.WOOD, null,
            "Starwood Log", "Sternholzstamm", p -> new Starlog(p, () -> STRIPPED_STARWOOD_LOG.get()), p -> wood(p, MapColor.COLOR_BLUE));
    public static final DeferredBlock<Starlog> STRIPPED_STARWOOD = add("stripped_starwood", Shape.LOG, Material.WOOD, "stripped_starwood_log",
            "Stripped Starwood", "Entrindetes Sternholz", p -> new Starlog(p, null), p -> wood(p, MapColor.TERRACOTTA_WHITE));
    public static final DeferredBlock<Starlog> STARWOOD = add("starwood", Shape.LOG, Material.WOOD, "starwood_log",
            "Starwood", "Sternholz", p -> new Starlog(p, () -> STRIPPED_STARWOOD.get()), p -> wood(p, MapColor.COLOR_BLUE));
    public static final DeferredBlock<Block> STARWOOD_PLANKS = add("starwood_planks", Shape.CUBE, Material.WOOD, null,
            "Starwood Planks", "Sternholzbretter", Block::new, p -> wood(p, MapColor.COLOR_BLUE));
    public static final DeferredBlock<StairBlock> STARWOOD_STAIRS = stairs("starwood_stairs", STARWOOD_PLANKS, Material.WOOD, "Starwood Stairs", "Sternholztreppe");
    public static final DeferredBlock<SlabBlock> STARWOOD_SLAB = slab("starwood_slab", STARWOOD_PLANKS, Material.WOOD, "Starwood Slab", "Sternholzstufe");
    public static final DeferredBlock<FenceBlock> STARWOOD_FENCE = add("starwood_fence", Shape.FENCE, Material.WOOD, "starwood_planks",
            "Starwood Fence", "Sternholzzaun", FenceBlock::new, p -> wood(p, MapColor.COLOR_BLUE));
    public static final DeferredBlock<FenceGateBlock> STARWOOD_FENCE_GATE = add("starwood_fence_gate", Shape.FENCE_GATE, Material.WOOD, "starwood_planks",
            "Starwood Fence Gate", "Sternholzzauntor", p -> new FenceGateBlock(WoodType.CHERRY, p), p -> wood(p, MapColor.COLOR_BLUE));
    public static final DeferredBlock<DoorBlock> STARWOOD_DOOR = add("starwood_door", Shape.DOOR, Material.WOOD, "starwood_planks",
            "Starwood Door", "Sternholztür", p -> new DoorBlock(BlockSetType.CHERRY, p), p -> wood(p, MapColor.COLOR_BLUE).noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<TrapDoorBlock> STARWOOD_TRAPDOOR = add("starwood_trapdoor", Shape.TRAPDOOR, Material.WOOD, "starwood_planks",
            "Starwood Trapdoor", "Sternholzfalltür", p -> new TrapDoorBlock(BlockSetType.CHERRY, p), p -> wood(p, MapColor.COLOR_BLUE).noOcclusion());
    public static final DeferredBlock<ButtonBlock> STARWOOD_BUTTON = add("starwood_button", Shape.BUTTON, Material.WOOD, "starwood_planks",
            "Starwood Button", "Sternholzknopf", p -> new ButtonBlock(BlockSetType.CHERRY, 30, p), p -> noCollision(p).strength(0.5f).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<PressurePlateBlock> STARWOOD_PRESSURE_PLATE = add("starwood_pressure_plate", Shape.PLATE, Material.WOOD, "starwood_planks",
            "Starwood Pressure Plate", "Sternholzdruckplatte", p -> new PressurePlateBlock(BlockSetType.CHERRY, p),
            p -> noCollision(wood(p, MapColor.COLOR_BLUE)).strength(0.5f).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> STARWOOD_LEAVES = add("starwood_leaves", Shape.LEAVES, Material.PLANT, null,
            "Starwood Leaves", "Sternholzlaub", AVBuilding::leaves, p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.2f).randomTicks()
                    .sound(SoundType.CHERRY_LEAVES).noOcclusion().lightLevel(s -> 6).isValidSpawn((s, l, pos, t) -> false)
                    .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<SaplingBlock> STARWOOD_SAPLING = add("starwood_sapling", Shape.SAPLING, Material.PLANT, null,
            "Starwood Sapling", "Sternholzsetzling", p -> new SaplingBlock(STARWOOD_GROWER, p), p -> noCollision(p).randomTicks().instabreak()
                    .sound(SoundType.CHERRY_SAPLING).lightLevel(s -> 4).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> GILDED_STARWOOD = add("gilded_starwood", Shape.CUBE, Material.WOOD, null,
            "Gilded Starwood", "Vergoldetes Sternholz", Block::new, p -> wood(p, MapColor.GOLD).lightLevel(s -> 3));

    // ------------------------------------------------------------------ Celestial Marble
    public static final DeferredBlock<Block> CELESTIAL_MARBLE = add("celestial_marble", Shape.CUBE, Material.STONE, null,
            "Celestial Marble", "Himmelsmarmor", Block::new, p -> stone(p, MapColor.SNOW, SoundType.CALCITE));
    public static final DeferredBlock<StairBlock> CELESTIAL_MARBLE_STAIRS = stairs("celestial_marble_stairs", CELESTIAL_MARBLE, Material.STONE,
            "Celestial Marble Stairs", "Himmelsmarmortreppe");
    public static final DeferredBlock<SlabBlock> CELESTIAL_MARBLE_SLAB = slab("celestial_marble_slab", CELESTIAL_MARBLE, Material.STONE,
            "Celestial Marble Slab", "Himmelsmarmorstufe");
    public static final DeferredBlock<WallBlock> CELESTIAL_MARBLE_WALL = wall("celestial_marble_wall", CELESTIAL_MARBLE, "Celestial Marble Wall", "Himmelsmarmormauer");
    public static final DeferredBlock<Block> CELESTIAL_BRICKS = add("celestial_bricks", Shape.CUBE, Material.STONE, null,
            "Celestial Bricks", "Himmelsziegel", Block::new, p -> stone(p, MapColor.SNOW, SoundType.CALCITE));
    public static final DeferredBlock<StairBlock> CELESTIAL_BRICK_STAIRS = stairs("celestial_brick_stairs", CELESTIAL_BRICKS, Material.STONE,
            "Celestial Brick Stairs", "Himmelsziegeltreppe");
    public static final DeferredBlock<SlabBlock> CELESTIAL_BRICK_SLAB = slab("celestial_brick_slab", CELESTIAL_BRICKS, Material.STONE,
            "Celestial Brick Slab", "Himmelsziegelstufe");
    public static final DeferredBlock<WallBlock> CELESTIAL_BRICK_WALL = wall("celestial_brick_wall", CELESTIAL_BRICKS, "Celestial Brick Wall", "Himmelsziegelmauer");
    public static final DeferredBlock<Block> CHISELED_CELESTIAL_MARBLE = add("chiseled_celestial_marble", Shape.CUBE, Material.STONE, null,
            "Chiseled Celestial Marble", "Gemeißelter Himmelsmarmor", Block::new, p -> stone(p, MapColor.SNOW, SoundType.CALCITE).lightLevel(s -> 4));
    public static final DeferredBlock<RotatedPillarBlock> CELESTIAL_PILLAR = add("celestial_pillar", Shape.PILLAR, Material.STONE, null,
            "Celestial Pillar", "Himmelssäule", RotatedPillarBlock::new, p -> stone(p, MapColor.SNOW, SoundType.CALCITE));
    public static final DeferredBlock<Block> GILDED_CELESTIAL_BRICKS = add("gilded_celestial_bricks", Shape.CUBE, Material.STONE, null,
            "Gilded Celestial Bricks", "Vergoldete Himmelsziegel", Block::new, p -> stone(p, MapColor.GOLD, SoundType.CALCITE).lightLevel(s -> 2));

    // ------------------------------------------------------------------ Sunstone
    public static final DeferredBlock<Block> SUNSTONE = add("sunstone", Shape.CUBE, Material.STONE, null,
            "Sunstone", "Sonnenstein", Block::new, p -> stone(p, MapColor.COLOR_ORANGE, SoundType.STONE));
    public static final DeferredBlock<Block> SUNSTONE_BRICKS = add("sunstone_bricks", Shape.CUBE, Material.STONE, null,
            "Sunstone Bricks", "Sonnensteinziegel", Block::new, p -> stone(p, MapColor.COLOR_ORANGE, SoundType.STONE));
    public static final DeferredBlock<StairBlock> SUNSTONE_BRICK_STAIRS = stairs("sunstone_brick_stairs", SUNSTONE_BRICKS, Material.STONE,
            "Sunstone Brick Stairs", "Sonnensteinziegeltreppe");
    public static final DeferredBlock<SlabBlock> SUNSTONE_BRICK_SLAB = slab("sunstone_brick_slab", SUNSTONE_BRICKS, Material.STONE,
            "Sunstone Brick Slab", "Sonnensteinziegelstufe");
    public static final DeferredBlock<WallBlock> SUNSTONE_BRICK_WALL = wall("sunstone_brick_wall", SUNSTONE_BRICKS, "Sunstone Brick Wall", "Sonnensteinziegelmauer");

    // ------------------------------------------------------------------ Moonstone
    public static final DeferredBlock<Block> MOONSTONE = add("moonstone", Shape.CUBE, Material.STONE, null,
            "Moonstone", "Mondstein", Block::new, p -> stone(p, MapColor.COLOR_LIGHT_GRAY, SoundType.STONE));
    public static final DeferredBlock<Block> MOONSTONE_BRICKS = add("moonstone_bricks", Shape.CUBE, Material.STONE, null,
            "Moonstone Bricks", "Mondsteinziegel", Block::new, p -> stone(p, MapColor.COLOR_LIGHT_GRAY, SoundType.STONE));
    public static final DeferredBlock<StairBlock> MOONSTONE_BRICK_STAIRS = stairs("moonstone_brick_stairs", MOONSTONE_BRICKS, Material.STONE,
            "Moonstone Brick Stairs", "Mondsteinziegeltreppe");
    public static final DeferredBlock<SlabBlock> MOONSTONE_BRICK_SLAB = slab("moonstone_brick_slab", MOONSTONE_BRICKS, Material.STONE,
            "Moonstone Brick Slab", "Mondsteinziegelstufe");
    public static final DeferredBlock<WallBlock> MOONSTONE_BRICK_WALL = wall("moonstone_brick_wall", MOONSTONE_BRICKS, "Moonstone Brick Wall", "Mondsteinziegelmauer");

    // ------------------------------------------------------------------ Umbral Stone
    public static final DeferredBlock<Block> UMBRAL_STONE = add("umbral_stone", Shape.CUBE, Material.STONE, null,
            "Umbral Stone", "Umbralstein", Block::new, p -> stone(p, MapColor.COLOR_PURPLE, SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> UMBRAL_BRICKS = add("umbral_bricks", Shape.CUBE, Material.STONE, null,
            "Umbral Bricks", "Umbralziegel", Block::new, p -> stone(p, MapColor.COLOR_PURPLE, SoundType.DEEPSLATE_BRICKS));
    public static final DeferredBlock<StairBlock> UMBRAL_BRICK_STAIRS = stairs("umbral_brick_stairs", UMBRAL_BRICKS, Material.STONE,
            "Umbral Brick Stairs", "Umbralziegeltreppe");
    public static final DeferredBlock<SlabBlock> UMBRAL_BRICK_SLAB = slab("umbral_brick_slab", UMBRAL_BRICKS, Material.STONE,
            "Umbral Brick Slab", "Umbralziegelstufe");
    public static final DeferredBlock<WallBlock> UMBRAL_BRICK_WALL = wall("umbral_brick_wall", UMBRAL_BRICKS, "Umbral Brick Wall", "Umbralziegelmauer");
    public static final DeferredBlock<Block> CHISELED_UMBRAL_BRICKS = add("chiseled_umbral_bricks", Shape.CUBE, Material.STONE, null,
            "Chiseled Umbral Bricks", "Gemeißelte Umbralziegel", Block::new, p -> stone(p, MapColor.COLOR_PURPLE, SoundType.DEEPSLATE_BRICKS).lightLevel(s -> 5));

    // ------------------------------------------------------------------ lights and glass
    public static final DeferredBlock<Block> ASTRAL_LAMP = add("astral_lamp", Shape.CUBE, Material.LIGHT, null,
            "Astral Lamp", "Astrallampe", Block::new, p -> light(p, MapColor.DIAMOND));
    public static final DeferredBlock<Block> SUNSTONE_LAMP = add("sunstone_lamp", Shape.CUBE, Material.LIGHT, null,
            "Sunstone Lamp", "Sonnensteinlampe", Block::new, p -> light(p, MapColor.GOLD));
    public static final DeferredBlock<Block> MOONSTONE_LAMP = add("moonstone_lamp", Shape.CUBE, Material.LIGHT, null,
            "Moonstone Lamp", "Mondsteinlampe", Block::new, p -> light(p, MapColor.SNOW));
    public static final DeferredBlock<Block> UMBRAL_LAMP = add("umbral_lamp", Shape.CUBE, Material.LIGHT, null,
            "Umbral Lamp", "Umbrallampe", Block::new, p -> light(p, MapColor.COLOR_PURPLE).lightLevel(s -> 11));
    public static final DeferredBlock<LanternBlock> STAR_LANTERN = add("star_lantern", Shape.LANTERN, Material.LIGHT, null,
            "Star Lantern", "Sternlaterne", LanternBlock::new, p -> p.mapColor(MapColor.GOLD).strength(3.5f).sound(SoundType.LANTERN)
                    .lightLevel(s -> 15).noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> CONSTELLATION_TILES = add("constellation_tiles", Shape.CUBE, Material.STONE, null,
            "Constellation Tiles", "Sternbildfliesen", Block::new, p -> stone(p, MapColor.COLOR_BLUE, SoundType.DEEPSLATE_TILES).lightLevel(s -> 3));
    public static final DeferredBlock<TransparentBlock> HALO_GLASS = add("halo_glass", Shape.GLASS, Material.GLASS, null,
            "Halo Glass", "Heiligenscheinglas", TransparentBlock::new, AVBuilding::glass);
    public static final DeferredBlock<TransparentBlock> NEBULA_GLASS = add("nebula_glass", Shape.GLASS, Material.GLASS, null,
            "Nebula Glass", "Nebelglas", TransparentBlock::new, p -> glass(p).lightLevel(s -> 2));

    public static final net.minecraft.tags.TagKey<Block> STARWOOD_LOGS_BLOCKS =
            net.minecraft.tags.TagKey.create(Registries.BLOCK, AstralVigil.id("starwood_logs"));
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> STARWOOD_LOGS =
            net.minecraft.tags.TagKey.create(Registries.ITEM, AstralVigil.id("starwood_logs"));

    private AVBuilding() {}

    /** Vanilla/convention block tags an entry belongs to (mining tool + shape family). */
    public static List<net.minecraft.tags.TagKey<Block>> blockTags(Entry e) {
        List<net.minecraft.tags.TagKey<Block>> t = new ArrayList<>();
        switch (e.material()) {
            case WOOD -> t.add(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE);
            case PLANT -> t.add(net.minecraft.tags.BlockTags.MINEABLE_WITH_HOE);
            case STONE, LIGHT -> t.add(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE);
            case GLASS -> {
                t.add(net.neoforged.neoforge.common.Tags.Blocks.GLASS_BLOCKS);
                t.add(net.minecraft.tags.BlockTags.IMPERMEABLE);
            }
        }
        boolean wood = e.material() == Material.WOOD;
        switch (e.shape()) {
            case LOG -> {
                t.add(net.minecraft.tags.BlockTags.LOGS);
                t.add(STARWOOD_LOGS_BLOCKS);
            }
            case STAIRS -> t.add(wood ? net.minecraft.tags.BlockTags.WOODEN_STAIRS : net.minecraft.tags.BlockTags.STAIRS);
            case SLAB -> t.add(wood ? net.minecraft.tags.BlockTags.WOODEN_SLABS : net.minecraft.tags.BlockTags.SLABS);
            case WALL -> t.add(net.minecraft.tags.BlockTags.WALLS);
            case FENCE -> t.add(net.minecraft.tags.BlockTags.WOODEN_FENCES);
            case FENCE_GATE -> t.add(net.minecraft.tags.BlockTags.FENCE_GATES);
            case DOOR -> t.add(net.minecraft.tags.BlockTags.WOODEN_DOORS);
            case TRAPDOOR -> t.add(net.minecraft.tags.BlockTags.WOODEN_TRAPDOORS);
            case BUTTON -> t.add(net.minecraft.tags.BlockTags.WOODEN_BUTTONS);
            case PLATE -> t.add(net.minecraft.tags.BlockTags.WOODEN_PRESSURE_PLATES);
            case LEAVES -> t.add(net.minecraft.tags.BlockTags.LEAVES);
            case SAPLING -> t.add(net.minecraft.tags.BlockTags.SAPLINGS);
            default -> {
                if (e.id().equals("starwood_planks")) {
                    t.add(net.minecraft.tags.BlockTags.PLANKS);
                }
            }
        }
        return t;
    }

    /** Item tags of an entry's block item. */
    public static List<net.minecraft.tags.TagKey<net.minecraft.world.item.Item>> itemTags(Entry e) {
        List<net.minecraft.tags.TagKey<net.minecraft.world.item.Item>> t = new ArrayList<>();
        boolean wood = e.material() == Material.WOOD;
        switch (e.shape()) {
            case LOG -> {
                t.add(net.minecraft.tags.ItemTags.LOGS);
                t.add(STARWOOD_LOGS);
            }
            case STAIRS -> t.add(wood ? net.minecraft.tags.ItemTags.WOODEN_STAIRS : net.minecraft.tags.ItemTags.STAIRS);
            case SLAB -> t.add(wood ? net.minecraft.tags.ItemTags.WOODEN_SLABS : net.minecraft.tags.ItemTags.SLABS);
            case WALL -> t.add(net.minecraft.tags.ItemTags.WALLS);
            case FENCE -> t.add(net.minecraft.tags.ItemTags.WOODEN_FENCES);
            case FENCE_GATE -> t.add(net.minecraft.tags.ItemTags.FENCE_GATES);
            case DOOR -> t.add(net.minecraft.tags.ItemTags.WOODEN_DOORS);
            case TRAPDOOR -> t.add(net.minecraft.tags.ItemTags.WOODEN_TRAPDOORS);
            case BUTTON -> t.add(net.minecraft.tags.ItemTags.WOODEN_BUTTONS);
            case PLATE -> t.add(net.minecraft.tags.ItemTags.WOODEN_PRESSURE_PLATES);
            case LEAVES -> t.add(net.minecraft.tags.ItemTags.LEAVES);
            case SAPLING -> t.add(net.minecraft.tags.ItemTags.SAPLINGS);
            case GLASS -> t.add(net.neoforged.neoforge.common.Tags.Items.GLASS_BLOCKS);
            default -> {
                if (e.id().equals("starwood_planks")) {
                    t.add(net.minecraft.tags.ItemTags.PLANKS);
                }
            }
        }
        return t;
    }

    /** Forces class loading (so every block above is queued in {@link AVBlocks#BLOCKS} before registration). */
    public static void init() {}

    public static Entry entry(Block block) {
        for (Entry e : ENTRIES) {
            if (e.block().get() == block) {
                return e;
            }
        }
        throw new IllegalArgumentException(String.valueOf(block));
    }

    // ------------------------------------------------------------------ helpers

    private static <B extends Block> DeferredBlock<B> add(String id, Shape shape, Material material, String base, String en, String de,
            Function<BlockBehaviour.Properties, B> factory, UnaryOperator<BlockBehaviour.Properties> props) {
        DeferredBlock<B> block = AVBlocks.register(id, factory, props);
        DeferredItem<BlockItem> item = AVItems.ITEMS.registerSimpleBlockItem(block);
        ENTRIES.add(new Entry(id, shape, material, block, item, base, en, de));
        PROPS.put(block, props);
        return block;
    }

    private static DeferredBlock<StairBlock> stairs(String id, DeferredBlock<Block> base, Material material, String en, String de) {
        return add(id, Shape.STAIRS, material, base.getId().getPath(), en, de, p -> new StairBlock(base.get().defaultBlockState(), p),
                p -> copy(p, base));
    }

    private static DeferredBlock<SlabBlock> slab(String id, DeferredBlock<Block> base, Material material, String en, String de) {
        return add(id, Shape.SLAB, material, base.getId().getPath(), en, de, SlabBlock::new, p -> copy(p, base));
    }

    private static DeferredBlock<WallBlock> wall(String id, DeferredBlock<Block> base, String en, String de) {
        return add(id, Shape.WALL, Material.STONE, base.getId().getPath(), en, de, WallBlock::new, p -> copy(p, base).forceSolidOn());
    }

    /** Copies the base block's look and feel (map colour, sound, strength) – the base is always registered earlier. */
    private static BlockBehaviour.Properties copy(BlockBehaviour.Properties p, DeferredBlock<Block> base) {
        return PROPS.get(base).apply(p);
    }

    private static BlockBehaviour.Properties wood(BlockBehaviour.Properties p, MapColor color) {
        return p.mapColor(color).instrument(NoteBlockInstrument.BASS).strength(2.0f, 3.0f).sound(SoundType.CHERRY_WOOD);
    }

    private static BlockBehaviour.Properties stone(BlockBehaviour.Properties p, MapColor color, SoundType sound) {
        return p.mapColor(color).instrument(NoteBlockInstrument.BASEDRUM).strength(1.8f, 6.0f).requiresCorrectToolForDrops().sound(sound);
    }

    private static BlockBehaviour.Properties light(BlockBehaviour.Properties p, MapColor color) {
        return p.mapColor(color).strength(1.5f, 6.0f).sound(SoundType.AMETHYST).lightLevel(s -> 15);
    }

    private static BlockBehaviour.Properties glass(BlockBehaviour.Properties p) {
        return p.mapColor(MapColor.NONE).strength(0.4f).sound(SoundType.GLASS).noOcclusion().isValidSpawn((s, l, pos, t) -> false)
                .isRedstoneConductor((s, l, pos) -> false).isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false);
    }

    private static BlockBehaviour.Properties noCollision(BlockBehaviour.Properties p) {
        //? if >=26.1 {
        return p.noCollision();
        //?} else {
        /*return p.noCollission();
        *///?}
    }

    private static Block leaves(BlockBehaviour.Properties p) {
        //? if >=26.1 {
        return new net.minecraft.world.level.block.UntintedParticleLeavesBlock(0.01f, net.minecraft.core.particles.ParticleTypes.END_ROD, p);
        //?} else {
        /*return new net.minecraft.world.level.block.LeavesBlock(p);
        *///?}
    }

    /** Starwood logs and wood: strippable with an axe (the bark peels off a pale, star-grained core). */
    public static class Starlog extends RotatedPillarBlock {
        private final Supplier<? extends Block> stripped;

        public Starlog(BlockBehaviour.Properties properties, Supplier<? extends Block> stripped) {
            super(properties);
            this.stripped = stripped;
        }

        @Override
        public BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility ability, boolean simulate) {
            if (stripped != null && ability == ItemAbilities.AXE_STRIP) {
                return stripped.get().defaultBlockState().setValue(AXIS, state.getValue(AXIS));
            }
            return super.getToolModifiedState(state, context, ability, simulate);
        }
    }
}
