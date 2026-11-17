package media.hofferbert.astralvigil.compat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
//? if >=26.1 {
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
//?} else {
/*import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
*///?}

/**
 * Tiny version-neutral persistence API for block entities: 26.1 uses ValueInput/ValueOutput, 1.21.1 CompoundTag +
 * registries.
 */
public final class DataIO {
    private DataIO() {}

    public interface Writer {
        void putInt(String key, int value);

        void putString(String key, String value);

        void putItems(String key, List<ItemStack> items);
    }

    public interface Reader {
        int getInt(String key, int fallback);

        String getString(String key, String fallback);

        List<ItemStack> getItems(String key);
    }

    //? if >=26.1 {
    public static Writer writer(ValueOutput out) {
        return new Writer() {
            @Override
            public void putInt(String key, int value) {
                out.putInt(key, value);
            }

            @Override
            public void putString(String key, String value) {
                out.putString(key, value);
            }

            @Override
            public void putItems(String key, List<ItemStack> items) {
                out.store(key, ItemStack.CODEC.listOf(), items.stream().filter(s -> !s.isEmpty()).toList());
            }
        };
    }

    public static Reader reader(ValueInput in) {
        return new Reader() {
            @Override
            public int getInt(String key, int fallback) {
                return in.getIntOr(key, fallback);
            }

            @Override
            public String getString(String key, String fallback) {
                return in.getStringOr(key, fallback);
            }

            @Override
            public List<ItemStack> getItems(String key) {
                return new ArrayList<>(in.read(key, ItemStack.CODEC.listOf()).orElse(List.of()));
            }
        };
    }
    //?} else {
    /*public static Writer writer(CompoundTag tag, HolderLookup.Provider registries) {
        return new Writer() {
            @Override
            public void putInt(String key, int value) {
                tag.putInt(key, value);
            }

            @Override
            public void putString(String key, String value) {
                tag.putString(key, value);
            }

            @Override
            public void putItems(String key, List<ItemStack> items) {
                ListTag list = new ListTag();
                for (ItemStack s : items) {
                    if (!s.isEmpty()) {
                        list.add(s.save(registries));
                    }
                }
                tag.put(key, list);
            }
        };
    }

    public static Reader reader(CompoundTag tag, HolderLookup.Provider registries) {
        return new Reader() {
            @Override
            public int getInt(String key, int fallback) {
                return tag.contains(key) ? tag.getInt(key) : fallback;
            }

            @Override
            public String getString(String key, String fallback) {
                return tag.contains(key) ? tag.getString(key) : fallback;
            }

            @Override
            public List<ItemStack> getItems(String key) {
                List<ItemStack> out = new ArrayList<>();
                for (Tag t : tag.getList(key, Tag.TAG_COMPOUND)) {
                    ItemStack.parse(registries, t).ifPresent(out::add);
                }
                return out;
            }
        };
    }
    *///?}
}
