package dev.drimoz.immaterialdrawers.client;

import dev.drimoz.immaterialdrawers.block.tile.chemical.ChemicalDrawerTile;
import dev.drimoz.immaterialdrawers.registry.IDChemicalContent;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;

import java.util.List;

/** Client registration for the chemical drawers. <b>Mekanism only</b>: called behind {@code Mods.mekanism()}. */
public final class ChemicalClient {

    private ChemicalClient() {
    }

    @SuppressWarnings("unchecked")
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (var drawer : IDChemicalContent.all()) {
            event.registerBlockEntityRenderer(
                    (BlockEntityType<? extends ChemicalDrawerTile>) drawer.getRight().get(),
                    context -> new ChemicalDrawerRenderer());
        }
    }

    public static void setRenderLayers() {
        for (var drawer : IDChemicalContent.all()) {
            ItemBlockRenderTypes.setRenderLayer(drawer.getLeft().get(), RenderType.cutout());
        }
    }

    public static List<Block> framedBlocks() {
        return IDChemicalContent.TYPES.stream()
                .map(type -> IDChemicalContent.drawer(type, true).getLeft().get())
                .toList();
    }
}
