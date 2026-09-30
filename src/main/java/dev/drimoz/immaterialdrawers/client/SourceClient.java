package dev.drimoz.immaterialdrawers.client;

import dev.drimoz.immaterialdrawers.block.tile.source.SourceDrawerTile;
import dev.drimoz.immaterialdrawers.registry.IDSourceContent;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Client registration for the Source Drawers. <b>Ars Nouveau only</b>: called behind {@code Mods.arsNouveau()}. */
public final class SourceClient {

    private SourceClient() {
    }

    @SuppressWarnings("unchecked")
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (var drawer : IDSourceContent.all()) {
            event.registerBlockEntityRenderer(
                    (BlockEntityType<? extends SourceDrawerTile>) drawer.getRight().get(),
                    context -> new SourceDrawerRenderer());
        }
    }

    public static void setRenderLayers() {
        for (var drawer : IDSourceContent.all()) {
            ItemBlockRenderTypes.setRenderLayer(drawer.getLeft().get(), RenderType.cutout());
        }
    }

    public static Block framedBlock() {
        return IDSourceContent.FRAMED_SOURCE_DRAWER.getLeft().get();
    }
}
