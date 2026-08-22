package dev.drimoz.immaterialdrawers.storage;

import com.buuz135.functionalstorage.block.config.FunctionalStorageConfig;

/**
 * How big an energy drawer gets, and why it stops there.
 *
 * <p>This is the whole of CLAUDE.md §11 in one place, because the numbers only make sense together
 * and getting them wrong is invisible: the drawer works, the last upgrade slot just silently does
 * nothing.
 *
 * <h3>The ceiling</h3>
 *
 * <p>{@link net.neoforged.neoforge.energy.IEnergyStorage} is an {@code int} API from end to end, so
 * a drawer cannot hold more than {@link Integer#MAX_VALUE} = 2,147,483,647 FE. Storing a
 * {@code long} internally and clamping on the way out is not an option: {@code getEnergyStored}
 * would lie to every cable, meter and tooltip in the game.
 *
 * <p>Functional Storage's storage upgrades are <b>multiplicative</b> and there are four slots, so
 * the worst case is the fourth power of the best upgrade. With Netherite at x32 that is
 * x1,048,576 — anything with a base above ~2,000 FE saturates before the fourth slot is even used.
 *
 * <h3>The divisor</h3>
 *
 * <p>Functional Storage solves the same problem for fluids with {@code FLUID_DIVISOR = 2}: fluid
 * drawers get x16 per Netherite upgrade rather than x32, so 32,000 mB x 16^4 = 2,097,152,000 mB
 * lands just under the ceiling. That is not a coincidence, it is a calibration, and this is the
 * same calibration done for energy:
 *
 * <pre>
 *   base     = BASE_UNITS x FE_PER_UNIT   = 500 x 1,000     =       500,000 FE
 *   factor   = (NETHERITE / DIVISOR)^4    = (32 / 4)^4       =         4,096
 *   maximum  = base x factor                                 = 2,048,000,000 FE
 *   ceiling  = Integer.MAX_VALUE                             = 2,147,483,647 FE
 * </pre>
 *
 * <p>All four upgrade slots do something, and the fourth one still fits with ~5% of headroom. That
 * headroom is deliberate — {@code NETHERITE_MULTIPLIER} is a config value a pack author can raise,
 * and {@link #capacityFor} clamps rather than overflows when they do.
 *
 * <p>A divisor of 2, matching fluids exactly, would have forced a base of ~32,000 FE — smaller than
 * a Powah Basic Energy Cell, which makes the block pointless before it is upgraded. A divisor of 8
 * would leave the top of the curve two thirds empty. Four is the value that uses the whole int.
 */
public final class EnergyScaling {

    /**
     * Divides every storage upgrade's multiplier for energy, exactly as
     * {@link FunctionalStorageConfig#FLUID_DIVISOR} does for fluids.
     *
     * <p>Ours rather than theirs: reusing {@code FLUID_STORAGE_MODIFIER} would have tied energy to
     * the fluid curve and capped the base at ~32,000 FE. See CLAUDE.md §10.
     */
    public static final int ENERGY_DIVISOR = 4;

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
     * <p>Clamped, not wrapped. The Max Storage upgrade reports a multiplier of
     * {@link Integer#MAX_VALUE} ({@code FunctionalStorageConfig.getLevelMult(-1)}), so an unclamped
     * cast here would produce a negative capacity and a drawer that refuses every FE offered to it.
     * The {@code long} in the multiplication is what keeps the {@code min} meaningful.
     */
    public static int capacityFor(double storageMultiplier) {
        return (int) Math.min(Integer.MAX_VALUE, Math.floor(storageMultiplier * (long) FE_PER_UNIT));
    }

    /**
     * The factor one storage upgrade of the given tier contributes to an energy drawer.
     *
     * @param levelMultiplier the tier's item multiplier, from
     *                        {@code FunctionalStorageConfig.getLevelMult}
     */
    public static float upgradeFactor(int levelMultiplier) {
        return levelMultiplier / (float) ENERGY_DIVISOR;
    }
}
