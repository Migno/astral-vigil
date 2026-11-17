package media.hofferbert.astralvigil.registry;

import java.util.function.Supplier;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.crafting.AstralForgeBlockEntity;
import media.hofferbert.astralvigil.crafting.ConvergenceAltarBlockEntity;
import media.hofferbert.astralvigil.crafting.PedestalBlockEntity;
import media.hofferbert.astralvigil.starlight.network.Devices;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AVBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AstralVigil.MOD_ID);

    public static final Supplier<BlockEntityType<Devices.Collector>> COLLECTOR =
            register("starlight_collector", Devices.Collector::new, AVBlocks.STARLIGHT_COLLECTOR);
    public static final Supplier<BlockEntityType<Devices.Lens>> LENS = register("lens", Devices.Lens::new, AVBlocks.LENS);
    public static final Supplier<BlockEntityType<Devices.Mirror>> MIRROR = register("mirror", Devices.Mirror::new, AVBlocks.MIRROR);
    public static final Supplier<BlockEntityType<Devices.Prism>> PRISM = register("prism", Devices.Prism::new, AVBlocks.PRISM);
    public static final Supplier<BlockEntityType<Devices.Reservoir>> RESERVOIR =
            register("starlight_reservoir", Devices.Reservoir::new, AVBlocks.STARLIGHT_RESERVOIR);

    public static final Supplier<BlockEntityType<PedestalBlockEntity>> PEDESTAL =
            register("pedestal", PedestalBlockEntity::new, AVBlocks.PEDESTAL);
    public static final Supplier<BlockEntityType<ConvergenceAltarBlockEntity>> CONVERGENCE_ALTAR =
            register("convergence_altar", ConvergenceAltarBlockEntity::new, AVBlocks.CONVERGENCE_ALTAR);
    public static final Supplier<BlockEntityType<AstralForgeBlockEntity>> ASTRAL_FORGE =
            register("astral_forge", AstralForgeBlockEntity::new, AVBlocks.ASTRAL_FORGE);

    public static final Supplier<BlockEntityType<Devices.Anchor>> STAR_ANCHOR =
            register("star_anchor", Devices.Anchor::new, AVBlocks.STAR_ANCHOR);

    private AVBlockEntities() {}

    private static <T extends BlockEntity> Supplier<BlockEntityType<T>> register(
            String name, BlockEntityType.BlockEntitySupplier<T> factory, Supplier<? extends Block> block) {
        //? if >=26.1 {
        return BLOCK_ENTITIES.register(name, () -> new BlockEntityType<>(factory, block.get()));
        //?} else {
        /*return BLOCK_ENTITIES.register(name, () -> BlockEntityType.Builder.of(factory, block.get()).build(null));
        *///?}
    }
}
