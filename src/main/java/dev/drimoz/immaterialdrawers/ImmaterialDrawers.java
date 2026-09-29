package dev.drimoz.immaterialdrawers;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.config.FunctionalStorageConfig;
import com.buuz135.functionalstorage.block.tile.StorageControllerExtensionTile;
import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import com.buuz135.functionalstorage.item.StorageUpgradeItem;
import com.buuz135.functionalstorage.item.component.FunctionalUpgradeBehavior;
import com.buuz135.functionalstorage.item.component.SizeProvider;
import dev.drimoz.immaterialdrawers.augment.ChargeNearbyBehavior;
import com.hrznstudio.titanium.module.BlockWithTile;
import com.hrznstudio.titanium.module.ModuleController;
import com.hrznstudio.titanium.nbthandler.NBTManager;
import com.hrznstudio.titanium.tab.TitaniumTab;
import dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile;
import dev.drimoz.immaterialdrawers.block.tile.energy.FramedEnergyDrawerTile;
import dev.drimoz.immaterialdrawers.datagen.IDDataGenerators;
import dev.drimoz.immaterialdrawers.registry.IDComponents;
import dev.drimoz.immaterialdrawers.registry.IDContent;
import dev.drimoz.immaterialdrawers.registry.IDFeatures;
import dev.drimoz.immaterialdrawers.registry.IDChemicalContent;
import dev.drimoz.immaterialdrawers.compat.Mods;
import dev.drimoz.immaterialdrawers.storage.ControllerEnergyStorage;
import dev.drimoz.immaterialdrawers.storage.EnergyScaling;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

