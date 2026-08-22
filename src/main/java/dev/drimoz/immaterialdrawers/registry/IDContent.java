package dev.drimoz.immaterialdrawers.registry;

import com.hrznstudio.titanium.module.BlockWithTile;
import com.hrznstudio.titanium.module.DeferredRegistryHelper;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.energy.EnergyDrawerBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Everything this mod puts in a registry.
 *
 * <p>Registration runs through Titanium's {@link DeferredRegistryHelper} rather than a plain
 * {@code DeferredRegister}, because {@code registerBlockWithTileItem} is what builds the
 * {@code BlockEntityType} for a {@code BasicTileBlock} and wires its capabilities. Using anything
 * else here would mean reimplementing that by hand.
 *
 * <p>The statics are assigned from {@code initModules}, which Titanium's {@code ModuleController}
 * calls from its own constructor - so they are set before any registry event fires, and null before
 * the mod class is constructed.
 */
public final class IDContent {

    /**
     * Registry path of the energy drawer. Written into every save that contains one, so it is as
     * permanent as the mod id itself.
     */
    public static final String ENERGY_DRAWER_NAME = "energy_drawer";

    public static BlockWithTile ENERGY_DRAWER;

    private IDContent() {
    }

    public static void register(DeferredRegistryHelper registries) {
        // Copper, not the stone bricks Functional Storage gives its fluid drawers: a distinct
        // material makes the unframed variant readable at a glance, and copper is what every other
        // mod already means by "this carries power". The Framed variant (task 5) is the real answer
        // for blending into an existing wall.
        ENERGY_DRAWER = registries.registerBlockWithTileItem(
                ENERGY_DRAWER_NAME,
                () -> new EnergyDrawerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK)),
                block -> () -> new EnergyDrawerBlock.EnergyDrawerItem(
                        (EnergyDrawerBlock) block.get(), new Item.Properties()),
                ImmaterialDrawers.TAB);
    }
}
