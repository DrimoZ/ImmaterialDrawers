package dev.drimoz.immaterialdrawers.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * The energy a drawer holds.
 *
 * <p>Adapted from Functional Storage's {@code fluid/BigFluidHandler}, which plays the same role for
 * the Fluid Drawer: a storage object owned by the tile, saved with it, and answering to the
 * drawer's void and creative options rather than to the plain capability contract.
 * Copyright (c) 2021 Buuz135, Rid — MIT. See NOTICE.
 *
 * <p><b>Why int and not long.</b> {@link IEnergyStorage} is an int API from end to end, so the
 * ceiling is {@link Integer#MAX_VALUE}. Storing a long internally and clamping on the way out would
 * make {@code getEnergyStored} lie to every cable, meter and Jade tooltip in the game. The cap is
 * respected instead, and the upgrade curve is calibrated to fit under it — see {@link EnergyScaling}.
 *
 * <p><b>What a drawer's options mean here.</b> Two of Functional Storage's three carry over:
 *
 * <ul>
 *   <li><b>Void</b> — reports every FE offered as accepted and drops what does not fit. A void
 *       drawer must not apply backpressure, or the machine feeding it stalls instead of running.</li>
 *   <li><b>Creative</b> — bottomless and infinite, mirroring {@code CustomFluidTank}: capacity and
 *       stored both read {@link Integer#MAX_VALUE}, and extraction hands out whatever is asked for
 *       without depleting anything.</li>
 *   <li><b>Locked</b> — deliberately absent. Locking a drawer pins it to the kind of thing it holds
 *       so an emptied drawer keeps its assignment. Forge Energy has exactly one kind of thing, so
 *       there is nothing to pin, and an energy drawer is never in the state locking exists to
 *       prevent. See {@code EnergyDrawerTile#setLocked}.</li>
 * </ul>
 */
public class BigEnergyStorage implements IEnergyStorage, INBTSerializable<CompoundTag> {

    private int capacity;
    private int energy;

    public BigEnergyStorage(int capacity) {
        this.capacity = capacity;
    }

    // Overridden by the tile, which knows its own drawer options. The defaults keep this class
    // usable on its own - in a test, or one day in an item stack that has no tile behind it.

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
        if (toReceive <= 0) {
            return 0;
        }
        if (isDrawerCreative()) {
            // Already infinite. Accepting is free and changes nothing, so no onChange either.
            return toReceive;
        }
        int accepted = Math.min(capacity - energy, toReceive);
        if (!simulate && accepted > 0) {
            energy += accepted;
            onChange();
        }
        // The void drawer's whole point: the sender is told it all went through.
        return isDrawerVoid() ? toReceive : accepted;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        if (toExtract <= 0) {
            return 0;
        }
        if (isDrawerCreative()) {
            return toExtract;
        }
        int extracted = Math.min(energy, toExtract);
        if (!simulate && extracted > 0) {
            energy -= extracted;
            onChange();
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return isDrawerCreative() ? Integer.MAX_VALUE : energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return isDrawerCreative() ? Integer.MAX_VALUE : capacity;
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
     * What is really stored, ignoring the creative upgrade.
     *
     * <p>{@link #getEnergyStored()} answers the capability contract, and a creative drawer lies to
     * it on purpose. Anything that needs the truth — deciding whether a broken drawer has contents
     * worth keeping, or whether an upgrade can be pulled out — has to ask this instead.
     */
    public int getStoredRaw() {
        return energy;
    }

    /** The configured capacity, ignoring the creative upgrade. Counterpart to {@link #getStoredRaw()}. */
    public int getCapacityRaw() {
        return capacity;
    }

    /**
     * Resizes the drawer when its storage upgrades change.
     *
     * <p>Shrinking spills, and that is the caller's problem to prevent rather than this object's to
     * hide: {@code EnergyDrawerTile} refuses to release an upgrade whose removal would not leave
     * room, the same way Functional Storage's {@code canChangeMultiplier} does. What is left here
     * is the case that guard cannot cover — a pack author lowering a multiplier in the config
     * between two sessions — where clamping is the only honest option, since the alternative is a
     * drawer reporting more stored than it can hold.
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
        // A drawer saved before its capacity was written reads as zero; keeping the constructor's
        // value is more useful than a drawer that suddenly holds nothing.
        if (tag.contains("Capacity")) {
            this.capacity = tag.getInt("Capacity");
        }
    }
}
