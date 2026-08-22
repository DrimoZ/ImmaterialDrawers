package dev.drimoz.immaterialdrawers.storage;

import com.buuz135.functionalstorage.block.config.FunctionalStorageConfig;

/**
 * How big an energy drawer gets, and why it stops there.
 *
 * <h3>The ceiling is long, not int</h3>
 *
 * <p>{@link net.neoforged.neoforge.energy.IEnergyStorage} is an {@code int} API from end to end, so
 * anything speaking it is capped at {@link Integer#MAX_VALUE} = 2,147,483,647 FE. Every mod that
 * stores more than that keeps a wider number internally and clamps at the boundary — Powah's own
 * cable is declared {@code receiveEnergy(long, boolean, Direction)} and reaches the standard
 * capability only through an adapter, which is how a Nitro Ender Cell advertises 18 billion FE.
 *
 * <p>This mod does the same. Storage is {@code long}: 9,223,372,036,854,775,807 FE, about four
 * billion times the int ceiling and more than any upgrade curve can reach. {@code BigInteger} would
 * be unbounded, and is not worth an allocation per operation for headroom that is already absurd.
 *
 * <p><b>What the clamp costs.</b> A cable, a meter or Jade reading the standard capability sees at
 * most 2.1B, because that is all an int can say. Powah accepts exactly that trade and shows the
 * true figure in its own screens; so do we — {@code BigEnergyStorage} exposes long accessors, and
 * every display in this mod uses them. Transfers are unaffected: nothing moves two billion FE in a
 * single operation.
 *
 * <p>This reverses what CLAUDE.md §11 originally decided. That entry rejected long-with-clamp on
 * the grounds that {@code getEnergyStored()} would lie to cables. It does lie. The alternative was
 * a storage block smaller than the cells of the mod sitting next to it.
 *
 * <h3>The curve</h3>
 *
 * <pre>
 *   base    = BASE_UNITS x FE_PER_UNIT  = 500 x 1,000  =        500,000 FE
 *   factor  = (NETHERITE / DIVISOR)^4   = (32 / 2)^4    =         65,536
 *   maximum = base x factor                             = 32,768,000,000 FE
 * </pre>
 *
 * <p>{@link #ENERGY_DIVISOR} is 2, the value Functional Storage gives fluids. It used to be 4, and
 * that number existed for exactly one reason: squeezing four Netherite upgrades under the int
 * ceiling. With the ceiling gone the constraint is balance rather than arithmetic, and matching the
 * sibling content type is the honest default — a fully upgraded energy drawer holds about 32.8B FE,
 * in the region of a couple of Powah Nitro cells, which is where an endgame wall of them belongs.
 */
public final class EnergyScaling {

    /**
     * Divides every storage upgrade's multiplier for energy, exactly as
     * {@code FunctionalStorageConfig.FLUID_DIVISOR} does for fluids — and now with the same value,
     * since energy is no longer fighting for room inside an int.
     */
    public static final int ENERGY_DIVISOR = 2;

    /**
     * Base size of an unupgraded drawer, in units — this is what goes into {@code DrawerProperties}
     * and what the upgrades multiply. Kept separate from {@link #FE_PER_UNIT} because the upgrade
     * arithmetic runs in floats: multiplying a small number and scaling once at the end loses far
     * less than carrying 500,000 through four float multiplications.
     */
    public static final int BASE_UNITS = 500;

    /** FE per unit. Mirrors the 1,000 mB per unit a fluid drawer uses. */
    public static final int FE_PER_UNIT = 1_000;

    private EnergyScaling() {
    }

    /**
     * Turns the drawer's storage multiplier into a capacity in FE.
     *
     * <p>Still clamped, just much further out. The Max Storage upgrade reports a multiplier of
     * {@link Integer#MAX_VALUE} ({@code FunctionalStorageConfig.getLevelMult(-1)}); multiplied by
     * the base and by FE_PER_UNIT that overflows even a long, so the result is capped at
     * {@link Long#MAX_VALUE} rather than allowed to wrap into a negative capacity — which would be
     * a drawer that refuses every FE offered to it.
     */
    public static long capacityFor(double storageMultiplier) {
        double capacity = storageMultiplier * FE_PER_UNIT;
        if (capacity >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return (long) Math.floor(capacity);
    }

    /**
     * The factor one storage upgrade of the given tier contributes to an energy drawer.
     *
     * @param levelMultiplier the tier's item multiplier, from
     *                        {@link FunctionalStorageConfig#getLevelMult(int)}
     */
    public static float upgradeFactor(int levelMultiplier) {
        return levelMultiplier / (float) ENERGY_DIVISOR;
    }
}
