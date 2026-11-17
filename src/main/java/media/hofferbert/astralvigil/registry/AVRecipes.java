package media.hofferbert.astralvigil.registry;

import java.util.function.Supplier;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.crafting.StarlightRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
//? if >=26.1 {
import net.minecraft.world.item.crafting.RecipeBookCategory;
//?} else {
/*import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
*///?}

public final class AVRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, AstralVigil.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, AstralVigil.MOD_ID);

    public static final Supplier<RecipeType<StarlightRecipe>> CONVERGENCE =
            TYPES.register("convergence", () -> RecipeType.simple(AstralVigil.id("convergence")));
    public static final Supplier<RecipeType<StarlightRecipe>> FORGE =
            TYPES.register("astral_forge", () -> RecipeType.simple(AstralVigil.id("astral_forge")));

    public static final Supplier<RecipeSerializer<StarlightRecipe>> CONVERGENCE_SERIALIZER =
            SERIALIZERS.register("convergence", () -> serializer(StarlightRecipe.Kind.CONVERGENCE));
    public static final Supplier<RecipeSerializer<StarlightRecipe>> FORGE_SERIALIZER =
            SERIALIZERS.register("astral_forge", () -> serializer(StarlightRecipe.Kind.FORGE));

    //? if >=26.1 {
    public static final DeferredRegister<RecipeBookCategory> BOOK_CATEGORIES =
            DeferredRegister.create(Registries.RECIPE_BOOK_CATEGORY, AstralVigil.MOD_ID);
    public static final Supplier<RecipeBookCategory> BOOK_CATEGORY =
            BOOK_CATEGORIES.register("starlight", RecipeBookCategory::new);
    //?}

    private AVRecipes() {}

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        SERIALIZERS.register(modBus);
        //? if >=26.1 {
        BOOK_CATEGORIES.register(modBus);
        //?}
    }

    private static RecipeSerializer<StarlightRecipe> serializer(StarlightRecipe.Kind kind) {
        //? if >=26.1 {
        return new RecipeSerializer<>(StarlightRecipe.codec(kind), StarlightRecipe.streamCodec(kind));
        //?} else {
        /*MapCodec<StarlightRecipe> codec = StarlightRecipe.codec(kind);
        StreamCodec<RegistryFriendlyByteBuf, StarlightRecipe> stream = StarlightRecipe.streamCodec(kind);
        return new RecipeSerializer<>() {
            @Override
            public MapCodec<StarlightRecipe> codec() {
                return codec;
            }

            @Override
            public StreamCodec<RegistryFriendlyByteBuf, StarlightRecipe> streamCodec() {
                return stream;
            }
        };
        *///?}
    }
}
