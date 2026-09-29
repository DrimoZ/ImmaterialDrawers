package dev.drimoz.immaterialdrawers.block.tile;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.tile.ControllableDrawerTile;
import com.buuz135.functionalstorage.block.tile.ItemControllableDrawerTile;
import com.buuz135.functionalstorage.item.StorageUpgradeItem;
import com.buuz135.functionalstorage.item.UpgradeItem;
import com.hrznstudio.titanium.block.BasicTileBlock;
import com.hrznstudio.titanium.component.inventory.InventoryComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.EmptyHandler;
import org.jetbrains.annotations.NotNull;

/**
 * What every drawer in this mod is, whatever it holds.
 *
 * <p><b>Why this extends the item drawer.</b> Functional Storage has no registry for drawer types.
 * Its Storage Controller keeps a network of drawers and decides membership with a hardcoded
 * {@code instanceof} chain in {@code ConnectedDrawers} - anything that is not an
 * {@code ItemControllableDrawerTile}, a {@code FluidDrawerTile} or an extension is dropped from the
 * network on every rebuild. Worse, {@code StorageControllerTile.serverTick} rebuilds the whole
 * network whenever the drawer count stops matching {@code itemHandlers + fluidHandlers + extensions},
 * so a tile that passes the filter without contributing to a counter makes that check false forever
 * and rebuilds every tick.
 *
 * <p>Extending {@code ItemControllableDrawerTile} and returning a zero-slot handler from
 * {@link #getStorage()} satisfies both: we are counted in {@code itemHandlers}, so the invariant
 * holds, and the controller's aggregation walks {@code getSlots()} - zero - so it sees nothing. Both
 * the filter and the invariant are in Functional Storage 1.20.1 exactly as on 1.21.1 (PORTING.md §3);
 * {@code IDGameTests} is the proof that the trick survives there too.
 *
 * <p><b>What differs on 1.20.1.</b> No data components: a storage upgrade's effect is
 * {@code StorageUpgradeItem.getStorageMultiplier() / getStorageDiv()}, multiplied across the slots
 * by {@code ControllableDrawerTile}, and each subclass says what its divisor is. No
 * {@code RegisterCapabilitiesEvent}: each subclass answers {@code getCapability} itself.
 *
 * @param <T> the concrete tile, as Functional Storage's self-typed hierarchy wants it
 */
public abstract class ImmaterialDrawerTile<T extends ImmaterialDrawerTile<T>> extends ItemControllableDrawerTile<T> {

    /**
     * The whole trick, in one field. {@link EmptyHandler}, not {@code new ItemStackHandler(0)}:
     * Functional Storage offers the held stack to {@code getStorage()} at the slot a player clicked,
     * and slot 0 of a zero-slot {@code ItemStackHandler} throws - that shipped in 0.1.0 on 1.21.1.
     */
    private static final IItemHandler NO_ITEMS = EmptyHandler.INSTANCE;

    /**
     * Per tile, never {@code LazyOptional.empty()}: Functional Storage invalidates whatever
     * {@link #getOptional()} returns when the tile's capabilities go, and invalidating the shared
     * empty instance is not something to do to every other mod in the pack.
     */
    private LazyOptional<IItemHandler> noItemsOptional = LazyOptional.of(() -> NO_ITEMS);

    protected ImmaterialDrawerTile(BasicTileBlock<T> base, BlockEntityType<T> entityType,
                                   BlockPos pos, BlockState state) {
        super(base, entityType, pos, state);
    }

    /**
     * Resizes the content storage after the storage upgrades changed. Called with the new
     * {@code getStorageMultiplier()} already in effect.
     */
    protected abstract void onStorageMultiplierChanged();

    /**
     * Whether the drawer can be resized to this multiplier without losing anything. Asked before an
     * upgrade is allowed out of its slot.
     *
     * <p>Implementations must read the raw stored amount, not what the capability reports: a
     * creative drawer reports the maximum and would refuse to give any upgrade back, forever.
     */
    protected abstract boolean canChangeMultiplier(int newStorageMultiplier);

    /**
     * Whether anything is stored, ignoring the creative upgrade - the raw truth, for the same reason
     * as {@link #canChangeMultiplier}.
     */
    protected abstract boolean hasContents();

