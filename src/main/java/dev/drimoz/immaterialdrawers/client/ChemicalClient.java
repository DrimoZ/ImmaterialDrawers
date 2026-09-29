package dev.drimoz.immaterialdrawers.client;

import com.hrznstudio.titanium.module.BlockWithTile;
import dev.drimoz.immaterialdrawers.block.tile.chemical.ChemicalDrawerTile;
import dev.drimoz.immaterialdrawers.registry.IDChemicalContent;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * The chemical drawers' client registrations.
 *
 * <p><b>Mekanism only - see {@code compat.Mods}.</b> Called from {@link IDClientSetup} and
 * {@link IDColors}, behind the guard; those two stay free of Mekanism so a client without it never
 * resolves a class from here.
 */
public final class ChemicalClient {

    private ChemicalClient() {
    }

    /**
     * One renderer per block entity type, six types. Missing one is the quiet failure the energy
     * drawer's registration describes: a drawer that works and never shows what it holds.
     */
    @SuppressWarnings("unchecked")
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (BlockWithTile drawer : IDChemicalContent.all()) {
            event.registerBlockEntityRenderer(
                    (BlockEntityType<? extends ChemicalDrawerTile>) drawer.type().get(),
                    context -> new ChemicalDrawerRenderer());
        }
    }

    /** Functional Storage's framed tint on the three framed layouts - see {@link IDColors}. */
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event, BlockColor handler) {
        event.register(handler, framed());
    }

    public static void registerItemColors(RegisterColorHandlersEvent.Item event, ItemColor handler) {
        event.register(handler, framed());
    }

    private static Block[] framed() {
        return IDChemicalContent.TYPES.stream()
                .map(type -> IDChemicalContent.drawer(type, true).getBlock())
                .toArray(Block[]::new);
    }
}
