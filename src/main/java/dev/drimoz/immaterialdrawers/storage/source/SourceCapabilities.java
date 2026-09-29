package dev.drimoz.immaterialdrawers.storage.source;

import com.hollingsworth.arsnouveau.api.source.ISourceCap;
import dev.drimoz.immaterialdrawers.compat.Mods;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

/**
 * Ars Nouveau's Source capability, reached by name.
 *
 * <p><b>Ars Nouveau only - see {@code compat.Mods}.</b>
 *
 * <p>Ars keeps it in {@code setup.registry.CapabilityRegistry}, which is its setup code, not its
 * {@code api} package. The name and type are the stable part - {@code ars_nouveau:source}, sided,
 * typed on the API's {@link ISourceCap} - and NeoForge interns capabilities by name, so this is Ars's
 * own object. The same arrangement as {@code ChemicalCapabilities}, held to the same test:
 * {@code weSpeakArsOwnCapability}.
 */
public final class SourceCapabilities {

    public static final BlockCapability<ISourceCap, @Nullable Direction> BLOCK = BlockCapability.createSided(
            ResourceLocation.fromNamespaceAndPath(Mods.ARS_NOUVEAU, "source"), ISourceCap.class);

    private SourceCapabilities() {
    }
}