    /**
     * The storage multiplier as it would be with one slot emptied.
     *
     * <p>The same arithmetic as {@code ControllableDrawerTile.maybeCacheUpgrades}, int truncation
     * included - {@code mult *= calculated} on an int - so the guard and the real multiplier can
     * never disagree about whether something fits.
     */
    protected int storageMultiplierWithout(InventoryComponent<?> upgrades, int emptiedSlot) {
        int mult = 1;
        for (int i = 0; i < upgrades.getSlots(); i++) {
            if (i != emptiedSlot && upgrades.getStackInSlot(i).getItem() instanceof StorageUpgradeItem upgrade) {
                mult *= upgrade.getStorageMultiplier() / getStorageDiv();
            }
        }
        return mult;
    }

    /**
     * Refreshes comparators next to the drawer. For the content storage's change hook.
     */
    protected void updateComparators() {
        if (level != null) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    /** Storage-upgrade slots, not content slots. Four, like every other drawer. */
    @Override
    public int getStorageSlotAmount() {
        return 4;
    }

    /**
     * Empty on purpose - see the class comment. Not null: the controller calls {@code getSlots()}
     * on whatever this returns without checking.
     */
    @Override
    public IItemHandler getStorage() {
        return NO_ITEMS;
    }

    @Override
    public LazyOptional<IItemHandler> getOptional() {
        return noItemsOptional;
    }

    /**
     * Forge invalidates a tile's capabilities on unload and brings them back with this. Our
     * optionals are ours to recreate: an invalidated {@code LazyOptional} never becomes valid again,
     * and a drawer reloaded from disk would answer no cable at all.
     */
    @Override
    public void reviveCaps() {
        super.reviveCaps();
        noItemsOptional = LazyOptional.of(() -> NO_ITEMS);
    }

    /**
     * Not meaningful for a drawer of ours: Functional Storage asks it for an item drawer's per-slot
     * size. Our capacity comes from the subclass's own scaling.
     */
    @Override
    public int getBaseSize(int lost) {
        return 1;
    }

    /**
     * The four storage-upgrade slots, and the rules for what may go in and come back out.
     *
     * <p>Modelled on {@code FluidDrawerTile.getStorageUpgradesConstructor()} of Functional Storage
     * 1.20.1. Copyright (c) 2021 Buuz135, Rid - MIT. See NOTICE.
     *
     * <ul>
     *   <li><b>The input filter</b>: storage-type upgrades, Creative included, and not the Iron
     *       downgrade - on 1.20.1 it only marks an item drawer as downgraded, and means nothing to a
     *       drawer whose size is not in stacks. Functional Storage's fluid drawer refuses it for the
     *       same reason.</li>
     *   <li><b>The extraction guard</b> refuses to hand back an upgrade whose removal would shrink
     *       the drawer below what it currently holds.</li>
     *   <li><b>The change hook</b> resizes the storage.</li>
     * </ul>
     */
    @Override
    public InventoryComponent<ControllableDrawerTile<T>> getStorageUpgradesConstructor() {
        return new InventoryComponent<ControllableDrawerTile<T>>(
                "storage_upgrades", 10, 70, getStorageSlotAmount()) {
            @NotNull
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (this.getStackInSlot(slot).getItem() instanceof StorageUpgradeItem
                        && !canChangeMultiplier(storageMultiplierWithout(this, slot))) {
                    return ItemStack.EMPTY;
                }
                return super.extractItem(slot, amount, simulate);
            }
        }
                .setInputFilter((stack, slot) -> {
                    if (stack.getItem().equals(
                            FunctionalStorage.STORAGE_UPGRADES.get(StorageUpgradeItem.StorageTier.IRON).get())) {
                        return false;
                    }
                    return stack.getItem() instanceof UpgradeItem upgrade
                            && upgrade.getType() == UpgradeItem.Type.STORAGE;
                })
                .setOnSlotChanged((stack, slot) -> {
                    setNeedsUpgradeCache(true);
                    onStorageMultiplierChanged();
                    updateComparators();
                })
                .setSlotLimit(1);
    }

    /**
     * Whether breaking this drawer may throw its state away. Inherited it would consult the
     * deliberately empty item handler and answer "empty" for a full drawer.
     */
    @Override
    public boolean isEverythingEmpty() {
        return !hasContents() && super.isEverythingEmpty();
    }
}
