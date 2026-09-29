package dev.drimoz.immaterialdrawers.gametest;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.tile.FluidDrawerTile;
import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import com.buuz135.functionalstorage.item.LinkingToolItem;
import com.buuz135.functionalstorage.item.StorageUpgradeItem;
import com.buuz135.functionalstorage.util.ConnectedDrawers;
import com.hrznstudio.titanium.module.BlockWithTile;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.tile.chemical.ChemicalDrawerTile;
import dev.drimoz.immaterialdrawers.registry.IDChemicalContent;
import dev.drimoz.immaterialdrawers.storage.chemical.ChemicalCapabilities;
import mekanism.api.Action;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * The Chemical Drawers, tested against Mekanism itself rather than against a stand-in.
 *
 * <p><b>Mekanism only - see {@code compat.Mods}.</b> Not a {@code @GameTestHolder}: NeoForge
 * initialises every holder class it finds in a dev run whether Mekanism is there or not, and this one
 * uses Mekanism on every line. {@code IDChemicalContent.init} registers it through
 * {@code RegisterGameTestsEvent} when Mekanism is present, and each test names its template namespace
 * itself, since there is no holder to take it from.
 *
 * <p>What is asserted is mostly <em>parity</em>: a chemical drawer is supposed to behave like the
 * fluid drawer beside it, so where Functional Storage has an answer - capacity, fill order, locking -
 * the test asks their drawer and compares, instead of hardcoding what we believe their answer is.
 */
@PrefixGameTestTemplate(false)
public final class IDChemicalGameTests {

    private static final String NS = ImmaterialDrawers.MOD_ID;
    private static final String PLATFORM = "energy_platform";
    private static final String WALL = "drawer_wall";

    private static final BlockPos DRAWER = new BlockPos(1, 1, 1);
    private static final BlockPos BESIDE = new BlockPos(0, 1, 1);
    private static final BlockPos CONTROLLER = new BlockPos(5, 1, 5);
    private static final int WALL_SIZE = 11;
    private static final int SETTLE_TICKS = 10;

    private IDChemicalGameTests() {
    }

    /**
     * Our capability object is Mekanism's own. {@code ChemicalCapabilities} recreates it by name from
     * the API instead of importing Mekanism's implementation class, which is only right if NeoForge
     * hands back the same interned instance. If it ever did not, every tube would ignore every drawer
     * and nothing would say why.
     */
    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void weSpeakMekanismsOwnCapability(GameTestHelper helper) {
        helper.assertTrue(ChemicalCapabilities.BLOCK == mekanism.common.capabilities.Capabilities.CHEMICAL.block(),
                "our chemical BlockCapability is not Mekanism's instance");
        helper.assertTrue(ChemicalCapabilities.ITEM == mekanism.common.capabilities.Capabilities.CHEMICAL.item(),
                "our chemical ItemCapability is not Mekanism's instance");
        helper.succeed();
    }

    /** All six blocks answer, from every side, with one tank per slot. */
    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void everyLayoutHasTheCapabilityFromEverySide(GameTestHelper helper) {
        for (FunctionalStorage.DrawerType type : IDChemicalContent.TYPES) {
            for (boolean framed : new boolean[]{false, true}) {
                helper.setBlock(DRAWER, IDChemicalContent.drawer(type, framed).getBlock());
                for (Direction side : Direction.values()) {
                    IChemicalHandler handler = capability(helper, side);
                    helper.assertTrue(handler != null, IDChemicalContent.nameFor(type, framed)
                            + " has no chemical capability from " + side);
                    helper.assertValueEqual(handler.getChemicalTanks(), type.getSlots(),
                            "tanks on " + IDChemicalContent.nameFor(type, framed));
                }
            }
        }
        helper.succeed();
    }

