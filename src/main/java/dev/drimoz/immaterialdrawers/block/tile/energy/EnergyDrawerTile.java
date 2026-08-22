package dev.drimoz.immaterialdrawers.block.tile.energy;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.tile.ControllableDrawerTile;
import com.buuz135.functionalstorage.block.tile.DrawerProperties;
import com.buuz135.functionalstorage.block.tile.ItemControllableDrawerTile;
import com.buuz135.functionalstorage.block.tile.StorageControllerExtensionTile;
import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import com.buuz135.functionalstorage.item.StorageUpgradeItem;
import com.buuz135.functionalstorage.item.component.SizeProvider;
import com.hrznstudio.titanium.annotation.Save;
import com.hrznstudio.titanium.block.BasicTileBlock;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.client.gui.EnergyDrawerInfoGuiAddon;
import com.hrznstudio.titanium.component.inventory.InventoryComponent;
import dev.drimoz.immaterialdrawers.registry.IDComponents;
import dev.drimoz.immaterialdrawers.storage.BigEnergyStorage;
import dev.drimoz.immaterialdrawers.storage.EnergyScaling;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

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
 * See CLAUDE.md §7 for the line-by-line reading this rests on, and {@code IDGameTests} for the
 * proof that it survives 50 drawers on a live controller.
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
        // The base size is in EnergyScaling's units, not the drawer type's slot amount, and the
        // component is ours rather than Functional Storage's - energy needs a harsher divisor than
        // fluids or the fourth upgrade slot does nothing. See EnergyScaling and IDComponents.
        super(base, entityType, pos, state,
                new DrawerProperties(EnergyScaling.baseUnits(), IDComponents.ENERGY_STORAGE_MODIFIER));

        this.energyStorage = new BigEnergyStorage(EnergyScaling.capacityFor(getStorageMultiplier())) {
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

    /**
     * Puts an energy bar in the drawer's screen.
     *
     * <p>At the same place and the same size as the fluid drawer's: a 48x48 tile of the drawer front
     * with the amount written across it. CLAUDE.md §8 planned to borrow Titanium's
     * {@code EnergyStorageComponent} for its screen addon, and an earlier version used Titanium's
     * {@code EnergyBarScreenAddon} directly - one line, works, and looks like a machine rather than
     * like a drawer. A wall of drawers should not have one block whose screen came from somewhere
     * else.
     */
    @OnlyIn(Dist.CLIENT)
    @Override
    public void initClient() {
        super.initClient();
        addGuiAddonFactory(() -> new EnergyDrawerInfoGuiAddon(64, 16,
                ResourceLocation.fromNamespaceAndPath(ImmaterialDrawers.MOD_ID, "textures/block/energy_drawer_front.png"),
                this::getEnergyStorage));
    }

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
     * Hands energy to whatever is next to the drawer that will take it.
     *
     * <p><b>Why a drawer pushes at all.</b> An item drawer never has to: hoppers, pipes and players
     * all pull items out of it. A fluid drawer barely has to, for the same reason. Forge Energy has
     * no such thing — nothing in the ecosystem pulls. Powah is the case in point: across
     * {@code CableTile}, {@code AbstractEnergyStorage} and {@code ChargeUtil} the only call it ever
     * makes on a neighbour is {@code receiveEnergy}, and the "extract" setting on a cable side means
     * <em>the cable may output here</em>, not <em>the cable will drain what is here</em>. A drawer
     * that only ever waits to be drained is a hole energy goes into.
     *
     * <p>So the drawer pushes, with no upgrade needed. That is a deliberate divergence from the
     * fluid drawer, which needs a Pusher: the fluid drawer has a world full of things that pull, and
     * this one does not. A directional upgrade can still come later to aim it.
     *
     * <p><b>What it will not push into.</b> Other energy drawers, controllers and extensions are
     * skipped. All of them accept energy, so without the guard a wall would shuffle the same FE
     * between its own blocks forever, and a drawer next to its controller would push into the
     * aggregate that is itself.
     */
    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state, EnergyDrawerTile tile) {
        super.serverTick(level, pos, state, tile);

        if (!EnergyScaling.pushesToNeighbours()) {
            return;
        }
        // Offset by position so a wall of drawers does not all scan on the same tick.
        if ((level.getGameTime() + pos.asLong()) % EnergyScaling.pushIntervalTicks() != 0) {
            return;
        }
        if (energyStorage.getStoredRaw() <= 0 && !isCreative()) {
            return;
        }
        pushToNeighbours(level, pos);
    }

    private void pushToNeighbours(Level level, BlockPos pos) {
        int budget = EnergyScaling.transferPerOperation(energyStorage.getCapacityRaw());

        for (Direction side : Direction.values()) {
            if (budget <= 0) {
                return;
            }
            BlockPos target = pos.relative(side);
            BlockEntity neighbour = level.getBlockEntity(target);
            if (neighbour instanceof EnergyDrawerTile
                    || neighbour instanceof StorageControllerTile<?>
                    || neighbour instanceof StorageControllerExtensionTile<?>) {
                continue;
            }

            IEnergyStorage other = level.getCapability(
                    Capabilities.EnergyStorage.BLOCK, target, side.getOpposite());
            if (other == null || !other.canReceive()) {
                continue;
            }

            // Offered, then taken: receiveEnergy reports what it actually accepted, so the drawer
            // only loses what arrived. Doing it the other way round drops FE whenever a machine
            // fills up mid-transfer.
            int accepted = other.receiveEnergy(budget, false);
            if (accepted > 0) {
                energyStorage.extractEnergy(accepted, false);
                budget -= accepted;
            }
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
    public EnergyDrawerTile getSelf() {
        return this;
    }

    public BigEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    /**
     * The four storage-upgrade slots, and the rules for what may go in and come back out.
     *
     * <p>Modelled on {@code FluidDrawerTile.getStorageUpgradesConstructor()}, with our component in
     * place of theirs. Three things are wired here and each one is load-bearing:
     *
     * <ul>
     *   <li><b>The input filter</b> decides which upgrades the slot will take at all. Anything
     *       carrying our component, plus the Iron downgrade and the Creative upgrade, which mean
     *       something to every drawer.</li>
     *   <li><b>The extraction guard</b> refuses to hand back an upgrade whose removal would shrink
     *       the drawer below what it currently holds. Without it, pulling an upgrade deletes
     *       energy, and the drawer looks like it ate it.</li>
     *   <li><b>The change hook</b> resizes the storage. Without it, the upgrades are decoration.</li>
     * </ul>
     */
    @Override
    public InventoryComponent<ControllableDrawerTile<EnergyDrawerTile>> getStorageUpgradesConstructor() {
        return new InventoryComponent<ControllableDrawerTile<EnergyDrawerTile>>(
                "storage_upgrades", 10, 70, getStorageSlotAmount()) {
            @NotNull
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (isStorageUpgradeLocked()) {
                    return ItemStack.EMPTY;
                }
                if (this.getStackInSlot(slot).has(IDComponents.ENERGY_STORAGE_MODIFIER.get())) {
                    // What the multiplier would be with this slot emptied. `replacement` is how
                    // SizeProvider models a hypothetical: a null entry means "read the real slot".
                    ItemStack[] replacement = new ItemStack[this.getSlots()];
                    replacement[slot] = ItemStack.EMPTY;

                    float newSize = SizeProvider.calculateAsFactor(
                            this, IDComponents.ENERGY_STORAGE_MODIFIER, baseSize, replacement);
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
                    // instance of the same version drift, see CLAUDE.md §11 - so neither this
                    // drawer nor theirs has the guard on 1.5.7, and adding our own would make the
                    // energy drawer behave differently from the one next to it in the wall.
                    // Restore it the day the floor moves to a release that has it.
                    //
                    // The Iron downgrade means "reset this drawer to its base size", which is as
                    // meaningful for energy as for anything else. Kept as its own case because
                    // Functional Storage special-cases it too, so a future release that changes
                    // what the downgrade carries will not silently ban it here.
                    if (stack.getItem().equals(
                            FunctionalStorage.STORAGE_UPGRADES.get(StorageUpgradeItem.StorageTier.IRON).get())) {
                        return true;
                    }
                    return stack.has(IDComponents.ENERGY_STORAGE_MODIFIER.get())
                            || stack.is(FunctionalStorage.CREATIVE_UPGRADE);
                })
                .setOnSlotChanged((stack, slot) -> {
                    setNeedsUpgradeCache(true);
                    this.energyStorage.setCapacity(EnergyScaling.capacityFor(getStorageMultiplier()));
                    syncObject(this.energyStorage);
                })
                .setSlotLimit(1);
    }

    /**
     * Whether the drawer can be resized to this multiplier without losing anything.
     *
     * <p>Asked before an upgrade is allowed out of its slot. The raw stored amount, not
     * {@code getEnergyStored()} - a creative drawer reports {@link Integer#MAX_VALUE} and would
     * refuse to give any upgrade back, forever.
     */
    protected boolean canChangeMultiplier(double newSizeMultiplier) {
        return energyStorage.getStoredRaw() <= EnergyScaling.capacityFor(newSizeMultiplier);
    }

    /**
     * Locking is a no-op beyond the block state, and that is the correct behaviour.
     *
     * <p>A locked item or fluid drawer keeps its assigned content type when emptied. Energy has one
     * content type, so an energy drawer is never in the state locking exists to prevent. The
     * override exists to say so — {@code FluidDrawerTile} does real work here, and the next person
     * to read both will want to know this omission was a decision.
     */
    @Override
    public void setLocked(boolean locked) {
        super.setLocked(locked);
    }

    /**
     * Whether breaking this drawer may throw its state away.
     *
     * <p>{@code Drawer.copyTo} writes the tile's NBT into the dropped item only when this is false.
     * Inherited from {@code ItemControllableDrawerTile} it would consult the deliberately empty item
     * handler and answer "empty" for a drawer holding two billion FE, which then evaporates when a
     * player picks it up. Raw storage again: {@code getEnergyStored()} lies for a creative drawer.
     */
    @Override
    public boolean isEverythingEmpty() {
        return energyStorage.getStoredRaw() == 0 && super.isEverythingEmpty();
    }

    @Override
    public boolean isInventoryEmpty() {
        return energyStorage.getStoredRaw() == 0;
    }
}
