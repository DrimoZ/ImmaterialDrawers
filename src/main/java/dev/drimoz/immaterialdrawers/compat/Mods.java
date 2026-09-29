package dev.drimoz.immaterialdrawers.compat;

import net.neoforged.fml.ModList;

/**
 * Which optional mods are installed - and the one place allowed to ask.
 *
 * <p><b>The rule this class exists to enforce.</b> Code that touches an optional mod's classes may
 * only run after one of these methods has said the mod is there. Not "should": the JVM resolves a
 * class the first time a method that uses it is linked, and a missing class at that point is a
 * {@code NoClassDefFoundError} that takes the whole game down with it - in a pack that never asked
 * for the feature.
 *
 * <p>So everything that uses Mekanism lives in classes that are only ever reached through a
 * {@code if (Mods.mekanism())} in a class that does not use Mekanism itself. This class is the
 * other half of that contract, and it must stay free of every optional import for the same reason.
 * See CLAUDE.md §16.
 */
public final class Mods {

    public static final String MEKANISM = "mekanism";
    public static final String ARS_NOUVEAU = "ars_nouveau";

    private Mods() {
    }

    /**
     * Whether Mekanism is installed. Safe from the mod constructor onwards: {@link ModList} is
     * complete before any mod is constructed.
     */
    public static boolean mekanism() {
        return ModList.get().isLoaded(MEKANISM);
    }

    /** Whether Ars Nouveau is installed - the Source Drawer. Same contract as {@link #mekanism()}. */
    public static boolean arsNouveau() {
        return ModList.get().isLoaded(ARS_NOUVEAU);
    }
}