import java.util.List;

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
        // Both tile classes, not just the base one: the scan is per class and the framed variant
        // adds a @Save field of its own. Miss it and the drawer keeps its energy but forgets the
        // textures the player framed it with.
        NBTManager.getInstance().scanTileClassForAnnotations(EnergyDrawerTile.class);
        NBTManager.getInstance().scanTileClassForAnnotations(FramedEnergyDrawerTile.class);

        IDComponents.DR.register(modBus);
        IDFeatures.init(modBus);

        // The chemical drawers exist only with Mekanism, and nothing that touches its classes may
        // run without it - see compat.Mods. The registration half is in initModules.
        if (Mods.mekanism()) {
            IDChemicalContent.init(modBus);
        }

        modBus.addListener(this::registerCapabilities);
        modBus.addListener(this::addEnergyScalingToStorageUpgrades);
    }

    @Override
    protected void initModules() {
        IDContent.register(getRegistries());
        if (Mods.mekanism()) {
            IDChemicalContent.register(getRegistries());
        }

        // The augment's codec, in Functional Storage's registry. This is their one supported
        // extension point (CLAUDE.md §9) and it is a plain registry entry - no mixin, no hook.
        getRegistries().registerGeneric(FunctionalUpgradeBehavior.REGISTRY_KEY, "charge_nearby",
                () -> ChargeNearbyBehavior.CODEC);
        addCreativeTab("main", IDFeatures.tabIcon(), MOD_ID, TAB);
    }

    /**
     * Datagen entry point. Titanium's ModuleController subscribes this to GatherDataEvent for us.
     */
    @Override
    public void addDataProvider(GatherDataEvent event) {
        IDDataGenerators.gather(event);
    }

    /**
     * Teaches Functional Storage's storage upgrades how much they scale an energy drawer.
     *
     * <p>Their upgrade items already carry a {@code SizeProvider} per resource — one for items, one
     * for fluids, one for controller range. Energy needs a fourth, with a harsher divisor than any
     * of them, or the fourth upgrade slot does nothing at all (see {@code EnergyScaling}).
     * {@code ModifyDefaultComponentsEvent} is how NeoForge lets one mod add a default component to
     * another mod's item, which is exactly this situation and needs no mixin.
     *
     * <p>The tiers are read from {@code FunctionalStorageConfig}, not copied: those multipliers are
     * config values, and a pack that doubles Netherite should move our curve with it rather than
     * leave energy quietly on the stock numbers.
     */
    private void addEnergyScalingToStorageUpgrades(ModifyDefaultComponentsEvent event) {
        FunctionalStorage.STORAGE_UPGRADES.forEach((tier, item) -> {
            SizeProvider provider = energyModifierFor(tier);
            event.modify(item.get(), builder -> builder.set(IDComponents.ENERGY_STORAGE_MODIFIER.get(), provider));
        });
    }

    /**
     * The energy-side {@code SizeProvider} for one upgrade tier, mirroring what
     * {@code StorageUpgradeItem} builds for items and fluids.
     *
     * <p>Iron is a downgrade, not an upgrade: it sets the base rather than multiplying it, which is
     * how a player undoes an over-upgraded drawer. Everything else multiplies, divided by
     * {@code ENERGY_DIVISOR}.
     */
    private static SizeProvider energyModifierFor(StorageUpgradeItem.StorageTier tier) {
        if (tier == StorageUpgradeItem.StorageTier.IRON) {
            return new SizeProvider.SetBase(1);
        }
        return new SizeProvider.ModifyFactor(
                EnergyScaling.upgradeFactor(FunctionalStorageConfig.getLevelMult(tier.getLevel())));
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
        // Once per block entity type, because that is what a provider is registered against - the
        // framed variant has its own type and would otherwise have no energy capability at all,
        // which reads as a drawer that silently refuses every cable.
        for (BlockWithTile drawer : List.of(IDContent.ENERGY_DRAWER, IDContent.FRAMED_ENERGY_DRAWER)) {
            event.registerBlockEntity(
                    Capabilities.EnergyStorage.BLOCK,
                    drawer.type().get(),
                    (blockEntity, side) -> blockEntity instanceof EnergyDrawerTile tile
                            ? tile.getEnergyStorage()
                            : null);
        }

        // And on Functional Storage's own controllers, so a cable on the controller reaches every
        // energy drawer linked to it - the same way their controller already stands in for the
        // items and the fluids of its network. Their controller cannot do this for us: it collects
        // our deliberately empty item handler and has no third kind of content to look for.
        //
        // Registering a provider on another mod's block entity type is ordinary NeoForge - it never
        // asks who owns the type - and it is the payoff of the reading in CLAUDE.md §7 that the
        // connected-drawer list is public. See ControllerEnergyStorage.
        for (BlockWithTile controller : List.of(
                FunctionalStorage.DRAWER_CONTROLLER,
                FunctionalStorage.FRAMED_DRAWER_CONTROLLER)) {
            event.registerBlockEntity(
                    Capabilities.EnergyStorage.BLOCK,
                    controller.type().get(),
                    (blockEntity, side) -> blockEntity instanceof StorageControllerTile<?> tile
                            ? ControllerEnergyStorage.of(tile)
                            : null);
        }

        // Extensions are not controllers; they forward to the one they are linked to. Same
        // treatment, so a wall reachable through an extension is reachable for energy too.
        for (BlockWithTile extension : List.of(
                FunctionalStorage.CONTROLLER_EXTENSION,
                FunctionalStorage.FRAMED_CONTROLLER_EXTENSION)) {
            event.registerBlockEntity(
                    Capabilities.EnergyStorage.BLOCK,
                    extension.type().get(),
                    (blockEntity, side) -> {
                        if (!(blockEntity instanceof StorageControllerExtensionTile<?> tile)
                                || tile.getControllerPos() == null
                                || tile.getLevel() == null
                                || !tile.getLevel().isLoaded(tile.getControllerPos())) {
                            return null;
                        }
                        return tile.getLevel().getBlockEntity(tile.getControllerPos())
                                instanceof StorageControllerTile<?> controller
                                ? ControllerEnergyStorage.of(controller)
                                : null;
                    });
        }
    }
}
