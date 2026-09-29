package dev.drimoz.immaterialdrawers.gametest;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.tile.FluidDrawerTile;
import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import com.buuz135.functionalstorage.item.LinkingToolItem;
import com.buuz135.functionalstorage.item.StorageUpgradeItem;
import com.hollingsworth.arsnouveau.api.source.ISourceCap;
import com.hollingsworth.arsnouveau.api.source.ISpecialSourceProvider;
import com.hollingsworth.arsnouveau.api.source.SourceManager;
import com.hollingsworth.arsnouveau.api.util.SourceUtil;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.tile.source.SourceDrawerTile;
import dev.drimoz.immaterialdrawers.registry.IDSourceContent;
import dev.drimoz.immaterialdrawers.storage.source.SourceCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * The Source Drawer, tested against Ars Nouveau's own code paths.
 *
 * <p><b>Ars Nouveau only - see {@code compat.Mods}.</b> Registered by {@code IDSourceContent.init}
 * through {@code RegisterGameTestsEvent}, for the reason {@code IDChemicalGameTests} gives.
 *
 * <p>The tests that matter most call {@link SourceUtil} - exactly what the enchanting apparatus,
 * imbuement and sourcelinks call - rather than our storage directly. A drawer that holds Source but
 * that Ars never finds would pass every other test here.
 */
@PrefixGameTestTemplate(false)
public final class IDSourceGameTests {

    private static final String NS = ImmaterialDrawers.MOD_ID;
    private static final String PLATFORM = "energy_platform";
    private static final String WALL = "drawer_wall";

    private static final BlockPos DRAWER = new BlockPos(1, 1, 1);
    private static final BlockPos BESIDE = new BlockPos(0, 1, 1);
    private static final BlockPos CONTROLLER = new BlockPos(5, 1, 5);

    /** Well inside the range any Ars consumer searches. */
    private static final int RANGE = 5;

    /**
     * A freshly placed block entity is queued and loaded on the next tick - which is when {@code onLoad}
     * runs and the drawer joins Ars's {@code SourceManager}. Anything asserting on that registry has to
     * wait for it, as a player always does.
     */
    private static final int LOAD_TICKS = 2;

    private IDSourceGameTests() {
    }

