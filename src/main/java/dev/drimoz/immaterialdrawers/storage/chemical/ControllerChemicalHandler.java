package dev.drimoz.immaterialdrawers.storage.chemical;

import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import dev.drimoz.immaterialdrawers.block.tile.chemical.ChemicalDrawerTile;
import dev.drimoz.immaterialdrawers.storage.ControllerNetwork;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.gas.GasStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Every chemical tank on a Storage Controller's network, as one set of tanks. <b>Mekanism only.</b>
 *
 * <p>The same arrangement as {@code ControllerEnergyStorage}: the network's positions are public, so
 * we read them and walk our drawers; attached to their controller by {@code IDChemicalContent}. The
 * tank list is cached for the current tick and rebuilt when the network is ({@code getItemHandlers()}
 * is replaced on every rebuild): a pipe asks tank by tank, and walking the wall on every call would be
 * thousands of lookups per tick per pipe.
 */
public final class ControllerChemicalHandler extends ChemicalTanks {

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

    private synchronized List<Tank> list() {
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
            for (int index = 0; index < drawer.getChemicalHandler().tanks(); index++) {
                found.add(new Tank(drawer, index));
            }
        }
        this.tanks = found;
        this.builtAt = level.getGameTime();
        this.builtFrom = network;
        return found;
    }

    private Tank tank(int index) {
        List<Tank> all = list();
        if (index < 0 || index >= all.size()) {
            return null;
        }
        Tank tank = all.get(index);
        return tank.drawer().isRemoved() ? null : tank;
    }

    @Override
    public int tanks() {
        return list().size();
    }

    @Override
    public ChemicalStack<?> stored(int index) {
        Tank tank = tank(index);
        return tank == null ? GasStack.EMPTY : tank.handler().stored(tank.index());
    }

    @Override
    public long capacity(int index) {
        Tank tank = tank(index);
        return tank == null ? 0 : tank.handler().capacity(tank.index());
    }

    @Override
    public boolean isValid(int index, ChemicalStack<?> stack) {
        Tank tank = tank(index);
        return tank != null && tank.handler().isValid(tank.index(), stack);
    }

    @Override
    public ChemicalStack<?> insert(int index, ChemicalStack<?> stack, Action action) {
        Tank tank = tank(index);
        return tank == null ? stack : tank.handler().insert(tank.index(), stack, action);
    }

    @Override
    public ChemicalStack<?> extract(int index, long amount, Action action) {
        Tank tank = tank(index);
        return tank == null ? GasStack.EMPTY : tank.handler().extract(tank.index(), amount, action);
    }

    @Override
    public void set(int index, ChemicalStack<?> stack) {
        Tank tank = tank(index);
        if (tank != null) {
            tank.handler().set(tank.index(), stack);
        }
    }
}
