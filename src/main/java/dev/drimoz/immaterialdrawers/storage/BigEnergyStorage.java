package dev.drimoz.immaterialdrawers.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * The energy a drawer holds.
 *
 * <p>Adapted from Functional Storage's {@code fluid/BigFluidHandler}, which plays the same role for
 * the Fluid Drawer: a storage object owned by the tile, saved with it, and answering to the
 * drawer's void and creative options rather than to the plain capability contract.
 * Copyright (c) 2021 Buuz135, Rid — MIT. See NOTICE.
 *
 * <p><b>Long inside, int at the boundary.</b> {@link IEnergyStorage} is an int API, so the standard
 * capability cannot express more than {@link Integer#MAX_VALUE}. The amounts are kept in a
 * {@code long} and clamped on the way out, which is what Powah does — its cable is declared
 * {@code receiveEnergy(long, …)} and reaches the standard capability through an adapter. A cable or
 * a meter therefore sees at most 2.1B; {@link #getStoredLong()} and {@link #getCapacityLong()} are
 * the truth, and every display in this mod asks for those. See {@link EnergyScaling}.
 *
 * <p><b>What a drawer's options mean here.</b> Two of Functional Storage's three carry over:
 *
 * <ul>
 *   <li><b>Void</b> — reports every FE offered as accepted and drops what does not fit. A void
 *       drawer must not apply backpressure, or the machine feeding it stalls instead of running.</li>
 *   <li><b>Creative</b> — bottomless and infinite, mirroring {@code CustomFluidTank}: capacity and
 *       stored both read the maximum, and extraction hands out whatever is asked for without
 *       depleting anything.</li>
 *   <li><b>Locked</b> — deliberately absent. Locking a drawer pins it to the kind of thing it holds
 *       so an emptied drawer keeps its assignment. Forge Energy has exactly one kind of thing, so
 *       there is nothing to pin, and an energy drawer is never in the state locking exists to
 *       prevent. See {@code EnergyDrawerTile#setLocked}.</li>
 * </ul>
 */
public class BigEnergyStorage implements IEnergyStorage, INBTSerializable<CompoundTag> {

    private long capacity;
    private long energy;

    public BigEnergyStorage(long capacity) {
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
        // capacity - energy cannot overflow: both are non-negative and capacity is the larger.
        // max(0, ..): capacity can sit below energy for a moment if a pack lowers the curve, and
        // receiveEnergy is contractually forbidden from returning a negative.
        long accepted = Math.max(0, Math.min(capacity - energy, toReceive));
        if (!simulate && accepted > 0) {
            energy += accepted;
            onChange();
        }
        // The void drawer's whole point: the sender is told it all went through.
        return isDrawerVoid() ? toReceive : (int) accepted;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        if (toExtract <= 0) {
            return 0;
        }
        if (isDrawerCreative()) {
            return toExtract;
        }
        long extracted = Math.min(energy, toExtract);
        if (!simulate && extracted > 0) {
            energy -= extracted;
            onChange();
        }
        return (int) extracted;
    }

    /**
     * Clamped to int, and knowingly wrong above 2.1B — see the class comment. Anything of ours that
     * needs the real figure calls {@link #getStoredLong()}.
     */
    @Override
    public int getEnergyStored() {
        return clampToInt(getStoredLong());
    }

    @Override
    public int getMaxEnergyStored() {
        return clampToInt(getCapacityLong());
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    /** What is stored, in full, with the creative upgrade taken into account. */
    public long getStoredLong() {
        return isDrawerCreative() ? Long.MAX_VALUE : energy;
    }

    /** What fits, in full, with the creative upgrade taken into account. */
    public long getCapacityLong() {
        return isDrawerCreative() ? Long.MAX_VALUE : capacity;
    }

    /**
     * What is really stored, ignoring the creative upgrade.
     *
     * <p>A creative drawer lies to the capability on purpose. Anything that needs the truth —
     * deciding whether a broken drawer has contents worth keeping, or whether an upgrade can be
     * pulled out — has to ask this instead.
     */
    public long getStoredRaw() {
        return energy;
    }

    /** The configured capacity, ignoring the creative upgrade. Counterpart to {@link #getStoredRaw()}. */
    public long getCapacityRaw() {
        return capacity;
    }

    /**
     * The most this drawer may offer someone else right now.
     *
     * <p>Anything handing energy outward has to clamp its offer to this, because
     * {@code receiveEnergy} on the far side is committed the moment it is called: offer more than
     * is held, and the receiver keeps the difference while this drawer gives up only what it has.
     * That was a real defect — a drawer with 1 FE handed a machine 2,500 every four ticks — so it
     * lives in one place now, and the next thing that pushes energy cannot reintroduce it.
     * {@code pushingEnergyNeverCreatesIt} is the test.
     *
     * <p>A creative drawer is unlimited, which is the one case where offering more than
     * {@link #getStoredRaw()} is correct.
     */
    public long availableToGive() {
        return isDrawerCreative() ? Long.MAX_VALUE : energy;
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
    public void setCapacity(long capacity) {
        this.capacity = capacity;
        if (energy > capacity) {
            energy = capacity;
            onChange();
        }
    }

    private static int clampToInt(long value) {
        return (int) Math.min(Integer.MAX_VALUE, value);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Energy", energy);
        tag.putLong("Capacity", capacity);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        // getLong reads an int tag just as happily, so drawers saved before the move to long load
        // with their contents intact.
        this.energy = tag.getLong("Energy");
        if (tag.contains("Capacity")) {
            this.capacity = tag.getLong("Capacity");
        }
    }
}
