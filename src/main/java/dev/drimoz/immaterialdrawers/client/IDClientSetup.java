package dev.drimoz.immaterialdrawers.client;

import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile;
import dev.drimoz.immaterialdrawers.registry.IDContent;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

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
    }
}