    /** It is still an item drawer with no items, which is what keeps it on a controller's network. */
    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void countsAsAnItemDrawerButHoldsNoItems(GameTestHelper helper) {
        ChemicalDrawerTile tile = place(helper, FunctionalStorage.DrawerType.X_4, false);
        helper.assertTrue(tile instanceof com.buuz135.functionalstorage.block.tile.ItemControllableDrawerTile<?>,
                "ChemicalDrawerTile is not an ItemControllableDrawerTile - the controller will drop it");
        helper.assertValueEqual(tile.getStorage().getSlots(), 0, "item slots on a chemical drawer");
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void storesWhatItIsGiven(GameTestHelper helper) {
        place(helper, FunctionalStorage.DrawerType.X_1, false);
        IChemicalHandler handler = capability(helper, Direction.UP);

        ChemicalStack remainder = handler.insertChemical(hydrogen(1000), Action.EXECUTE);
        helper.assertTrue(remainder.isEmpty(), "a fresh drawer refused 1000 mB of hydrogen");
        helper.assertValueEqual(handler.getChemicalInTank(0).getAmount(), 1000L, "hydrogen stored");

        ChemicalStack out = handler.extractChemical(400, Action.EXECUTE);
        helper.assertTrue(out.is(chemical("hydrogen")), "extracted something other than hydrogen");
        helper.assertValueEqual(out.getAmount(), 400L, "hydrogen extracted");
        helper.assertValueEqual(handler.getChemicalInTank(0).getAmount(), 600L, "hydrogen left");
        helper.succeed();
    }

    /**
     * Capacity is the fluid drawer's, layout for layout and upgrade for upgrade - asked of a real
     * Functional Storage fluid drawer placed beside ours, not written down from memory.
     */
    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void capacityMatchesTheFluidDrawer(GameTestHelper helper) {
        List<BlockWithTile> fluid = List.of(
                FunctionalStorage.FLUID_DRAWER_1, FunctionalStorage.FLUID_DRAWER_2, FunctionalStorage.FLUID_DRAWER_4);
        for (int i = 0; i < IDChemicalContent.TYPES.size(); i++) {
            FunctionalStorage.DrawerType type = IDChemicalContent.TYPES.get(i);
            ChemicalDrawerTile ours = place(helper, type, false);
            helper.setBlock(BESIDE, fluid.get(i).getBlock());
            FluidDrawerTile theirs = (FluidDrawerTile) helper.getBlockEntity(BESIDE);

            helper.assertValueEqual(ours.getChemicalHandler().getChemicalTankCapacity(0),
                    (long) theirs.getFluidHandler().getTankCapacity(0), "base capacity, " + type);

            ItemStack upgrade = new ItemStack(FunctionalStorage.STORAGE_UPGRADES.get(StorageUpgradeItem.StorageTier.GOLD).get());
            ours.getStorageUpgrades().insertItem(0, upgrade.copy(), false);
            theirs.getStorageUpgrades().insertItem(0, upgrade.copy(), false);
            helper.assertValueEqual(ours.getChemicalHandler().getChemicalTankCapacity(0),
                    (long) theirs.getFluidHandler().getTankCapacity(0), "capacity with a gold upgrade, " + type);
        }
        helper.succeed();
    }

    /**
     * {@code BigFluidHandler.fill}'s order: a second chemical goes to an empty tank, and more of the
     * first goes back to the tank already holding it - not into another empty one.
     */
    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void fillsTanksInTheFluidDrawersOrder(GameTestHelper helper) {
        place(helper, FunctionalStorage.DrawerType.X_4, false);
        IChemicalHandler handler = capability(helper, null);

        handler.insertChemical(hydrogen(100), Action.EXECUTE);
        handler.insertChemical(oxygen(100), Action.EXECUTE);
        handler.insertChemical(hydrogen(50), Action.EXECUTE);

        helper.assertTrue(handler.getChemicalInTank(0).is(chemical("hydrogen")), "tank 0 should hold hydrogen");
        helper.assertValueEqual(handler.getChemicalInTank(0).getAmount(), 150L, "hydrogen gathered in one tank");
        helper.assertTrue(handler.getChemicalInTank(1).is(chemical("oxygen")), "tank 1 should hold oxygen");
        helper.assertTrue(handler.getChemicalInTank(2).isEmpty(), "tank 2 should still be empty");
        helper.succeed();
    }

    /** A locked drawer keeps its chemical when emptied, and refuses any other. */
    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void aLockedDrawerKeepsItsChemical(GameTestHelper helper) {
        ChemicalDrawerTile tile = place(helper, FunctionalStorage.DrawerType.X_1, false);
        IChemicalHandler handler = capability(helper, null);
        handler.insertChemical(hydrogen(500), Action.EXECUTE);
        tile.setLocked(true);
        handler.extractChemical(500, Action.EXECUTE);

        helper.assertTrue(handler.getChemicalInTank(0).isEmpty(), "the drawer should be empty");
        helper.assertValueEqual(handler.insertChemical(oxygen(100), Action.EXECUTE).getAmount(), 100L,
                "oxygen refused by a drawer locked to hydrogen");
        helper.assertTrue(handler.insertChemical(hydrogen(100), Action.EXECUTE).isEmpty(),
                "hydrogen accepted by a drawer locked to hydrogen");
        helper.assertTrue(!tile.isEverythingEmpty(),
                "a locked drawer with an assignment reads as empty, and would lose it when broken");
        helper.succeed();
    }

    /** Void swallows the overflow of what it holds, so the machine upstream never stalls. */
    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void aVoidDrawerSwallowsTheOverflow(GameTestHelper helper) {
        ChemicalDrawerTile tile = place(helper, FunctionalStorage.DrawerType.X_1, false);
        tile.getUtilityUpgrades().insertItem(0, new ItemStack(FunctionalStorage.VOID_UPGRADE.get()), false);
        helper.assertTrue(tile.isVoid(), "the void upgrade did not take");

        IChemicalHandler handler = capability(helper, null);
        long capacity = handler.getChemicalTankCapacity(0);
        helper.assertTrue(handler.insertChemical(hydrogen(capacity + 5000), Action.EXECUTE).isEmpty(),
                "a void drawer pushed back");
        helper.assertValueEqual(handler.getChemicalInTank(0).getAmount(), capacity, "stored, capped at capacity");
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void aCreativeDrawerIsBottomless(GameTestHelper helper) {
        ChemicalDrawerTile tile = place(helper, FunctionalStorage.DrawerType.X_1, false);
        tile.getStorageUpgrades().insertItem(0, new ItemStack(FunctionalStorage.CREATIVE_UPGRADE.get()), false);
        helper.assertTrue(tile.isCreative(), "the creative upgrade did not take");

        IChemicalHandler handler = capability(helper, null);
        handler.insertChemical(hydrogen(1), Action.EXECUTE);
        helper.assertValueEqual(handler.extractChemical(1_000_000, Action.EXECUTE).getAmount(), 1_000_000L, "first extraction");
        helper.assertValueEqual(handler.extractChemical(1_000_000, Action.EXECUTE).getAmount(), 1_000_000L, "second extraction");
        helper.assertValueEqual(handler.getChemicalInTank(0).getAmount(), Long.MAX_VALUE, "creative contents");
        helper.succeed();
    }

    /** A storage upgrade cannot come out of a drawer too full to do without it. */
    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void anUpgradeCannotBeRemovedIfTheChemicalWouldNotFit(GameTestHelper helper) {
        ChemicalDrawerTile tile = place(helper, FunctionalStorage.DrawerType.X_1, false);
        long base = tile.getChemicalHandler().getChemicalTankCapacity(0);
        tile.getStorageUpgrades().insertItem(0,
                new ItemStack(FunctionalStorage.STORAGE_UPGRADES.get(StorageUpgradeItem.StorageTier.DIAMOND).get()), false);
        capability(helper, null).insertChemical(hydrogen(base * 2), Action.EXECUTE);

        helper.assertTrue(tile.getStorageUpgrades().extractItem(0, 1, false).isEmpty(),
                "the upgrade came out and the drawer now holds more than it can");
        helper.succeed();
    }

    /** Drawers, contents and assignments survive a save. */
    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void contentsAndLocksSurviveASave(GameTestHelper helper) {
        ChemicalDrawerTile tile = place(helper, FunctionalStorage.DrawerType.X_2, false);
        tile.getChemicalHandler().insertChemical(1, oxygen(777), Action.EXECUTE);
        tile.setLocked(true);
        CompoundTag saved = tile.getChemicalHandler().serializeNBT(helper.getLevel().registryAccess());

        helper.setBlock(BESIDE, IDChemicalContent.drawer(FunctionalStorage.DrawerType.X_2, false).getBlock());
        ChemicalDrawerTile copy = (ChemicalDrawerTile) helper.getBlockEntity(BESIDE);
        copy.getChemicalHandler().deserializeNBT(helper.getLevel().registryAccess(), saved);

        helper.assertValueEqual(copy.getChemicalHandler().getStoredRaw(1), 777L, "oxygen after reload");
        helper.assertTrue(copy.getChemicalHandler().getFilter(1).is(chemical("oxygen")), "lock assignment after reload");
        helper.succeed();
    }

    /**
     * A player with a real Mekanism chemical tank: right-click empties it into the slot, left-click
     * fills it back.
     */
    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void aChemicalTankFillsAndEmptiesTheDrawerByHand(GameTestHelper helper) {
        ChemicalDrawerTile tile = place(helper, FunctionalStorage.DrawerType.X_1, false);
        ItemStack tank = new ItemStack(BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, "basic_chemical_tank")));
        IChemicalHandler item = tank.getCapability(ChemicalCapabilities.ITEM);
        helper.assertTrue(item != null, "Mekanism's chemical tank item has no chemical capability");
        // Mekanism rate-limits its tank items per operation, so filling one takes several calls - which
        // is also the reason the drawer's hand interaction loops. See ChemicalDrawerTile.MAX_ROUNDS.
        for (int round = 0; round < 10 && item.getChemicalInTank(0).getAmount() < 2000; round++) {
            item.insertChemical(hydrogen(2000 - item.getChemicalInTank(0).getAmount()), Action.EXECUTE);
        }
        helper.assertValueEqual(item.getChemicalInTank(0).getAmount(), 2000L, "hydrogen in the tank to begin with");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, tank);
        tile.onSlotActivated(player, InteractionHand.MAIN_HAND, Direction.NORTH, 0.5, 0.5, 0.5, 0);
        helper.assertValueEqual(tile.getChemicalHandler().getStoredRaw(0), 2000L, "hydrogen poured into the drawer");
        helper.assertTrue(player.getMainHandItem().getCapability(ChemicalCapabilities.ITEM).getChemicalInTank(0).isEmpty(),
                "the tank still holds hydrogen");

