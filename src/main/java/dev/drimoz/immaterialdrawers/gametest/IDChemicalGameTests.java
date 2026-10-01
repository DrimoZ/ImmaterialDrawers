package dev.drimoz.immaterialdrawers.gametest;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.tile.FluidDrawerTile;
import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import com.buuz135.functionalstorage.item.LinkingToolItem;
import com.buuz135.functionalstorage.item.StorageUpgradeItem;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.IDFramedBlock;
import dev.drimoz.immaterialdrawers.block.tile.chemical.ChemicalDrawerTile;
import dev.drimoz.immaterialdrawers.recipe.FramedDrawerRecipe;
import dev.drimoz.immaterialdrawers.registry.IDChemicalContent;
import dev.drimoz.immaterialdrawers.storage.chemical.ChemicalCapabilities;
import dev.drimoz.immaterialdrawers.storage.chemical.ChemicalTanks;
import mekanism.api.Action;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.ChemicalType;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.IMekanismChemicalHandler;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.chemical.infuse.IInfusionHandler;
import mekanism.api.chemical.infuse.InfusionStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Objects;

/**
 * The chemical drawers against Mekanism 10.4's four capabilities. <b>Mekanism only</b> - registered
 * by {@code IDChemicalContent.init} through {@code RegisterGameTestsEvent}, never as a holder.
 *
 * <p>What is particular to 1.20.1 is tested first: one drawer answers all four capabilities, a tank
 * taken by one type is closed to the three others, and the type survives a save.
 */
@PrefixGameTestTemplate(false)
public final class IDChemicalGameTests {

    private static final String NS = ImmaterialDrawers.MOD_ID;
    private static final String PLATFORM = "energy_platform";
    private static final String WALL = "drawer_wall";

    private static final BlockPos DRAWER = new BlockPos(1, 1, 1);
    private static final BlockPos BESIDE = new BlockPos(0, 1, 1);
    private static final BlockPos CONTROLLER = new BlockPos(5, 1, 5);

