package dev.drimoz.immaterialdrawers.gametest;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import com.buuz135.functionalstorage.item.LinkingToolItem;
import com.buuz135.functionalstorage.util.ConnectedDrawers;
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
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

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

    /** Matches src/main/resources/data/immaterialdrawers/structure/drawer_wall.nbt — 11 x 5 x 11. */
    private static final String WALL = "drawer_wall";

    private static final int WALL_SIZE = 11;

    /**
     * Comfortably more than the point at which a per-tick rebuild stops being a rounding error and
     * starts being the server's frame budget, and well inside the controller's linking range of 8.
     */
    private static final int WALL_DRAWERS = 50;

    /** Ticks allowed for the controller to notice the new drawers and build its network once. */
    private static final int SETTLE_TICKS = 10;

    /** Ticks of doing nothing at all, during which no rebuild is allowed to happen. */
    private static final int IDLE_TICKS = 60;

    /** One block above the platform's floor, in the middle. */
    private static final BlockPos DRAWER = new BlockPos(1, 1, 1);

    /** Middle of the wall structure, so every drawer around it is within linking range. */
    private static final BlockPos CONTROLLER = new BlockPos(5, 1, 5);

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

    /**
     * The real shape of the §7 risk: a wall of energy drawers on one Storage Controller, ticking.
     *
     * <p>The test above proves the drawer is <em>counted</em>. This one proves the count stays
     * balanced while the controller is actually running, which is the thing that matters — the
     * failure it guards against does not look like a bug. If our drawers were in the network
     * without contributing to {@code itemHandlers}, {@code StorageControllerTile.serverTick} would
     * find its invariant false on every tick and rebuild the entire network every tick, forever,
     * on every controller in the world. The drawers would keep working. The server would not.
     *
     * <p>Detection is by identity, not by arithmetic: {@code ConnectedDrawers.rebuild()} assigns
     * {@code this.itemHandlers = new ArrayList<>()}, so a rebuild between two observations shows up
     * as a different list object. That catches a rebuild even in the case where the invariant is
     * restored by the time we look at it.
     *
     * <p><b>Why the test idles before it asserts anything.</b> Linking does not build the network.
     * {@code ConnectedDrawers} is constructed in the tile's constructor, where {@code getLevel()}
     * is still null, and its {@code rebuild()} is a no-op without a level — so the rebuild that
     * {@code addConnectedDrawers} triggers leaves the handler lists empty. The controller's own
     * {@code serverTick} is what calls {@code setLevel} and rebuilds for real. The invariant being
     * false for a tick or two after linking is therefore normal and is not what this test is
     * about; being false <em>forever</em> is.
     */
    @GameTest(template = WALL, timeoutTicks = 300)
    public static void aWallOfDrawersDoesNotRebuildTheControllerEveryTick(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, FunctionalStorage.DRAWER_CONTROLLER.getBlock());

        List<BlockPos> placed = new ArrayList<>();
        for (int y = 1; y <= 3 && placed.size() < WALL_DRAWERS; y++) {
            for (int x = 0; x < WALL_SIZE && placed.size() < WALL_DRAWERS; x++) {
                for (int z = 0; z < WALL_SIZE && placed.size() < WALL_DRAWERS; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (pos.equals(CONTROLLER)) {
                        continue;
                    }
                    helper.setBlock(pos, IDContent.ENERGY_DRAWER.getBlock());
                    placed.add(pos);
                }
            }
        }
        helper.assertValueEqual(placed.size(), WALL_DRAWERS, "drawers placed");

        BlockEntity be = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(be instanceof StorageControllerTile<?>, "no Storage Controller was placed");
        StorageControllerTile<?> controller = (StorageControllerTile<?>) be;

        // What the Linking Tool calls when a player drags a box over a wall of drawers. Absolute
        // positions: the controller looks them up in the level, not in the test's frame.
        controller.addConnectedDrawers(LinkingToolItem.ActionMode.ADD,
                placed.stream().map(helper::absolutePos).toArray(BlockPos[]::new));

        ConnectedDrawers network = controller.getConnectedDrawers();
        helper.assertValueEqual(network.getConnectedDrawers().size(), WALL_DRAWERS,
                "drawers accepted into the controller network");

        // Written to once the network has settled, read again after idling. A one-element array
        // rather than a field: game tests run concurrently in the same level.
        List<IItemHandler>[] settledHandlers = new List[1];

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    assertNetworkInvariantHolds(helper, network, "once the network has settled");
                    helper.assertValueEqual(network.getItemHandlers().size(), WALL_DRAWERS,
                            "energy drawers counted as item handlers by the controller");
                    settledHandlers[0] = network.getItemHandlers();
                })
                .thenIdle(IDLE_TICKS)
                .thenExecute(() -> {
                    assertNetworkInvariantHolds(helper, network, "after idling");
                    helper.assertTrue(network.getItemHandlers() == settledHandlers[0],
                            "The controller rebuilt its network while nothing happened. Its "
                                    + "per-tick check is connectedDrawers == itemHandlers + "
                                    + "fluidHandlers + extensions; an energy drawer that stops "
                                    + "satisfying it makes every controller in the world rebuild "
                                    + "on every tick. See CLAUDE.md §7.");
                })
                .thenSucceed();
    }

    /** The exact expression {@code StorageControllerTile.serverTick} tests before rebuilding. */
    private static void assertNetworkInvariantHolds(GameTestHelper helper, ConnectedDrawers network, String when) {
        int counted = network.getItemHandlers().size()
                + network.getFluidHandlers().size()
                + network.getExtensions();
        helper.assertValueEqual(counted, network.getConnectedDrawers().size(),
                "handlers+extensions counted against drawers in the network, " + when);
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
