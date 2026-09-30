package dev.drimoz.immaterialdrawers.gametest;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.FramedDrawerBlock;
import com.buuz135.functionalstorage.client.model.FramedDrawerModelData;
import com.buuz135.functionalstorage.block.tile.ItemControllableDrawerTile;
import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import com.buuz135.functionalstorage.item.LinkingToolItem;
import com.buuz135.functionalstorage.item.StorageUpgradeItem;
import com.buuz135.functionalstorage.util.ConnectedDrawers;
import dev.drimoz.immaterialdrawers.IDConfig;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile;
import dev.drimoz.immaterialdrawers.block.tile.energy.FramedEnergyDrawerTile;
import dev.drimoz.immaterialdrawers.recipe.FramedDrawerRecipe;
import dev.drimoz.immaterialdrawers.registry.IDContent;
import dev.drimoz.immaterialdrawers.registry.IDFeatures;
import dev.drimoz.immaterialdrawers.storage.EnergyScaling;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import com.mojang.authlib.GameProfile;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

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

    // ---------------------------------------------------------------------------------------------
    // Step 2 - storage upgrades. On 1.20.1 there is no size component: an upgrade's effect is
    // getStorageMultiplier() / getStorageDiv(), and the energy drawer's divisor is ENERGY_DIVISOR.
    // ---------------------------------------------------------------------------------------------

    /** An unupgraded drawer is worth having on its own, or the upgrades have nothing to scale. */
    @GameTest(template = PLATFORM)
    public static void unupgradedDrawerHoldsTheBaseCapacity(GameTestHelper helper) {
        EnergyDrawerTile tile = placeDrawer(helper);
        assertEquals(helper, tile.getEnergyStorage().getCapacityLong(), EnergyScaling.baseCapacity(),
                "base capacity in FE");
        helper.succeed();
    }

    /**
     * All four upgrade slots do something (CLAUDE.md §11). Strictly increasing catches a divisor
     * that saturates early; staying positive catches an overflow. Both fail silently in game.
     */
    @GameTest(template = PLATFORM)
    public static void everyStorageUpgradeSlotChangesTheCapacity(GameTestHelper helper) {
        EnergyDrawerTile tile = placeDrawer(helper);
        Item netherite = upgrade(StorageUpgradeItem.StorageTier.NETHERITE);

        long previous = tile.getEnergyStorage().getCapacityLong();
        for (int slot = 0; slot < tile.getStorageSlotAmount(); slot++) {
            tile.getStorageUpgrades().insertItem(slot, new ItemStack(netherite), false);

            long now = tile.getEnergyStorage().getCapacityLong();
            helper.assertTrue(now > previous,
                    "Storage upgrade " + (slot + 1) + " of " + tile.getStorageSlotAmount()
                            + " did not change the capacity: still " + now + " FE. Is getStorageDiv() "
                            + "still ENERGY_DIVISOR, and does the slot accept Functional Storage's upgrades?");
            helper.assertTrue(now > 0, "Capacity overflowed to " + now + " FE after upgrade " + (slot + 1));
            previous = now;
        }
        helper.succeed();
    }

    /**
     * The Max Storage upgrade reports a multiplier of {@link Integer#MAX_VALUE}. On 1.20.1 Functional
     * Storage folds it into an int ({@code mult *= calculated}, which saturates rather than wraps),
     * and our long arithmetic takes it from there: positive, and past the int ceiling.
     */
    @GameTest(template = PLATFORM)
    public static void maxStorageUpgradeSaturatesWithoutOverflowing(GameTestHelper helper) {
        EnergyDrawerTile tile = placeDrawer(helper);
        tile.getStorageUpgrades().insertItem(0,
                new ItemStack(upgrade(StorageUpgradeItem.StorageTier.MAX_STORAGE)), false);

        long capacity = tile.getEnergyStorage().getCapacityLong();
        helper.assertTrue(capacity > 0, "The Max Storage upgrade wrapped the capacity to " + capacity);
        helper.assertTrue(capacity > (long) Integer.MAX_VALUE,
                "The Max Storage upgrade left the capacity at " + capacity + ", inside an int.");
        helper.succeed();
    }

    /**
     * An upgrade whose removal would not leave room for the stored energy stays in its slot, or
     * pulling it deletes the difference.
     */
    @GameTest(template = PLATFORM)
    public static void anUpgradeCannotBeRemovedIfTheEnergyWouldNotFit(GameTestHelper helper) {
        EnergyDrawerTile tile = placeDrawer(helper);
        tile.getStorageUpgrades().insertItem(0,
                new ItemStack(upgrade(StorageUpgradeItem.StorageTier.NETHERITE)), false);

        long upgradedCapacity = tile.getEnergyStorage().getCapacityLong();
        helper.assertTrue(upgradedCapacity > EnergyScaling.baseCapacity(), "the upgrade did not enlarge the drawer");

        // receiveEnergy is an int API, so filling a long-sized drawer takes more than one call.
        while (tile.getEnergyStorage().getStoredLong() < upgradedCapacity
                && tile.getEnergyStorage().receiveEnergy(Integer.MAX_VALUE, false) > 0) {
            // keep going until it stops accepting
        }
        helper.assertTrue(tile.getStorageUpgrades().extractItem(0, 1, false).isEmpty(),
                "The storage upgrade came out of a full drawer. Everything above the base capacity "
                        + "would have been deleted.");

        while (tile.getEnergyStorage().getStoredLong() > 0
                && tile.getEnergyStorage().extractEnergy(Integer.MAX_VALUE, false) > 0) {
            // and more than one to empty it again
        }
        helper.assertTrue(!tile.getStorageUpgrades().extractItem(0, 1, false).isEmpty(),
                "The storage upgrade is stuck in an empty drawer");
        helper.succeed();
    }

    /** Bottomless both ways, like a creative fluid drawer - and paying out without being filled. */
    @GameTest(template = PLATFORM)
    public static void aCreativeDrawerIsBottomless(GameTestHelper helper) {
        EnergyDrawerTile tile = placeDrawer(helper);
        tile.getStorageUpgrades().insertItem(0, new ItemStack(FunctionalStorage.CREATIVE_UPGRADE.get()), false);
        helper.assertTrue(tile.isCreative(), "the creative upgrade did not take");

        IEnergyStorage storage = capability(helper, DRAWER, null);
        helper.assertTrue(storage != null, "no energy capability on a creative drawer");
        assertEquals(helper, storage.getMaxEnergyStored(), Integer.MAX_VALUE, "creative capacity");
        assertEquals(helper, storage.getEnergyStored(), Integer.MAX_VALUE, "creative contents");

        assertEquals(helper, storage.extractEnergy(1_000_000, false), 1_000_000, "first extraction");
        assertEquals(helper, storage.extractEnergy(1_000_000, false), 1_000_000, "second extraction");
        assertEquals(helper, storage.getEnergyStored(), Integer.MAX_VALUE, "contents after extracting");
        helper.succeed();
    }

    /**
     * Pushing energy out never creates any: a drawer holding 1 FE once handed a neighbour 2,500 and
     * lost 1. Checked against a real receiver from a real mod - Powah's starter cell, in the dev run
     * for exactly this (CLAUDE.md §4). The assertion is conservation, not how much moves.
     */
    @GameTest(template = PLATFORM, timeoutTicks = 300)
    public static void pushingEnergyNeverCreatesIt(GameTestHelper helper) {
        Block cell = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("powah", "energy_cell_starter"));
        helper.assertTrue(cell != null && cell != Blocks.AIR,
                "Powah is not in the run, so this test cannot check what it exists to check. It is "
                        + "declared modRuntimeOnly in build.gradle.");

        BlockPos cellPos = DRAWER.east();
        EnergyDrawerTile tile = placeDrawer(helper);
        helper.setBlock(cellPos, cell);

        final int seeded = 1;
        assertEquals(helper, tile.getEnergyStorage().receiveEnergy(seeded, false), seeded, "seeded energy");

        helper.startSequence()
                // Several pushes: the interval is four ticks.
                .thenIdle(40)
                .thenExecute(() -> {
                    long inDrawer = tile.getEnergyStorage().getStoredLong();
                    IEnergyStorage cellStorage = capability(helper, cellPos, null);
                    helper.assertTrue(cellStorage != null, "the Powah cell has no energy capability");
                    long inCell = cellStorage.getEnergyStored();

                    assertEquals(helper, inDrawer + inCell, (long) seeded,
                            "total FE across the drawer and its neighbour. More than was put in means "
                                    + "the push offers more than the drawer holds");
                })
                .thenSucceed();
    }

    // ---------------------------------------------------------------------------------------------
    // Step 3 - the framed variant, the data, and the player-facing regressions of 0.1.0.
    // ---------------------------------------------------------------------------------------------

    /** Its own block entity type, and still answering every cable. */
    @GameTest(template = PLATFORM)
    public static void framedDrawerHasItsOwnEnergyCapability(GameTestHelper helper) {
        helper.setBlock(DRAWER, IDContent.FRAMED_ENERGY_DRAWER.getLeft().get());
        helper.assertTrue(IDContent.FRAMED_ENERGY_DRAWER.getRight().get() != IDContent.ENERGY_DRAWER.getRight().get(),
                "the two drawers share a block entity type, so this test proves nothing");

        IEnergyStorage storage = capability(helper, DRAWER, null);
        helper.assertTrue(storage != null, "No energy capability on the framed energy drawer");
        assertEquals(helper, storage.receiveEnergy(1_000, false), 1_000, "energy accepted");
        helper.succeed();
    }

    /**
     * Our framing recipe takes our framed drawer, and the stack it builds carries the design the way
     * Functional Storage's does - their {@code fill}, their {@code Style} tag. Theirs cannot take our
     * block on 1.20.1: it tests their classes.
     */
    @GameTest(template = PLATFORM)
    public static void framedDrawerIsFramableByOurRecipe(GameTestHelper helper) {
        ItemStack drawer = new ItemStack(IDContent.FRAMED_ENERGY_DRAWER.getLeft().get());
        ItemStack side = new ItemStack(Items.OAK_PLANKS);
        ItemStack front = new ItemStack(Items.STONE);

        helper.assertTrue(FramedDrawerRecipe.matches(side, front, drawer), "our framing recipe rejected our framed drawer");
        helper.assertTrue(!FramedDrawerRecipe.matches(side, front, new ItemStack(IDContent.ENERGY_DRAWER.getLeft().get())),
                "our framing recipe accepted the unframed drawer");

        ItemStack framed = FramedDrawerBlock.fill(side, front, drawer, new ItemStack(Items.DEEPSLATE));
        FramedDrawerModelData design = FramedDrawerBlock.getDrawerModelData(framed);
        helper.assertTrue(design != null, "framing produced a stack with no style on it");
        helper.assertTrue(design.getDesign().get("front") == Items.STONE, "the front is not what it was framed with");
        helper.assertTrue(design.getDesign().get("side") == Items.OAK_PLANKS, "the sides are not what they were framed with");

        helper.assertTrue(helper.getLevel().getRecipeManager()
                        .byKey(new ResourceLocation(ImmaterialDrawers.MOD_ID, "framed")).isPresent(),
                "the framing recipe is not loaded, so no crafting grid will ever frame a drawer");
        helper.succeed();
    }

    /** The placed drawer keeps its design through a save, and hands it to the model. */
    @GameTest(template = PLATFORM)
    public static void framedDrawerRemembersItsDesign(GameTestHelper helper) {
        helper.setBlock(DRAWER, IDContent.FRAMED_ENERGY_DRAWER.getLeft().get());
        helper.assertTrue(helper.getBlockEntity(DRAWER) instanceof FramedEnergyDrawerTile,
                "the framed energy drawer has the wrong tile behind it");
        FramedEnergyDrawerTile tile = (FramedEnergyDrawerTile) helper.getBlockEntity(DRAWER);

        Map<String, Item> design = new HashMap<>();
        design.put("particle", Items.OAK_PLANKS);
        design.put("side", Items.OAK_PLANKS);
        design.put("front", Items.STONE);
        design.put("front_divider", Items.DEEPSLATE);
        tile.setFramedDrawerModelData(new FramedDrawerModelData(design));

        // Through NBT and back, the way a reload does - the @Save field is only there if the tile
        // class was scanned.
        FramedEnergyDrawerTile reloaded = (FramedEnergyDrawerTile) BlockEntity.loadStatic(
                tile.getBlockPos(), tile.getBlockState(), tile.saveWithFullMetadata());
        helper.assertTrue(reloaded != null && reloaded.getFramedDrawerModelData().getDesign().get("front") == Items.STONE,
                "the design did not survive a save - is FramedEnergyDrawerTile scanned by NBTManager?");
        helper.assertTrue(tile.getModelData().get(FramedDrawerModelData.FRAMED_PROPERTY) != null,
                "the drawer's ModelData carries no design, so the model has nothing to render with");
        helper.succeed();
    }

    /** 0.1.0 shipped with no mineable tag: no tool was correct, and a broken drawer dropped nothing. */
    @GameTest(template = PLATFORM)
    public static void aPickaxeIsTheRightToolForEveryDrawer(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
        ForgeRegistries.BLOCKS.getValues().stream()
                .filter(block -> ImmaterialDrawers.MOD_ID.equals(ForgeRegistries.BLOCKS.getKey(block).getNamespace()))
                .forEach(block -> helper.assertTrue(player.hasCorrectToolForDrops(block.defaultBlockState()),
                        ForgeRegistries.BLOCKS.getKey(block) + " drops nothing when mined with a pickaxe"));
        helper.succeed();
    }

    /** 0.1.0 threw on the server when the front was clicked with anything in hand. */
    @GameTest(template = PLATFORM)
    public static void clickingTheFrontWithAnItemDoesNotThrow(GameTestHelper helper) {
        EnergyDrawerTile tile = placeDrawer(helper);
        Player player = helper.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));

        tile.onSlotActivated(player, InteractionHand.MAIN_HAND, Direction.NORTH, 0.5, 0.5, 0.5, 0);
        assertEquals(helper, player.getMainHandItem().getCount(), 1, "sticks left in hand");
        tile.onClicked(player, 0);
        helper.succeed();
    }

    /** The config switch reaches the recipe condition, and with everything on the recipes are loaded. */
    @GameTest(template = PLATFORM)
    public static void aDisabledFeatureLosesItsRecipe(GameTestHelper helper) {
        ICondition condition = IDFeatures.enabled(IDFeatures.Feature.ENERGY_DRAWER);
        boolean before = IDConfig.ENERGY_DRAWER_ENABLED;
        try {
            IDConfig.ENERGY_DRAWER_ENABLED = false;
            helper.assertTrue(!condition.test(ICondition.IContext.EMPTY), "the recipe condition ignores a disabled energy drawer");
            IDConfig.ENERGY_DRAWER_ENABLED = true;
            helper.assertTrue(condition.test(ICondition.IContext.EMPTY), "the recipe condition refuses an enabled energy drawer");
        } finally {
            IDConfig.ENERGY_DRAWER_ENABLED = before;
        }
        for (String recipe : List.of("energy_drawer", "framed_energy_drawer", "wireless_charger")) {
            helper.assertTrue(helper.getLevel().getRecipeManager()
                            .byKey(new ResourceLocation(ImmaterialDrawers.MOD_ID, recipe)).isPresent(),
                    "recipe " + recipe + " is missing although its feature is enabled");
        }
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------
    // Step 4 - the Wireless Charger, and Functional Storage's Redstone Upgrade.
    // ---------------------------------------------------------------------------------------------

    /**
     * The charger goes into a utility slot and fills what a nearby player carries, conserving
     * energy. On 1.20.1 this also proves our tick finds it: Functional Storage never calls it.
     *
     * <p>{@code makeMockPlayer} builds a Player the level does not know, so it is positioned and added
     * by hand, or {@code getEntitiesOfClass} never sees it.
     */
    @GameTest(template = PLATFORM, timeoutTicks = 300)
    public static void wirelessChargerFillsGearWithoutInventingEnergy(GameTestHelper helper) {
        Item battery = ForgeRegistries.ITEMS.getValue(new ResourceLocation("powah", "battery_basic"));
        helper.assertTrue(battery != null && battery != Items.AIR,
                "Powah is not in the run, so there is no chargeable item to test with");

        EnergyDrawerTile tile = placeDrawer(helper);
        ItemStack left = tile.getUtilityUpgrades().insertItem(0, new ItemStack(IDContent.WIRELESS_CHARGER.get()), false);
        helper.assertTrue(left.isEmpty(), "the drawer's utility slot refused the Wireless Charger");

        final int seeded = 50_000;
        tile.getEnergyStorage().receiveEnergy(seeded, false);

        // Forge's FakePlayer, added to the level so getEntitiesOfClass sees it. Not makeMockPlayer: once Ars
        // is in the run, its mana tick sends every ticking player a packet and casts to ServerPlayer.
        // Not makeMockServerPlayerInLevel: its connection has no channel, and the first packet throws.
        // A FakePlayer is a ServerPlayer whose network handler swallows packets.
        Player player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "charger_test"));
        player.setPos(helper.absoluteVec(new Vec3(DRAWER.getX() + 0.5, DRAWER.getY(), DRAWER.getZ() + 0.5)));
        helper.getLevel().addFreshEntity(player);
        ItemStack cell = new ItemStack(battery);
        player.getInventory().items.set(0, cell);

        helper.startSequence()
                .thenIdle(60)
                .thenExecute(() -> {
                    IEnergyStorage carried = cell.getCapability(ForgeCapabilities.ENERGY).orElse(null);
                    helper.assertTrue(carried != null, "the Powah battery exposes no energy capability");
                    long inBattery = carried.getEnergyStored();
                    long inDrawer = tile.getEnergyStorage().getStoredLong();

                    helper.assertTrue(inBattery > 0,
                            "The charger moved nothing: EnergyDrawerTile.serverTick does not find it in the "
                                    + "utility slots, or it does not find the player");
                    assertEquals(helper, inDrawer + inBattery, (long) seeded,
                            "total FE across the drawer and the battery it charged");
                    player.discard();
                })
                .thenSucceed();
    }

    /**
     * Functional Storage's own Redstone Upgrade drives a signal from the charge - the same number the
     * comparator reads. Their tick updates the neighbours for it; the signal is ours, because theirs
     * reads the empty item handler.
     */
    @GameTest(template = PLATFORM)
    public static void functionalStorageRedstoneUpgradeReadsTheCharge(GameTestHelper helper) {
        EnergyDrawerTile tile = placeDrawer(helper);
        BlockPos absolute = helper.absolutePos(DRAWER);
        var state = helper.getBlockState(DRAWER);

        assertEquals(helper, state.getSignal(helper.getLevel(), absolute, Direction.NORTH), 0,
                "signal with no Redstone Upgrade");

        tile.getUtilityUpgrades().insertItem(0, new ItemStack(FunctionalStorage.REDSTONE_UPGRADE.get()), false);
        assertEquals(helper, state.getSignal(helper.getLevel(), absolute, Direction.NORTH), 0,
                "signal from an empty drawer");

        // Half full: 1 + 0.5 * 14 = 8.
        tile.getEnergyStorage().receiveEnergy((int) (tile.getEnergyStorage().getCapacityRaw() / 2), false);
        int signal = state.getSignal(helper.getLevel(), absolute, Direction.NORTH);
        assertEquals(helper, signal, 8, "redstone signal at half charge");
        assertEquals(helper, signal, state.getAnalogOutputSignal(helper.getLevel(), absolute),
                "redstone signal against comparator signal");
        helper.succeed();
    }

    private static Item upgrade(StorageUpgradeItem.StorageTier tier) {
        return FunctionalStorage.STORAGE_UPGRADES.get(tier).get();
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
