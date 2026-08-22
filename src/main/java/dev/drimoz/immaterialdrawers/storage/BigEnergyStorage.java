package dev.drimoz.immaterialdrawers.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * The energy a drawer holds.
 *
 * <p>Adapted from Functional Storage's {@code fluid/BigFluidHandler}, which plays the same role for
 * the Fluid Drawer: a storage object owned by the tile, saved with it, and answering to the drawer's
 * void / creative / locked options rather than to the plain capability contract.
 * Copyright (c) 2021 Buuz135, Rid - MIT. See NOTICE.
 *
 * <p><b>Why int and not long.</b> {@link IEnergyStorage} is an int API from end to end, so the
 * ceiling is {@link Integer#MAX_VALUE}. Storing a long internally and clamping on the way out would
 * make {@code getEnergyStored} lie to every cable, meter and Jade tooltip in the game. The cap is
 * respected instead, and the upgrade scaling is divided to stay under it - see
 * {@link #ENERGY_DIVISOR} and CLAUDE.md §11.
 */
public class BigEnergyStorage implements IEnergyStorage, INBTSerializable<CompoundTag> {

    /**
     * Base capacity of an unupgraded Energy Drawer.
     *
     * <p>Provisional. The number that matters is this one multiplied by the storage upgrades:
     * four multiplicative netherite upgrades are x32^4, so a base of 1,000,000 FE saturates the
     * int ceiling on the third slot and the fourth does nothing at all.
     */
    public static final int BASE_CAPACITY = 100_000;

    /**
     * Mirrors Functional Storage's {@code FLUID_DIVISOR}, which halves every storage multiplier for
     * fluid drawers. Energy needs a harsher one for the reason above. Provisional until task 4
     * wires the upgrades up and the real curve can be measured rather than guessed.
     */
    public static final int ENERGY_DIVISOR = 4;

    private int capacity;
    private int energy;

    public BigEnergyStorage(int capacity) {
        this.capacity = capacity;
    }

    // Overridden by the tile, which knows its own drawer options. Defaults keep this class usable
    // on its own - in a test, or in an item stack that has no tile behind it.

    public boolean isDrawerVoid() {
        return false;
    }

    public boolean isDrawerCreative() {
        return false;
    }

    /** Called whenever the stored amount changes, so the tile can sync and update comparators. */
    public void onChange() {
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        if (toReceive <= 0 || !canReceive()) {
            return 0;
        }
        int accepted = Math.min(capacity - energy, toReceive);
        // A void drawer reports the whole amount as accepted and drops the excess, which is what
        // "void" means everywhere else in Functional Storage: the sender must not see backpressure.
        int reported = isDrawerVoid() ? toReceive : accepted;
        if (!simulate && accepted > 0) {
            energy += accepted;
            onChange();
        }
        return reported;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        if (toExtract <= 0 || !canExtract()) {
            return 0;
        }
        int extracted = Math.min(energy, toExtract);
        if (!simulate && extracted > 0 && !isDrawerCreative()) {
            energy -= extracted;
            onChange();
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return isDrawerCreative() ? capacity : energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    /**
     * Resizes the drawer when its storage upgrades change.
     *
     * <p>Shrinking spills: energy above the new capacity is dropped rather than kept in a field
     * that no longer reports it. Functional Storage guards the equivalent case by refusing to
     * remove an upgrade that would not fit what is stored ({@code canChangeMultiplier}); until
     * task 4 wires that guard up here, this at least keeps the object self-consistent.
     */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
        if (energy > capacity) {
            energy = capacity;
            onChange();
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Energy", energy);
        tag.putInt("Capacity", capacity);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.energy = tag.getInt("Energy");
        // A drawer saved before its capacity was written back reads as zero; keeping the
        // constructor's value is more useful than a drawer that holds nothing.
        if (tag.contains("Capacity")) {
            this.capacity = tag.getInt("Capacity");
        }
    }
}
