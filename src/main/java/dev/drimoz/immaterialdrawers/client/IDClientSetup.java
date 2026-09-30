package dev.drimoz.immaterialdrawers.client;

import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile;
import dev.drimoz.immaterialdrawers.compat.Mods;
import dev.drimoz.immaterialdrawers.registry.IDContent;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.List;

/** Client-side registration: renderers, the cube's standalone models, render layers. */
@Mod.EventBusSubscriber(modid = ImmaterialDrawers.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class IDClientSetup {

    private IDClientSetup() {
    }

    /** One renderer, both drawers - registered per block entity type, and the framed one has its own. */
    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (var drawer : List.of(IDContent.ENERGY_DRAWER, IDContent.FRAMED_ENERGY_DRAWER)) {
            event.registerBlockEntityRenderer(
                    (BlockEntityType<? extends EnergyDrawerTile>) drawer.getRight().get(),
                    context -> new EnergyDrawerRenderer());
        }
        if (Mods.arsNouveau()) {
            SourceClient.registerRenderers(event);
        }
    }

    /**
     * The cube's two halves. No block or item points at them, so nothing loads them otherwise - and
     * asking for an unregistered model does not fail, it hands back the pink-and-black missing one.
     */
    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(EnergyDrawerRenderer.CUBE_FRAME);
        event.register(EnergyDrawerRenderer.CUBE_CORE);
    }

    /**
     * Cutout, as Functional Storage sets its fluid drawers: the front's window is transparent, and
     * 1.20.1 still takes the render layer from here rather than from the model.
     */
    @SubscribeEvent
    public static void setRenderLayers(FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(IDContent.ENERGY_DRAWER.getLeft().get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(IDContent.FRAMED_ENERGY_DRAWER.getLeft().get(), RenderType.cutout());
        if (Mods.arsNouveau()) {
            SourceClient.setRenderLayers();
        }
    }
}
