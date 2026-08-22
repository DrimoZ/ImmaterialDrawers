package dev.drimoz.immaterialdrawers.gametest;

import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile;
import dev.drimoz.immaterialdrawers.registry.IDContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The blocking spike of CLAUDE.md §12, written as a test rather than as something to check by hand.
 *
 * <p>Two readings of third-party code hold this mod's architecture up, and neither had been run
 * when they were made:
 *
 * <ol>
 *   <li>Titanium already registers an {@code EnergyStorage.BLOCK} provider on every block entity
 *       type it creates, and that provider only answers for a {@code PoweredTile} - which we
 *       cannot be, because we must be a {@code ControllableDrawerTile} and Java has single
 *       inheritance. Registering a second provider is only survivable because NeoForge walks a
 *       list of providers until one returns non-null.</li>
 *   <li>Being an {@code ItemControllableDrawerTile} with a zero-slot handler is what keeps the
 *       Storage Controller's per-tick invariant true. If the handler ever stops being empty, or
 *       stops existing, the failure is a rebuilt network every tick on every controller in the
 *       world - a dead server, not a visible bug.</li>
 * </ol>
 *
 * <p>Both are cheap to assert and expensive to discover in the wild, and both can be broken by a
 * release of a mod we do not control. That is why these stay after the spike is over.
 *
 * <p>Run headlessly with {@code ./gradlew runGameTestServer}, or {@code /test run immaterialdrawers}
 * in a client.
 */
@GameTestHolder(ImmaterialDrawers.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IDGameTests {

    /** Matches src/main/resources/data/immaterialdrawers/structure/energy_platform.nbt. */
    private static final String PLATFORM = "energy_platform";

    /** One block above the platform's floor, in the middle. */
    private static final BlockPos DRAWER = new BlockPos(1, 1, 1);

    private IDGameTests() {
    }

    /**
     * The spike itself: our provider is reachable even though Titanium got there first.
     */
    @GameTest(template = PLATFORM)
    public static void energyCapabilityIsPresent(GameTestHelper helper) {
        IEnergyStorage storage = placeDrawerAndGetCapability(helper, null);

        helper.assertTrue(storage != null,
                "No EnergyStorage capability on the energy drawer. Titanium's own provider is "
                        + "registered first and returns null for a non-PoweredTile; if NeoForge "
                        + "stopped falling through to the next provider, the whole design in "
                        + "CLAUDE.md §8 is wrong and the block needs to stop going through "
                        + "Titanium's registerBlockWithTile.");
        helper.succeed();
    }

    /**
     * The capability is reachable from every side, not only from the one the block faces.
     * A drawer in a wall is touched from whichever side has room for a cable.
     */
    @GameTest(template = PLATFORM)
    public static void energyCapabilityIsPresentFromEverySide(GameTestHelper helper) {
        for (Direction side : Direction.values()) {
            helper.assertTrue(placeDrawerAndGetCapability(helper, side) != null,
                    "No EnergyStorage capability from side " + side);
        }
        helper.succeed();
    }

    /**
     * The capability that comes back is the drawer's own storage, and it works.
     *
     * <p>A provider that returns some other object, or a fresh one per query, would pass the test
     * above and lose every FE put into it.
     */
    @GameTest(template = PLATFORM)
    public static void energyCapabilityStoresWhatItIsGiven(GameTestHelper helper) {
        IEnergyStorage storage = placeDrawerAndGetCapability(helper, null);
        helper.assertTrue(storage != null, "No EnergyStorage capability on the energy drawer");

        int accepted = storage.receiveEnergy(1_000, false);
        helper.assertValueEqual(accepted, 1_000, "energy accepted");

        // Queried again, not reused: this is the half that catches a provider handing out a new
        // storage object on every call.
        IEnergyStorage requeried = capability(helper, null);
        helper.assertTrue(requeried != null, "capability vanished after a write");
        helper.assertValueEqual(requeried.getEnergyStored(), 1_000, "energy stored");

        int extracted = requeried.extractEnergy(400, false);
        helper.assertValueEqual(extracted, 400, "energy extracted");
        helper.assertValueEqual(requeried.getEnergyStored(), 600, "energy left after extraction");

        helper.succeed();
    }

    /**
     * The zero-slot item handler that keeps the Storage Controller's network arithmetic honest.
     *
     * <p>{@code StorageControllerTile.serverTick} rebuilds its whole network whenever the drawer
     * count stops equalling {@code itemHandlers + fluidHandlers + extensions}. We are counted in
     * {@code itemHandlers} because we are an {@code ItemControllableDrawerTile}, and we contribute
     * nothing to the aggregated inventory because {@code getSlots()} is zero. Both halves matter:
     * lose the first and the drawer is dropped from every network, lose the second and the drawer
     * starts advertising phantom item slots. See CLAUDE.md §7.
     */
    @GameTest(template = PLATFORM)
    public static void drawerCountsAsAnItemDrawerButHoldsNoItems(GameTestHelper helper) {
        helper.setBlock(DRAWER, IDContent.ENERGY_DRAWER.getBlock());
        BlockEntity be = helper.getBlockEntity(DRAWER);

        helper.assertTrue(be instanceof EnergyDrawerTile,
                "The energy drawer has no EnergyDrawerTile behind it");
        EnergyDrawerTile tile = (EnergyDrawerTile) be;

        helper.assertTrue(tile instanceof com.buuz135.functionalstorage.block.tile.ItemControllableDrawerTile<?>,
                "EnergyDrawerTile no longer extends ItemControllableDrawerTile - Functional "
                        + "Storage's ConnectedDrawers filter will drop it from every controller "
                        + "network");
        helper.assertValueEqual(tile.getStorage().getSlots(), 0, "item slots on an energy drawer");

        helper.succeed();
    }

    private static IEnergyStorage placeDrawerAndGetCapability(GameTestHelper helper, Direction side) {
        helper.setBlock(DRAWER, IDContent.ENERGY_DRAWER.getBlock());
        return capability(helper, side);
    }

    private static IEnergyStorage capability(GameTestHelper helper, Direction side) {
        return helper.getLevel().getCapability(
                Capabilities.EnergyStorage.BLOCK, helper.absolutePos(DRAWER), side);
    }
}
