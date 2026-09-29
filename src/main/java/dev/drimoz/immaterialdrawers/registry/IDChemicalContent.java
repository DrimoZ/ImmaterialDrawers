package dev.drimoz.immaterialdrawers.registry;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.tile.StorageControllerExtensionTile;
import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import com.hrznstudio.titanium.module.BlockWithTile;
import com.hrznstudio.titanium.module.DeferredRegistryHelper;
import com.hrznstudio.titanium.nbthandler.NBTManager;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.chemical.ChemicalDrawerBlock;
import dev.drimoz.immaterialdrawers.block.chemical.FramedChemicalDrawerBlock;
import dev.drimoz.immaterialdrawers.block.tile.chemical.ChemicalDrawerTile;
import dev.drimoz.immaterialdrawers.block.tile.chemical.FramedChemicalDrawerTile;
import dev.drimoz.immaterialdrawers.gametest.IDChemicalGameTests;
import dev.drimoz.immaterialdrawers.storage.chemical.ChemicalCapabilities;
import dev.drimoz.immaterialdrawers.storage.chemical.ControllerChemicalHandler;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The Chemical Drawers: everything this mod registers only when Mekanism is installed.
 *
 * <p><b>Mekanism only - see {@code compat.Mods}.</b> This is the one class the always-loaded code
 * calls into, and it only ever does so from behind {@code if (Mods.mekanism())}: its method
 * signatures are free of Mekanism types so the callers can be verified without Mekanism present, and
 * everything past them may use its API freely.
 *
 * <p>Laid out as Functional Storage lays out its fluid drawers: three layouts - 1x1, 1x2, 2x2 - each
 * with a framed twin, each its own block and block entity type. Six registry names, written into
 * every save that contains one and therefore as permanent as the mod id:
 * {@code chemical_drawer_1}, {@code _2}, {@code _4} and their {@code framed_} counterparts.
 */
public final class IDChemicalContent {

    /** The layouts, in the order Functional Storage registers its fluid drawers. */
    public static final List<FunctionalStorage.DrawerType> TYPES = List.of(
            FunctionalStorage.DrawerType.X_1, FunctionalStorage.DrawerType.X_2, FunctionalStorage.DrawerType.X_4);

    private static final Map<FunctionalStorage.DrawerType, BlockWithTile> DRAWERS =
            new EnumMap<>(FunctionalStorage.DrawerType.class);
    private static final Map<FunctionalStorage.DrawerType, BlockWithTile> FRAMED =
            new EnumMap<>(FunctionalStorage.DrawerType.class);

    private IDChemicalContent() {
    }

    /** {@code chemical_drawer_1}, {@code framed_chemical_drawer_4} and so on. */
    public static String nameFor(FunctionalStorage.DrawerType type, boolean framed) {
        return (framed ? "framed_" : "") + "chemical_drawer_" + type.getSlots();
    }

    public static BlockWithTile drawer(FunctionalStorage.DrawerType type, boolean framed) {
        return (framed ? FRAMED : DRAWERS).get(type);
    }

    /** Every chemical drawer, unframed first, in layout order. */
    public static List<BlockWithTile> all() {
        List<BlockWithTile> all = new ArrayList<>(DRAWERS.values());
        all.addAll(FRAMED.values());
        return all;
    }

    /**
     * Iron, where the fluid drawer is stone bricks and the energy drawer copper: how long it takes to
     * break and how it sounds, not how it looks - the textures are the graphite family's.
     */
    public static void register(DeferredRegistryHelper registries) {
        for (FunctionalStorage.DrawerType type : TYPES) {
            DRAWERS.put(type, registries.registerBlockWithTileItem(nameFor(type, false),
                    () -> new ChemicalDrawerBlock(type, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)),
                    block -> () -> new BlockItem(block.get(), new Item.Properties()),
                    ImmaterialDrawers.TAB));
        }
        for (FunctionalStorage.DrawerType type : TYPES) {
            FRAMED.put(type, registries.registerBlockWithTileItem(nameFor(type, true),
                    () -> new FramedChemicalDrawerBlock(type, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)),
                    block -> () -> new BlockItem(block.get(), new Item.Properties()),
                    ImmaterialDrawers.TAB));
        }
    }

    /**
     * The rest of the wiring, from the mod constructor.
     *
     * <p>Both tile classes are scanned for {@code @Save}: the scan is per class, and the framed one adds
     * a field of its own - miss it and a framed drawer forgets its textures on reload.
     */
    public static void init(IEventBus modBus) {
        NBTManager.getInstance().scanTileClassForAnnotations(ChemicalDrawerTile.class);
        NBTManager.getInstance().scanTileClassForAnnotations(FramedChemicalDrawerTile.class);
        modBus.addListener(IDChemicalContent::registerCapabilities);
        // Registered here rather than found by @GameTestHolder: NeoForge initialises every holder class
        // it finds in a dev run, Mekanism or not, and this one uses Mekanism throughout.
        modBus.addListener((RegisterGameTestsEvent event) -> event.register(IDChemicalGameTests.class));
    }

    /**
     * Mekanism's chemical capability, on every chemical drawer and on Functional Storage's controllers
     * and extensions - the same three places the energy capability goes, for the same reasons. See
     * {@code ImmaterialDrawers#registerCapabilities}.
     */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (BlockWithTile drawer : all()) {
            event.registerBlockEntity(ChemicalCapabilities.BLOCK, drawer.type().get(),
                    (blockEntity, side) -> blockEntity instanceof ChemicalDrawerTile tile
                            ? tile.getChemicalHandler()
                            : null);
        }
        for (BlockWithTile controller : List.of(
                FunctionalStorage.DRAWER_CONTROLLER, FunctionalStorage.FRAMED_DRAWER_CONTROLLER)) {
            event.registerBlockEntity(ChemicalCapabilities.BLOCK, controller.type().get(),
                    (blockEntity, side) -> blockEntity instanceof StorageControllerTile<?> tile
                            ? ControllerChemicalHandler.of(tile)
                            : null);
        }
        for (BlockWithTile extension : List.of(
                FunctionalStorage.CONTROLLER_EXTENSION, FunctionalStorage.FRAMED_CONTROLLER_EXTENSION)) {
            event.registerBlockEntity(ChemicalCapabilities.BLOCK, extension.type().get(),
                    (blockEntity, side) -> {
                        if (!(blockEntity instanceof StorageControllerExtensionTile<?> tile)
                                || tile.getControllerPos() == null
                                || tile.getLevel() == null
                                || !tile.getLevel().isLoaded(tile.getControllerPos())) {
                            return null;
                        }
                        return tile.getLevel().getBlockEntity(tile.getControllerPos())
                                instanceof StorageControllerTile<?> controller
                                ? ControllerChemicalHandler.of(controller)
                                : null;
                    });
        }
    }
}
