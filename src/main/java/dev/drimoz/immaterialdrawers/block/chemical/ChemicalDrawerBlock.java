package dev.drimoz.immaterialdrawers.block.chemical;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.item.FSAttachments;
import com.buuz135.functionalstorage.item.component.EmitRedstoneBehavior;
import com.hrznstudio.titanium.recipe.generator.TitaniumShapedRecipeBuilder;
import dev.drimoz.immaterialdrawers.block.ImmaterialDrawerBlock;
import dev.drimoz.immaterialdrawers.block.tile.chemical.ChemicalDrawerTile;
import dev.drimoz.immaterialdrawers.compat.Mods;
import dev.drimoz.immaterialdrawers.registry.IDChemicalContent;
import dev.drimoz.immaterialdrawers.registry.IDFeatures;
import dev.drimoz.immaterialdrawers.storage.chemical.BigChemicalHandler;
import dev.drimoz.immaterialdrawers.util.ChemicalFormat;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;

import java.util.List;

/**
 * The Chemical Drawer block, in each of Functional Storage's three layouts.
 *
 * <p><b>Mekanism only - see {@code compat.Mods}.</b>
 *
 * <p>Modelled on Functional Storage's {@code block/FluidDrawerBlock}, which is one class for the three
 * sizes; so is this. Copyright (c) 2021 Buuz135, Rid - MIT. See NOTICE.
 */
public class ChemicalDrawerBlock extends ImmaterialDrawerBlock<ChemicalDrawerTile> {

    /**
     * Mekanism's own item for "a pipe that carries chemicals". In the centre of the recipe where the
     * fluid drawer has an empty bucket and the energy drawer a redstone block: the drawer is made from
     * what it holds. Looked up by name, because an item reference would need Mekanism's
     * implementation classes rather than its API.
     */
    private static final ResourceLocation PRESSURIZED_TUBE =
            ResourceLocation.fromNamespaceAndPath(Mods.MEKANISM, "basic_pressurized_tube");

    public ChemicalDrawerBlock(FunctionalStorage.DrawerType type, Properties properties) {
        this(IDChemicalContent.nameFor(type, false), type, properties);
    }

    protected ChemicalDrawerBlock(String name, FunctionalStorage.DrawerType type, Properties properties) {
        super(name, properties, ChemicalDrawerTile.class, type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public BlockEntityType.BlockEntitySupplier<ChemicalDrawerTile> getTileEntityFactory() {
        return (pos, state) -> new ChemicalDrawerTile(this,
                (BlockEntityType<ChemicalDrawerTile>) IDChemicalContent.drawer(type, false).type().get(),
                pos, state, type);
    }

    /**
     * {@code Drawer.getFluidComparatorSignal}, over our tanks: the average fullness of the slots, so a
     * 4-slot drawer with one full tank reads a quarter. Redefined for the same reason as the energy
     * drawer's - their dispatch sees an item drawer with no items and says zero.
     */
    @Override
    protected int signalFor(ChemicalDrawerTile tile) {
        BigChemicalHandler handler = tile.getChemicalHandler();
        double fullness = 0;
        boolean hasContents = false;
        for (int tank = 0; tank < handler.getChemicalTanks(); tank++) {
            ChemicalStack stack = handler.getChemicalInTank(tank);
            if (!stack.isEmpty()) {
                hasContents = true;
                long capacity = handler.getChemicalTankCapacity(tank);
                fullness += capacity <= 0 ? 0 : Math.min(1D, stack.getAmount() / (double) capacity);
            }
        }
        return comparatorSignal(fullness / handler.getChemicalTanks(), hasContents);
    }

    /**
     * Functional Storage's Redstone Upgrade, reading the slot it was configured for - their
     * {@code EmitRedstoneBehavior} fluid branch, which cannot see us: it matches
     * {@code FluidDrawerTile} and otherwise falls to the item branch, where our empty handler makes it
     * say nothing. Same arithmetic as theirs for fluids, so the same upgrade on the fluid drawer next
     * door reads the same way.
     */
    @Override
    protected int redstoneSignal(ChemicalDrawerTile tile, ItemStack upgrade) {
        int watched = upgrade.getOrDefault(FSAttachments.SLOT, 0);
        BigChemicalHandler handler = tile.getChemicalHandler();
        if (watched >= handler.getChemicalTanks()) {
            return 0;
        }
        long capacity = handler.getChemicalTankCapacity(watched);
        return capacity <= 0 ? 0 : (int) Math.floor(
                Math.min(1D, handler.getChemicalInTank(watched).getAmount() / (double) capacity) * 15);
    }

    /**
     * Planks around a pressurized tube: Functional Storage's fluid drawer recipes, shape for shape,
     * with the tube in place of the bucket - one for 1x1, two for 1x2, four for 2x2.
     *
     * <p>Behind a {@code mod_loaded} condition, not only because the block needs Mekanism: the recipe
     * file ships in the jar whether Mekanism is there or not, and an unconditioned recipe naming a
     * missing item is an error in every log of every pack without it.
     */
    @Override
    public void registerRecipe(RecipeOutput consumer) {
        RecipeOutput output = consumer.withConditions(new ModLoadedCondition(Mods.MEKANISM), IDFeatures.enabled(IDFeatures.Feature.CHEMICAL_DRAWERS));
        Item tube = BuiltInRegistries.ITEM.get(PRESSURIZED_TUBE);
        switch (type) {
            case X_2 -> TitaniumShapedRecipeBuilder.shapedRecipe(this, 2)
                    .pattern("PTP").pattern("PPP").pattern("PTP")
                    .define('P', shell()).define('T', tube)
                    .save(output);
            case X_4 -> TitaniumShapedRecipeBuilder.shapedRecipe(this, 4)
                    .pattern("TPT").pattern("PPP").pattern("TPT")
                    .define('P', shell()).define('T', tube)
                    .save(output);
            default -> TitaniumShapedRecipeBuilder.shapedRecipe(this)
                    .pattern("PPP").pattern("PTP").pattern("PPP")
                    .define('P', shell()).define('T', tube)
                    .save(output);
        }
    }

    /** What the recipe surrounds the tube with. Planks, like every unframed drawer. */
    protected Ingredient shell() {
        return Ingredient.of(ItemTags.PLANKS);
    }

    /** Each non-empty tank as "amount of chemical", the way the fluid drawer lists its fluids. */
    @Override
    protected void appendContents(CompoundTag tile, Item.TooltipContext context, List<Component> tooltip) {
        HolderLookup.Provider registries = context.registries();
        if (registries == null) {
            return;
        }
        CompoundTag tanks = tile.getCompound("chemicalHandler");
        for (int tank = 0; tank < type.getSlots(); tank++) {
            ChemicalStack stack = BigChemicalHandler.read(registries, tanks, String.valueOf(tank));
            if (!stack.isEmpty()) {
                tooltip.add(Component.literal(" - ")
                        .append(Component.literal(ChemicalFormat.format(stack.getAmount())).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(" of ").withStyle(ChatFormatting.WHITE))
                        .append(stack.getTextComponent().copy().withStyle(ChatFormatting.GOLD)));
            }
        }
    }
}
