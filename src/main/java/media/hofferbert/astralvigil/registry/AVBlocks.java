package media.hofferbert.astralvigil.registry;

import java.util.function.Function;
import java.util.function.UnaryOperator;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.block.FallenStarBlock;
import media.hofferbert.astralvigil.block.RitualBlock;
import media.hofferbert.astralvigil.block.StarlightDeviceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AVBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AstralVigil.MOD_ID);

    /** Core of a starfall crater. Glows, drops Astral Shards. */
    public static final DeferredBlock<FallenStarBlock> FALLEN_STAR = register("fallen_star", FallenStarBlock::new, p -> p
            .mapColor(MapColor.COLOR_CYAN)
            .strength(4.0f, 9.0f)
            .requiresCorrectToolForDrops()
            .lightLevel(state -> 12)
            .sound(SoundType.AMETHYST));

    /** Scorched stone lining starfall craters. */
    public static final DeferredBlock<Block> METEORIC_STONE = register("meteoric_stone", Block::new, p -> p
            .mapColor(MapColor.COLOR_BLACK)
            .strength(2.0f, 6.0f)
            .requiresCorrectToolForDrops()
            .sound(SoundType.DEEPSLATE));

    // Starlight network devices (models from Blockbench, see art/bbmodel/block)
    public static final DeferredBlock<StarlightDeviceBlock> STARLIGHT_COLLECTOR = register("starlight_collector", StarlightDeviceBlock::collector, p -> device(p).lightLevel(s -> 7));
    public static final DeferredBlock<StarlightDeviceBlock> LENS = register("lens", StarlightDeviceBlock::lens, p -> device(p).lightLevel(s -> 4));
    public static final DeferredBlock<StarlightDeviceBlock> MIRROR = register("mirror", StarlightDeviceBlock::mirror, p -> device(p));
    public static final DeferredBlock<StarlightDeviceBlock> PRISM = register("prism", StarlightDeviceBlock::prism, p -> device(p).lightLevel(s -> 6));
    public static final DeferredBlock<StarlightDeviceBlock> STARLIGHT_RESERVOIR = register("starlight_reservoir", StarlightDeviceBlock::reservoir, p -> device(p).lightLevel(s -> 9));

    // Phase 4 – crafting
    public static final DeferredBlock<RitualBlock> PEDESTAL = register("pedestal", RitualBlock::pedestal, p -> ritual(p));
    public static final DeferredBlock<RitualBlock> CONVERGENCE_ALTAR = register("convergence_altar", RitualBlock::altar, p -> ritual(p).lightLevel(s -> 10));
    public static final DeferredBlock<RitualBlock> ASTRAL_FORGE = register("astral_forge", RitualBlock::forge, p -> ritual(p).lightLevel(s -> 8));

    private AVBlocks() {}

    private static BlockBehaviour.Properties ritual(BlockBehaviour.Properties p) {
        return p.mapColor(MapColor.COLOR_BLACK).strength(3.0f, 9.0f).sound(SoundType.DEEPSLATE).noOcclusion().requiresCorrectToolForDrops();
    }

    private static BlockBehaviour.Properties device(BlockBehaviour.Properties p) {
        return p.mapColor(MapColor.GOLD).strength(2.5f, 6.0f).sound(SoundType.AMETHYST).noOcclusion().requiresCorrectToolForDrops();
    }

    static <B extends Block> DeferredBlock<B> register(
            String name, Function<BlockBehaviour.Properties, B> factory, UnaryOperator<BlockBehaviour.Properties> props) {
        //? if >=26.1 {
        return BLOCKS.registerBlock(name, factory, props);
        //?} else {
        /*return BLOCKS.registerBlock(name, factory, props.apply(BlockBehaviour.Properties.of()));
        *///?}
    }
}
