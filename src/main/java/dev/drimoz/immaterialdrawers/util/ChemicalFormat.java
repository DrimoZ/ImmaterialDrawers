package dev.drimoz.immaterialdrawers.util;

/**
 * Formats an amount in mB the way Functional Storage formats one on a fluid drawer.
 *
 * <p>Adapted from Functional Storage's {@code util/NumberUtils#getFormatedFluidBigNumber}.
 * Copyright (c) 2021 Buuz135, Rid — MIT. See NOTICE.
 *
 * <p>Their scheme counts in buckets once past a thousand mB, and names the thousands of buckets with
 * the unit one step down: 32,000 mB is "32 B", 1,500,000 mB "1.5K B". A chemical drawer uses the same
 * words as the fluid drawer beside it, so that "32 B" means the same volume on both. Theirs takes an
 * {@code int}; chemicals are {@code long}, so this carries on past "M B" through {@link EnergyFormat}'s
 * units.
 *
 * <p>No Mekanism class in here, deliberately: it is plain arithmetic, and it can live outside the
 * guard.
 */
public final class ChemicalFormat {

    private ChemicalFormat() {
    }

    public static String format(long millibuckets) {
        if (millibuckets < 1000) {
            return millibuckets + " mB";
        }
        // EnergyFormat names 1,000 as "1K"; for mB that is one bucket. Shift every unit down by one.
        String scaled = EnergyFormat.format(millibuckets);
        char unit = scaled.charAt(scaled.length() - 1);
        String number = scaled.substring(0, scaled.length() - 1);
        return switch (unit) {
            case 'K' -> number + " B";
            case 'M' -> number + "K B";
            case 'B' -> number + "M B";
            case 'T' -> number + "B B";
            case 'Q' -> number + "T B";
            default -> scaled + " mB";
        };
    }
}
