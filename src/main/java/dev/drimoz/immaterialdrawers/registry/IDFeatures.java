package dev.drimoz.immaterialdrawers.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.drimoz.immaterialdrawers.IDConfig;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.compat.Mods;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * The parts of this mod a pack author can switch off, and what switching one off means.
 *
 * <p><b>Off means unobtainable, not unregistered.</b> A disabled feature loses its recipes - through
 * the {@link FeatureEnabledCondition} this class registers - and disappears from the creative tab.
 * Its blocks and items stay registered, and the ones already in the world keep working.
 *
 * <p>Unregistering was the obvious alternative and it is wrong twice over. It deletes every placed
 * drawer, and its contents, from any world that already has one. And registries have to match
 * between a server and its clients: a pack whose server disables a drawer and whose client does not
 * would refuse every connection. Titanium also registers content before it loads any config, so the
 * switch could not gate registration even if that were wanted. See CLAUDE.md §17.
 */
public final class IDFeatures {

    public enum Feature implements StringRepresentable {
        ENERGY_DRAWER("energy_drawer", () -> IDConfig.ENERGY_DRAWER_ENABLED),
        CHEMICAL_DRAWERS("chemical_drawers", () -> IDConfig.CHEMICAL_DRAWERS_ENABLED),
        SOURCE_DRAWER("source_drawer", () -> IDConfig.SOURCE_DRAWER_ENABLED),
        WIRELESS_CHARGER("wireless_charger", () -> IDConfig.WIRELESS_CHARGER_ENABLED);

        public static final Codec<Feature> CODEC = StringRepresentable.fromEnum(Feature::values);

        private final String id;
        private final BooleanSupplier enabled;

        Feature(String id, BooleanSupplier enabled) {
            this.id = id;
            this.enabled = enabled;
        }

        /** Read from the config on every call: nothing about a feature's state may be cached. */
        public boolean isEnabled() {
            return enabled.getAsBoolean();
        }

        @Override
        public String getSerializedName() {
            return id;
        }

        /**
         * What this feature puts in the creative tab. The chemical drawers are only asked for behind
         * the Mekanism guard - see {@code compat.Mods}.
         */
        public List<ItemLike> items() {
            List<ItemLike> items = new ArrayList<>();
            switch (this) {
                case ENERGY_DRAWER -> {
                    items.add(IDContent.ENERGY_DRAWER.getBlock());
                    items.add(IDContent.FRAMED_ENERGY_DRAWER.getBlock());
                }
                case CHEMICAL_DRAWERS -> {
                    if (Mods.mekanism()) {
                        IDChemicalContent.all().forEach(drawer -> items.add(drawer.getBlock()));
                    }
                }
                case SOURCE_DRAWER -> {
                    if (Mods.arsNouveau()) {
                        IDSourceContent.all().forEach(drawer -> items.add(drawer.getBlock()));
                    }
                }
                case WIRELESS_CHARGER -> items.add(IDContent.WIRELESS_CHARGER.get());
            }
            return items;
        }
    }

    /**
     * {@code {"type": "immaterialdrawers:feature_enabled", "feature": "energy_drawer"}} - true while
     * the feature is switched on. Evaluated when data packs load, so a change to the config takes
     * effect at the next world load or {@code /reload}.
     */
    public record FeatureEnabledCondition(Feature feature) implements ICondition {

        public static final MapCodec<FeatureEnabledCondition> CODEC =
                Feature.CODEC.fieldOf("feature").xmap(FeatureEnabledCondition::new, FeatureEnabledCondition::feature);

        @Override
        public boolean test(IContext context) {
            return feature.isEnabled();
        }

        @Override
        public MapCodec<? extends ICondition> codec() {
            return CODEC;
        }
    }

    private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, ImmaterialDrawers.MOD_ID);

    static {
        CONDITIONS.register("feature_enabled", () -> FeatureEnabledCondition.CODEC);
    }

    private static final ResourceLocation TAB = ResourceLocation.fromNamespaceAndPath(ImmaterialDrawers.MOD_ID, "main");

    private IDFeatures() {
    }

    public static void init(IEventBus modBus) {
        CONDITIONS.register(modBus);
        // LOWEST: Titanium fills our tab from its own listener, and this has to run after it to have
        // anything to take out.
        modBus.addListener(EventPriority.LOWEST, IDFeatures::hideDisabled);
    }

    public static ICondition enabled(Feature feature) {
        return new FeatureEnabledCondition(feature);
    }

    /** The first enabled drawer, for the tab icon - an icon of something the player cannot get reads as a bug. */
    public static Supplier<ItemStack> tabIcon() {
        return () -> {
            for (Feature feature : Feature.values()) {
                if (feature.isEnabled() && !feature.items().isEmpty()) {
                    return new ItemStack(feature.items().getFirst());
                }
            }
            return new ItemStack(IDContent.ENERGY_DRAWER.getBlock());
        };
    }

    private static void hideDisabled(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().location().equals(TAB)) {
            return;
        }
        for (Feature feature : Feature.values()) {
            if (!feature.isEnabled()) {
                for (ItemLike item : feature.items()) {
                    event.remove(new ItemStack(item), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                }
            }
        }
    }
}
