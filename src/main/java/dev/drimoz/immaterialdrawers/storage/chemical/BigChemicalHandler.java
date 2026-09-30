package dev.drimoz.immaterialdrawers.storage.chemical;

import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.merged.BoxedChemicalStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.Arrays;

/**
 * A chemical drawer's tanks. <b>Mekanism only</b> - see {@code compat.Mods}.
 *
 * <p>Modelled on Functional Storage's {@code fluid/BigFluidHandler}: one tank per drawer slot, one
 * shared capacity, lock / void / creative. Copyright (c) 2021 Buuz135, Rid - MIT. See NOTICE.
 *
 * <p>A tank holds a chemical of any of the four types - its type is whatever came first, and
 * {@link BoxedChemicalStack} writes it to NBT with the stack. The typed handlers Mekanism asks for
 * are {@link ChemicalTanks}'s views.
 */
public abstract class BigChemicalHandler extends ChemicalTanks implements INBTSerializable<CompoundTag> {

    private static final ChemicalStack<?> EMPTY = GasStack.EMPTY;

    private final ChemicalStack<?>[] stored;
    private final ChemicalStack<?>[] filter;
    private long capacity;

    protected BigChemicalHandler(int tanks, long capacity) {
        this.stored = new ChemicalStack<?>[tanks];
        this.filter = new ChemicalStack<?>[tanks];
        Arrays.fill(this.stored, EMPTY);
        Arrays.fill(this.filter, EMPTY);
        this.capacity = capacity;
    }

    public abstract void onChange();

    public abstract boolean isDrawerLocked();

    public abstract boolean isDrawerVoid();

    public abstract boolean isDrawerCreative();

    @Override
    public int tanks() {
        return stored.length;
    }

    @Override
    public ChemicalStack<?> stored(int tank) {
        ChemicalStack<?> stack = stored[tank];
        return !stack.isEmpty() && isDrawerCreative() ? copyWithAmount(stack, Long.MAX_VALUE) : stack;
    }

    @Override
    public long capacity(int tank) {
        return isDrawerCreative() ? Long.MAX_VALUE : capacity;
    }

    @Override
    public boolean isValid(int tank, ChemicalStack<?> stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (isDrawerLocked() && !same(stack, filter[tank])) {
            return false;
        }
        return stored[tank].isEmpty() || same(stack, stored[tank]);
    }

    @Override
    public ChemicalStack<?> insert(int tank, ChemicalStack<?> stack, Action action) {
        if (stack.isEmpty() || !isValid(tank, stack)) {
            return stack;
        }
        if (isDrawerCreative()) {
            // Already infinite: taking it changes nothing but the chemical the tank shows.
            if (stored[tank].isEmpty() && action.execute()) {
                stored[tank] = copyWithAmount(stack, capacity);
                onChange();
            }
            return copyWithAmount(stack, 0);
        }
        long amount = stack.getAmount();
        long accepted = Math.max(0, Math.min(capacity - stored[tank].getAmount(), amount));
        if (accepted > 0 && action.execute()) {
            if (stored[tank].isEmpty()) {
                stored[tank] = copyWithAmount(stack, accepted);
            } else {
                stored[tank].grow(accepted);
            }
            onChange();
        }
        // A void drawer tells the sender it all went through.
        return copyWithAmount(stack, isDrawerVoid() ? 0 : amount - accepted);
    }

    @Override
    public ChemicalStack<?> extract(int tank, long amount, Action action) {
        ChemicalStack<?> held = stored[tank];
        if (held.isEmpty() || amount <= 0) {
            return EMPTY;
        }
        if (isDrawerCreative()) {
            return copyWithAmount(held, amount);
        }
        long taken = Math.min(amount, held.getAmount());
        ChemicalStack<?> result = copyWithAmount(held, taken);
        if (action.execute()) {
            held.shrink(taken);
            if (held.isEmpty()) {
                stored[tank] = EMPTY;
            }
            onChange();
        }
        return result;
    }

    @Override
    public void set(int tank, ChemicalStack<?> stack) {
        stored[tank] = stack.isEmpty() ? EMPTY : stack.copy();
        onChange();
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
        boolean clamped = false;
        for (ChemicalStack<?> stack : stored) {
            if (stack.getAmount() > capacity) {
                stack.setAmount(capacity);
                clamped = true;
            }
        }
        if (clamped) {
            onChange();
        }
    }

    /** Fixes each tank on what it holds now, so an emptied locked drawer keeps its assignment. */
    public void lockHandler() {
        for (int tank = 0; tank < stored.length; tank++) {
            filter[tank] = stored[tank].isEmpty() ? EMPTY : copyWithAmount(stored[tank], 1);
        }
    }

    public ChemicalStack<?> getFilter(int tank) {
        return filter[tank];
    }

    public void setFilter(int tank, ChemicalStack<?> stack) {
        filter[tank] = stack.isEmpty() ? EMPTY : copyWithAmount(stack, 1);
    }

    /** The raw truth, ignoring creative. */
    public ChemicalStack<?> getStoredRaw(int tank) {
        return stored[tank];
    }

    public long getCapacityRaw() {
        return capacity;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        for (int tank = 0; tank < stored.length; tank++) {
            tag.put(String.valueOf(tank), BoxedChemicalStack.box(stored[tank]).write(new CompoundTag()));
            tag.put("Locked" + tank, BoxedChemicalStack.box(filter[tank]).write(new CompoundTag()));
        }
        tag.putLong("Capacity", capacity);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains("Capacity")) {
            this.capacity = tag.getLong("Capacity");
        }
        for (int tank = 0; tank < stored.length; tank++) {
            stored[tank] = read(tag, String.valueOf(tank));
            filter[tank] = read(tag, "Locked" + tank);
        }
    }

    public static ChemicalStack<?> read(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_COMPOUND)) {
            return EMPTY;
        }
        ChemicalStack<?> stack = BoxedChemicalStack.read(tag.getCompound(key)).getChemicalStack();
        return stack == null || stack.isEmpty() ? EMPTY : stack;
    }
}
