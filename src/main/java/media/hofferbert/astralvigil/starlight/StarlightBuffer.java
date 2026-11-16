package media.hofferbert.astralvigil.starlight;

import java.util.Arrays;

/** Starlight storage split by {@link Tint}, sharing one total capacity. Pure Java, unit-tested. */
public final class StarlightBuffer {
    private final int[] amounts = new int[Tint.values().length];
    private int capacity;

    public StarlightBuffer(int capacity) {
        this.capacity = Math.max(0, capacity);
    }

    public int capacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = Math.max(0, capacity);
    }

    public int get(Tint tint) {
        return amounts[tint.ordinal()];
    }

    public int total() {
        int sum = 0;
        for (int a : amounts) {
            sum += a;
        }
        return sum;
    }

    public int space() {
        return Math.max(0, capacity - total());
    }

    /** @return amount actually stored */
    public int insert(Tint tint, int amount, boolean simulate) {
        int accepted = Math.max(0, Math.min(amount, space()));
        if (!simulate) {
            amounts[tint.ordinal()] += accepted;
        }
        return accepted;
    }

    /** @return amount actually removed */
    public int extract(Tint tint, int amount, boolean simulate) {
        int removed = Math.max(0, Math.min(amount, amounts[tint.ordinal()]));
        if (!simulate) {
            amounts[tint.ordinal()] -= removed;
        }
        return removed;
    }

    public int[] toArray() {
        return Arrays.copyOf(amounts, amounts.length);
    }

    public void fromArray(int[] values) {
        Arrays.fill(amounts, 0);
        for (int i = 0; i < Math.min(values.length, amounts.length); i++) {
            amounts[i] = Math.max(0, values[i]);
        }
    }

    /** Comparator-style fill level 0..15. */
    public int signal() {
        if (capacity <= 0 || total() <= 0) {
            return 0;
        }
        return 1 + (int) Math.floor(14.0 * total() / capacity);
    }
}
