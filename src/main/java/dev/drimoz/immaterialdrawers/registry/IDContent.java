package dev.drimoz.immaterialdrawers.registry;

import com.hrznstudio.titanium.module.BlockWithTile;
import com.hrznstudio.titanium.module.DeferredRegistryHelper;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.energy.EnergyDrawerBlock;
import dev.drimoz.immaterialdrawers.block.energy.FramedEnergyDrawerBlock;
import com.buuz135.functionalstorage.item.UpgradeItem;
import dev.drimoz.immaterialdrawers.augment.ChargeNearbyBehavior;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
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

    /** Registry path of the framed variant. Permanent for the same reason. */
    public static final String FRAMED_ENERGY_DRAWER_NAME = "framed_energy_drawer";

    public static BlockWithTile ENERGY_DRAWER;

    public static BlockWithTile FRAMED_ENERGY_DRAWER;

    /**
     * The Wireless Charger augment.
     *
     * <p>An {@code UpgradeItem} built from a behaviour is a utility upgrade carrying the
     * {@code FUNCTIONAL_BEHAVIOR} component - Functional Storage's own Redstone Upgrade is made the
     * same way, in the same one line.
     */
    public static DeferredHolder<Item, Item> WIRELESS_CHARGER;

    private IDContent() {
    }

    public static void register(DeferredRegistryHelper registries) {
        // Copper, not the stone bricks Functional Storage gives its fluid drawers: a distinct
        // material makes the unframed variant readable at a glance, and copper is what every other
        // mod already means by "this carries power".
        ENERGY_DRAWER = registries.registerBlockWithTileItem(
                ENERGY_DRAWER_NAME,
                () -> new EnergyDrawerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK)),
                block -> () -> new EnergyDrawerBlock.EnergyDrawerItem(
                        (EnergyDrawerBlock) block.get(), new Item.Properties()),
                ImmaterialDrawers.TAB);

        // The framed variant is the real answer to blending into an existing wall - the player
        // gives it the same textures as the drawers around it, instead of us shipping a variant per
        // wood type and hoping one matches what they built with.
        //
        // Same properties as the unframed one: what it looks like is the player's business, but how
        // long it takes to break should not depend on which one they chose.
        FRAMED_ENERGY_DRAWER = registries.registerBlockWithTileItem(
                FRAMED_ENERGY_DRAWER_NAME,
                () -> new FramedEnergyDrawerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK)),
                block -> () -> new EnergyDrawerBlock.EnergyDrawerItem(
                        (EnergyDrawerBlock) block.get(), new Item.Properties()),
                ImmaterialDrawers.TAB);

        WIRELESS_CHARGER = registries.registerGeneric(Registries.ITEM, "wireless_charger",
                () -> new UpgradeItem(ChargeNearbyBehavior.INSTANCE));
    }
}
