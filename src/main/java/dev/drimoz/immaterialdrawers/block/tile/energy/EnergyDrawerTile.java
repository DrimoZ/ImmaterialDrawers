package dev.drimoz.immaterialdrawers.block.tile.energy;

import com.buuz135.functionalstorage.block.tile.DrawerProperties;
import com.buuz135.functionalstorage.item.component.SizeProvider;
import com.hrznstudio.titanium.annotation.Save;
import com.hrznstudio.titanium.block.BasicTileBlock;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.tile.ImmaterialDrawerTile;
import dev.drimoz.immaterialdrawers.client.gui.EnergyDrawerInfoGuiAddon;
import dev.drimoz.immaterialdrawers.registry.IDComponents;
import dev.drimoz.immaterialdrawers.storage.BigEnergyStorage;
import dev.drimoz.immaterialdrawers.storage.ControllerEnergyStorage;
import dev.drimoz.immaterialdrawers.storage.EnergyScaling;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.function.Supplier;

/**
 * A drawer that holds Forge Energy.
 *
 * <p>Everything about being a drawer Functional Storage does not know about - why this is an item
 * drawer with no item slots, the storage-upgrade slots, the 1.5.7 chunk-load fix - lives in
 * {@link ImmaterialDrawerTile}. What is here is what is particular to energy: a storage that is
 * {@code long} inside and {@code int} at the capability, and a drawer that pushes, because nothing
 * in the Forge Energy ecosystem pulls.
 */
public class EnergyDrawerTile extends ImmaterialDrawerTile<EnergyDrawerTile> {

    @Save
    public BigEnergyStorage energyStorage;

    /**
     * One capability cache per side, built on first push and kept for the life of the tile.
     * Not saved: it is a view of the world, not state.
     */
    private BlockCapabilityCache<IEnergyStorage, Direction>[] neighbourCaches;

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
                updateComparators();
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
        // Offset by position so a wall of drawers does not all scan on the same tick. floorMod
        // because pos.asLong() is very often negative.
        if (Math.floorMod(level.getGameTime() + pos.asLong(), EnergyScaling.pushIntervalTicks()) != 0) {
            return;
        }
        if (energyStorage.getStoredRaw() <= 0 && !isCreative()) {
            return;
        }
        if (level instanceof ServerLevel serverLevel) {
            pushToNeighbours(serverLevel, pos);
        }
    }

    /**
     * Hands out at most what the drawer is actually holding.
     *
     * <p><b>The clamp is the whole method.</b> An earlier version offered a budget derived from
     * <em>capacity</em> and then took back only what it had. {@code receiveEnergy} is committed, so
     * a drawer holding 1 FE would hand a neighbour 2,500 and lose 1: not a rounding error, a
     * generator. There is a game test for it now, because the reason it shipped is that there was
     * not one.
     *
     * <p>Neighbours are read through {@link BlockCapabilityCache} rather than looked up every time.
     * Six {@code getBlockEntity} plus six {@code getCapability} per drawer per push is around 75
     * lookups a tick on the fifty-drawer wall the tests exercise, and CLAUDE.md §7 exists because
     * that kind of per-tick cost is what kills a server running a wall of these.
     *
     * <p>Our own blocks are skipped by looking at the storage object rather than the block entity —
     * a drawer, a controller and an extension all hand back one of our two storage classes. Without
     * that, a wall would shuffle the same FE between its own blocks forever.
     */
    private void pushToNeighbours(ServerLevel level, BlockPos pos) {
        int budget = (int) Math.min(
                EnergyScaling.transferPerOperation(energyStorage.getCapacityRaw()),
                energyStorage.availableToGive());
        if (budget <= 0) {
            return;
        }
        ensureNeighbourCaches(level, pos);

        for (Direction side : Direction.values()) {
            if (budget <= 0) {
                return;
            }
            IEnergyStorage other = neighbourCaches[side.ordinal()].getCapability();
            if (other == null || !other.canReceive()
                    || other instanceof BigEnergyStorage
                    || other instanceof ControllerEnergyStorage) {
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

    private void ensureNeighbourCaches(ServerLevel level, BlockPos pos) {
        if (neighbourCaches != null) {
            return;
        }
        @SuppressWarnings("unchecked")
        BlockCapabilityCache<IEnergyStorage, Direction>[] caches =
                new BlockCapabilityCache[Direction.values().length];
        for (Direction side : Direction.values()) {
            caches[side.ordinal()] = BlockCapabilityCache.create(
                    Capabilities.EnergyStorage.BLOCK, level, pos.relative(side), side.getOpposite());
        }
        this.neighbourCaches = caches;
    }

    @Override
    public EnergyDrawerTile getSelf() {
        return this;
    }

    public BigEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    /** Ours, not Functional Storage's fluid one - see {@code IDComponents.ENERGY_STORAGE_MODIFIER}. */
    @Override
    protected Supplier<DataComponentType<SizeProvider>> storageModifier() {
        return IDComponents.ENERGY_STORAGE_MODIFIER;
    }

    @Override
    protected void onStorageMultiplierChanged() {
        this.energyStorage.setCapacity(EnergyScaling.capacityFor(getStorageMultiplier()));
        syncObject(this.energyStorage);
    }

    /** Raw storage: {@code getEnergyStored()} reports {@link Integer#MAX_VALUE} for a creative drawer. */
    @Override
    protected boolean canChangeMultiplier(double newSizeMultiplier) {
        return energyStorage.getStoredRaw() <= EnergyScaling.capacityFor(newSizeMultiplier);
    }

    @Override
    protected boolean hasContents() {
        return energyStorage.getStoredRaw() != 0;
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
}
