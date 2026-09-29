package dev.drimoz.immaterialdrawers.gametest;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.tile.ItemControllableDrawerTile;
import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import com.buuz135.functionalstorage.item.LinkingToolItem;
import com.buuz135.functionalstorage.util.ConnectedDrawers;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile;
import dev.drimoz.immaterialdrawers.registry.IDContent;
import dev.drimoz.immaterialdrawers.storage.EnergyScaling;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The blocking spike of the 1.20.1 backport (PORTING.md §6, step 1), written as tests.
 *
 * <p>The 1.21.1 branch rests on two readings of third-party code; on Forge 1.20.1 one of them
 * changes shape and the other must be re-proved:
 *
 * <ol>
 *   <li><b>The energy capability.</b> Forge asks the block entity, so the tile answers it itself -
 *       and for Functional Storage's controller, which is not ours, the answer comes through an
 *       {@code AttachCapabilitiesEvent} provider that their {@code getCapability} must fall through
 *       to. If it does not, a cable on the controller sees nothing.</li>
 *   <li><b>The zero-slot item handler</b> that keeps the Storage Controller's per-tick invariant
 *       true. The filter and the invariant were read in Functional Storage 1.2.14; this is where
 *       the reading becomes a fact, on 50 drawers.</li>
 * </ol>
 *
 * <p>If any of these fails, the backport stops and the architecture is rethought before anything
 * else is ported. Run headlessly with {@code ./gradlew runGameTestServer}.
 */
