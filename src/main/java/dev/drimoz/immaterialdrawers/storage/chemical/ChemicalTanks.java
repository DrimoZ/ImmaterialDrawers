package dev.drimoz.immaterialdrawers.storage.chemical;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.ChemicalType;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.IMekanismChemicalHandler;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.chemical.infuse.IInfusionHandler;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.chemical.pigment.IPigmentHandler;
import mekanism.api.chemical.pigment.Pigment;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.api.chemical.slurry.ISlurryHandler;
import mekanism.api.chemical.slurry.Slurry;
import mekanism.api.chemical.slurry.SlurryStack;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Tanks that hold any of Mekanism 10.4's four chemical types, seen through four typed handlers.
 * <b>Mekanism only</b> - see {@code compat.Mods}.
 *
 * <p><b>Why four views.</b> Mekanism 10.7 (1.21.1) has one chemical API; 10.4 (1.20.1) has four -
 * gases, infusion types, pigments, slurries - each with its own handler and capability (PORTING.md
 * §3). One drawer holds any of them, the way Mekanism's own tanks do, so a slot holds a stack of
 * whatever type came first, and each view shows only its own type: a slot taken by another type is
 * an empty tank of capacity 0 to it, so a pipe moves on.
 *
 * <p>Subclasses implement the operations on untyped stacks; the views are written once, here, for
 * both a drawer ({@link BigChemicalHandler}) and a controller's network ({@link ControllerChemicalHandler}).
 */
public abstract class ChemicalTanks {

    public final IGasHandler gas = new GasView();
    public final IInfusionHandler infusion = new InfusionView();
    public final IPigmentHandler pigment = new PigmentView();
    public final ISlurryHandler slurry = new SlurryView();

    public abstract int tanks();

    /** What a tank shows - a creative drawer shows a full one. Any type, or an empty stack. */
    public abstract ChemicalStack<?> stored(int tank);

    public abstract long capacity(int tank);

    public abstract boolean isValid(int tank, ChemicalStack<?> stack);

    /** Returns what did not fit. */
    public abstract ChemicalStack<?> insert(int tank, ChemicalStack<?> stack, Action action);

    public abstract ChemicalStack<?> extract(int tank, long amount, Action action);

    public abstract void set(int tank, ChemicalStack<?> stack);

    public IChemicalHandler<?, ?> view(ChemicalType type) {
        return switch (type) {
            case GAS -> gas;
            case INFUSION -> infusion;
            case PIGMENT -> pigment;
            case SLURRY -> slurry;
        };
    }

    /** Tanks already holding this chemical first, then empty ones - Functional Storage's fill order. */
    public ChemicalStack<?> insertAny(ChemicalStack<?> stack, Action action) {
        if (stack.isEmpty()) {
            return stack;
        }
        for (int pass = 0; pass < 2; pass++) {
            for (int tank = 0; tank < tanks(); tank++) {
                ChemicalStack<?> held = stored(tank);
                boolean candidate = pass == 0 ? same(held, stack) : held.isEmpty();
                if (candidate && insert(tank, stack, Action.SIMULATE).getAmount() < stack.getAmount()) {
                    return insert(tank, stack, action);
                }
            }
        }
        return stack;
    }

    public static <C extends Chemical<C>> ChemicalStack<C> copyWithAmount(ChemicalStack<C> stack, long amount) {
        ChemicalStack<C> copy = stack.copy();
        copy.setAmount(amount);
        return copy;
    }

    /** The same chemical - across types, which can never match. */
    public static boolean same(ChemicalStack<?> a, ChemicalStack<?> b) {
        return !a.isEmpty() && !b.isEmpty() && a.getType() == b.getType();
    }