    private IDChemicalGameTests() {
    }

    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void theDrawerAnswersAllFourChemicalCapabilities(GameTestHelper helper) {
        place(helper, FunctionalStorage.DrawerType.X_1);
        for (ChemicalType type : ChemicalType.values()) {
            helper.assertTrue(capability(helper, DRAWER, ChemicalCapabilities.of(type)) != null,
                    "no " + type.getSerializedName() + " capability on the chemical drawer");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void aGasGoesInAndComesBackOut(GameTestHelper helper) {
        place(helper, FunctionalStorage.DrawerType.X_1);
        IGasHandler gas = capability(helper, DRAWER, ChemicalCapabilities.GAS);
        assertEquals(helper, gas.insertChemical(hydrogen(4000), Action.EXECUTE).getAmount(), 0L, "hydrogen left over");
        assertEquals(helper, gas.getChemicalInTank(0).getAmount(), 4000L, "hydrogen stored");
        assertEquals(helper, gas.extractChemical(1500, Action.EXECUTE).getAmount(), 1500L, "hydrogen taken out");
        assertEquals(helper, gas.getChemicalInTank(0).getAmount(), 2500L, "hydrogen left");
        helper.succeed();
    }

    /** A tank holding hydrogen is a tank of capacity 0 to the infusion view, so a pipe moves on. */
    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void aTankTakenByOneTypeIsClosedToTheOthers(GameTestHelper helper) {
        place(helper, FunctionalStorage.DrawerType.X_1);
        capability(helper, DRAWER, ChemicalCapabilities.GAS).insertChemical(hydrogen(1000), Action.EXECUTE);
        IInfusionHandler infusion = capability(helper, DRAWER, ChemicalCapabilities.INFUSION);
        assertEquals(helper, infusion.getTankCapacity(0), 0L, "capacity the infusion view sees in a gas tank");
        assertEquals(helper, infusion.insertChemical(redstone(500), Action.EXECUTE).getAmount(), 500L,
                "redstone infusion refused by a tank holding hydrogen");
        helper.assertTrue(infusion.getChemicalInTank(0).isEmpty(), "the infusion view shows the gas");
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void aTwoTankDrawerHoldsTwoTypes(GameTestHelper helper) {
        place(helper, FunctionalStorage.DrawerType.X_2);
        capability(helper, DRAWER, ChemicalCapabilities.GAS).insertChemical(hydrogen(1000), Action.EXECUTE);
        IInfusionHandler infusion = capability(helper, DRAWER, ChemicalCapabilities.INFUSION);
        assertEquals(helper, infusion.insertChemical(redstone(500), Action.EXECUTE).getAmount(), 0L,
                "redstone left over, with the second tank free");
        assertEquals(helper, infusion.getChemicalInTank(1).getAmount(), 500L, "redstone in the second tank");
        helper.succeed();
    }

    /**
     * What Mekanism's Jade / TOP integration lists: one row per slot across the four views, not four
     * rows per slot. Two gases and an infusion in a 2x2 leave one slot empty, shown once, by the gas
     * view. Pipes still see every tank in every view - the display list must not change their indices.
     */
    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void probesSeeOneRowPerSlot(GameTestHelper helper) {
        ChemicalTanks tanks = place(helper, FunctionalStorage.DrawerType.X_4).getChemicalHandler();
        tanks.insert(0, hydrogen(1000), Action.EXECUTE);
        tanks.insert(1, oxygen(1000), Action.EXECUTE);
        tanks.insert(2, redstone(500), Action.EXECUTE);

        int rows = 0;
        for (ChemicalType type : ChemicalType.values()) {
            IChemicalHandler<?, ?> view = tanks.view(type);
            helper.assertTrue(view instanceof IMekanismChemicalHandler<?, ?, ?>, type + " view is not what Mekanism's probes read");
            rows += ((IMekanismChemicalHandler<?, ?, ?>) view).getChemicalTanks(null).size();
            assertEquals(helper, view.getTanks(), 4, "tanks a pipe sees in the " + type + " view");
        }
        assertEquals(helper, rows, 4, "rows across the four views");
        assertEquals(helper, ((IMekanismChemicalHandler<?, ?, ?>) tanks.gas).getChemicalTanks(null).size(), 3,
                "gas rows (two gases and the empty slot)");
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void layoutsHaveOneTwoFourTanks(GameTestHelper helper) {
        for (FunctionalStorage.DrawerType type : IDChemicalContent.TYPES) {
            assertEquals(helper, place(helper, type).getChemicalHandler().tanks(), type.getSlots(), "tanks in " + type);
        }
        helper.succeed();
    }

    /** Through NBT and back: BoxedChemicalStack has to carry the type, or an infusion reloads as a gas. */
    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void theTypeSurvivesASave(GameTestHelper helper) {
        ChemicalDrawerTile tile = place(helper, FunctionalStorage.DrawerType.X_1);
        capability(helper, DRAWER, ChemicalCapabilities.INFUSION).insertChemical(redstone(700), Action.EXECUTE);
        ChemicalDrawerTile reloaded = (ChemicalDrawerTile) BlockEntity.loadStatic(
                tile.getBlockPos(), tile.getBlockState(), tile.saveWithFullMetadata());
        helper.assertTrue(reloaded != null, "the drawer did not reload");
        var stack = reloaded.getChemicalHandler().getStoredRaw(0);
        helper.assertTrue(ChemicalType.getTypeFor(stack) == ChemicalType.INFUSION, "the infusion reloaded as another type");
        assertEquals(helper, stack.getAmount(), 700L, "infusion amount after reload");
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void aLockedDrawerKeepsItsChemical(GameTestHelper helper) {
        ChemicalDrawerTile tile = place(helper, FunctionalStorage.DrawerType.X_1);
        IGasHandler gas = capability(helper, DRAWER, ChemicalCapabilities.GAS);
        gas.insertChemical(hydrogen(1000), Action.EXECUTE);
        tile.setLocked(true);
        gas.extractChemical(1000, Action.EXECUTE);
        assertEquals(helper, gas.insertChemical(oxygen(100), Action.EXECUTE).getAmount(), 100L,
                "oxygen refused by a locked, emptied hydrogen tank");
        assertEquals(helper, gas.insertChemical(hydrogen(100), Action.EXECUTE).getAmount(), 0L,
                "hydrogen accepted back into its locked tank");
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void aVoidDrawerSwallowsTheOverflow(GameTestHelper helper) {
        ChemicalDrawerTile tile = place(helper, FunctionalStorage.DrawerType.X_1);
        tile.getUtilityUpgrades().insertItem(0, new ItemStack(FunctionalStorage.VOID_UPGRADE.get()), false);
        long capacity = tile.getChemicalHandler().capacity(0);
        IGasHandler gas = capability(helper, DRAWER, ChemicalCapabilities.GAS);
        assertEquals(helper, gas.insertChemical(hydrogen(capacity + 5000), Action.EXECUTE).getAmount(), 0L,
                "hydrogen reported left over by a void drawer");
        assertEquals(helper, tile.getChemicalHandler().getStoredRaw(0).getAmount(), capacity, "hydrogen kept");
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void aCreativeDrawerIsBottomless(GameTestHelper helper) {
        ChemicalDrawerTile tile = place(helper, FunctionalStorage.DrawerType.X_1);
        IGasHandler gas = capability(helper, DRAWER, ChemicalCapabilities.GAS);
        gas.insertChemical(hydrogen(10), Action.EXECUTE);
        tile.getStorageUpgrades().insertItem(0, new ItemStack(FunctionalStorage.CREATIVE_UPGRADE.get()), false);
        helper.assertTrue(tile.isCreative(), "the creative upgrade did not take");
        for (int round = 0; round < 2; round++) {
            assertEquals(helper, gas.extractChemical(1_000_000, Action.EXECUTE).getAmount(), 1_000_000L,
                    "hydrogen from a creative drawer, round " + round);
        }
        helper.succeed();
    }

    /** Base capacity per tank as a fluid drawer's, and every storage slot raising it. */
    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void capacityFollowsTheFluidCurve(GameTestHelper helper) {
        ChemicalDrawerTile tile = place(helper, FunctionalStorage.DrawerType.X_1);
        helper.setBlock(BESIDE, FunctionalStorage.FLUID_DRAWER_1.getLeft().get());
        FluidDrawerTile fluid = (FluidDrawerTile) helper.getBlockEntity(BESIDE);
        assertEquals(helper, tile.getChemicalHandler().capacity(0), (long) fluid.getFluidHandler().getTankCapacity(0),
                "base capacity, next to a fluid drawer's");
        long previous = tile.getChemicalHandler().capacity(0);
        for (int slot = 0; slot < 4; slot++) {
            tile.getStorageUpgrades().insertItem(slot,
                    new ItemStack(FunctionalStorage.STORAGE_UPGRADES.get(StorageUpgradeItem.StorageTier.NETHERITE).get()), false);
            long now = tile.getChemicalHandler().capacity(0);
            helper.assertTrue(now > previous, "storage slot " + slot + " did not raise the capacity");
            previous = now;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void anUpgradeCannotBeRemovedIfTheChemicalsWouldNotFit(GameTestHelper helper) {
        ChemicalDrawerTile tile = place(helper, FunctionalStorage.DrawerType.X_1);
        long base = tile.getChemicalHandler().capacity(0);
        tile.getStorageUpgrades().insertItem(0,
                new ItemStack(FunctionalStorage.STORAGE_UPGRADES.get(StorageUpgradeItem.StorageTier.DIAMOND).get()), false);
        capability(helper, DRAWER, ChemicalCapabilities.GAS).insertChemical(hydrogen(base * 2), Action.EXECUTE);
        helper.assertTrue(tile.getStorageUpgrades().extractItem(0, 1, false).isEmpty(),
                "the upgrade came out and the drawer now holds more than it can");
        helper.succeed();
    }

    /** A pipe on the Storage Controller reaches every chemical drawer on its network, in and out. */
    @GameTest(templateNamespace = NS, template = WALL, timeoutTicks = 300)
    public static void theControllerMovesChemicalsForItsWholeNetwork(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, FunctionalStorage.DRAWER_CONTROLLER.getLeft().get());
        BlockPos[] drawers = {new BlockPos(0, 1, 0), new BlockPos(1, 1, 0)};
        for (BlockPos pos : drawers) {
            helper.setBlock(pos, IDChemicalContent.drawer(FunctionalStorage.DrawerType.X_1, false).getLeft().get());
        }
        StorageControllerTile<?> controller = (StorageControllerTile<?>) helper.getBlockEntity(CONTROLLER);
        controller.addConnectedDrawers(LinkingToolItem.ActionMode.ADD,
                java.util.Arrays.stream(drawers).map(helper::absolutePos).toArray(BlockPos[]::new));

        helper.startSequence().thenIdle(10).thenExecute(() -> {
            IGasHandler network = capability(helper, CONTROLLER, ChemicalCapabilities.GAS);
            helper.assertTrue(network != null, "the Storage Controller has no gas capability");
            assertEquals(helper, network.getTanks(), 2, "tanks on the network");
            long perTank = network.getTankCapacity(0);
            assertEquals(helper, network.insertChemical(hydrogen(perTank + 500), Action.EXECUTE).getAmount(), 500L,
                    "hydrogen left over after filling one tank - the second holds nothing yet");
            assertEquals(helper, network.extractChemical(700, Action.EXECUTE).getAmount(), 700L, "hydrogen out of the network");
        }).thenSucceed();
    }

    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void theFramedChemicalDrawerIsFramable(GameTestHelper helper) {
        var framed = IDChemicalContent.drawer(FunctionalStorage.DrawerType.X_2, true).getLeft().get();
        helper.assertTrue(framed instanceof IDFramedBlock, "the framed chemical drawer is not an IDFramedBlock");
        helper.assertTrue(FramedDrawerRecipe.matches(new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.STONE), new ItemStack(framed)),
                "the framing recipe rejected the framed chemical drawer");
        helper.succeed();
    }

    private static ChemicalDrawerTile place(GameTestHelper helper, FunctionalStorage.DrawerType type) {
        helper.setBlock(DRAWER, IDChemicalContent.drawer(type, false).getLeft().get());
        BlockEntity be = helper.getBlockEntity(DRAWER);
        helper.assertTrue(be instanceof ChemicalDrawerTile, "the chemical drawer has no tile behind it");
        return (ChemicalDrawerTile) be;
    }

    private static <T> T capability(GameTestHelper helper, BlockPos pos, Capability<T> capability) {
        BlockEntity be = helper.getBlockEntity(pos);
        return be == null ? null : be.getCapability(capability, null).orElse(null);
    }

    private static GasStack hydrogen(long amount) {
        return new GasStack(MekanismAPI.gasRegistry().getValue(new ResourceLocation("mekanism", "hydrogen")), amount);
    }

    private static GasStack oxygen(long amount) {
        return new GasStack(MekanismAPI.gasRegistry().getValue(new ResourceLocation("mekanism", "oxygen")), amount);
    }

    private static InfusionStack redstone(long amount) {
        return new InfusionStack(MekanismAPI.infuseTypeRegistry().getValue(new ResourceLocation("mekanism", "redstone")), amount);
    }

    private static void assertEquals(GameTestHelper helper, Object actual, Object expected, String name) {
        helper.assertTrue(Objects.equals(actual, expected), "Expected " + name + " to be " + expected + ", but was " + actual);
    }
}
