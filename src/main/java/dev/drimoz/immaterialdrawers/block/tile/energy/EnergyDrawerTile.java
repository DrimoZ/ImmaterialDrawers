package dev.drimoz.immaterialdrawers.block.tile.energy;

import com.hrznstudio.titanium.annotation.Save;
import com.hrznstudio.titanium.block.BasicTileBlock;
import dev.drimoz.immaterialdrawers.IDConfig;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.tile.ImmaterialDrawerTile;
import dev.drimoz.immaterialdrawers.client.gui.EnergyDrawerInfoGuiAddon;
import dev.drimoz.immaterialdrawers.storage.BigEnergyStorage;
import dev.drimoz.immaterialdrawers.storage.ControllerEnergyStorage;
import dev.drimoz.immaterialdrawers.storage.EnergyScaling;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A drawer that holds Forge Energy.
 *
 * <p>Everything about being a drawer Functional Storage does not know about lives in
 * {@link ImmaterialDrawerTile}. What is here is what is particular to energy: a storage that is
 * {@code long} inside and {@code int} at the capability, and a drawer that pushes, because nothing
 * in the Forge Energy ecosystem pulls.
 *
 * <p><b>The capability, on Forge.</b> On 1.21.1 the energy capability is a provider registered from
 * outside, because Titanium's own provider answers only for a {@code PoweredTile} and NeoForge walks
 * a list of them (CLAUDE.md §8). Forge 1.20.1 asks the block entity itself, so the tile answers in
 * {@link #getCapability} and that whole problem does not exist here.
 */
public class EnergyDrawerTile extends ImmaterialDrawerTile<EnergyDrawerTile> {

    @Save
    public BigEnergyStorage energyStorage;

    private LazyOptional<IEnergyStorage> energyOptional = LazyOptional.of(this::getEnergyStorage);

    public EnergyDrawerTile(BasicTileBlock<EnergyDrawerTile> base, BlockEntityType<EnergyDrawerTile> entityType,
                            BlockPos pos, BlockState state) {
        super(base, entityType, pos, state);

        this.energyStorage = new BigEnergyStorage(capacity()) {
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
     * The capacity for the current upgrades. Functional Storage's multiplier is only the product of
     * the upgrade factors on 1.20.1 - the base size is ours to apply, in {@code EnergyScaling}'s
     * units.
     */
    private long capacity() {
        return capacityFor(getStorageMultiplier());
    }

    private static long capacityFor(int storageMultiplier) {
        return EnergyScaling.capacityFor((double) EnergyScaling.baseUnits() * storageMultiplier);
    }

    /**
     * The drawer's screen: a tile of the drawer front with the charge, where a fluid drawer's screen
     * shows its tank. Not Titanium's energy bar - that looks like a machine in a wall of drawers.
     */
    @OnlyIn(Dist.CLIENT)
    @Override
    public void initClient() {
        super.initClient();
        addGuiAddonFactory(() -> new EnergyDrawerInfoGuiAddon(64, 16,
                new ResourceLocation(ImmaterialDrawers.MOD_ID, "textures/block/energy_drawer_front.png"),
                this::getEnergyStorage));
    }

    /**
     * Energy's divisor, not the fluids'. On 1.20.1 this one override is the whole of what the
     * {@code energy_storage_modifier} component does on 1.21.1 - see {@code EnergyScaling}.
     */
    @Override
    public double getStorageDiv() {
        return IDConfig.ENERGY_DIVISOR;
    }

    @NotNull
    @Override
    public <U> LazyOptional<U> getCapability(@NotNull Capability<U> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyOptional.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyOptional.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        energyOptional = LazyOptional.of(this::getEnergyStorage);
    }

    /**
     * Hands energy to whatever is next to the drawer that will take it. Nothing in the Forge Energy
     * ecosystem pulls, so a drawer that only waits to be drained is a hole energy goes into - see
     * the 1.21.1 branch for the reading of Powah that this rests on.
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
        pushToNeighbours(level, pos);
    }

    /**
     * Hands out at most what the drawer is actually holding - the clamp is the whole method, and
     * {@code pushingEnergyNeverCreatesIt} is why.
     *
     * <p>Neighbours are looked up at push time, not cached: Forge 1.20.1 has no
     * {@code BlockCapabilityCache}, and at one push every few ticks, staggered across a wall, the
     * lookups are not where a server's time goes.
     */
    private void pushToNeighbours(Level level, BlockPos pos) {
        int budget = (int) Math.min(
                EnergyScaling.transferPerOperation(energyStorage.getCapacityRaw()),
                energyStorage.availableToGive());

        for (Direction side : Direction.values()) {
            if (budget <= 0) {
                return;
            }
            BlockEntity neighbour = level.getBlockEntity(pos.relative(side));
            if (neighbour == null) {
                continue;
            }
            IEnergyStorage other = neighbour.getCapability(ForgeCapabilities.ENERGY, side.getOpposite()).orElse(null);
            // Our own blocks are skipped by storage class: a drawer, a controller and an extension
            // all hand back one of these two, and a wall would otherwise shuffle FE forever.
            if (other == null || !other.canReceive()
                    || other instanceof BigEnergyStorage
                    || other instanceof ControllerEnergyStorage) {
                continue;
            }
            int accepted = other.receiveEnergy(budget, false);
            if (accepted > 0) {
                energyStorage.extractEnergy(accepted, false);
                budget -= accepted;
            }
        }
    }

    @Override
    public EnergyDrawerTile getSelf() {
        return this;
    }

    public BigEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    @Override
    protected void onStorageMultiplierChanged() {
        this.energyStorage.setCapacity(capacity());
        syncObject(this.energyStorage);
    }

    /** Raw storage: {@code getEnergyStored()} reports {@link Integer#MAX_VALUE} for a creative drawer. */
    @Override
    protected boolean canChangeMultiplier(int newStorageMultiplier) {
        return energyStorage.getStoredRaw() <= capacityFor(newStorageMultiplier);
    }

    @Override
    protected boolean hasContents() {
        return energyStorage.getStoredRaw() != 0;
    }
}
