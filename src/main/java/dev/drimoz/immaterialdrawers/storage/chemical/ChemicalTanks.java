package dev.drimoz.immaterialdrawers.storage.chemical;

import mekanism.api.Action;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.ChemicalType;
import mekanism.api.chemical.IChemicalHandler;
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

    /** One type's window on the tanks. */
    private abstract class View<C extends Chemical<C>, S extends ChemicalStack<C>> implements IChemicalHandler<C, S> {

        private final ChemicalType type;

        View(ChemicalType type) {
            this.type = type;
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
