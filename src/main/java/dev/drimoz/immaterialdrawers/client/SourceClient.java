package dev.drimoz.immaterialdrawers.client;

import com.hrznstudio.titanium.module.BlockWithTile;
import dev.drimoz.immaterialdrawers.block.tile.source.SourceDrawerTile;
import dev.drimoz.immaterialdrawers.registry.IDSourceContent;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * The Source Drawer's client registrations. <b>Ars Nouveau only - see {@code compat.Mods}.</b>
 * Called from {@link IDClientSetup} and {@link IDColors} behind the guard, like {@link ChemicalClient}.
 */
public final class SourceClient {

    private SourceClient() {
    }

    @SuppressWarnings("unchecked")
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (BlockWithTile drawer : IDSourceContent.all()) {
            event.registerBlockEntityRenderer(
                    (BlockEntityType<? extends SourceDrawerTile>) drawer.type().get(),
                    context -> new SourceDrawerRenderer());
        }
    }

    public static void registerBlockColors(RegisterColorHandlersEvent.Block event, BlockColor handler) {
        event.register(handler, IDSourceContent.FRAMED_SOURCE_DRAWER.getBlock());
    }

    public static void registerItemColors(RegisterColorHandlersEvent.Item event, ItemColor handler) {
        event.register(handler, IDSourceContent.FRAMED_SOURCE_DRAWER.getBlock());
    }
}
