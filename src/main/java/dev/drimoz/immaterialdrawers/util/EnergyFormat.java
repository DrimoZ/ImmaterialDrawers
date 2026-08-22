package dev.drimoz.immaterialdrawers.util;

import java.text.DecimalFormat;

/**
 * Formats an FE amount the way a drawer formats a stack count.
 *
 * <p>Adapted from Functional Storage's {@code util/NumberUtils#getFormatedBigNumber}.
 * Copyright (c) 2021 Buuz135, Rid — MIT. See NOTICE.
 *
 * <p>Theirs takes an {@code int} and stops at billions, which is all a stack count or a fluid
 * amount can ever be. Energy is stored in a {@code long} here, so this carries the same thresholds
 * and the same rounding upward through trillions and quadrillions. The point is that the number on
 * an energy drawer is written exactly like the number on the item drawer beside it, only longer.
 */
public final class EnergyFormat {

    private static final DecimalFormat WITH_UNITS = new DecimalFormat("####0.#");

    private static final long THOUSAND = 1_000L;
    private static final long MILLION = 1_000_000L;
    private static final long BILLION = 1_000_000_000L;
    private static final long TRILLION = 1_000_000_000_000L;
    private static final long QUADRILLION = 1_000_000_000_000_000L;

    private EnergyFormat() {
    }

    public static String format(long amount) {
        if (amount >= QUADRILLION) {
            return WITH_UNITS.format(amount / (double) QUADRILLION) + "Q";
        }
        if (amount >= TRILLION) {
            return WITH_UNITS.format(amount / (double) TRILLION) + "T";
        }
        if (amount >= BILLION) {
            return WITH_UNITS.format(amount / (double) BILLION) + "B";
        }
        if (amount >= MILLION) {
            double value = amount / (double) MILLION;
            // Their rule: past a hundred of a unit, drop the decimal. "834M" reads better than
            // "834.2M", and the extra digit is noise at that scale.
            return WITH_UNITS.format(amount > 100 * MILLION ? Math.round(value) : value) + "M";
        }
        if (amount >= THOUSAND) {
            double value = amount / (double) THOUSAND;
            return WITH_UNITS.format(amount > 100 * THOUSAND ? Math.round(value) : value) + "K";
        }
        return String.valueOf(amount);
    }
}
