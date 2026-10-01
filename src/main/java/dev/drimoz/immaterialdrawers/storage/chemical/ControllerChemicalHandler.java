package dev.drimoz.immaterialdrawers.storage.chemical;

import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import dev.drimoz.immaterialdrawers.block.tile.chemical.ChemicalDrawerTile;
import dev.drimoz.immaterialdrawers.storage.ControllerNetwork;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A Storage Controller's chemicals: every tank of every chemical drawer on its network, as one
 * handler.
 *
 * <p><b>Mekanism only - see {@code compat.Mods}.</b>
 *
 * <p>The chemical counterpart of Functional Storage's {@code fluid/ControllerFluidHandler}, which
 * their controller builds for fluid drawers and cannot build for ours - it collects our empty item
 * handler and has no fourth kind of content to look for. Same arrangement as
 * {@code ControllerEnergyStorage}: the network's positions are public, so the aggregation is done
 * here and registered against <em>their</em> block entity type. A tube on the controller reaches the
 * whole wall.
 *
 * <p>Filling follows theirs exactly: the first tank already holding the chemical, then the first
 * empty one, one tank per call. The tanks are listed in network order, which is priority then
 * distance, so a drawer given a priority fills first here too.
 *
 * <p><b>Why this one caches and the energy one does not.</b> Energy is asked for as one total. A
 * chemical handler is asked tank by tank - a pressurized tube pulling from a controller walks
 * {@code getChemicalTanks()} and calls into each index - so walking the network per call would be one
 * walk per tank, per tube, per tick: fifty drawers of four tanks is ten thousand block entity
 * lookups a tick for one tube. The tank list is therefore kept for the rest of the tick it was built
 * in, and dropped early when the controller rebuilds its network (which replaces its handler list,
 * so the identity of that list is the version stamp). A tank whose drawer was removed in between is
 * skipped rather than trusted.
 */
public final class ControllerChemicalHandler implements IChemicalHandler {

    /**
     * One handler per controller, for the same reasons as {@code ControllerEnergyStorage.CACHE}:
     * NeoForge's capability cache holds on to what a provider returns, weak keys so an unloaded
     * controller is not kept alive by us, synchronised because probe mods do not promise a thread.
     */
    private static final Map<StorageControllerTile<?>, ControllerChemicalHandler> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final StorageControllerTile<?> controller;

    private List<Tank> tanks = List.of();
    private long builtAt = Long.MIN_VALUE;
    private Object builtFrom;

    private record Tank(ChemicalDrawerTile drawer, int index) {
        BigChemicalHandler handler() {
            return drawer.getChemicalHandler();
        }
    }

    private ControllerChemicalHandler(StorageControllerTile<?> controller) {
        this.controller = controller;
    }

    public static ControllerChemicalHandler of(StorageControllerTile<?> controller) {
        return CACHE.computeIfAbsent(controller, ControllerChemicalHandler::new);
    }

    private synchronized List<Tank> tanks() {
        Level level = controller.getLevel();
        if (level == null) {
            return List.of();
        }
        Object network = controller.getConnectedDrawers().getItemHandlers();
        if (builtAt == level.getGameTime() && builtFrom == network) {
            return tanks;
        }
        List<Tank> found = new ArrayList<>();
        for (ChemicalDrawerTile drawer : ControllerNetwork.drawersOf(controller, ChemicalDrawerTile.class)) {
            for (int index = 0; index < drawer.getChemicalHandler().getChemicalTanks(); index++) {
                found.add(new Tank(drawer, index));
            }
        }
        this.tanks = found;
        this.builtAt = level.getGameTime();
        this.builtFrom = network;
        return found;
    }

    private Tank tank(int index) {
        List<Tank> all = tanks();
        if (index < 0 || index >= all.size()) {
            return null;
        }
        Tank tank = all.get(index);
        return tank.drawer().isRemoved() ? null : tank;
    }

    @Override
    public int getChemicalTanks() {
        return tanks().size();
    }

    @Override
    public ChemicalStack getChemicalInTank(int index) {
        Tank tank = tank(index);
        return tank == null ? ChemicalStack.EMPTY : tank.handler().getChemicalInTank(tank.index());
    }

    @Override
    public void setChemicalInTank(int index, ChemicalStack stack) {
        Tank tank = tank(index);
        if (tank != null) {
            tank.handler().setChemicalInTank(tank.index(), stack);
        }
    }

    @Override
    public long getChemicalTankCapacity(int index) {
        Tank tank = tank(index);
        return tank == null ? 0 : tank.handler().getChemicalTankCapacity(tank.index());
    }

    @Override
    public boolean isValid(int index, ChemicalStack stack) {
        Tank tank = tank(index);
        return tank != null && tank.handler().isValid(tank.index(), stack);
    }

    @Override
    public ChemicalStack insertChemical(int index, ChemicalStack stack, Action action) {
        Tank tank = tank(index);
        return tank == null ? stack : tank.handler().insertChemical(tank.index(), stack, action);
    }

    @Override
    public ChemicalStack extractChemical(int index, long amount, Action action) {
        Tank tank = tank(index);
        return tank == null ? ChemicalStack.EMPTY : tank.handler().extractChemical(tank.index(), amount, action);
    }

    /** {@code ControllerFluidHandler.fill}: a tank already holding it, then an empty one. */
    @Override
    public ChemicalStack insertChemical(ChemicalStack stack, Action action) {
        if (stack.isEmpty()) {
            return ChemicalStack.EMPTY;
        }
        for (int pass = 0; pass < 2; pass++) {
            for (Tank tank : tanks()) {
                if (tank.drawer().isRemoved()) {
                    continue;
                }
                ChemicalStack held = tank.handler().getChemicalInTank(tank.index());
                boolean candidate = pass == 0
                        ? !held.isEmpty() && ChemicalStack.isSameChemical(held, stack)
                        : held.isEmpty();
                if (candidate && tank.handler().insertChemical(tank.index(), stack, Action.SIMULATE)
                        .getAmount() < stack.getAmount()) {
                    return tank.handler().insertChemical(tank.index(), stack, action);
                }
            }
        }
        return stack;
    }

    @Override
    public ChemicalStack extractChemical(long amount, Action action) {
        for (Tank tank : tanks()) {
            if (!tank.drawer().isRemoved() && !tank.handler().getChemicalInTank(tank.index()).isEmpty()) {
                return tank.handler().extractChemical(tank.index(), amount, action);
            }
        }
        return ChemicalStack.EMPTY;
    }

    @Override
    public ChemicalStack extractChemical(ChemicalStack stack, Action action) {
        if (stack.isEmpty()) {
            return ChemicalStack.EMPTY;
        }
        for (Tank tank : tanks()) {
            if (!tank.drawer().isRemoved()
                    && ChemicalStack.isSameChemical(tank.handler().getChemicalInTank(tank.index()), stack)) {
                return tank.handler().extractChemical(tank.index(), stack.getAmount(), action);
            }
        }
        return ChemicalStack.EMPTY;
    }
}
