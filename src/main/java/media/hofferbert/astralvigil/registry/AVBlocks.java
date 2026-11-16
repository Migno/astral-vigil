package media.hofferbert.astralvigil.registry;

import java.util.function.Function;
import java.util.function.UnaryOperator;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.block.FallenStarBlock;
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

    private AVBlocks() {}

    static <B extends Block> DeferredBlock<B> register(
            String name, Function<BlockBehaviour.Properties, B> factory, UnaryOperator<BlockBehaviour.Properties> props) {
        //? if >=26.1 {
        return BLOCKS.registerBlock(name, factory, props);
        //?} else {
        /*return BLOCKS.registerBlock(name, factory, props.apply(BlockBehaviour.Properties.of()));
        *///?}
    }
}
