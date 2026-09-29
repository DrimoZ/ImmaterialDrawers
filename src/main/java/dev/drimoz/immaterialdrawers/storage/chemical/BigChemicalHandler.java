package dev.drimoz.immaterialdrawers.storage.chemical;

import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.Arrays;

/**
 * The chemicals a drawer holds, one tank per slot.
 *
 * <p><b>Mekanism only - see {@code compat.Mods}.</b>
 *
 * <p>Adapted from Functional Storage's {@code fluid/BigFluidHandler} and its {@code CustomFluidTank}.
 * Copyright (c) 2021 Buuz135, Rid — MIT. See NOTICE. Same shape, same rules, same order of
 * preference, so that a chemical drawer behaves like the fluid drawer beside it in every way a player
 * or a tube can observe:
 *
 * <ul>
 *   <li><b>Filling without a slot</b> goes to the first tank already holding that chemical and with
 *       room, then to the first empty one - one tank per call, like {@code BigFluidHandler.fill}. A
 *       tube pushes again next tick; the rest lands then.</li>
 *   <li><b>Locked</b> pins each tank to what it held when the drawer was locked. A locked tank that was
 *       empty accepts nothing, until a player assigns it by hand - exactly the fluid drawer.</li>
 *   <li><b>Void</b> reports everything valid as accepted and destroys the overflow, so the machine
 *       upstream never stalls.</li>
 *   <li><b>Creative</b> is bottomless and infinite: every non-empty tank reads full at the maximum and
 *       hands out whatever is asked without depleting.</li>
 * </ul>
 *
 * <p><b>Long, and for once that is the API's idea.</b> Mekanism's chemical API is {@code long} from
 * end to end, so unlike Forge Energy there is no int boundary to clamp at and nothing to lie about:
 * what a tube sees is the truth.
 */
public abstract class BigChemicalHandler implements IChemicalHandler, INBTSerializable<CompoundTag> {

    private final ChemicalStack[] stored;
    private final ChemicalStack[] filter;
    private long capacity;

    protected BigChemicalHandler(int tanks, long capacity) {
        this.stored = new ChemicalStack[tanks];
        this.filter = new ChemicalStack[tanks];
        Arrays.fill(this.stored, ChemicalStack.EMPTY);
        Arrays.fill(this.filter, ChemicalStack.EMPTY);
        this.capacity = capacity;
    }

    /** Called whenever what is stored changes, so the tile can sync and update comparators. */
    public abstract void onChange();

    public abstract boolean isDrawerLocked();

    public abstract boolean isDrawerVoid();

    public abstract boolean isDrawerCreative();

    @Override
    public int getChemicalTanks() {
        return stored.length;
    }

    /**
     * What a tank holds. Mekanism's contract says callers must not modify the stack they get back;
     * the creative case hands out a copy anyway, because it reports an amount that is not stored.
     */
    @Override
    public ChemicalStack getChemicalInTank(int tank) {
        ChemicalStack stack = stored[tank];
        if (!stack.isEmpty() && isDrawerCreative()) {
            return stack.copyWithAmount(Long.MAX_VALUE);
        }
        return stack;
    }

    @Override
    public void setChemicalInTank(int tank, ChemicalStack stack) {
        stored[tank] = stack.isEmpty() ? ChemicalStack.EMPTY : stack.copy();
        onChange();
    }

    @Override
    public long getChemicalTankCapacity(int tank) {
        return isDrawerCreative() ? Long.MAX_VALUE : capacity;
    }

    /**
     * Whether this chemical may go in this tank: not denylisted, the tank's own chemical if it has one,
     * and the locked filter's if the drawer is locked.
     */
    @Override
    public boolean isValid(int tank, ChemicalStack stack) {
        if (stack.isEmpty() || stack.is(ChemicalCapabilities.DENYLIST)) {
            return false;
        }
        if (isDrawerLocked() && !ChemicalStack.isSameChemical(stack, filter[tank])) {
            return false;
        }
        return stored[tank].isEmpty() || ChemicalStack.isSameChemical(stack, stored[tank]);
    }

    @Override
    public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {
        if (stack.isEmpty()) {
            return ChemicalStack.EMPTY;
        }
        if (!isValid(tank, stack)) {
            return stack;
        }
        if (isDrawerCreative()) {
            // Already infinite: taking it changes nothing but the chemical the tank is showing.
            if (stored[tank].isEmpty() && action.execute()) {
                stored[tank] = stack.copyWithAmount(capacity);
                onChange();
            }
            return ChemicalStack.EMPTY;
        }
        long amount = stack.getAmount();
        long accepted = Math.max(0, Math.min(capacity - stored[tank].getAmount(), amount));
        if (accepted > 0 && action.execute()) {
            if (stored[tank].isEmpty()) {
                stored[tank] = stack.copyWithAmount(accepted);
            } else {
                stored[tank].grow(accepted);
            }
            onChange();
        }
        if (isDrawerVoid() || accepted == amount) {
            // The void drawer's whole point: the sender is told it all went through.
            return ChemicalStack.EMPTY;
        }
        return stack.copyWithAmount(amount - accepted);
    }

