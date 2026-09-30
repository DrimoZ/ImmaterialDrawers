package dev.drimoz.immaterialdrawers.registry;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
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
import mekanism.api.chemical.ChemicalType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The chemical drawers: 1, 2 and 4 tanks, plain and framed. <b>Mekanism only</b>: reached only
 * through {@code Mods.mekanism()}. Registry names as on 1.21.1, so a world keeps its drawers.
 */
public final class IDChemicalContent {

    public static final List<FunctionalStorage.DrawerType> TYPES = List.of(
            FunctionalStorage.DrawerType.X_1, FunctionalStorage.DrawerType.X_2, FunctionalStorage.DrawerType.X_4);

    private static final ResourceLocation CONTROLLER_CHEMICALS = new ResourceLocation(ImmaterialDrawers.MOD_ID, "controller_chemicals");

    private static final Map<FunctionalStorage.DrawerType, Pair<RegistryObject<Block>, RegistryObject<BlockEntityType<?>>>> DRAWERS =
            new EnumMap<>(FunctionalStorage.DrawerType.class);
    private static final Map<FunctionalStorage.DrawerType, Pair<RegistryObject<Block>, RegistryObject<BlockEntityType<?>>>> FRAMED =
            new EnumMap<>(FunctionalStorage.DrawerType.class);

    private IDChemicalContent() {
    }

    public static String nameFor(FunctionalStorage.DrawerType type, boolean framed) {
        return (framed ? "framed_" : "") + "chemical_drawer_" + type.getSlots();
    }

    public static Pair<RegistryObject<Block>, RegistryObject<BlockEntityType<?>>> drawer(FunctionalStorage.DrawerType type, boolean framed) {
        return (framed ? FRAMED : DRAWERS).get(type);
    }

    public static List<Pair<RegistryObject<Block>, RegistryObject<BlockEntityType<?>>>> all() {
        List<Pair<RegistryObject<Block>, RegistryObject<BlockEntityType<?>>>> all = new ArrayList<>(DRAWERS.values());
        all.addAll(FRAMED.values());
        return all;
    }

    public static void register(DeferredRegistryHelper registries) {
        for (FunctionalStorage.DrawerType type : TYPES) {
            DRAWERS.put(type, registries.registerBlockWithTileItem(nameFor(type, false),
                    () -> new ChemicalDrawerBlock(type, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)),
                    block -> () -> new BlockItem(block.get(), new Item.Properties()),
                    ImmaterialDrawers.TAB));
        }
        for (FunctionalStorage.DrawerType type : TYPES) {
            FRAMED.put(type, registries.registerBlockWithTileItem(nameFor(type, true),
                    () -> new FramedChemicalDrawerBlock(type, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                            .noOcclusion().isViewBlocking((state, level, pos) -> false)),
                    block -> () -> new BlockItem(block.get(), new Item.Properties()),
                    ImmaterialDrawers.TAB));
        }
    }

    public static void init(IEventBus modBus) {
        NBTManager.getInstance().scanTileClassForAnnotations(ChemicalDrawerTile.class);
        NBTManager.getInstance().scanTileClassForAnnotations(FramedChemicalDrawerTile.class);
        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, IDChemicalContent::attachControllerChemicals);
        // Not a @GameTestHolder: Forge loads every holder class in dev, Mekanism or not (CLAUDE.md §16).
        modBus.addListener((RegisterGameTestsEvent event) -> event.register(IDChemicalGameTests.class));
    }

    /**
     * The four chemical capabilities on Functional Storage's controllers - the whole network's tanks,
     * as for energy. Extensions forward to their controller's {@code getCapability}.
     */
    private static void attachControllerChemicals(AttachCapabilitiesEvent<BlockEntity> event) {
        if (!(event.getObject() instanceof StorageControllerTile<?> controller)) {
            return;
        }
        Map<ChemicalType, LazyOptional<?>> optionals = new EnumMap<>(ChemicalType.class);
        for (ChemicalType type : ChemicalType.values()) {
            optionals.put(type, LazyOptional.of(() -> ControllerChemicalHandler.of(controller).view(type)));
        }
        event.addCapability(CONTROLLER_CHEMICALS, new ICapabilityProvider() {
            @NotNull
            @Override
            public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                ChemicalType type = ChemicalCapabilities.typeOf(cap);
                return type == null ? LazyOptional.empty() : optionals.get(type).cast();
            }
        });
        event.addListener(() -> optionals.values().forEach(LazyOptional::invalidate));
    }
}