    /** Ours is Ars's instance, by name - see {@code SourceCapabilities}. */
    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void weSpeakArsOwnCapability(GameTestHelper helper) {
        helper.assertTrue(SourceCapabilities.BLOCK
                        == com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.SOURCE_CAPABILITY,
                "our Source BlockCapability is not Ars Nouveau's instance");
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void bothDrawersHaveTheCapabilityFromEverySide(GameTestHelper helper) {
        for (var drawer : IDSourceContent.all()) {
            helper.setBlock(DRAWER, drawer.getBlock());
            for (Direction side : Direction.values()) {
                helper.assertTrue(capability(helper, side) != null,
                        drawer.getBlock() + " has no Source capability from " + side);
            }
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void countsAsAnItemDrawerButHoldsNoItems(GameTestHelper helper) {
        SourceDrawerTile tile = place(helper);
        helper.assertTrue(tile instanceof com.buuz135.functionalstorage.block.tile.ItemControllableDrawerTile<?>,
                "SourceDrawerTile is not an ItemControllableDrawerTile - the controller will drop it");
        helper.assertValueEqual(tile.getStorage().getSlots(), 0, "item slots on a source drawer");
        helper.succeed();
    }

    /** What a relay does: through the capability, in and out. */
    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void storesWhatARelayGivesIt(GameTestHelper helper) {
        place(helper);
        ISourceCap cap = capability(helper, null);
        helper.assertValueEqual(cap.receiveSource(5000, false), 5000, "Source accepted");
        helper.assertValueEqual(cap.extractSource(2000, false), 2000, "Source extracted");
        helper.assertValueEqual(cap.getSource(), 3000, "Source left");
        helper.succeed();
    }

    /**
     * The enchanting apparatus's path: {@code SourceUtil.takeSourceMultiple} around a position, which
     * only knows Ars's jars and {@code SourceManager}. If the drawer did not join the manager, this
     * returns null and every Ars consumer ignores it.
     */
    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void arsConsumersFindTheDrawer(GameTestHelper helper) {
        SourceDrawerTile tile = place(helper);
        tile.getSourceStorage().receiveSource(5000, false);
        helper.startSequence().thenIdle(LOAD_TICKS).thenExecute(() -> {
        List<ISpecialSourceProvider> taken = SourceUtil.takeSourceMultiple(
                helper.absolutePos(BESIDE), helper.getLevel(), RANGE, 3000);
        helper.assertTrue(taken != null, "Ars found no Source near a drawer holding 5000");
        helper.assertValueEqual(tile.getSourceStorage().getStoredRaw(), 2000, "Source left after Ars took 3000");
        }).thenSucceed();
    }

    /** The sourcelinks' path: {@code SourceUtil.canGiveSource} lists the drawer as somewhere to put Source. */
    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void sourcelinksCanFillTheDrawer(GameTestHelper helper) {
        place(helper);
        helper.startSequence().thenIdle(LOAD_TICKS).thenExecute(() -> {
        BlockPos drawer = helper.absolutePos(DRAWER);
        boolean listed = SourceUtil.canGiveSource(helper.absolutePos(BESIDE), helper.getLevel(), RANGE).stream()
                .anyMatch(provider -> provider.getCurrentPos().equals(drawer));
        helper.assertTrue(listed, "a sourcelink nearby would not see the drawer as somewhere to put Source");
        }).thenSucceed();
    }

    /**
     * A creative drawer really supplies through Ars's path. {@code takeSourceMultiple} counts
     * {@code before - after} as taken, so a creative storage that read full both times would give
     * nothing - see {@code BigSourceStorage.TileView}.
     */
    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void aCreativeDrawerSuppliesArs(GameTestHelper helper) {
        SourceDrawerTile tile = place(helper);
        tile.getStorageUpgrades().insertItem(0, new ItemStack(FunctionalStorage.CREATIVE_UPGRADE.get()), false);
        helper.assertTrue(tile.isCreative(), "the creative upgrade did not take");
        helper.startSequence().thenIdle(LOAD_TICKS).thenExecute(() -> {
        for (int round = 0; round < 2; round++) {
            helper.assertTrue(SourceUtil.takeSourceMultiple(
                            helper.absolutePos(BESIDE), helper.getLevel(), RANGE, 1_000_000) != null,
                    "a creative drawer did not supply Ars, round " + round);
        }
        }).thenSucceed();
    }

    /** A simulated removal through the tile interface changes nothing - the published default would. */
    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void aSimulatedRemovalLeavesTheSource(GameTestHelper helper) {
        SourceDrawerTile tile = place(helper);
        tile.getSourceStorage().receiveSource(1000, false);
        tile.getSourceStorage().asTile().removeSource(600, true);
        helper.assertValueEqual(tile.getSourceStorage().getStoredRaw(), 1000, "Source after a simulated removal");
        helper.succeed();
    }

    /** Once broken, the drawer's entry in Ars's registry reports itself invalid, so Ars drops it. */
    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void aBrokenDrawerLeavesArsRegistry(GameTestHelper helper) {
        place(helper);
        helper.startSequence().thenIdle(LOAD_TICKS).thenExecute(() -> {
        BlockPos drawer = helper.absolutePos(DRAWER);
        List<ISpecialSourceProvider> ours = SourceManager.INSTANCE.getCopySetForLevel(helper.getLevel()).stream()
                .filter(provider -> provider.getCurrentPos().equals(drawer) && provider.isValid())
                .toList();
        helper.assertValueEqual(ours.size(), 1, "valid registry entries for a placed drawer");

        helper.setBlock(DRAWER, Blocks.AIR);
        helper.assertTrue(!ours.getFirst().isValid(), "a broken drawer still claims to hold Source");
        }).thenSucceed();
    }

    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void aVoidDrawerSwallowsTheOverflow(GameTestHelper helper) {
        SourceDrawerTile tile = place(helper);
        tile.getUtilityUpgrades().insertItem(0, new ItemStack(FunctionalStorage.VOID_UPGRADE.get()), false);
        int capacity = tile.getSourceStorage().getSourceCapacity();
        helper.assertValueEqual(capability(helper, null).receiveSource(capacity + 5000, false), capacity + 5000,
                "Source reported accepted by a void drawer");
        helper.assertValueEqual(tile.getSourceStorage().getStoredRaw(), capacity, "Source kept, capped at capacity");
        helper.succeed();
    }

    /**
     * The fluid drawer's capacity at the base, and every storage slot adding to it without passing
     * the int ceiling of Ars's API.
     */
    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void capacityFollowsTheFluidCurveAndFitsAnInt(GameTestHelper helper) {
        SourceDrawerTile tile = place(helper);
        helper.setBlock(BESIDE, FunctionalStorage.FLUID_DRAWER_1.getBlock());
        FluidDrawerTile fluid = (FluidDrawerTile) helper.getBlockEntity(BESIDE);
        helper.assertValueEqual(tile.getSourceStorage().getSourceCapacity(), fluid.getFluidHandler().getTankCapacity(0),
                "base capacity, next to a fluid drawer's");

        int previous = tile.getSourceStorage().getSourceCapacity();
        for (int slot = 0; slot < 4; slot++) {
            tile.getStorageUpgrades().insertItem(slot,
                    new ItemStack(FunctionalStorage.STORAGE_UPGRADES.get(StorageUpgradeItem.StorageTier.NETHERITE).get()), false);
            int now = tile.getSourceStorage().getSourceCapacity();
            helper.assertTrue(now > previous && now > 0,
                    "storage slot " + slot + " did not raise the capacity (" + previous + " -> " + now + ")");
            previous = now;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void anUpgradeCannotBeRemovedIfTheSourceWouldNotFit(GameTestHelper helper) {
        SourceDrawerTile tile = place(helper);
        int base = tile.getSourceStorage().getSourceCapacity();
        tile.getStorageUpgrades().insertItem(0,
                new ItemStack(FunctionalStorage.STORAGE_UPGRADES.get(StorageUpgradeItem.StorageTier.DIAMOND).get()), false);
        tile.getSourceStorage().receiveSource(base * 2, false);
        helper.assertTrue(tile.getStorageUpgrades().extractItem(0, 1, false).isEmpty(),
                "the upgrade came out and the drawer now holds more than it can");
        helper.succeed();
    }

    /** A relay aimed at the Storage Controller reaches every source drawer on its network. */
    @GameTest(templateNamespace = NS, template = WALL, timeoutTicks = 300)
    public static void theControllerMovesSourceForItsWholeNetwork(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, FunctionalStorage.DRAWER_CONTROLLER.getBlock());
        List<BlockPos> placed = new ArrayList<>();
        for (int x = 0; x < 3; x++) {
            BlockPos pos = new BlockPos(x, 1, 0);
            helper.setBlock(pos, IDSourceContent.SOURCE_DRAWER.getBlock());
            placed.add(pos);
        }
        StorageControllerTile<?> controller = (StorageControllerTile<?>) helper.getBlockEntity(CONTROLLER);
        controller.addConnectedDrawers(LinkingToolItem.ActionMode.ADD,
                placed.stream().map(helper::absolutePos).toArray(BlockPos[]::new));

        helper.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    ISourceCap network = helper.getLevel().getCapability(
                            SourceCapabilities.BLOCK, helper.absolutePos(CONTROLLER), null);
                    helper.assertTrue(network != null, "the Storage Controller has no Source capability");
                    int perDrawer = ((SourceDrawerTile) helper.getBlockEntity(placed.getFirst()))
                            .getSourceStorage().getSourceCapacity();
                    helper.assertValueEqual(network.getSourceCapacity(), perDrawer * 3, "capacity across the network");
                    helper.assertValueEqual(network.receiveSource(perDrawer * 2, false), perDrawer * 2,
                            "Source accepted, spilling into a second drawer");
                    helper.assertValueEqual(network.extractSource(perDrawer, false), perDrawer, "Source extracted");
                    helper.assertValueEqual(network.getSource(), perDrawer, "Source left in the network");
                })
                .thenSucceed();
    }

    private static SourceDrawerTile place(GameTestHelper helper) {
        helper.setBlock(DRAWER, IDSourceContent.SOURCE_DRAWER.getBlock());
        BlockEntity be = helper.getBlockEntity(DRAWER);
        helper.assertTrue(be instanceof SourceDrawerTile, "the source drawer has no tile behind it");
        return (SourceDrawerTile) be;
    }

    private static ISourceCap capability(GameTestHelper helper, Direction side) {
        return helper.getLevel().getCapability(SourceCapabilities.BLOCK, helper.absolutePos(DRAWER), side);
    }
}
