package dev.drimoz.immaterialdrawers;

import com.hrznstudio.titanium.module.ModuleController;
import com.hrznstudio.titanium.nbthandler.NBTManager;
import com.hrznstudio.titanium.tab.TitaniumTab;
import dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile;
import dev.drimoz.immaterialdrawers.registry.IDContent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Drawers for what you can't hold.
 *
 * <p>Functional Storage stores items and fluids. This mod adds drawers for everything else, energy
 * first. The umbrella name is deliberate and the mod id is unchangeable: the id is written into
 * every save that contains one of these blocks, so nothing here is allowed to be about energy
 * specifically - see CLAUDE.md §2.
 */
@Mod(ImmaterialDrawers.MOD_ID)
public class ImmaterialDrawers extends ModuleController {

    public static final String MOD_ID = "immaterialdrawers";

    public static final TitaniumTab TAB = new TitaniumTab(ResourceLocation.fromNamespaceAndPath(MOD_ID, "main"));

    public ImmaterialDrawers(Dist dist, IEventBus modBus, ModContainer container) {
        // Registration happens inside this call: ModuleController's constructor runs
        // initModules(). Nothing below may depend on an instance field of this class, because
        // none of them are assigned yet when it does.
        super(container);

        // Titanium's @Save is reflective and opt-in per class. Without this scan the drawer's
        // energy is not written to disk at all, and the failure is silent.
        NBTManager.getInstance().scanTileClassForAnnotations(EnergyDrawerTile.class);

        modBus.addListener(this::registerCapabilities);
    }

    @Override
    protected void initModules() {
        IDContent.register(getRegistries());
        addCreativeTab("main", () -> new ItemStack(IDContent.ENERGY_DRAWER.getBlock()), MOD_ID, TAB);
    }

    /**
     * Registers the energy capability by hand, because Titanium will not do it for us.
     *
     * <p>Titanium's {@code DeferredRegistryHelper.registerBlockWithTile} already registers an
     * {@code EnergyStorage.BLOCK} provider on every block entity type it creates - but that
     * provider only answers for a {@code PoweredTile}, and {@code PoweredTile} and
     * {@code ControllableDrawerTile} are two sibling subclasses of {@code ActiveTile}. Java has
     * single inheritance and the drawer half is not negotiable, so Titanium's provider will return
     * null for us forever.
     *
     * <p>That is survivable because NeoForge keeps a <em>list</em> of providers per block and walks
     * it until one returns non-null ({@code BlockCapability.getCapability}). Titanium's returns
     * null, ours answers. This method is the second half of the reason task 1 exists as a game
     * test: the reading is sound, and the test is what makes it a fact. See CLAUDE.md §8.
     */
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                IDContent.ENERGY_DRAWER.type().get(),
                (blockEntity, side) -> blockEntity instanceof EnergyDrawerTile drawer
                        ? drawer.getEnergyStorage()
                        : null);
    }
}
