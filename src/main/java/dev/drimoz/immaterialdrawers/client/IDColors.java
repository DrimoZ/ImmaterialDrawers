package dev.drimoz.immaterialdrawers.client;

import com.buuz135.functionalstorage.client.FramedColors;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.registry.IDContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * Tints the framed drawer with the colour of whatever it was framed with.
 *
 * <p>Functional Storage's {@link FramedColors} does the work and does it generically — both of its
 * {@code getColor} methods branch on {@code FramedTile} and {@code FramedBlock}, so they answer
 * correctly for our drawer without knowing it exists. What is <em>not</em> generic is the
 * registration: they subscribe their own listener and feed it
 * {@code FunctionalStorage.FRAMED_BLOCKS}, a list built by scanning their own block registry
 * ({@code FunctionalStorage.java:404}). Our block can never be in it.
 *
 * <p>So the handler is theirs and the registration is ours. Reusing their instance rather than
 * writing an equivalent one is deliberate: the two must agree about what colour a framed block is,
 * and the cheapest way to guarantee that is for there to be only one implementation.
 *
 * <p>This matters for blocks that carry a tint of their own — leaves, grass, vines. Frame a drawer
 * with birch leaves and the drawer takes the biome colour the leaves have, and keeps taking it when
 * the player moves it to another biome. Frame it with oak planks and nothing happens, because oak
 * planks have no tint either. That is the same behaviour as every framed drawer in Functional
 * Storage, which is the point.
 */
@EventBusSubscriber(modid = ImmaterialDrawers.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class IDColors {

    private static final FramedColors HANDLER = new FramedColors();

    private IDColors() {
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(HANDLER, IDContent.FRAMED_ENERGY_DRAWER.getBlock());
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(HANDLER, IDContent.FRAMED_ENERGY_DRAWER.getBlock());
    }
}
