package media.hofferbert.astralvigil.gametest;

import media.hofferbert.astralvigil.AstralVigil;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
//? if >=26.1 {
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredRegister;
//?} else {
/*import java.util.Collection;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
*///?}

/** Registers every {@link AVGameTests.Def} with the version-specific GameTest system. */
public final class GameTestRegistration {
    private GameTestRegistration() {}

    //? if >=26.1 {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, AstralVigil.MOD_ID);

    static {
        for (AVGameTests.Def def : AVGameTests.ALL) {
            FUNCTIONS.register(def.name(), () -> def.body());
        }
    }

    public static void register(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(GameTestRegistration::onRegisterTests);
    }

    private static void onRegisterTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env =
                event.registerEnvironment(AstralVigil.id("default"), new TestEnvironmentDefinition.AllOf());
        for (AVGameTests.Def def : AVGameTests.ALL) {
            var data = new TestData<>(
                    env, AstralVigil.id(def.structure()), def.maxTicks(), 0, true,
                    Rotation.NONE, false, 1, 1, def.skyAccess(), 0);
            var function = ResourceKey.create(Registries.TEST_FUNCTION, AstralVigil.id(def.name()));
            event.registerTest(AstralVigil.id(def.name()), new FunctionGameTestInstance(function, data));
        }
    }
    //?} else {
    /*public static void register(IEventBus modBus) {
        modBus.addListener((RegisterGameTestsEvent event) -> event.register(GameTestRegistration.class));
    }

    @GameTestGenerator
    public static Collection<TestFunction> generateTests() {
        return AVGameTests.ALL.stream()
                .map(def -> new TestFunction(
                        "astralvigil", "astralvigil." + def.name(), AstralVigil.id(def.structure()).toString(),
                        Rotation.NONE, def.maxTicks(), 0L, true, false, 1, 1, def.skyAccess(), def.body()))
                .toList();
    }
    *///?}
}
