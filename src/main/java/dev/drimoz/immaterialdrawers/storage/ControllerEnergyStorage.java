package dev.drimoz.immaterialdrawers.storage;

import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.ArrayList;
import java.util.List;

/**
 * A Storage Controller's energy: the sum of every energy drawer on its network.
 *
 * <p>This is the half of CLAUDE.md §7 that was planned and not built until someone tried to use it.
 * Functional Storage's controller aggregates the item handlers and the fluid handlers of its
 * network and exposes them as one inventory and one tank. It cannot aggregate ours, because it has
 * never heard of us: it collects our deliberately empty item handler, finds nothing in it, and has
 * no third kind of content to look for.
 *
 * <p>What it does give us is the network itself. {@code StorageControllerTile.getConnectedDrawers()}
 * and {@code ConnectedDrawers.getConnectedDrawers()} are both public, so the list of linked
 * positions is readable from outside and the aggregation can be done here instead. Registering this
 * against <em>their</em> block entity type is allowed — NeoForge never asks who owns the type — so a
 * cable on the controller reaches the whole wall.
 *
 * <p><b>What this does not fix.</b> The controller's own screen still shows items and fluids and no
 * energy. That panel is built from screen addons on their tile and there is no hook for a fourth
 * one; adding it would mean a mixin into a mod with 56M downloads, which §7 rules out. Jade reads
 * this capability and shows the total, and so does any cable, meter or machine. What is missing is
 * a panel in their GUI, not the energy.
 *
 * <p>Nothing is cached. The drawer list changes whenever the network rebuilds, and a stale reference
 * to a removed drawer is a leak that voids energy; walking the positions costs one lookup per
 * drawer, on an operation that already touches every one of them.
 */
public class ControllerEnergyStorage implements IEnergyStorage {

    private final StorageControllerTile<?> controller;

    public ControllerEnergyStorage(StorageControllerTile<?> controller) {
        this.controller = controller;
    }

    private List<EnergyDrawerTile> drawers() {
        Level level = controller.getLevel();
        if (level == null) {
            return List.of();
        }
        List<EnergyDrawerTile> found = new ArrayList<>();
        for (Long packed : controller.getConnectedDrawers().getConnectedDrawers()) {
            BlockPos pos = BlockPos.of(packed);
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof EnergyDrawerTile drawer) {
                found.add(drawer);
            }
        }
        return found;
    }

    /**
     * Fills the drawers in network order, which is the order the controller itself keeps them in —
     * by priority, then by distance. A player who set a priority on a drawer expects it to mean
     * something here too.
     */
    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        int accepted = 0;
        for (EnergyDrawerTile drawer : drawers()) {
            accepted += drawer.getEnergyStorage().receiveEnergy(toReceive - accepted, simulate);
            if (accepted >= toReceive) {
                break;
            }
        }
        return accepted;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        int extracted = 0;
        for (EnergyDrawerTile drawer : drawers()) {
            extracted += drawer.getEnergyStorage().extractEnergy(toExtract - extracted, simulate);
            if (extracted >= toExtract) {
                break;
            }
        }
        return extracted;
    }

    /**
     * The whole network's contents, summed in full.
     *
     * <p>Summing the drawers' <em>clamped</em> int views would cap each drawer at 2.1B before the
     * addition even started, so a wall of maxed drawers would read as a handful of them. The
     * addition runs on the long values and saturates once, at the end.
     */
    public long getStoredLong() {
        long total = 0;
        for (EnergyDrawerTile drawer : drawers()) {
            total = saturatedAdd(total, drawer.getEnergyStorage().getStoredLong());
        }
        return total;
    }

    /** The whole network's capacity, summed the same way. */
    public long getCapacityLong() {
        long total = 0;
        for (EnergyDrawerTile drawer : drawers()) {
            total = saturatedAdd(total, drawer.getEnergyStorage().getCapacityLong());
        }
        return total;
    }

    /**
     * Clamped to int for the capability, and knowingly wrong above 2.1B — the same trade
     * {@code BigEnergyStorage} makes, for the same reason. {@link #getStoredLong()} is the truth.
     */
    @Override
    public int getEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, getStoredLong());
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, getCapacityLong());
    }

    /**
     * A long is enormous, but a network is unbounded: enough creative drawers, each reporting
     * {@link Long#MAX_VALUE}, and a plain sum wraps negative. Saturating costs one comparison per
     * drawer and removes the question.
     */
    private static long saturatedAdd(long running, long addition) {
        long sum = running + addition;
        return sum < running ? Long.MAX_VALUE : sum;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
