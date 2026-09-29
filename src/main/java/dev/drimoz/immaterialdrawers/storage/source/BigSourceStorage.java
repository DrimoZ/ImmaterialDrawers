package dev.drimoz.immaterialdrawers.storage.source;

import com.hollingsworth.arsnouveau.api.source.ISourceCap;
import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * The Source a drawer holds, and both of the ways Ars Nouveau asks for it.
 *
 * <p><b>Ars Nouveau only - see {@code compat.Mods}.</b>
 *
 * <p>Modelled on Ars Nouveau's {@code SourceStorage} for the capability half, and on the drawer
 * storages of this mod for the options - void and creative behave as on {@code BigEnergyStorage}.
 *
 * <p><b>Two interfaces, one object behind them.</b> Ars looks for Source in two places that do not
 * talk to each other. Relays, splitters and turrets use the {@code ars_nouveau:source} capability,
 * {@link ISourceCap}, which this class implements. Everything that consumes or generates Source
 * nearby - the enchanting apparatus, imbuement, sourcelinks - walks {@code SourceUtil}, which only
 * knows its own jars and the providers registered with {@code SourceManager}, and speaks
 * {@link ISourceTile}. The two interfaces cannot be one class - both declare {@code setSource(int)},
 * one returning {@code void} and one {@code int} - so {@link #asTile()} is a view onto the same
 * numbers. See {@code SourceDrawerProvider}.
 *
 * <p><b>Int, and it fits.</b> Ars's Source API is {@code int} throughout. The drawer follows the fluid
 * curve, and its top - 32 units x 1,000 x 16^4 = 2,097,152,000 - sits under
 * {@link Integer#MAX_VALUE}, so all four upgrade slots do something and nothing needs clamping but
 * the Max Storage upgrade. See {@code SourceDrawerTile#capacityFor}.
 */
public abstract class BigSourceStorage implements ISourceCap, INBTSerializable<CompoundTag> {

    private int source;
    private int capacity;

    private final ISourceTile tile = new TileView();

    protected BigSourceStorage(int capacity) {
        this.capacity = capacity;
    }

    public abstract void onChange();

    public abstract boolean isDrawerVoid();

    public abstract boolean isDrawerCreative();

    /** The {@link ISourceTile} face of this storage, for {@code SourceManager}. */
    public ISourceTile asTile() {
        return tile;
    }

    // ---- ISourceCap: what relays and turrets use -------------------------------------------------

    @Override
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

    @Override
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

    @Override
    public boolean canAcceptSource(int amount) {
        return receiveSource(amount, true) > 0;
    }

    @Override
    public boolean canProvideSource(int amount) {
        return extractSource(amount, true) > 0;
    }

    /**
     * Overridden, not inherited. The interface's default is {@code canAcceptSource(1)}, which is
     * {@code receiveSource}, which a naive implementation guards with {@code canReceive()} - a loop.
     * Ars's own {@code SourceStorage} breaks it the same way.
     */
    @Override
    public boolean canReceive() {
        return true;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    /** No per-operation limit: a drawer is storage, not a pipe, and relays bring their own rate. */
    @Override
    public int getMaxExtract() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getMaxReceive() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getSource() {
        return isDrawerCreative() ? Integer.MAX_VALUE : source;
    }

    @Override
    public int getSourceCapacity() {
        return isDrawerCreative() ? Integer.MAX_VALUE : capacity;
    }

    @Override
    public void setSource(int amount) {
        int clamped = Math.clamp(amount, 0, capacity);
        if (clamped != source) {
            source = clamped;
            onChange();
        }
    }

    /** Sizing belongs to the storage upgrades, not to whoever holds the capability. */
    @Override
    public void setMaxSource(int max) {
    }

    // ---- The raw truth, and resizing ------------------------------------------------------------

    public int getStoredRaw() {
        return source;
    }

    public int getCapacityRaw() {
        return capacity;
    }

    /** Resized by the storage upgrades. Shrinking below what is held is prevented before it happens. */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
        if (source > capacity) {
            source = capacity;
            onChange();
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Source", source);
        tag.putInt("Capacity", capacity);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.source = tag.getInt("Source");
        if (tag.contains("Capacity")) {
            this.capacity = tag.getInt("Capacity");
        }
    }

    /**
     * {@link ISourceTile}, the way {@code SourceUtil} uses it: {@code addSource} and {@code removeSource}
     * return the <em>new total</em>, and callers compute what moved by comparing before and after.
     *
     * <p>That comparison is why the creative case needs care. {@code takeSourceMultiple} reads
     * {@code getSource()}, calls {@code removeSource(n)}, and counts {@code before - after} as taken. A
     * creative drawer that reported full both times would be counted as giving nothing, and every
     * consumer would walk past it. So it reports the maximum, and answers a removal with the maximum
     * minus what was asked - without depleting anything. Ars special-cases its own creative jar by
     * class; ours cannot be named there, so it has to satisfy the arithmetic instead.
     */
    private final class TileView implements ISourceTile {

        @Override
        public int getTransferRate() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean canAcceptSource() {
            return BigSourceStorage.this.canAcceptSource(1);
        }

        @Override
        public int getSource() {
            return BigSourceStorage.this.getSource();
        }

        @Override
        public int getMaxSource() {
            return getSourceCapacity();
        }

        /**
         * The two-argument forms, as Ars's own machines implement them: they return what <em>moved</em>
         * and honour {@code simulate}. The interface's defaults in the published jar ignore
         * {@code simulate} and call the one-argument form - a simulated removal would really empty the
         * drawer.
         */
        @Override
        public int addSource(int amount, boolean simulate) {
            return receiveSource(amount, simulate);
        }

        @Override
        public int removeSource(int amount, boolean simulate) {
            return extractSource(amount, simulate);
        }

        @Override
        public int setSource(int amount) {
            if (!isDrawerCreative()) {
                BigSourceStorage.this.setSource(amount);
            }
            return getSource();
        }

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
    }
}
