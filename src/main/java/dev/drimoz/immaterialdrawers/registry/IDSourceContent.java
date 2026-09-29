package dev.drimoz.immaterialdrawers.registry;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.tile.StorageControllerExtensionTile;
import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import com.hrznstudio.titanium.module.BlockWithTile;
import com.hrznstudio.titanium.module.DeferredRegistryHelper;
import com.hrznstudio.titanium.nbthandler.NBTManager;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.source.FramedSourceDrawerBlock;
import dev.drimoz.immaterialdrawers.block.source.SourceDrawerBlock;
import dev.drimoz.immaterialdrawers.block.tile.source.FramedSourceDrawerTile;
import dev.drimoz.immaterialdrawers.block.tile.source.SourceDrawerTile;
import dev.drimoz.immaterialdrawers.gametest.IDSourceGameTests;
import dev.drimoz.immaterialdrawers.storage.source.ControllerSourceStorage;
import dev.drimoz.immaterialdrawers.storage.source.SourceCapabilities;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.List;

/**
 * The Source Drawer: everything this mod registers only when Ars Nouveau is installed.
 *
 * <p><b>Ars Nouveau only - see {@code compat.Mods}.</b> Same contract as {@code IDChemicalContent}:
 * the always-loaded code calls in only from behind {@code if (Mods.arsNouveau())}, through signatures
 * free of Ars types.
 *
 * <p>Two registry names, permanent: {@code source_drawer} and {@code framed_source_drawer}. One layout,
 * like the energy drawer - see {@code SourceDrawerTile}.
 */
public final class IDSourceContent {

    public static final String SOURCE_DRAWER_NAME = "source_drawer";
    public static final String FRAMED_SOURCE_DRAWER_NAME = "framed_source_drawer";

    public static BlockWithTile SOURCE_DRAWER;
    public static BlockWithTile FRAMED_SOURCE_DRAWER;

    private IDSourceContent() {
    }

    public static List<BlockWithTile> all() {
        return List.of(SOURCE_DRAWER, FRAMED_SOURCE_DRAWER);
    }

    /**
     * Amethyst, where the fluid drawer is stone bricks, the energy drawer copper and the chemical drawer
     * iron: it is how the block sounds and breaks, and amethyst is vanilla's word for magic.
     */
    public static void register(DeferredRegistryHelper registries) {
        SOURCE_DRAWER = registries.registerBlockWithTileItem(SOURCE_DRAWER_NAME,
                () -> new SourceDrawerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK)),
                block -> () -> new BlockItem(block.get(), new Item.Properties()),
                ImmaterialDrawers.TAB);
        FRAMED_SOURCE_DRAWER = registries.registerBlockWithTileItem(FRAMED_SOURCE_DRAWER_NAME,
                () -> new FramedSourceDrawerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK)),
                block -> () -> new BlockItem(block.get(), new Item.Properties()),
                ImmaterialDrawers.TAB);
    }

    public static void init(IEventBus modBus) {
        NBTManager.getInstance().scanTileClassForAnnotations(SourceDrawerTile.class);
        NBTManager.getInstance().scanTileClassForAnnotations(FramedSourceDrawerTile.class);
        modBus.addListener(IDSourceContent::registerCapabilities);
        // Not a @GameTestHolder, for the reason IDChemicalContent gives.
        modBus.addListener((RegisterGameTestsEvent event) -> event.register(IDSourceGameTests.class));
    }

    /**
     * The Source capability on both drawers and on Functional Storage's controllers and extensions,
     * so a relay aimed at a controller reaches the whole wall. Ars's {@code SourceManager} - the other
     * way in - is joined by each drawer itself, in {@code SourceDrawerTile#onLoad}.
     */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (BlockWithTile drawer : all()) {
            event.registerBlockEntity(SourceCapabilities.BLOCK, drawer.type().get(),
                    (blockEntity, side) -> blockEntity instanceof SourceDrawerTile tile ? tile.getSourceStorage() : null);
        }
        for (BlockWithTile controller : List.of(
                FunctionalStorage.DRAWER_CONTROLLER, FunctionalStorage.FRAMED_DRAWER_CONTROLLER)) {
            event.registerBlockEntity(SourceCapabilities.BLOCK, controller.type().get(),
                    (blockEntity, side) -> blockEntity instanceof StorageControllerTile<?> tile
                            ? ControllerSourceStorage.of(tile)
                            : null);
        }
        for (BlockWithTile extension : List.of(
                FunctionalStorage.CONTROLLER_EXTENSION, FunctionalStorage.FRAMED_CONTROLLER_EXTENSION)) {
            event.registerBlockEntity(SourceCapabilities.BLOCK, extension.type().get(),
                    (blockEntity, side) -> {
                        if (!(blockEntity instanceof StorageControllerExtensionTile<?> tile)
                                || tile.getControllerPos() == null
                                || tile.getLevel() == null
                                || !tile.getLevel().isLoaded(tile.getControllerPos())) {
                            return null;
                        }
                        return tile.getLevel().getBlockEntity(tile.getControllerPos())
                                instanceof StorageControllerTile<?> controller
                                ? ControllerSourceStorage.of(controller)
                                : null;
                    });
        }
    }
}
