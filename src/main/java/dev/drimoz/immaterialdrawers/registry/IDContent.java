package dev.drimoz.immaterialdrawers.registry;

import com.hrznstudio.titanium.module.DeferredRegistryHelper;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.energy.EnergyDrawerBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.RegistryObject;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Everything this mod puts in a registry.
 *
 * <p>Registration runs through Titanium's {@link DeferredRegistryHelper}, because
 * {@code registerBlockWithTileItem} is what builds the {@code BlockEntityType} for a
 * {@code BasicTileBlock}. On 1.20.1 it hands back a {@link Pair} of block and type rather than 1.21.1's
 * {@code BlockWithTile} - same two things, left and right.
 *
 * <p>The statics are assigned from {@code initModules}, which Titanium's {@code ModuleController}
 * calls from its own constructor.
 *
 * <p><b>1.20.1 backport, step 1</b> (PORTING.md §6): the energy drawer only. The framed variant and
 * the Wireless Charger come back with their own steps, under the same registry names.
 */
public final class IDContent {

    /**
     * Registry path of the energy drawer. Written into every save that contains one, so it is as
     * permanent as the mod id itself - and identical on every branch, so a world keeps its drawers
     * across a version upgrade.
     */
    public static final String ENERGY_DRAWER_NAME = "energy_drawer";

    /** Registry path of the framed variant. Permanent for the same reason. */
    public static final String FRAMED_ENERGY_DRAWER_NAME = "framed_energy_drawer";

    public static Pair<RegistryObject<Block>, RegistryObject<BlockEntityType<?>>> ENERGY_DRAWER;

    private IDContent() {
    }

    public static void register(DeferredRegistryHelper registries) {
        // Copper: a distinct material makes the unframed variant readable at a glance, and copper is
        // what every other mod already means by "this carries power".
        ENERGY_DRAWER = registries.registerBlockWithTileItem(
                ENERGY_DRAWER_NAME,
                () -> new EnergyDrawerBlock(BlockBehaviour.Properties.copy(Blocks.COPPER_BLOCK)),
                block -> () -> new EnergyDrawerBlock.EnergyDrawerItem(
                        (EnergyDrawerBlock) block.get(), new Item.Properties()),
                ImmaterialDrawers.TAB);
    }
}
