package dev.drimoz.immaterialdrawers.gametest;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.FramedDrawerBlock;
import com.buuz135.functionalstorage.block.tile.FramedTile;
import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import com.buuz135.functionalstorage.client.model.FramedDrawerModelData;
import com.buuz135.functionalstorage.recipe.FramedDrawerRecipe;
import com.buuz135.functionalstorage.item.LinkingToolItem;
import com.buuz135.functionalstorage.item.StorageUpgradeItem;
import com.buuz135.functionalstorage.util.ConnectedDrawers;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile;
import dev.drimoz.immaterialdrawers.block.tile.energy.FramedEnergyDrawerTile;
import dev.drimoz.immaterialdrawers.registry.IDContent;
import dev.drimoz.immaterialdrawers.storage.EnergyScaling;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /** An unupgraded drawer is worth having on its own, or the upgrades have nothing to scale. */
    @GameTest(template = PLATFORM)
    public static void unupgradedDrawerHoldsTheBaseCapacity(GameTestHelper helper) {
        EnergyDrawerTile tile = placeDrawer(helper);

        helper.assertValueEqual(tile.getEnergyStorage().getCapacityLong(),
                (long) EnergyScaling.BASE_UNITS * EnergyScaling.FE_PER_UNIT, "base capacity in FE");
        helper.succeed();
    }

    /**
     * All four upgrade slots do something, and the fourth one still fits in an int.
     *
     * <p>This is CLAUDE.md §11 turned into an assertion. {@code IEnergyStorage} is an int API, and
     * Functional Storage's storage upgrades are multiplicative across four slots — so a base chosen
     * without doing the arithmetic saturates on the third upgrade and the player's fourth Netherite
     * upgrade, the most expensive item in the chain, does <em>nothing</em>. Nothing crashes, nothing
     * logs, and the tooltip reads the same before and after.
     *
     * <p>Both halves are asserted deliberately: strictly increasing catches a divisor that is too
     * small, and staying under the ceiling catches one that is too large.
     */
    @GameTest(template = PLATFORM)
    public static void everyStorageUpgradeSlotChangesTheCapacity(GameTestHelper helper) {
        EnergyDrawerTile tile = placeDrawer(helper);
        Item netherite = upgrade(StorageUpgradeItem.StorageTier.NETHERITE);

        // The long accessor, not the capability one. Past the fourth Netherite upgrade the clamped
        // int view saturates, and a test written against it would report the ceiling as a bug in
        // the curve - which is the very thing the ceiling stopped being.
        long previous = tile.getEnergyStorage().getCapacityLong();
        for (int slot = 0; slot < tile.getStorageSlotAmount(); slot++) {
            tile.getStorageUpgrades().insertItem(slot, new ItemStack(netherite), false);

            long now = tile.getEnergyStorage().getCapacityLong();
            helper.assertTrue(now > previous,
                    "Storage upgrade " + (slot + 1) + " of " + tile.getStorageSlotAmount()
                            + " did not change the capacity: still " + now + " FE. The int ceiling "
                            + "has been hit early — EnergyScaling.ENERGY_DIVISOR is too small for "
                            + "the base, and the last upgrade slots are decoration.");
            helper.assertTrue(now > 0,
                    "Capacity overflowed to " + now + " FE after upgrade " + (slot + 1));
            previous = now;
        }

        helper.succeed();
    }

    /**
     * The Max Storage upgrade does not wrap the capacity negative.
     *
     * <p>It reports a multiplier of {@link Integer#MAX_VALUE}, so the cast to a capacity is where an
     * energy drawer would go negative and start refusing every FE offered to it. In a long it lands
     * around 5.4e14 and never reaches the clamp - which is the point of moving to long, and the
     * reason this asserts the property rather than a number: the arithmetic has to stay positive and
     * huge, and whether it saturates is a detail of where the curve happens to fall.
     */
    @GameTest(template = PLATFORM)
    public static void maxStorageUpgradeSaturatesWithoutOverflowing(GameTestHelper helper) {
        EnergyDrawerTile tile = placeDrawer(helper);
        tile.getStorageUpgrades().insertItem(0,
                new ItemStack(upgrade(StorageUpgradeItem.StorageTier.MAX_STORAGE)), false);

        long capacity = tile.getEnergyStorage().getCapacityLong();
        helper.assertTrue(capacity > 0,
                "The Max Storage upgrade wrapped the capacity to " + capacity
                        + ". A drawer with a negative capacity refuses every FE offered to it.");
        helper.assertTrue(capacity > (long) Integer.MAX_VALUE,
                "The Max Storage upgrade left the capacity at " + capacity + ", inside an int. "
                        + "Something is still clamping to int where it should not.");
        helper.succeed();
    }

    /**
     * An upgrade whose removal would not leave room for the stored energy stays in its slot.
     *
     * <p>Functional Storage guards the same case with {@code canChangeMultiplier}. Without it,
     * pulling an upgrade silently deletes the difference, and from the player's side the drawer ate
     * their power.
     */
    @GameTest(template = PLATFORM)
    public static void anUpgradeCannotBeRemovedIfTheEnergyWouldNotFit(GameTestHelper helper) {
        EnergyDrawerTile tile = placeDrawer(helper);
        tile.getStorageUpgrades().insertItem(0,
                new ItemStack(upgrade(StorageUpgradeItem.StorageTier.NETHERITE)), false);

        long upgradedCapacity = tile.getEnergyStorage().getCapacityLong();
        long base = (long) EnergyScaling.BASE_UNITS * EnergyScaling.FE_PER_UNIT;
        helper.assertTrue(upgradedCapacity > base, "the upgrade did not enlarge the drawer");

        // More than the drawer could hold without the upgrade.
        // receiveEnergy is an int API, so filling a long-sized drawer takes more than one call.
        while (tile.getEnergyStorage().getStoredLong() < upgradedCapacity
                && tile.getEnergyStorage().receiveEnergy(Integer.MAX_VALUE, false) > 0) {
            // keep going until it stops accepting
        }

        helper.assertTrue(tile.getStorageUpgrades().extractItem(0, 1, false).isEmpty(),
                "The storage upgrade came out of a full drawer. Everything above the base capacity "
                        + "would have been deleted.");

        // Drained back under the base, it comes out.
        while (tile.getEnergyStorage().getStoredLong() > 0
                && tile.getEnergyStorage().extractEnergy(Integer.MAX_VALUE, false) > 0) {
            // and more than one to empty it again
        }
        helper.assertTrue(!tile.getStorageUpgrades().extractItem(0, 1, false).isEmpty(),
                "The storage upgrade is stuck in an empty drawer");

        helper.succeed();
    }

    /**
     * A creative drawer is bottomless in both directions, the way a creative fluid drawer is.
     *
     * <p>Mirrors {@code BigFluidHandler.CustomFluidTank}: capacity and contents both read
     * {@link Integer#MAX_VALUE}, and draining hands out whatever was asked for without depleting.
     * The naive version of this — reporting the configured capacity and extracting
     * {@code min(stored, asked)} — returns zero forever from a creative drawer nobody filled first.
     */
    @GameTest(template = PLATFORM)
    public static void aCreativeDrawerIsBottomless(GameTestHelper helper) {
        EnergyDrawerTile tile = placeDrawer(helper);
        tile.getStorageUpgrades().insertItem(0, new ItemStack(FunctionalStorage.CREATIVE_UPGRADE.get()), false);
        helper.assertTrue(tile.isCreative(), "the creative upgrade did not take");

        IEnergyStorage storage = capability(helper, null);
        helper.assertTrue(storage != null, "no EnergyStorage capability on a creative drawer");
        helper.assertValueEqual(storage.getMaxEnergyStored(), Integer.MAX_VALUE, "creative capacity");
        helper.assertValueEqual(storage.getEnergyStored(), Integer.MAX_VALUE, "creative contents");

        // Never filled, and it still pays out - twice.
        helper.assertValueEqual(storage.extractEnergy(1_000_000, false), 1_000_000, "first extraction");
        helper.assertValueEqual(storage.extractEnergy(1_000_000, false), 1_000_000, "second extraction");
        helper.assertValueEqual(storage.getEnergyStored(), Integer.MAX_VALUE, "contents after extracting");

        helper.succeed();
    }

    /**
     * The framed variant has its own block entity type, so it needs its own capability provider.
     *
     * <p>Providers are registered against a {@code BlockEntityType}, and
     * {@code registerBlockWithTileItem} builds a fresh one per block. Registering only the unframed
     * drawer leaves the framed one with no energy capability at all — it places, it renders, it
     * joins a controller network, and every cable in the game ignores it.
     */
    @GameTest(template = PLATFORM)
    public static void framedDrawerHasItsOwnEnergyCapability(GameTestHelper helper) {
        helper.setBlock(DRAWER, IDContent.FRAMED_ENERGY_DRAWER.getBlock());

        helper.assertTrue(
                IDContent.FRAMED_ENERGY_DRAWER.type().get() != IDContent.ENERGY_DRAWER.type().get(),
                "the two drawers share a block entity type, so this test proves nothing");

        IEnergyStorage storage = capability(helper, null);
        helper.assertTrue(storage != null,
                "No EnergyStorage capability on the framed energy drawer. Its block entity type is "
                        + "not the unframed one's, and a provider registered on that type does not "
                        + "cover this one.");

        helper.assertValueEqual(storage.receiveEnergy(1_000, false), 1_000, "energy accepted");
        helper.succeed();
    }

    /**
     * Functional Storage's own framing recipe accepts our drawer, with nothing added on our side.
     *
     * <p>This is the payoff of {@code FramedBlock} being an empty marker interface that
     * {@code FramedDrawerRecipe} tests with {@code instanceof}: the recipe generalises to a block
     * from another mod by accident of how it was written. It is the only extension point in
     * Functional Storage that does — which is exactly why it is worth a test rather than an
     * assumption, and why the test should fail loudly if a release ever narrows it to their own
     * blocks.
     */
    @GameTest(template = PLATFORM)
    public static void framedDrawerIsFramableByFunctionalStorage(GameTestHelper helper) {
        ItemStack drawer = new ItemStack(IDContent.FRAMED_ENERGY_DRAWER.asItem());
        CraftingInput grid = CraftingInput.of(2, 2, List.of(
                new ItemStack(Items.OAK_PLANKS),   // sides and particle
                new ItemStack(Items.STONE),        // front
                drawer,
                new ItemStack(Items.DEEPSLATE)));  // divider

        helper.assertTrue(new FramedDrawerRecipe().matches(grid, helper.getLevel()),
                "Functional Storage's framing recipe rejected our framed drawer");

        ItemStack framed = FramedDrawerBlock.fill(grid.getItem(0), grid.getItem(1),
                grid.getItem(2), grid.getItem(3));
        FramedDrawerModelData design = FramedDrawerBlock.getDrawerModelData(framed);

        helper.assertTrue(design != null, "framing produced a stack with no style on it");
        helper.assertTrue(design.getDesign().get("front") == Items.STONE,
                "the front of the framed drawer is not what it was framed with");
        helper.assertTrue(design.getDesign().get("side") == Items.OAK_PLANKS,
                "the sides of the framed drawer are not what it was framed with");

        helper.succeed();
    }

    /**
     * The placed drawer holds on to its design, and hands it to the renderer.
     *
     * <p>Two separate things, both easy to lose: the {@code @Save} field that survives a reload —
     * Titanium's annotation scan is per class, and the framed tile adds a field the base tile does
     * not have — and the {@code ModelData} the block model reads the textures out of.
     */
    @GameTest(template = PLATFORM)
    public static void framedDrawerRemembersItsDesign(GameTestHelper helper) {
        helper.setBlock(DRAWER, IDContent.FRAMED_ENERGY_DRAWER.getBlock());
        BlockEntity be = helper.getBlockEntity(DRAWER);

        helper.assertTrue(be instanceof FramedEnergyDrawerTile,
                "the framed energy drawer has the wrong tile behind it");
        helper.assertTrue(be instanceof FramedTile,
                "the framed drawer is not a FramedTile, so none of Functional Storage's framing "
                        + "code will see it");
        FramedEnergyDrawerTile tile = (FramedEnergyDrawerTile) be;

        Map<String, Item> design = new HashMap<>();
        design.put("particle", Items.OAK_PLANKS);
        design.put("side", Items.OAK_PLANKS);
        design.put("front", Items.STONE);
        design.put("front_divider", Items.DEEPSLATE);
        tile.setFramedDrawerModelData(new FramedDrawerModelData(design));

        helper.assertTrue(tile.getFramedDrawerModelData().getDesign().get("front") == Items.STONE,
                "the drawer did not keep the design it was given");
        helper.assertTrue(
                tile.getModelData().get(FramedDrawerModelData.FRAMED_PROPERTY) != null,
                "the drawer's ModelData carries no design, so the model has nothing to render with");

        helper.succeed();
    }

    /**
     * A cable on the Storage Controller reaches every energy drawer linked to it.
     *
     * <p>The controller already stands in for the items and the fluids of its network; it cannot do
     * the same for energy, because it collects our deliberately empty item handler and has no third
     * kind of content to look for. So the aggregate is registered from our side, against
     * <em>their</em> block entity type — NeoForge never asks who owns a type — reading the linked
     * positions off the public {@code getConnectedDrawers()}. See CLAUDE.md §7.
     *
     * <p>Both directions are asserted. Insert alone would pass with an aggregate that reports a
     * capacity and swallows what it is given.
     */
    @GameTest(template = WALL, timeoutTicks = 300)
    public static void theControllerMovesEnergyForItsWholeNetwork(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, FunctionalStorage.DRAWER_CONTROLLER.getBlock());

        List<BlockPos> placed = new ArrayList<>();
        for (int x = 0; x < 4; x++) {
            BlockPos pos = new BlockPos(x, 1, 0);
            helper.setBlock(pos, IDContent.ENERGY_DRAWER.getBlock());
            placed.add(pos);
        }

        StorageControllerTile<?> controller = (StorageControllerTile<?>) helper.getBlockEntity(CONTROLLER);
        controller.addConnectedDrawers(LinkingToolItem.ActionMode.ADD,
                placed.stream().map(helper::absolutePos).toArray(BlockPos[]::new));

        int perDrawer = EnergyScaling.BASE_UNITS * EnergyScaling.FE_PER_UNIT;

        helper.startSequence()
                // The controller builds its network on its own tick, not when the link is made.
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    IEnergyStorage network = helper.getLevel().getCapability(
                            Capabilities.EnergyStorage.BLOCK, helper.absolutePos(CONTROLLER), null);
                    helper.assertTrue(network != null,
                            "The Storage Controller has no EnergyStorage capability, so nothing can "
                                    + "push or pull energy through it");

                    helper.assertValueEqual(network.getMaxEnergyStored(), perDrawer * placed.size(),
                            "capacity summed over the network");

                    // Insert more than one drawer can take, to prove it spills into the next.
                    int inserted = network.receiveEnergy(perDrawer * 3, false);
                    helper.assertValueEqual(inserted, perDrawer * 3, "energy accepted by the network");
                    helper.assertValueEqual(network.getEnergyStored(), perDrawer * 3, "energy stored across the network");

                    // And that it really landed in the drawers, not in the aggregate.
                    int inDrawers = 0;
                    for (BlockPos pos : placed) {
                        inDrawers += ((EnergyDrawerTile) helper.getBlockEntity(pos)).getEnergyStorage().getEnergyStored();
                    }
                    helper.assertValueEqual(inDrawers, perDrawer * 3, "energy actually held by the drawers");

                    int extracted = network.extractEnergy(perDrawer * 2, false);
                    helper.assertValueEqual(extracted, perDrawer * 2, "energy extracted from the network");
                    helper.assertValueEqual(network.getEnergyStored(), perDrawer, "energy left in the network");
                })
                .thenSucceed();
    }

    private static Item upgrade(StorageUpgradeItem.StorageTier tier) {
        return FunctionalStorage.STORAGE_UPGRADES.get(tier).get();
    }

    private static EnergyDrawerTile placeDrawer(GameTestHelper helper) {
        helper.setBlock(DRAWER, IDContent.ENERGY_DRAWER.getBlock());
        BlockEntity be = helper.getBlockEntity(DRAWER);
        helper.assertTrue(be instanceof EnergyDrawerTile, "the energy drawer has no tile behind it");
        return (EnergyDrawerTile) be;
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