    /**
     * One type's window on the tanks.
     *
     * <p><b>Why it is an {@link IMekanismChemicalHandler}.</b> For a chemical block that is not one of
     * its own, Mekanism's Jade / TOP / WTHIT integration walks each of the four typed capabilities and
     * shows every tank of each - empty ones included - so a 2x2 drawer read as sixteen rows, mostly
     * "Empty". For an {@code IMekanismChemicalHandler} it shows {@link #getChemicalTanks} instead, and
     * that list is display only (checked in the 10.4 jar: outside Mekanism's own handlers, only
     * {@code LookingAtUtils} and the Dropper - which works on Mekanism's tiles - read it). So the list
     * is one row per slot across the four views: each view lists the slots holding its type, and the
     * gas view also lists the empty ones. Every sided method is redirected to the slot-indexed ones
     * below, so pipes see exactly what they saw before.
     */
    private abstract class View<C extends Chemical<C>, S extends ChemicalStack<C>>
            implements IMekanismChemicalHandler<C, S, IChemicalTank<C, S>> {

        private final ChemicalType type;

        View(ChemicalType type) {
            this.type = type;
        }

        @Override
        public List<IChemicalTank<C, S>> getChemicalTanks(@Nullable Direction side) {
            List<IChemicalTank<C, S>> shown = new ArrayList<>();
            for (int tank = 0; tank < tanks(); tank++) {
                ChemicalStack<?> held = stored(tank);
                if (mine(held) || (type == ChemicalType.GAS && held.isEmpty())) {
                    shown.add(new SlotTank(tank));
                }
            }
            return shown;
        }

        @Override
        public IChemicalTank<C, S> getChemicalTank(int tank, @Nullable Direction side) {
            return tank >= 0 && tank < tanks() ? new SlotTank(tank) : null;
        }

        @Override
        public void onContentsChanged() {
            // The drawer marks itself dirty and syncs on every change already.
        }

        // ---- The sided half, redirected to the slot-indexed half: a side changes nothing here. ----

        @Override
        public int getTanks(@Nullable Direction side) {
            return getTanks();
        }

        @Override
        public S getChemicalInTank(int tank, @Nullable Direction side) {
            return getChemicalInTank(tank);
        }

        @Override
        public void setChemicalInTank(int tank, S stack, @Nullable Direction side) {
            setChemicalInTank(tank, stack);
        }

        @Override
        public long getTankCapacity(int tank, @Nullable Direction side) {
            return getTankCapacity(tank);
        }

        @Override
        public boolean isValid(int tank, S stack, @Nullable Direction side) {
            return isValid(tank, stack);
        }

        @Override
        public S insertChemical(int tank, S stack, @Nullable Direction side, Action action) {
            return insertChemical(tank, stack, action);
        }

        @Override
        public S extractChemical(int tank, long amount, @Nullable Direction side, Action action) {
            return extractChemical(tank, amount, action);
        }

        @Override
        public S insertChemical(S stack, @Nullable Direction side, Action action) {
            return insertChemical(stack, action);
        }

        @Override
        public S extractChemical(long amount, @Nullable Direction side, Action action) {
            return extractChemical(amount, action);
        }

        @Override
        public S extractChemical(S stack, @Nullable Direction side, Action action) {
            return extractChemical(stack, action);
        }

        /** One slot, seen as a tank of this view's type - what {@link #getChemicalTanks} hands out. */
        private final class SlotTank implements IChemicalTank<C, S> {

            private final int slot;

            SlotTank(int slot) {
                this.slot = slot;
            }

            @Override
            public S getEmptyStack() {
                return View.this.getEmptyStack();
            }

            @SuppressWarnings("unchecked")
            @Override
            public S createStack(S stored, long size) {
                return (S) copyWithAmount(stored, size);
            }

            @Override
            public S getStack() {
                return getChemicalInTank(slot);
            }

            @Override
            public void setStack(S stack) {
                setChemicalInTank(slot, stack);
            }

            @Override
            public void setStackUnchecked(S stack) {
                setChemicalInTank(slot, stack);
            }

            @Override
            public S insert(S stack, Action action, AutomationType automationType) {
                return insertChemical(slot, stack, action);
            }

            @Override
            public S extract(long amount, Action action, AutomationType automationType) {
                return extractChemical(slot, amount, action);
            }

            @Override
            public long getCapacity() {
                return getTankCapacity(slot);
            }

            @Override
            public boolean isValid(S stack) {
                return View.this.isValid(slot, stack);
            }

            @Override
            public void onContentsChanged() {
            }

            @Override
            public void deserializeNBT(CompoundTag nbt) {
                // A view, not a store: the drawer saves its own tanks.
            }
        }

        private boolean mine(ChemicalStack<?> stack) {
            return !stack.isEmpty() && ChemicalType.getTypeFor(stack) == type;
        }

        private boolean open(int tank) {
            ChemicalStack<?> held = stored(tank);
            return held.isEmpty() || mine(held);
        }

        @SuppressWarnings("unchecked")
        private S cast(ChemicalStack<?> stack) {
            return stack == null || stack.isEmpty() || !mine(stack) ? getEmptyStack() : (S) stack;
        }

        @Override
        public int getTanks() {
            return tanks();
        }

        @Override
        public S getChemicalInTank(int tank) {
            return cast(stored(tank));
        }

        @Override
        public void setChemicalInTank(int tank, S stack) {
            set(tank, stack);
        }

        @Override
        public long getTankCapacity(int tank) {
            return open(tank) ? capacity(tank) : 0;
        }

        @Override
        public boolean isValid(int tank, S stack) {
            return ChemicalTanks.this.isValid(tank, stack);
        }

        @Override
        public S insertChemical(int tank, S stack, Action action) {
            if (stack.isEmpty()) {
                return getEmptyStack();
            }
            ChemicalStack<?> left = insert(tank, stack, action);
            return left.isEmpty() ? getEmptyStack() : cast(left);
        }

        @Override
        public S extractChemical(int tank, long amount, Action action) {
            return open(tank) ? cast(extract(tank, amount, action)) : getEmptyStack();
        }

        @Override
        public S insertChemical(S stack, Action action) {
            if (stack.isEmpty()) {
                return getEmptyStack();
            }
            ChemicalStack<?> left = insertAny(stack, action);
            return left.isEmpty() ? getEmptyStack() : cast(left);
        }

        @Override
        public S extractChemical(long amount, Action action) {
            for (int tank = 0; tank < tanks(); tank++) {
                if (mine(stored(tank))) {
                    return cast(extract(tank, amount, action));
                }
            }
            return getEmptyStack();
        }

        @Override
        public S extractChemical(S stack, Action action) {
            if (stack.isEmpty()) {
                return getEmptyStack();
            }
            for (int tank = 0; tank < tanks(); tank++) {
                if (same(stored(tank), stack)) {
                    return cast(extract(tank, stack.getAmount(), action));
                }
            }
            return getEmptyStack();
        }
    }

    private final class GasView extends View<Gas, GasStack> implements IGasHandler {
        GasView() {
            super(ChemicalType.GAS);
        }
    }

    private final class InfusionView extends View<InfuseType, InfusionStack> implements IInfusionHandler {
        InfusionView() {
            super(ChemicalType.INFUSION);
        }
    }

    private final class PigmentView extends View<Pigment, PigmentStack> implements IPigmentHandler {
        PigmentView() {
            super(ChemicalType.PIGMENT);
        }
    }

    private final class SlurryView extends View<Slurry, SlurryStack> implements ISlurryHandler {
        SlurryView() {
            super(ChemicalType.SLURRY);
        }
    }
}
