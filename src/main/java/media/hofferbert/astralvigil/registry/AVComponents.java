package media.hofferbert.astralvigil.registry;

import java.util.function.Supplier;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.gear.GearData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AVComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, AstralVigil.MOD_ID);

    /** Living-gear state: deeds, sigils, stance, charge, cooldown. */
    public static final Supplier<DataComponentType<GearData>> GEAR = COMPONENTS.registerComponentType(
            "gear", b -> b.persistent(GearData.CODEC).networkSynchronized(GearData.STREAM_CODEC));

    private AVComponents() {}
}
