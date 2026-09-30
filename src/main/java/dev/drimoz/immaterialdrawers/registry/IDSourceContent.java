package dev.drimoz.immaterialdrawers.registry;

import com.hrznstudio.titanium.module.DeferredRegistryHelper;
import com.hrznstudio.titanium.nbthandler.NBTManager;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.source.FramedSourceDrawerBlock;
import dev.drimoz.immaterialdrawers.block.source.SourceDrawerBlock;
import dev.drimoz.immaterialdrawers.block.tile.source.FramedSourceDrawerTile;
import dev.drimoz.immaterialdrawers.block.tile.source.SourceDrawerTile;
import dev.drimoz.immaterialdrawers.gametest.IDSourceGameTests;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * The Source Drawers. <b>Ars Nouveau only</b>: reached only through {@code Mods.arsNouveau()}.
 *
 * <p>No capability on 1.20.1: Ars 4.12 has none for Source ({@code ISourceCap} is 5.x). The drawer is
 * an {@code ISourceTile} found through {@code SourceManager}, and Functional Storage's controller
 * gets no Source aggregate - nothing in 4.12 would ask it.
 */
public final class IDSourceContent {

    public static final String SOURCE_DRAWER_NAME = "source_drawer";
    public static final String FRAMED_SOURCE_DRAWER_NAME = "framed_source_drawer";

    public static Pair<RegistryObject<Block>, RegistryObject<BlockEntityType<?>>> SOURCE_DRAWER;
    public static Pair<RegistryObject<Block>, RegistryObject<BlockEntityType<?>>> FRAMED_SOURCE_DRAWER;

    private IDSourceContent() {
    }

    public static List<Pair<RegistryObject<Block>, RegistryObject<BlockEntityType<?>>>> all() {
        return List.of(SOURCE_DRAWER, FRAMED_SOURCE_DRAWER);
    }

    public static void register(DeferredRegistryHelper registries) {
        SOURCE_DRAWER = registries.registerBlockWithTileItem(SOURCE_DRAWER_NAME,
                () -> new SourceDrawerBlock(BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK)),
                block -> () -> new BlockItem(block.get(), new Item.Properties()),
                ImmaterialDrawers.TAB);
        FRAMED_SOURCE_DRAWER = registries.registerBlockWithTileItem(FRAMED_SOURCE_DRAWER_NAME,
                () -> new FramedSourceDrawerBlock(BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK)
                        .noOcclusion().isViewBlocking((state, level, pos) -> false)),
                block -> () -> new BlockItem(block.get(), new Item.Properties()),
                ImmaterialDrawers.TAB);
    }

    public static void init(IEventBus modBus) {
        NBTManager.getInstance().scanTileClassForAnnotations(SourceDrawerTile.class);
        NBTManager.getInstance().scanTileClassForAnnotations(FramedSourceDrawerTile.class);
        // Not a @GameTestHolder: Forge loads every holder class in dev, Ars or not (CLAUDE.md §16).
        modBus.addListener((RegisterGameTestsEvent event) -> event.register(IDSourceGameTests.class));
    }
}
