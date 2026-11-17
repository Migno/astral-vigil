package media.hofferbert.astralvigil.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeInput;

/** Recipe input: an optional centre/catalyst stack plus an unordered list of side stacks. */
public record ItemsInput(ItemStack center, List<ItemStack> items) implements RecipeInput {
    public ItemsInput {
        items = items.stream().filter(s -> !s.isEmpty()).toList();
    }

    @Override
    public ItemStack getItem(int index) {
        return index == 0 ? center : items.get(index - 1);
    }

    @Override
    public int size() {
        return items.size() + 1;
    }

    /** Unordered match: every ingredient consumes exactly one distinct stack, no stack left over. */
    public static boolean matchesUnordered(List<Ingredient> ingredients, List<ItemStack> stacks) {
        if (ingredients.size() != stacks.size()) {
            return false;
        }
        return assign(ingredients, new ArrayList<>(stacks), 0);
    }

    private static boolean assign(List<Ingredient> ingredients, List<ItemStack> remaining, int index) {
        if (index == ingredients.size()) {
            return remaining.isEmpty();
        }
        Ingredient ingredient = ingredients.get(index);
        for (int i = 0; i < remaining.size(); i++) {
            ItemStack stack = remaining.get(i);
            if (ingredient.test(stack)) {
                remaining.remove(i);
                if (assign(ingredients, remaining, index + 1)) {
                    return true;
                }
                remaining.add(i, stack);
            }
        }
        return false;
    }
}
