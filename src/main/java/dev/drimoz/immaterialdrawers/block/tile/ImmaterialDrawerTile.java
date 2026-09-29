package dev.drimoz.immaterialdrawers.block.tile;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.tile.ControllableDrawerTile;
import com.buuz135.functionalstorage.block.tile.DrawerProperties;
import com.buuz135.functionalstorage.block.tile.ItemControllableDrawerTile;
import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import com.buuz135.functionalstorage.item.StorageUpgradeItem;
import com.buuz135.functionalstorage.item.component.SizeProvider;
import com.hrznstudio.titanium.block.BasicTileBlock;
import com.hrznstudio.titanium.component.inventory.InventoryComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.EmptyItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

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
 * holds, and the controller's aggregation walks {@code getSlots()} - zero - so it sees nothing. See
 * CLAUDE.md §7 for the line-by-line reading this rests on, and {@code IDGameTests} for the proof that
 * it survives 50 drawers on a live controller.
 *
 * <p>This class exists so the second kind of drawer did not have to rediscover that. It holds the
 * parts that are about <em>being a drawer Functional Storage does not know about</em> - the empty
 * item handler, the storage-upgrade slots, the 1.5.7 chunk-load fix - and leaves each subclass the
 * part that is about its content: what a storage upgrade scales, and whether it is empty.
 *
 * @param <T> the concrete tile, as Functional Storage's self-typed hierarchy wants it
 */
public abstract class ImmaterialDrawerTile<T extends ImmaterialDrawerTile<T>> extends ItemControllableDrawerTile<T> {

    /**
     * The whole trick, in one field. Shared and immutable in the ways that matter - a handler with
     * no slots has no state to share.
     *
     * <p>{@link EmptyItemHandler}, not {@code new ItemStackHandler(0)}. Functional Storage offers
     * the held stack to {@code getStorage()} at the slot a player clicked, and
     * {@code ItemStackHandler} validates slot indices: slot 0 of a zero-slot one throws. That shipped
     * in 0.1.0 - see {@code clickingTheFrontWithAnItemDoesNotThrow}. {@code EmptyItemHandler} answers
     * every slot with "nothing here, nothing fits".
     */
    private static final IItemHandler NO_ITEMS = EmptyItemHandler.INSTANCE;

    protected ImmaterialDrawerTile(BasicTileBlock<T> base, BlockEntityType<T> entityType,
                                   BlockPos pos, BlockState state, DrawerProperties properties) {
        super(base, entityType, pos, state, properties);
    }

    /**
     * The size component a storage upgrade must carry to be accepted, and that
     * {@code getStorageMultiplier()} reads.
     *
     * <p>Called from the upgrade slot's filters, which Functional Storage builds from inside its own
     * constructor - so this must not read a field of the subclass. Return a constant.
     */
    protected abstract Supplier<DataComponentType<SizeProvider>> storageModifier();

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
    protected abstract boolean canChangeMultiplier(double newSizeMultiplier);

    /**
     * Whether anything is stored, ignoring the creative upgrade - the raw truth, for the same reason
     * as {@link #canChangeMultiplier}.
     */
    protected abstract boolean hasContents();

    /**
     * Takes the controller-rebuild-on-invalidate back off Functional Storage 1.5.7, because their
     * version of it deadlocks the server thread while a chunk is loading.
     *
     * <p><b>The failure.</b> Loading a chunk that contains a linked drawer hangs the world at 100%,
     * for good. {@code LevelChunk.setBlockEntity} → {@code clearRemoved} →
     * {@code invalidateCapabilities}, and 1.5.7's override calls
     * {@code Level.getBlockEntity(controllerPos)} to rebuild the controller's network. That is a
     * <em>blocking</em> chunk fetch, issued from inside chunk post-load, on the thread that has to
     * run the load: if the controller's chunk is in the same batch, the server thread ends up
     * waiting for a task only it can execute.
     *
     * <p>Their {@code isLoaded} guard does not help. It answers for the chunk holder existing, not
     * for the chunk being available to this thread right now.
     *
     * <p><b>Already fixed upstream, not released.</b> The {@code 1.21} branch replaced the lookup
     * with {@code getChunkSource().getChunkNow(...)}, which returns null rather than waiting. This
     * is that fix applied to our tiles only — their own drawers keep the bug until 1.5.8 ships, and
     * fixing it for them would need a mixin. Delete this override when the floor moves to 1.5.8.
     *
     * <p><b>Why not {@code super}.</b> Calling it is the bug. The two things worth keeping are
     * reimplemented: NeoForge's one-line invalidation, which
     * {@code IBlockEntityExtension.invalidateCapabilities} is only a convenience for, and the
     * controller rebuild with the non-blocking lookup. NeoForge marks that method
     * {@code @NonExtendable} — advice Functional Storage did not take either, which is the reason
     * this override has to exist at all.
     */
    @Override
    public void invalidateCapabilities() {
        if (level == null) {
            return;
        }
        level.invalidateCapabilities(worldPosition);

        BlockPos controllerPos = getControllerPos();
        if (level.isClientSide() || controllerPos == null || level.isOutsideBuildHeight(controllerPos)) {
            return;
        }

        LevelChunk chunk = level.getChunkSource().getChunkNow(
                SectionPos.blockToSectionCoord(controllerPos.getX()),
                SectionPos.blockToSectionCoord(controllerPos.getZ()));
        if (chunk != null && chunk.getBlockEntity(controllerPos) instanceof StorageControllerTile<?> controller) {
            controller.getConnectedDrawers().rebuild();
        }
        // If the chunk is not there yet, nothing is lost: the controller rebuilds its own network on
        // its next tick, which is how it recovers from every other reason the invariant breaks.
    }