@GameTestHolder(ImmaterialDrawers.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IDGameTests {

    /** Matches src/main/resources/data/immaterialdrawers/structures/energy_platform.nbt. */
    private static final String PLATFORM = "energy_platform";

    /** Matches src/main/resources/data/immaterialdrawers/structures/drawer_wall.nbt - 11 x 5 x 11. */
    private static final String WALL = "drawer_wall";

    private static final int WALL_SIZE = 11;

    /** Well past where a per-tick rebuild stops being a rounding error, inside the linking range. */
    private static final int WALL_DRAWERS = 50;

    /** Ticks allowed for the controller to notice the new drawers and build its network once. */
    private static final int SETTLE_TICKS = 10;

    /** Ticks of doing nothing at all, during which no rebuild is allowed to happen. */
    private static final int IDLE_TICKS = 60;

    private static final BlockPos DRAWER = new BlockPos(1, 1, 1);

    /** Middle of the wall structure, so every drawer around it is within linking range. */
    private static final BlockPos CONTROLLER = new BlockPos(5, 1, 5);

    private IDGameTests() {
    }

    /** The drawer answers the Forge energy capability, through its own {@code getCapability}. */
    @GameTest(template = PLATFORM)
    public static void energyCapabilityIsPresent(GameTestHelper helper) {
        placeDrawer(helper);
        helper.assertTrue(capability(helper, DRAWER, null) != null,
                "No energy capability on the energy drawer: EnergyDrawerTile.getCapability is not "
                        + "being asked, or does not answer ForgeCapabilities.ENERGY");
        helper.succeed();
    }

    /** From every side: a drawer in a wall is touched from whichever side has room for a cable. */
    @GameTest(template = PLATFORM)
    public static void energyCapabilityIsPresentFromEverySide(GameTestHelper helper) {
        placeDrawer(helper);
        for (Direction side : Direction.values()) {
            helper.assertTrue(capability(helper, DRAWER, side) != null, "No energy capability from side " + side);
        }
        helper.succeed();
    }

    /**
     * The capability is the drawer's own storage, and it works. A provider that handed out a fresh
     * storage per query would pass the test above and lose every FE put into it.
     */
    @GameTest(template = PLATFORM)
    public static void energyCapabilityStoresWhatItIsGiven(GameTestHelper helper) {
        placeDrawer(helper);
        IEnergyStorage storage = capability(helper, DRAWER, null);
        helper.assertTrue(storage != null, "No energy capability on the energy drawer");

        assertEquals(helper, storage.receiveEnergy(1_000, false), 1_000, "energy accepted");

        IEnergyStorage requeried = capability(helper, DRAWER, null);
        helper.assertTrue(requeried != null, "capability vanished after a write");
        assertEquals(helper, requeried.getEnergyStored(), 1_000, "energy stored");

        assertEquals(helper, requeried.extractEnergy(400, false), 400, "energy extracted");
        assertEquals(helper, requeried.getEnergyStored(), 600, "energy left after extraction");

        helper.succeed();
    }

    /**
     * Counted as an item drawer, holding no items: lose the first half and the drawer is dropped
     * from every network, lose the second and it advertises phantom item slots. CLAUDE.md §7.
     */
    @GameTest(template = PLATFORM)
    public static void drawerCountsAsAnItemDrawerButHoldsNoItems(GameTestHelper helper) {
        EnergyDrawerTile tile = placeDrawer(helper);

        helper.assertTrue(tile instanceof ItemControllableDrawerTile<?>,
                "EnergyDrawerTile no longer extends ItemControllableDrawerTile - Functional "
                        + "Storage's ConnectedDrawers filter will drop it from every controller network");
        assertEquals(helper, tile.getStorage().getSlots(), 0, "item slots on an energy drawer");

        helper.succeed();
    }

    /**
     * The real shape of the §7 risk: 50 energy drawers on one Storage Controller, ticking.
     *
     * <p>Detection is by identity: {@code ConnectedDrawers.rebuild()} assigns fresh handler lists, so
     * a rebuild between two observations shows as a different list object, even if the invariant is
     * true again by the time we look.
     *
     * <p>Idles before asserting because linking does not build the network: the controller's own
     * {@code serverTick} does, a tick or two later. See CLAUDE.md §7.
     */
    @GameTest(template = WALL, timeoutTicks = 300)
    public static void aWallOfDrawersDoesNotRebuildTheControllerEveryTick(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, FunctionalStorage.DRAWER_CONTROLLER.getLeft().get());

        List<BlockPos> placed = new ArrayList<>();
        for (int y = 1; y <= 3 && placed.size() < WALL_DRAWERS; y++) {
            for (int x = 0; x < WALL_SIZE && placed.size() < WALL_DRAWERS; x++) {
                for (int z = 0; z < WALL_SIZE && placed.size() < WALL_DRAWERS; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (pos.equals(CONTROLLER)) {
                        continue;
                    }
                    helper.setBlock(pos, IDContent.ENERGY_DRAWER.getLeft().get());
                    placed.add(pos);
                }
            }
        }
        assertEquals(helper, placed.size(), WALL_DRAWERS, "drawers placed");

        StorageControllerTile<?> controller = controllerAt(helper);
        // What the Linking Tool calls. Absolute positions: the controller looks them up in the level.
        controller.addConnectedDrawers(LinkingToolItem.ActionMode.ADD,
                placed.stream().map(helper::absolutePos).toArray(BlockPos[]::new));

        ConnectedDrawers network = controller.getConnectedDrawers();
        assertEquals(helper, network.getConnectedDrawers().size(), WALL_DRAWERS,
                "drawers accepted into the controller network");

        // One-element array rather than a field: game tests run concurrently in the same level.
        @SuppressWarnings("unchecked")
        List<IItemHandler>[] settledHandlers = new List[1];

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    assertNetworkInvariantHolds(helper, network, "once the network has settled");
                    assertEquals(helper, network.getItemHandlers().size(), WALL_DRAWERS,
                            "energy drawers counted as item handlers by the controller");
                    settledHandlers[0] = network.getItemHandlers();
                })
                .thenIdle(IDLE_TICKS)
                .thenExecute(() -> {
                    assertNetworkInvariantHolds(helper, network, "after idling");
                    helper.assertTrue(network.getItemHandlers() == settledHandlers[0],
                            "The controller rebuilt its network while nothing happened: an energy "
                                    + "drawer no longer satisfies connectedDrawers == itemHandlers + "
                                    + "fluidHandlers + extensions. See CLAUDE.md §7.");
                })
                .thenSucceed();
    }

    /**
     * A cable on the Storage Controller reaches every energy drawer linked to it - through a
     * provider we attached to <em>their</em> block entity, which only works if their
     * {@code getCapability} falls through to attached providers.
     *
     * <p>Both directions: insert alone would pass with an aggregate that swallows what it is given.
     */
    @GameTest(template = WALL, timeoutTicks = 300)
    public static void theControllerMovesEnergyForItsWholeNetwork(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, FunctionalStorage.DRAWER_CONTROLLER.getLeft().get());

        List<BlockPos> placed = new ArrayList<>();
        for (int x = 0; x < 4; x++) {
            BlockPos pos = new BlockPos(x, 1, 0);
            helper.setBlock(pos, IDContent.ENERGY_DRAWER.getLeft().get());
            placed.add(pos);
        }

        StorageControllerTile<?> controller = controllerAt(helper);
        controller.addConnectedDrawers(LinkingToolItem.ActionMode.ADD,
                placed.stream().map(helper::absolutePos).toArray(BlockPos[]::new));

        int perDrawer = (int) EnergyScaling.baseCapacity();

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    IEnergyStorage network = capability(helper, CONTROLLER, null);
                    helper.assertTrue(network != null,
                            "The Storage Controller has no energy capability: the provider attached "
                                    + "by AttachCapabilitiesEvent is not reached by their getCapability");

                    assertEquals(helper, network.getMaxEnergyStored(), perDrawer * placed.size(),
                            "capacity summed over the network");

                    // More than one drawer can take, to prove it spills into the next.
                    assertEquals(helper, network.receiveEnergy(perDrawer * 3, false), perDrawer * 3,
                            "energy accepted by the network");
                    assertEquals(helper, network.getEnergyStored(), perDrawer * 3,
                            "energy stored across the network");

                    int inDrawers = 0;
                    for (BlockPos pos : placed) {
                        inDrawers += ((EnergyDrawerTile) helper.getBlockEntity(pos)).getEnergyStorage().getEnergyStored();
                    }
                    assertEquals(helper, inDrawers, perDrawer * 3, "energy actually held by the drawers");

                    assertEquals(helper, network.extractEnergy(perDrawer * 2, false), perDrawer * 2,
                            "energy extracted from the network");
                    assertEquals(helper, network.getEnergyStored(), perDrawer, "energy left in the network");
                })
                .thenSucceed();
    }

    /** The exact expression {@code StorageControllerTile.serverTick} tests before rebuilding. */
    private static void assertNetworkInvariantHolds(GameTestHelper helper, ConnectedDrawers network, String when) {
        int counted = network.getItemHandlers().size()
                + network.getFluidHandlers().size()
                + network.getExtensions();
        assertEquals(helper, counted, network.getConnectedDrawers().size(),
                "handlers+extensions counted against drawers in the network, " + when);
    }

    /** 1.20.1's {@code GameTestHelper} has no {@code assertValueEqual}; this is it. */
    private static void assertEquals(GameTestHelper helper, Object actual, Object expected, String name) {
        helper.assertTrue(Objects.equals(actual, expected),
                "Expected " + name + " to be " + expected + ", but was " + actual);
    }

    private static EnergyDrawerTile placeDrawer(GameTestHelper helper) {
        helper.setBlock(DRAWER, IDContent.ENERGY_DRAWER.getLeft().get());
        BlockEntity be = helper.getBlockEntity(DRAWER);
        helper.assertTrue(be instanceof EnergyDrawerTile, "the energy drawer has no tile behind it");
        return (EnergyDrawerTile) be;
    }

    private static StorageControllerTile<?> controllerAt(GameTestHelper helper) {
        BlockEntity be = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(be instanceof StorageControllerTile<?>, "no Storage Controller was placed");
        return (StorageControllerTile<?>) be;
    }

    /** Asked of the block entity, the way every Forge 1.20.1 cable asks. */
    private static IEnergyStorage capability(GameTestHelper helper, BlockPos pos, Direction side) {
        BlockEntity be = helper.getBlockEntity(pos);
        return be == null ? null : be.getCapability(ForgeCapabilities.ENERGY, side).orElse(null);
    }
}