    @Override
    public ChemicalStack extractChemical(int tank, long amount, Action action) {
        ChemicalStack held = stored[tank];
        if (held.isEmpty() || amount <= 0) {
            return ChemicalStack.EMPTY;
        }
        if (isDrawerCreative()) {
            return held.copyWithAmount(amount);
        }
        long taken = Math.min(amount, held.getAmount());
        ChemicalStack result = held.copyWithAmount(taken);
        if (action.execute()) {
            held.shrink(taken);
            if (held.isEmpty()) {
                stored[tank] = ChemicalStack.EMPTY;
            }
            onChange();
        }
        return result;
    }

    /**
     * The fluid drawer's order: a tank that already holds this chemical first, then an empty one.
     * Mekanism's default would take the first tank that accepts at all, which on a 4-slot drawer
     * splits one chemical across empty tanks while the tank already holding it has room.
     */
    @Override
    public ChemicalStack insertChemical(ChemicalStack stack, Action action) {
        if (stack.isEmpty()) {
            return ChemicalStack.EMPTY;
        }
        for (int pass = 0; pass < 2; pass++) {
            for (int tank = 0; tank < stored.length; tank++) {
                boolean candidate = pass == 0
                        ? !stored[tank].isEmpty() && ChemicalStack.isSameChemical(stored[tank], stack)
                        : stored[tank].isEmpty();
                if (candidate && insertChemical(tank, stack, Action.SIMULATE).getAmount() < stack.getAmount()) {
                    return insertChemical(tank, stack, action);
                }
            }
        }
        return stack;
    }

    /** Whatever the first non-empty tank holds, like {@code BigFluidHandler.drain(int, …)}. */
    @Override
    public ChemicalStack extractChemical(long amount, Action action) {
        for (int tank = 0; tank < stored.length; tank++) {
            if (!stored[tank].isEmpty()) {
                return extractChemical(tank, amount, action);
            }
        }
        return ChemicalStack.EMPTY;
    }

    /** The first tank holding this chemical. */
    @Override
    public ChemicalStack extractChemical(ChemicalStack stack, Action action) {
        if (stack.isEmpty()) {
            return ChemicalStack.EMPTY;
        }
        for (int tank = 0; tank < stored.length; tank++) {
            if (!stored[tank].isEmpty() && ChemicalStack.isSameChemical(stored[tank], stack)) {
                return extractChemical(tank, stack.getAmount(), action);
            }
        }
        return ChemicalStack.EMPTY;
    }

    /**
     * Resizes every tank when the storage upgrades change. Shrinking below what is held is prevented
     * by the tile before it happens; the clamp is for the case it cannot see, a pack lowering the
     * config between two sessions.
     */
    public void setCapacity(long capacity) {
        this.capacity = capacity;
        boolean clamped = false;
        for (ChemicalStack stack : stored) {
            if (stack.getAmount() > capacity) {
                stack.setAmount(capacity);
                clamped = true;
            }
        }
        if (clamped) {
            onChange();
        }
    }

    /** Pins every tank to what it holds now. An empty tank is pinned to nothing, as a fluid drawer's is. */
    public void lockHandler() {
        for (int tank = 0; tank < stored.length; tank++) {
            filter[tank] = stored[tank].isEmpty() ? ChemicalStack.EMPTY : stored[tank].copyWithAmount(1);
        }
    }

    /** What a locked tank is pinned to; empty when it is pinned to nothing. */
    public ChemicalStack getFilter(int tank) {
        return filter[tank];
    }

    /** Assigns a locked, empty tank by hand - the right-click with a full tank item. */
    public void setFilter(int tank, ChemicalStack stack) {
        filter[tank] = stack.isEmpty() ? ChemicalStack.EMPTY : stack.copyWithAmount(1);
    }

    /** What is really stored in a tank, ignoring the creative upgrade. */
    public long getStoredRaw(int tank) {
        return stored[tank].getAmount();
    }

    /** The configured capacity of each tank, ignoring the creative upgrade. */
    public long getCapacityRaw() {
        return capacity;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        for (int tank = 0; tank < stored.length; tank++) {
            tag.put(String.valueOf(tank), stored[tank].saveOptional(provider));
            tag.put("Locked" + tank, filter[tank].saveOptional(provider));
        }
        tag.putLong("Capacity", capacity);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        if (tag.contains("Capacity")) {
            this.capacity = tag.getLong("Capacity");
        }
        for (int tank = 0; tank < stored.length; tank++) {
            stored[tank] = read(provider, tag, String.valueOf(tank));
            filter[tank] = read(provider, tag, "Locked" + tank);
        }
    }

    /**
     * Reads one tank back. A chemical that no longer exists - its mod removed from the pack - comes
     * back as empty rather than failing the whole drawer, which is what {@code parseOptional} is for.
     */
    public static ChemicalStack read(HolderLookup.Provider provider, CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_COMPOUND)
                ? ChemicalStack.parseOptional(provider, tag.getCompound(key))
                : ChemicalStack.EMPTY;
    }
}