    /**
     * Refreshes comparators next to the drawer. For the content storage's change hook.
     *
     * <p>Vanilla rather than {@code ControllableDrawerTile#updateComparatorOutput}: that method is
     * public on the 1.21 branch but not in the released 1.5.7 we compile against, and a comparator
     * refresh is not worth a hard floor on the Functional Storage version.
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

    /**
     * The four storage-upgrade slots, and the rules for what may go in and come back out.
     *
     * <p>Modelled on {@code FluidDrawerTile.getStorageUpgradesConstructor()}, with the subclass's
     * component in place of theirs. Three things are wired here and each one is load-bearing:
     *
     * <ul>
     *   <li><b>The input filter</b> decides which upgrades the slot will take at all. Anything
     *       carrying the drawer's component, plus the Iron downgrade and the Creative upgrade, which
     *       mean something to every drawer.</li>
     *   <li><b>The extraction guard</b> refuses to hand back an upgrade whose removal would shrink
     *       the drawer below what it currently holds. Without it, pulling an upgrade deletes
     *       contents, and the drawer looks like it ate them.</li>
     *   <li><b>The change hook</b> resizes the storage. Without it, the upgrades are decoration.</li>
     * </ul>
     */
    @Override
    public InventoryComponent<ControllableDrawerTile<T>> getStorageUpgradesConstructor() {
        return new InventoryComponent<ControllableDrawerTile<T>>(
                "storage_upgrades", 10, 70, getStorageSlotAmount()) {
            @NotNull
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (isStorageUpgradeLocked()) {
                    return ItemStack.EMPTY;
                }
                if (this.getStackInSlot(slot).has(storageModifier().get())) {
                    // What the multiplier would be with this slot emptied. `replacement` is how
                    // SizeProvider models a hypothetical: a null entry means "read the real slot".
                    ItemStack[] replacement = new ItemStack[this.getSlots()];
                    replacement[slot] = ItemStack.EMPTY;

                    float newSize = SizeProvider.calculateAsFactor(
                            this, storageModifier(), baseSize, replacement);
                    if (!canChangeMultiplier(newSize)) {
                        return ItemStack.EMPTY;
                    }
                }
                return super.extractItem(slot, amount, simulate);
            }
        }
                .setInputFilter((stack, slot) -> {
                    if (isStorageUpgradeLocked()) {
                        return false;
                    }
                    // Functional Storage's 1.21 branch also calls canUseStorageUpgradeWithCreative
                    // here, which stops a creative drawer accepting further storage upgrades. That
                    // method does not exist in the released 1.5.7 we compile against - the second
                    // instance of the same version drift, see CLAUDE.md §11 - so neither our drawers
                    // nor theirs have the guard on 1.5.7, and adding our own would make ours behave
                    // differently from the one next to it in the wall. Restore it the day the floor
                    // moves to a release that has it.
                    //
                    // The Iron downgrade means "reset this drawer to its base size", which is as
                    // meaningful for energy or chemicals as for anything else. Kept as its own case
                    // because Functional Storage special-cases it too, so a future release that
                    // changes what the downgrade carries will not silently ban it here.
                    if (stack.getItem().equals(
                            FunctionalStorage.STORAGE_UPGRADES.get(StorageUpgradeItem.StorageTier.IRON).get())) {
                        return true;
                    }
                    return stack.has(storageModifier().get()) || stack.is(FunctionalStorage.CREATIVE_UPGRADE);
                })
                .setOnSlotChanged((stack, slot) -> {
                    setNeedsUpgradeCache(true);
                    onStorageMultiplierChanged();
                    updateComparators();
                })
                .setSlotLimit(1);
    }

    /**
     * Whether breaking this drawer may throw its state away.
     *
     * <p>{@code Drawer.copyTo} writes the tile's NBT into the dropped item only when this is false.
     * Inherited from {@code ItemControllableDrawerTile} it would consult the deliberately empty item
     * handler and answer "empty" for a full drawer, whose contents then evaporate when a player picks
     * it up.
     */
    @Override
    public boolean isEverythingEmpty() {
        return !hasContents() && super.isEverythingEmpty();
    }

    @Override
    public boolean isInventoryEmpty() {
        return !hasContents();
    }
}