        tile.onClicked(player, 0);
        helper.assertValueEqual(tile.getChemicalHandler().getStoredRaw(0), 0L, "hydrogen taken back out");
        helper.assertValueEqual(player.getMainHandItem().getCapability(ChemicalCapabilities.ITEM)
                .getChemicalInTank(0).getAmount(), 2000L, "hydrogen back in the tank");
        helper.succeed();
    }

    /** Half full reads as it does on a fluid drawer: 1 + floor(0.5 x 14) = 8. */
    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void theComparatorReadsTheFill(GameTestHelper helper) {
        ChemicalDrawerTile tile = place(helper, FunctionalStorage.DrawerType.X_1, false);
        long capacity = tile.getChemicalHandler().getChemicalTankCapacity(0);
        capability(helper, null).insertChemical(hydrogen(capacity / 2), Action.EXECUTE);
        var state = helper.getBlockState(DRAWER);
        helper.assertValueEqual(state.getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(DRAWER)), 8,
                "comparator on a half-full drawer");
        helper.succeed();
    }

    /**
     * A tube on the Storage Controller reaches every tank on its network, in network order, and the
     * chemical really lands in the drawers.
     */
    @GameTest(templateNamespace = NS, template = WALL, timeoutTicks = 300)
    public static void theControllerMovesChemicalsForItsWholeNetwork(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, FunctionalStorage.DRAWER_CONTROLLER.getBlock());
        List<BlockPos> placed = new ArrayList<>();
        for (int x = 0; x < 3; x++) {
            BlockPos pos = new BlockPos(x, 1, 0);
            helper.setBlock(pos, IDChemicalContent.drawer(FunctionalStorage.DrawerType.X_2, false).getBlock());
            placed.add(pos);
        }
        StorageControllerTile<?> controller = (StorageControllerTile<?>) helper.getBlockEntity(CONTROLLER);
        controller.addConnectedDrawers(LinkingToolItem.ActionMode.ADD,
                placed.stream().map(helper::absolutePos).toArray(BlockPos[]::new));

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    IChemicalHandler network = helper.getLevel().getCapability(
                            ChemicalCapabilities.BLOCK, helper.absolutePos(CONTROLLER), null);
                    helper.assertTrue(network != null, "the Storage Controller has no chemical capability");
                    helper.assertValueEqual(network.getChemicalTanks(), 6, "tanks across the network");

                    long perTank = network.getChemicalTankCapacity(0);
                    // Like ControllerFluidHandler, one tank per call: a tube pushes again next tick.
                    long inserted = 0;
                    for (int call = 0; call < 3; call++) {
                        inserted += perTank - network.insertChemical(hydrogen(perTank), Action.EXECUTE).getAmount();
                    }
                    helper.assertValueEqual(inserted, perTank * 3, "hydrogen accepted by the network");

                    long inDrawers = 0;
                    for (BlockPos pos : placed) {
                        ChemicalDrawerTile drawer = (ChemicalDrawerTile) helper.getBlockEntity(pos);
                        inDrawers += drawer.getChemicalHandler().getStoredRaw(0) + drawer.getChemicalHandler().getStoredRaw(1);
                    }
                    helper.assertValueEqual(inDrawers, perTank * 3, "hydrogen actually held by the drawers");

                    helper.assertValueEqual(network.extractChemical(hydrogen(perTank), Action.EXECUTE).getAmount(),
                            perTank, "hydrogen extracted through the controller");
                })
                .thenSucceed();
    }

    /**
     * The §7 risk again, for the new type: fifty chemical drawers of every layout on one controller,
     * counted as item handlers, and no rebuild while nothing happens.
     */
    @GameTest(templateNamespace = NS, template = WALL, timeoutTicks = 300)
    public static void aWallOfChemicalDrawersDoesNotRebuildTheControllerEveryTick(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, FunctionalStorage.DRAWER_CONTROLLER.getBlock());
        List<BlockPos> placed = new ArrayList<>();
        int n = 0;
        for (int y = 1; y <= 3 && placed.size() < 50; y++) {
            for (int x = 0; x < WALL_SIZE && placed.size() < 50; x++) {
                for (int z = 0; z < WALL_SIZE && placed.size() < 50; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (pos.equals(CONTROLLER)) {
                        continue;
                    }
                    FunctionalStorage.DrawerType type = IDChemicalContent.TYPES.get(n % 3);
                    helper.setBlock(pos, IDChemicalContent.drawer(type, n % 2 == 0).getBlock());
                    placed.add(pos);
                    n++;
                }
            }
        }
        StorageControllerTile<?> controller = (StorageControllerTile<?>) helper.getBlockEntity(CONTROLLER);
        controller.addConnectedDrawers(LinkingToolItem.ActionMode.ADD,
                placed.stream().map(helper::absolutePos).toArray(BlockPos[]::new));
        ConnectedDrawers network = controller.getConnectedDrawers();

        @SuppressWarnings("unchecked")
        List<IItemHandler>[] settled = new List[1];
        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    helper.assertValueEqual(network.getItemHandlers().size(), 50, "chemical drawers counted as item handlers");
                    helper.assertValueEqual(network.getItemHandlers().size() + network.getFluidHandlers().size()
                            + network.getExtensions(), network.getConnectedDrawers().size(), "the controller's invariant");
                    settled[0] = network.getItemHandlers();
                })
                .thenIdle(60)
                .thenExecute(() -> helper.assertTrue(network.getItemHandlers() == settled[0],
                        "the controller rebuilt its network while nothing happened"))
                .thenSucceed();
    }

    // ---- Real blocks from Mekanism, not calls on our handlers ------------------------------------
    //
    // Everything above talks to the drawer through its capability. These put Mekanism's own
    // transmitters between two drawers and let Mekanism move things: a tube network or a cable network
    // that never sees the drawer would pass every test above and still be a bug a player hits in the
    // first minute.

    private static final BlockPos LEFT = new BlockPos(0, 1, 1);
    private static final BlockPos MIDDLE = new BlockPos(1, 1, 1);
    private static final BlockPos RIGHT = new BlockPos(2, 1, 1);
    private static final int TRANSFER_TICKS = 80;

    private static net.minecraft.world.level.block.Block mekanismBlock(String path) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, path));
    }

    /**
     * A pressurized tube set to pull from one drawer carries the chemical into the drawer at its other
     * end - out through our capability, across Mekanism's network, in through our capability again.
     */
    @GameTest(templateNamespace = NS, template = PLATFORM, timeoutTicks = 300)
    public static void aPressurizedTubeMovesChemicalsBetweenDrawers(GameTestHelper helper) {
        helper.setBlock(LEFT, IDChemicalContent.drawer(FunctionalStorage.DrawerType.X_1, false).getBlock());
        helper.setBlock(RIGHT, IDChemicalContent.drawer(FunctionalStorage.DrawerType.X_1, false).getBlock());
        helper.setBlock(MIDDLE, mekanismBlock("basic_pressurized_tube"));
        ChemicalDrawerTile from = (ChemicalDrawerTile) helper.getBlockEntity(LEFT);
        ChemicalDrawerTile to = (ChemicalDrawerTile) helper.getBlockEntity(RIGHT);
        from.getChemicalHandler().insertChemical(0, hydrogen(10_000), Action.EXECUTE);

        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> ((mekanism.common.tile.transmitter.TileEntityTransmitter) helper.getBlockEntity(MIDDLE))
                        .getTransmitter().setConnectionTypeRaw(Direction.WEST, mekanism.common.lib.transmitter.ConnectionType.PULL))
                .thenIdle(TRANSFER_TICKS)
                .thenExecute(() -> {
                    long left = from.getChemicalHandler().getStoredRaw(0);
                    long right = to.getChemicalHandler().getStoredRaw(0);
                    helper.assertTrue(right > 0, "the tube moved nothing into the second drawer");
                    helper.assertTrue(left < 10_000, "the tube pulled nothing out of the first drawer");
                    helper.assertTrue(left + right <= 10_000,
                            "hydrogen was created on the way: " + left + " + " + right + " > 10000");
                })
                .thenSucceed();
    }

    /**
     * An energy drawer pushes through a Mekanism universal cable into another. Mekanism counts in
     * Joules and converts at the boundary, so this is also the check that nothing is gained in the
     * round trip.
     */
    @GameTest(templateNamespace = NS, template = PLATFORM, timeoutTicks = 300)
    public static void aUniversalCableCarriesEnergyBetweenDrawers(GameTestHelper helper) {
        helper.setBlock(LEFT, dev.drimoz.immaterialdrawers.registry.IDContent.ENERGY_DRAWER.getBlock());
        helper.setBlock(RIGHT, dev.drimoz.immaterialdrawers.registry.IDContent.ENERGY_DRAWER.getBlock());
        helper.setBlock(MIDDLE, mekanismBlock("basic_universal_cable"));
        var from = (dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile) helper.getBlockEntity(LEFT);
        var to = (dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile) helper.getBlockEntity(RIGHT);
        from.getEnergyStorage().receiveEnergy(200_000, false);

        helper.startSequence()
                .thenIdle(TRANSFER_TICKS)
                .thenExecute(() -> {
                    long left = from.getEnergyStorage().getStoredLong();
                    long right = to.getEnergyStorage().getStoredLong();
                    helper.assertTrue(right > 0, "no energy reached the second drawer through the cable");
                    helper.assertTrue(left + right <= 200_000,
                            "energy was created on the way: " + left + " + " + right + " > 200000");
                })
                .thenSucceed();
    }

    private static ChemicalDrawerTile place(GameTestHelper helper, FunctionalStorage.DrawerType type, boolean framed) {
        helper.setBlock(DRAWER, IDChemicalContent.drawer(type, framed).getBlock());
        BlockEntity be = helper.getBlockEntity(DRAWER);
        helper.assertTrue(be instanceof ChemicalDrawerTile, "the chemical drawer has no tile behind it");
        return (ChemicalDrawerTile) be;
    }

    private static IChemicalHandler capability(GameTestHelper helper, Direction side) {
        return helper.getLevel().getCapability(ChemicalCapabilities.BLOCK, helper.absolutePos(DRAWER), side);
    }

    private static Chemical chemical(String name) {
        return MekanismAPI.CHEMICAL_REGISTRY.get(ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, name));
    }

    private static ChemicalStack hydrogen(long amount) {
        return new ChemicalStack(chemical("hydrogen"), amount);
    }

    private static ChemicalStack oxygen(long amount) {
        return new ChemicalStack(chemical("oxygen"), amount);
    }
}
