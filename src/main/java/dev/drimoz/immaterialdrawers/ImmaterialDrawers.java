package dev.drimoz.immaterialdrawers;

import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import com.hrznstudio.titanium.module.ModuleController;
import com.hrznstudio.titanium.nbthandler.NBTManager;
import com.hrznstudio.titanium.tab.TitaniumTab;
import dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile;
import dev.drimoz.immaterialdrawers.block.tile.energy.FramedEnergyDrawerTile;
import dev.drimoz.immaterialdrawers.datagen.IDDataGenerators;
import dev.drimoz.immaterialdrawers.registry.IDContent;
import dev.drimoz.immaterialdrawers.registry.IDFeatures;
import dev.drimoz.immaterialdrawers.registry.IDSourceContent;
import dev.drimoz.immaterialdrawers.compat.Mods;
import dev.drimoz.immaterialdrawers.storage.ControllerEnergyStorage;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Drawers for what you can't hold.
 *
 * <p>Functional Storage stores items and fluids. This mod adds drawers for everything else, energy
 * first. The umbrella name is deliberate and the mod id is unchangeable: the id is written into
 * every save that contains one of these blocks - see CLAUDE.md §2.
 *
 * <p><b>1.20.1 backport</b> (PORTING.md). Forge, not NeoForge, because Functional Storage 1.20.1 is
 * published for Forge only.
 */
@Mod(ImmaterialDrawers.MOD_ID)
public class ImmaterialDrawers extends ModuleController {

    public static final String MOD_ID = "immaterialdrawers";

    public static final TitaniumTab TAB = new TitaniumTab(new ResourceLocation(MOD_ID, "main"));

    private static final ResourceLocation CONTROLLER_ENERGY = new ResourceLocation(MOD_ID, "controller_energy");

    public ImmaterialDrawers() {
        // Registration happens inside this call: ModuleController's constructor runs initModules().
        super();

        // Titanium's @Save is reflective and opt-in per class. Without this scan the drawer's
        // energy is not written to disk at all, and the failure is silent.
        NBTManager.getInstance().scanTileClassForAnnotations(EnergyDrawerTile.class);
        // Per class: the framed tile adds a @Save field of its own. Miss it and the drawer keeps its
        // energy but forgets its design on reload.
        NBTManager.getInstance().scanTileClassForAnnotations(FramedEnergyDrawerTile.class);

        IDFeatures.init(FMLJavaModLoadingContext.get().getModEventBus());
        // The Source Drawers exist only with Ars Nouveau, and nothing that touches its classes may run
        // without it - see compat.Mods. The registration half is in initModules.
        if (Mods.arsNouveau()) {
            IDSourceContent.init(FMLJavaModLoadingContext.get().getModEventBus());
        }
        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, ImmaterialDrawers::attachControllerEnergy);
    }

    /** Datagen entry point. Titanium's ModuleController subscribes this to GatherDataEvent for us. */
    @Override
    public void addDataProvider(GatherDataEvent event) {
        IDDataGenerators.gather(event);
    }

    @Override
    protected void initModules() {
        IDContent.register(getRegistries());
        if (Mods.arsNouveau()) {
            IDSourceContent.register(getRegistries());
        }
        addCreativeTab("main", () -> new ItemStack(IDContent.ENERGY_DRAWER.getLeft().get()), MOD_ID, TAB);
    }

    /**
     * Gives Functional Storage's controllers an energy capability: a cable on the controller reaches
     * every energy drawer linked to it (CLAUDE.md §7).
     *
     * <p>On 1.21.1 this is a provider registered against their {@code BlockEntityType}. Forge 1.20.1
     * has no such registry; it asks the block entity, and a block entity's own {@code getCapability}
     * falls through to the providers attached by this event. Theirs does:
     * {@code StorageControllerTile.getCapability} ends in {@code super.getCapability}, and the
     * extension forwards to its controller's {@code getCapability}, so extensions are covered with no
     * attachment of their own (PORTING.md §3).
     *
     * <p>Attached to every {@code StorageControllerTile}: the framed controller is one too.
     */
    private static void attachControllerEnergy(AttachCapabilitiesEvent<BlockEntity> event) {
        if (!(event.getObject() instanceof StorageControllerTile<?> controller)) {
            return;
        }
        LazyOptional<IEnergyStorage> energy = LazyOptional.of(() -> ControllerEnergyStorage.of(controller));
        event.addCapability(CONTROLLER_ENERGY, new ICapabilityProvider() {
            @NotNull
            @Override
            public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                return cap == ForgeCapabilities.ENERGY ? energy.cast() : LazyOptional.empty();
            }
        });
        event.addListener(energy::invalidate);
    }
}
