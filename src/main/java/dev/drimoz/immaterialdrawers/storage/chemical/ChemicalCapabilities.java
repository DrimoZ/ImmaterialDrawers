package dev.drimoz.immaterialdrawers.storage.chemical;

import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.jetbrains.annotations.Nullable;

/**
 * Mekanism's chemical capabilities, reached through its API only.
 *
 * <p><b>Mekanism only - see {@code compat.Mods}.</b> Never reference this class outside a
 * {@code Mods.mekanism()} guard.
 *
 * <p>Mekanism keeps its capability objects in {@code mekanism.common.capabilities.Capabilities},
 * which is its implementation, not its API, and moves whenever it likes. What is stable is the
 * <em>name</em>: {@code mekanism:chemical_handler}, typed on the API's {@link IChemicalHandler}.
 * NeoForge interns capabilities by name - {@code CapabilityRegistry.create} hands back the existing
 * instance when the name and classes match, and throws when they do not - so creating ours with the
 * same name and type returns Mekanism's own object, not a lookalike. The game test
 * {@code weSpeakMekanismsOwnCapability} holds that to be true, because if it ever stopped being true
 * every tube would quietly stop seeing the drawers.
 */
public final class ChemicalCapabilities {

    private static final ResourceLocation NAME =
            ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, "chemical_handler");

    /** What pressurized tubes, machines and Mekanism's own tanks look for on a block. */
    public static final BlockCapability<IChemicalHandler, @Nullable Direction> BLOCK =
            BlockCapability.createSided(NAME, IChemicalHandler.class);

    /** What a chemical tank item exposes - how a player fills or empties a drawer by hand. */
    public static final ItemCapability<IChemicalHandler, @Nullable Void> ITEM =
            ItemCapability.createVoid(NAME, IChemicalHandler.class);

    /**
     * Chemicals a drawer refuses, the counterpart of Functional Storage's
     * {@code functionalstorage:fluid_drawer_storage_denylist}. Empty by default: it is there for pack
     * authors, not for us to guess at.
     */
    public static final TagKey<Chemical> DENYLIST = TagKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME,
            ResourceLocation.fromNamespaceAndPath(ImmaterialDrawers.MOD_ID, "chemical_drawer_denylist"));

    private ChemicalCapabilities() {
    }
}
