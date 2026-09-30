package dev.drimoz.immaterialdrawers.gametest;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.tile.FluidDrawerTile;
import com.buuz135.functionalstorage.block.tile.ItemControllableDrawerTile;
import com.buuz135.functionalstorage.item.StorageUpgradeItem;
import com.hollingsworth.arsnouveau.api.source.ISpecialSourceProvider;
import com.hollingsworth.arsnouveau.api.source.SourceManager;
import com.hollingsworth.arsnouveau.api.util.SourceUtil;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.IDFramedBlock;
import dev.drimoz.immaterialdrawers.block.tile.source.SourceDrawerTile;
import dev.drimoz.immaterialdrawers.recipe.FramedDrawerRecipe;
import dev.drimoz.immaterialdrawers.registry.IDSourceContent;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Objects;

/**
 * The Source Drawer against Ars Nouveau 4.12's own paths. <b>Ars Nouveau only</b> - registered by
 * {@code IDSourceContent.init} through {@code RegisterGameTestsEvent}, never as a holder.
 *
 * <p>The tests that matter call {@link SourceUtil} - what the enchanting apparatus, imbuement and
 * sourcelinks call - rather than our storage. There is no relay test on this branch: a 4.12 relay only
 * moves Source between Ars's own {@code AbstractSourceMachine}s (see {@code SourceDrawerTile}).
 */
@PrefixGameTestTemplate(false)
public final class IDSourceGameTests {

    private static final String NS = ImmaterialDrawers.MOD_ID;
    private static final String PLATFORM = "energy_platform";

    private static final BlockPos DRAWER = new BlockPos(1, 1, 1);
    private static final BlockPos BESIDE = new BlockPos(0, 1, 1);

    /** Well inside the range any Ars consumer searches. */
    private static final int RANGE = 5;

    /** {@code onLoad} - where the drawer joins SourceManager - runs the tick after the block is placed. */
    private static final int LOAD_TICKS = 2;

    private IDSourceGameTests() {
    }

    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void countsAsAnItemDrawerButHoldsNoItems(GameTestHelper helper) {
        SourceDrawerTile tile = place(helper);
        helper.assertTrue(tile instanceof ItemControllableDrawerTile<?>, "not an item drawer to Functional Storage");
        assertEquals(helper, tile.getStorage().getSlots(), 0, "item slots on a source drawer");
        helper.succeed();
    }

    /** The apparatus's path: {@code SourceUtil.takeSource} around a position, through SourceManager. */
    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void arsConsumersFindTheDrawer(GameTestHelper helper) {
        SourceDrawerTile tile = place(helper);
        tile.getSourceStorage().receiveSource(5000, false);
        helper.startSequence().thenIdle(LOAD_TICKS).thenExecute(() -> {
            ISpecialSourceProvider taken = SourceUtil.takeSource(helper.absolutePos(BESIDE), helper.getLevel(), RANGE, 3000);
            helper.assertTrue(taken != null, "Ars found no Source near a drawer holding 5000");
            assertEquals(helper, tile.getSourceStorage().getStoredRaw(), 2000, "Source left after Ars took 3000");
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

    /** A creative drawer supplies Ars, and keeps supplying. */
    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void aCreativeDrawerSuppliesArs(GameTestHelper helper) {
        SourceDrawerTile tile = place(helper);
        tile.getStorageUpgrades().insertItem(0, new ItemStack(FunctionalStorage.CREATIVE_UPGRADE.get()), false);
        helper.assertTrue(tile.isCreative(), "the creative upgrade did not take");
        helper.startSequence().thenIdle(LOAD_TICKS).thenExecute(() -> {
            for (int round = 0; round < 2; round++) {
                helper.assertTrue(SourceUtil.takeSource(helper.absolutePos(BESIDE), helper.getLevel(), RANGE, 1_000_000) != null,
                        "a creative drawer did not supply Ars, round " + round);
            }
        }).thenSucceed();
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
            assertEquals(helper, ours.size(), 1, "valid registry entries for a placed drawer");
            helper.setBlock(DRAWER, Blocks.AIR);
            helper.assertTrue(!ours.get(0).isValid(), "a broken drawer still claims to hold Source");
        }).thenSucceed();
    }

    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void aVoidDrawerSwallowsTheOverflow(GameTestHelper helper) {
        SourceDrawerTile tile = place(helper);
        tile.getUtilityUpgrades().insertItem(0, new ItemStack(FunctionalStorage.VOID_UPGRADE.get()), false);
        int capacity = tile.getSourceStorage().getSourceCapacity();
        assertEquals(helper, tile.getSourceStorage().receiveSource(capacity + 5000, false), capacity + 5000,
                "Source reported accepted by a void drawer");
        assertEquals(helper, tile.getSourceStorage().getStoredRaw(), capacity, "Source kept, capped at capacity");
        helper.succeed();
    }

    /** The fluid drawer's capacity at the base, and every storage slot adding to it inside an int. */
    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void capacityFollowsTheFluidCurveAndFitsAnInt(GameTestHelper helper) {
        SourceDrawerTile tile = place(helper);
        helper.setBlock(BESIDE, FunctionalStorage.FLUID_DRAWER_1.getLeft().get());
        FluidDrawerTile fluid = (FluidDrawerTile) helper.getBlockEntity(BESIDE);
        assertEquals(helper, tile.getSourceStorage().getSourceCapacity(), fluid.getFluidHandler().getTankCapacity(0),
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

    /** The framed Source Drawer is framed by the same recipe as the energy one: it is an IDFramedBlock. */
    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void theFramedSourceDrawerIsFramable(GameTestHelper helper) {
        ItemStack drawer = new ItemStack(IDSourceContent.FRAMED_SOURCE_DRAWER.getLeft().get());
        helper.assertTrue(IDSourceContent.FRAMED_SOURCE_DRAWER.getLeft().get() instanceof IDFramedBlock,
                "the framed Source Drawer is not an IDFramedBlock");
        helper.assertTrue(FramedDrawerRecipe.matches(new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.STONE), drawer),
                "the framing recipe rejected the framed Source Drawer");
        helper.succeed();
    }

    private static SourceDrawerTile place(GameTestHelper helper) {
        helper.setBlock(DRAWER, IDSourceContent.SOURCE_DRAWER.getLeft().get());
        BlockEntity be = helper.getBlockEntity(DRAWER);
        helper.assertTrue(be instanceof SourceDrawerTile, "the source drawer has no tile behind it");
        return (SourceDrawerTile) be;
    }

    private static void assertEquals(GameTestHelper helper, Object actual, Object expected, String name) {
        helper.assertTrue(Objects.equals(actual, expected), "Expected " + name + " to be " + expected + ", but was " + actual);
    }
}
