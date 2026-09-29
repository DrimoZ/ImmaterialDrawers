package dev.drimoz.immaterialdrawers.client;

import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.compat.Mods;
import dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile;
import dev.drimoz.immaterialdrawers.registry.IDContent;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Client-side registrations that belong on the mod bus. */
@EventBusSubscriber(modid = ImmaterialDrawers.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class IDClientSetup {

    private IDClientSetup() {
    }

    /**
     * One renderer, both drawers.
     *
     * <p>Registered per block entity type, and the framed variant has its own — the same trap as
     * the energy capability, with a quieter symptom: the drawer would work and simply never show
     * what it holds.
     */
    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (var drawer : java.util.List.of(IDContent.ENERGY_DRAWER, IDContent.FRAMED_ENERGY_DRAWER)) {
            event.registerBlockEntityRenderer(
                    (BlockEntityType<? extends EnergyDrawerTile>) drawer.type().get(),
                    context -> new EnergyDrawerRenderer());
        }
        if (Mods.mekanism()) {
            ChemicalClient.registerRenderers(event);
        }
    }

    /**
     * The energy cube's two halves. No block or item points at these models, so nothing would load
     * them otherwise — and asking the model manager for one that was never registered does not
     * fail, it hands back the missing model, a pink-and-black cube turning in every drawer.
     */
    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(EnergyDrawerRenderer.CUBE_FRAME);
        event.register(EnergyDrawerRenderer.CUBE_CORE);
    }
}
