package media.hofferbert.astralvigil.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import media.hofferbert.astralvigil.registry.AVRecipes;
import media.hofferbert.astralvigil.research.Alignment;
import media.hofferbert.astralvigil.starlight.Tint;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
//? if >=26.1 {
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeBookCategory;
//?} else {
/*import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
*///?}

/**
 * Starlight-powered crafting, used by two recipe types:
 * <ul>
 *   <li>{@code astralvigil:convergence} – Convergence Altar: {@code center} on the altar, {@code ingredients} on the
 *       pedestals (unordered, one item each).</li>
 *   <li>{@code astralvigil:astral_forge} – Astral Forge: {@code ingredients} only (unordered, one item each).</li>
 * </ul>
 * Inputs are plain {@link Ingredient}s (tags recommended), so datapacks and KubeJS can add/replace recipes.
 *
 * @param starlight starlight consumed over the whole process
 * @param tint required starlight tint ({@code pure} accepts only pure light)
 * @param time process duration in ticks
 * @param alignment optional alignment; nearby blocks of that alignment stabilise the altar
 */
public record StarlightRecipe(
        Kind kind,
        Optional<Ingredient> center,
        List<Ingredient> ingredients,
        //? if >=26.1 {
        ItemStackTemplate result,
        //?} else {
        /*ItemStack result,
        *///?}
        int starlight,
        Tint tint,
        int time,
        Optional<Alignment> alignment)
        implements Recipe<ItemsInput> {

    public enum Kind {
        CONVERGENCE,
        FORGE
    }

    public static final Codec<Tint> TINT_CODEC =
            Codec.STRING.xmap(s -> Tint.valueOf(s.toUpperCase(Locale.ROOT)), Tint::id);
    public static final Codec<Alignment> ALIGNMENT_CODEC =
            Codec.STRING.xmap(s -> Alignment.byId(s).orElseThrow(), Alignment::id);

    public static MapCodec<StarlightRecipe> codec(Kind kind) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                        Ingredient.CODEC.optionalFieldOf("center").forGetter(StarlightRecipe::center),
                        Ingredient.CODEC.listOf(1, 12).fieldOf("ingredients").forGetter(StarlightRecipe::ingredients),
                        //? if >=26.1 {
                        ItemStackTemplate.CODEC.fieldOf("result").forGetter(StarlightRecipe::result),
                        //?} else {
                        /*ItemStack.CODEC.fieldOf("result").forGetter(StarlightRecipe::result),
                        *///?}
                        Codec.intRange(0, 1_000_000).optionalFieldOf("starlight", 0).forGetter(StarlightRecipe::starlight),
                        TINT_CODEC.optionalFieldOf("tint", Tint.PURE).forGetter(StarlightRecipe::tint),
                        Codec.intRange(1, 72000).optionalFieldOf("time", 100).forGetter(StarlightRecipe::time),
                        ALIGNMENT_CODEC.optionalFieldOf("alignment").forGetter(StarlightRecipe::alignment))
                .apply(i, (c, ing, res, sl, t, time, al) -> new StarlightRecipe(kind, c, ing, res, sl, t, time, al)));
    }

    public static StreamCodec<RegistryFriendlyByteBuf, StarlightRecipe> streamCodec(Kind kind) {
        return StreamCodec.of(
                (buf, r) -> {
                    ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC).encode(buf, r.center());
                    Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, r.ingredients());
                    //? if >=26.1 {
                    ItemStackTemplate.STREAM_CODEC.encode(buf, r.result());
                    //?} else {
                    /*ItemStack.STREAM_CODEC.encode(buf, r.result());
                    *///?}
                    buf.writeVarInt(r.starlight());
                    buf.writeVarInt(r.tint().ordinal());
                    buf.writeVarInt(r.time());
                    buf.writeVarInt(r.alignment().map(a -> a.ordinal() + 1).orElse(0));
                },
                buf -> {
                    Optional<Ingredient> center = ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC).decode(buf);
                    List<Ingredient> ingredients = Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
                    //? if >=26.1 {
                    ItemStackTemplate result = ItemStackTemplate.STREAM_CODEC.decode(buf);
                    //?} else {
                    /*ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
                    *///?}
                    int starlight = buf.readVarInt();
                    Tint tint = Tint.byOrdinal(buf.readVarInt());
                    int time = buf.readVarInt();
                    int al = buf.readVarInt();
                    Optional<Alignment> alignment = al == 0 ? Optional.empty() : Optional.of(Alignment.values()[al - 1]);
                    return new StarlightRecipe(kind, center, ingredients, result, starlight, tint, time, alignment);
                });
    }

    @Override
    public boolean matches(ItemsInput input, Level level) {
        if (center.isPresent()) {
            if (!center.get().test(input.center())) {
                return false;
            }
        } else if (!input.center().isEmpty()) {
            return false;
        }
        return ItemsInput.matchesUnordered(ingredients, input.items());
    }

    /** A fresh result stack. (26.1 stores an ItemStackTemplate: ItemStacks cannot be created during datagen.) */
    public ItemStack resultCopy() {
        //? if >=26.1 {
        return result.create();
        //?} else {
        /*return result.copy();
        *///?}
    }

    @Override
    public RecipeSerializer<StarlightRecipe> getSerializer() {
        return kind == Kind.CONVERGENCE ? AVRecipes.CONVERGENCE_SERIALIZER.get() : AVRecipes.FORGE_SERIALIZER.get();
    }

    @Override
    public RecipeType<StarlightRecipe> getType() {
        return kind == Kind.CONVERGENCE ? AVRecipes.CONVERGENCE.get() : AVRecipes.FORGE.get();
    }

    @Override
    public boolean isSpecial() {
        return true; // never shown in the vanilla recipe book
    }

    //? if >=26.1 {
    @Override
    public ItemStack assemble(ItemsInput input) {
        return resultCopy();
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return AVRecipes.BOOK_CATEGORY.get();
    }
    //?} else {
    /*@Override
    public ItemStack assemble(ItemsInput input, HolderLookup.Provider registries) {
        return resultCopy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return resultCopy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        center.ifPresent(list::add);
        list.addAll(ingredients);
        return list;
    }
    *///?}
}
