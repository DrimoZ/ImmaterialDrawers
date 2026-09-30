package dev.drimoz.immaterialdrawers.storage.source;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * A Source Drawer's Source. <b>Ars Nouveau only</b> - see {@code compat.Mods}.
 *
 * <p><b>Ars 4.12 (1.20.1) has one interface, not two.</b> On 1.21.1 Ars 5.x has an {@code ISourceCap}
 * capability beside {@code ISourceTile}, and the two cannot be one class (both declare
 * {@code setSource(int)}, one void, one int), hence a view there. 4.12 has only {@code ISourceTile},
 * so this is it. Everything Ars reaches us through - {@code SourceManager}, which the apparatus,
 * imbuement and sourcelinks use - asks this interface.
 *
 * <p>Void and creative as on 1.21.1. Creative reads full and pays out without emptying:
 * {@code removeSource(n)} answers {@code MAX - n}, the way Ars counts what it took.
 */
public abstract class BigSourceStorage implements ISourceTile, INBTSerializable<CompoundTag> {

    private int source;
    private int capacity;

    protected BigSourceStorage(int capacity) {
        this.capacity = capacity;
    }

    public abstract void onChange();

    public abstract boolean isDrawerVoid();

    public abstract boolean isDrawerCreative();

    /** Kept from 1.21.1, where Ars asks a separate view: here the storage is the tile interface. */
    public ISourceTile asTile() {
        return this;
    }

    /** What fits, committed unless simulated. A void drawer swallows the rest. */
    public int receiveSource(int toReceive, boolean simulate) {
        if (toReceive <= 0) {
            return 0;
        }
        if (isDrawerCreative()) {
            return toReceive;
        }
        int accepted = Math.max(0, Math.min(capacity - source, toReceive));
        if (!simulate && accepted > 0) {
            source += accepted;
            onChange();
        }
        return isDrawerVoid() ? toReceive : accepted;
    }

    /** What was there, committed unless simulated. A creative drawer never runs dry. */
    public int extractSource(int toExtract, boolean simulate) {
        if (toExtract <= 0) {
            return 0;
        }
        if (isDrawerCreative()) {
            return toExtract;
        }
        int taken = Math.min(source, toExtract);
        if (!simulate && taken > 0) {
            source -= taken;
            onChange();
        }
        return taken;
    }

    // ---- ISourceTile ------------------------------------------------------------------------------

    @Override
    public int getTransferRate() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean canAcceptSource() {
        return receiveSource(1, true) > 0;
    }

    @Override
    public int getSource() {
        return isDrawerCreative() ? Integer.MAX_VALUE : source;
    }

    @Override
    public int getMaxSource() {
        return getSourceCapacity();
    }

    /** Capacity is the storage upgrades' business, not Ars's. */
    @Override
    public void setMaxSource(int max) {
    }

    @Override
    public int setSource(int amount) {
        if (!isDrawerCreative()) {
            int clamped = Math.max(0, Math.min(amount, capacity));
            if (clamped != source) {
                source = clamped;
                onChange();
            }
        }
        return getSource();
    }

    /** Ars's contract: returns the new amount. Negative amounts remove, as in Ars's own tiles. */
    @Override
    public int addSource(int amount) {
        if (isDrawerCreative()) {
            return Integer.MAX_VALUE;
        }
        if (amount > 0) {
            receiveSource(amount, false);
        } else if (amount < 0) {
            extractSource(-amount, false);
        }
        return source;
    }

    @Override
    public int removeSource(int amount) {
        if (isDrawerCreative()) {
            return Integer.MAX_VALUE - Math.max(0, amount);
        }
        if (amount > 0) {
            extractSource(amount, false);
        } else if (amount < 0) {
            receiveSource(-amount, false);
        }
        return source;
    }

    // ---- The raw truth, and resizing ------------------------------------------------------------

    public int getSourceCapacity() {
        return isDrawerCreative() ? Integer.MAX_VALUE : capacity;
    }

    public int getStoredRaw() {
        return source;
    }

    public int getCapacityRaw() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
        if (source > capacity) {
            source = capacity;
            onChange();
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Source", source);
        tag.putInt("Capacity", capacity);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.source = tag.getInt("Source");
        if (tag.contains("Capacity")) {
            this.capacity = tag.getInt("Capacity");
        }
    }
}
