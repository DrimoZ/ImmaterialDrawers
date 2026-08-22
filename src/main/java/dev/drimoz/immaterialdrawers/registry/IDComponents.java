package dev.drimoz.immaterialdrawers.registry;

import com.buuz135.functionalstorage.item.component.SizeProvider;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Data components this mod registers.
 *
 * <p>Registered with a plain {@link DeferredRegister} rather than through Titanium: Titanium's
 * helper exists to build block entity types and wire their capabilities, and a data component has
 * neither. Functional Storage registers its own the same way.
 */
public final class IDComponents {

    public static final DeferredRegister<DataComponentType<?>> DR =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ImmaterialDrawers.MOD_ID);

    /**
     * How much one storage upgrade scales an <em>energy</em> drawer.
     *
     * <p>Functional Storage's upgrade items already carry {@code item_storage_modifier},
     * {@code fluid_storage_modifier} and {@code controller_range_modifier}; this is a fourth, in
     * our namespace, attached to their items at load through {@code ModifyDefaultComponentsEvent}.
     *
     * <p>Reusing their fluid component would have been one line instead of this file. It would also
     * have welded energy to the fluid curve — the same {@code FLUID_DIVISOR} of 2, and therefore a
     * base capacity of ~32,000 FE, which is less than the cheapest energy cell in any tech mod.
     * Energy needs a harsher divisor than fluids for the fourth upgrade slot to do anything at all,
     * and a component of our own is the only way to have one. See {@code EnergyScaling}.
     *
     * <p>Persistent only, no stream codec — same as Functional Storage's. These are <em>default</em>
     * components on an item, so the client derives them from the item definition and there is
     * nothing to synchronise.
     */
    public static final Supplier<DataComponentType<SizeProvider>> ENERGY_STORAGE_MODIFIER =
            DR.register("energy_storage_modifier",
                    () -> DataComponentType.<SizeProvider>builder().persistent(SizeProvider.CODEC).build());

    private IDComponents() {
    }
}
