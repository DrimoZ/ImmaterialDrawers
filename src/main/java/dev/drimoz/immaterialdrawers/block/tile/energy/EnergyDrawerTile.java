package dev.drimoz.immaterialdrawers.block.tile.energy;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.tile.DrawerProperties;
import com.buuz135.functionalstorage.block.tile.ItemControllableDrawerTile;
import com.buuz135.functionalstorage.item.FSAttachments;
import com.hrznstudio.titanium.annotation.Save;
import com.hrznstudio.titanium.block.BasicTileBlock;
import dev.drimoz.immaterialdrawers.storage.BigEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A drawer that holds Forge Energy.
 *
 * <p><b>Why this extends the item drawer and not the fluid one.</b> Functional Storage has no
 * registry for drawer types. Its Storage Controller keeps a network of drawers and decides
 * membership with a hardcoded {@code instanceof} chain in {@code ConnectedDrawers} - anything that
 * is not an {@code ItemControllableDrawerTile}, a {@code FluidDrawerTile} or an extension is
 * dropped from the network on every rebuild. Worse, {@code StorageControllerTile.serverTick}
 * rebuilds the whole network whenever the drawer count stops matching
 * {@code itemHandlers + fluidHandlers + extensions}, so a tile that passes the filter without
 * contributing to a counter makes that check false forever and rebuilds every tick.
 *
 * <p>Extending {@code ItemControllableDrawerTile} and returning a zero-slot handler from
 * {@link #getStorage()} satisfies both: we are counted in {@code itemHandlers}, so the invariant
 * holds, and the controller's aggregation walks {@code getSlots()} - zero - so it sees nothing.
 * The energy is aggregated separately, from the connected-drawer positions the controller exposes
 * publicly. See CLAUDE.md §7 for the line-by-line reading this rests on.
 *
 * <p>A mixin into {@code ConnectedDrawers} would be the direct fix and is deliberately not used:
 * it is a mixin into a third-party mod with 56M downloads, and this works inside the public
 * contract.
 */
public class EnergyDrawerTile extends ItemControllableDrawerTile<EnergyDrawerTile> {

    /**
     * The whole trick, in one field. Shared and immutable in the ways that matter - an
     * {@link ItemStackHandler} with no slots has no state to share.
     */
    private static final IItemHandler NO_ITEMS = new ItemStackHandler(0);

    @Save
    public BigEnergyStorage energyStorage;

    public EnergyDrawerTile(BasicTileBlock<EnergyDrawerTile> base, BlockEntityType<EnergyDrawerTile> entityType,
                            BlockPos pos, BlockState state) {
        // X_1 because Forge Energy has exactly one kind of content: the 2- and 4-slot geometries
        // would have nothing to put in slots 2 to 4. See CLAUDE.md §5.
        //
        // ITEM_STORAGE_MODIFIER is Functional Storage's own component, reused as-is. Task 4 swaps
        // it for immaterialdrawers:energy_storage_modifier so energy can scale on its own curve
        // (CLAUDE.md §10, option 2) - until then, borrowing beats inventing.
        super(base, entityType, pos, state,
                new DrawerProperties(FunctionalStorage.DrawerType.X_1.getSlotAmount(),
                        FSAttachments.ITEM_STORAGE_MODIFIER));

        this.energyStorage = new BigEnergyStorage(BigEnergyStorage.BASE_CAPACITY) {
            @Override
            public void onChange() {
                syncObject(energyStorage);
                // Vanilla rather than ControllableDrawerTile#updateComparatorOutput: that method is
                // public on the 1.21 branch but not in the released 1.5.7 we compile against, and a
                // comparator refresh is not worth a hard floor on the Functional Storage version.
                if (EnergyDrawerTile.this.getLevel() != null) {
                    EnergyDrawerTile.this.getLevel().updateNeighbourForOutputSignal(
                            EnergyDrawerTile.this.getBlockPos(), EnergyDrawerTile.this.getBlockState().getBlock());
                }
            }

            @Override
            public boolean isDrawerVoid() {
                return isVoid();
            }

            @Override
            public boolean isDrawerCreative() {
                return isCreative();
            }
        };
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
    public EnergyDrawerTile getSelf() {
        return this;
    }

    public BigEnergyStorage getEnergyStorage() {
        return energyStorage;
    }
}
