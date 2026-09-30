package dev.drimoz.immaterialdrawers.registry;

import com.google.gson.JsonObject;
import dev.drimoz.immaterialdrawers.IDConfig;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * The config switches (CLAUDE.md §17): "off" means unobtainable - no recipe, not in the creative
 * tab - never unregistered, so drawers already placed keep working.
 *
 * <p>On 1.20.1 a recipe condition is an {@link ICondition} with a JSON serializer registered through
 * {@link CraftingHelper#register}, where 1.21.1 registers a codec. Same JSON on disk:
 * {@code {"type": "immaterialdrawers:feature_enabled", "feature": "energy_drawer"}}.
 */
public final class IDFeatures {

    public enum Feature {
        ENERGY_DRAWER("energy_drawer", () -> IDConfig.ENERGY_DRAWER_ENABLED),
        CHEMICAL_DRAWERS("chemical_drawers", () -> IDConfig.CHEMICAL_DRAWERS_ENABLED),
        SOURCE_DRAWER("source_drawer", () -> IDConfig.SOURCE_DRAWER_ENABLED),
        WIRELESS_CHARGER("wireless_charger", () -> IDConfig.WIRELESS_CHARGER_ENABLED);

        private final String id;
        private final BooleanSupplier enabled;

        Feature(String id, BooleanSupplier enabled) {
            this.id = id;
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return enabled.getAsBoolean();
        }

        public String id() {
            return id;
        }

        static Feature byId(String id) {
            for (Feature feature : values()) {
                if (feature.id.equals(id)) {
                    return feature;
                }
            }
            throw new IllegalArgumentException("Unknown Immaterial Drawers feature: " + id);
        }

        /** What this switch hides from the creative tab. Grows as each step ports its content. */
        public List<ItemLike> items() {
            List<ItemLike> items = new ArrayList<>();
            if (this == ENERGY_DRAWER) {
                items.add(IDContent.ENERGY_DRAWER.getLeft().get());
                items.add(IDContent.FRAMED_ENERGY_DRAWER.getLeft().get());
            }
            return items;
        }
    }

    public record FeatureEnabledCondition(Feature feature) implements ICondition {

        public static final ResourceLocation ID = new ResourceLocation(ImmaterialDrawers.MOD_ID, "feature_enabled");

        @Override
        public ResourceLocation getID() {
            return ID;
        }

        @Override
        public boolean test(IContext context) {
            return feature.isEnabled();
        }
    }

    private static final IConditionSerializer<FeatureEnabledCondition> SERIALIZER = new IConditionSerializer<>() {
        @Override
        public void write(JsonObject json, FeatureEnabledCondition condition) {
            json.addProperty("feature", condition.feature().id());
        }

        @Override
        public FeatureEnabledCondition read(JsonObject json) {
            return new FeatureEnabledCondition(Feature.byId(json.get("feature").getAsString()));
        }

        @Override
        public ResourceLocation getID() {
            return FeatureEnabledCondition.ID;
        }
    };

    private static final ResourceLocation TAB = new ResourceLocation(ImmaterialDrawers.MOD_ID, "main");

    private IDFeatures() {
    }

    public static void init(IEventBus modBus) {
        // During registration, which is where Forge wants condition serializers registered.
        modBus.addListener((RegisterEvent event) -> {
            if (event.getRegistryKey().equals(ForgeRegistries.Keys.RECIPE_SERIALIZERS)) {
                CraftingHelper.register(SERIALIZER);
            }
        });
        // LOWEST: Titanium fills our tab from its own listener, and this has to run after it.
        modBus.addListener(EventPriority.LOWEST, IDFeatures::hideDisabled);
    }

    public static ICondition enabled(Feature feature) {
        return new FeatureEnabledCondition(feature);
    }

    private static void hideDisabled(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().location().equals(TAB)) {
            return;
        }
        for (Feature feature : Feature.values()) {
            if (!feature.isEnabled()) {
                for (ItemLike item : feature.items()) {
                    event.getEntries().remove(new ItemStack(item));
                }
            }
        }
    }
}
