package dev.drimoz.immaterialdrawers.storage;

import com.buuz135.functionalstorage.block.config.FunctionalStorageConfig;
import dev.drimoz.immaterialdrawers.IDConfig;

/**
 * How big an energy drawer gets, how fast it moves energy, and why.
 *
 * <p>The arithmetic lives here; every number it uses lives in {@link IDConfig} and is editable.
 * Nothing below is a constant — reading them through methods is what lets a pack change the curve
 * without a jar edit, and is why none of these values may be cached anywhere.
 *
 * <h3>The ceiling is long, not int</h3>
 *
 * <p>{@link net.neoforged.neoforge.energy.IEnergyStorage} is an {@code int} API from end to end, so
 * anything speaking it is capped at {@link Integer#MAX_VALUE} = 2,147,483,647 FE. Every mod holding
 * more keeps a wider number internally and clamps at the boundary — Powah's cable is declared
 * {@code receiveEnergy(long, boolean, Direction)} and reaches the standard capability only through
 * an adapter, which is how a Nitro Ender Cell advertises 18 billion FE.
 *
 * <p>Storage here is {@code long}: 9,223,372,036,854,775,807 FE, about four billion times the int
 * ceiling. {@code BigInteger} would be unbounded and is not worth an allocation per operation for
 * headroom already this absurd.
 *
 * <p><b>What the clamp costs.</b> A cable, a meter or Jade reading the standard capability sees at
 * most 2.1B. Powah accepts exactly that trade and shows the true figure in its own screens; so do
 * we — {@code BigEnergyStorage} exposes long accessors and every display in this mod uses them.
 *
 * <p>This reverses what CLAUDE.md §11 originally decided. That entry rejected long-with-clamp on the
 * grounds that {@code getEnergyStored()} would lie to cables. It does lie. The alternative was a
 * storage block smaller than the cells of the mod sitting next to it.
 *
 * <h3>The default curve</h3>
 *
 * <pre>
 *   base    = ENERGY_BASE_UNITS x FE_PER_UNIT = 500 x 1,000 =       500,000 FE
 *   factor  = (NETHERITE / ENERGY_DIVISOR)^4  = (32 / 4)^4   =         4,096
 *   maximum = base x factor                                  = 2,048,000,000 FE
 * </pre>
 *
 * <p>{@code ENERGY_DIVISOR} defaults to 4. That number was originally chosen to fit four Netherite
 * upgrades under the int ceiling, and the move to long removed that constraint — but it is kept,
 * because it is also a balance decision and it is the curve the mod has been played with. Anyone who
 * wants the extra headroom the long buys can lower the divisor in the config: at 2 the same four
 * upgrades reach about 32.8B FE, and the storage will hold it.
 */
public final class EnergyScaling {

    private EnergyScaling() {
    }

    /** Base size of an unupgraded drawer, in units. This is what goes into {@code DrawerProperties}. */
    public static int baseUnits() {
        return IDConfig.ENERGY_BASE_UNITS;
    }

    /** FE per unit. Mirrors the 1,000 mB per unit a fluid drawer uses. */
    public static int fePerUnit() {
        return IDConfig.FE_PER_UNIT;
    }

    /** FE held by a drawer with no storage upgrades in it. */
    public static long baseCapacity() {
        return (long) baseUnits() * fePerUnit();
    }

    /**
     * Turns the drawer's storage multiplier into a capacity in FE.
     *
     * <p>Clamped, just very far out. The Max Storage upgrade reports a multiplier of
     * {@link Integer#MAX_VALUE} ({@code FunctionalStorageConfig.getLevelMult(-1)}); with a large
     * base and a small divisor that can overflow even a long, so the result saturates rather than
     * wrapping into a negative capacity — a drawer that refuses every FE offered to it.
     */
    public static long capacityFor(double storageMultiplier) {
        double capacity = storageMultiplier * fePerUnit();
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
        return levelMultiplier / (float) IDConfig.ENERGY_DIVISOR;
    }

    /** Whether drawers hand energy to their neighbours unprompted. */
    public static boolean pushesToNeighbours() {
        return IDConfig.PUSH_TO_NEIGHBOURS;
    }

    /** Ticks between pushes. */
    public static int pushIntervalTicks() {
        return IDConfig.ENERGY_PUSH_INTERVAL_TICKS;
    }

    /**
     * The most a drawer of this capacity will hand out in one push.
     *
     * <p>Clamped to int because {@link net.neoforged.neoforge.energy.IEnergyStorage#receiveEnergy}
     * cannot be told about more, and floored at 1 so a tiny drawer still moves something.
     */
    public static int transferPerOperation(long capacity) {
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE, capacity / IDConfig.ENERGY_TRANSFER_DIVISOR));
    }
}
