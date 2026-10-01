package dev.drimoz.immaterialdrawers.block.chemical;

import com.buuz135.functionalstorage.FunctionalStorage;
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
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.crafting.conditions.ModLoadedCondition;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.function.Consumer;

/**
 * A chemical drawer block, 1, 2 or 4 tanks. <b>Mekanism only</b> - see {@code compat.Mods}.
 *
 * <p>Modelled on Functional Storage 1.20.1's {@code block/FluidDrawerBlock}.
 * Copyright (c) 2021 Buuz135, Rid - MIT. See NOTICE.
 */
public class ChemicalDrawerBlock extends ImmaterialDrawerBlock<ChemicalDrawerTile> {

    private static final ResourceLocation PRESSURIZED_TUBE = new ResourceLocation(Mods.MEKANISM, "basic_pressurized_tube");

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
                (BlockEntityType<ChemicalDrawerTile>) IDChemicalContent.drawer(type, false).getRight().get(), pos, state, type);
    }

    /** The average fill of the tanks, on vanilla's container scale - as a fluid drawer reports. */
    @Override
    protected int signalFor(ChemicalDrawerTile tile) {
        BigChemicalHandler handler = tile.getChemicalHandler();
        double fullness = 0;
        boolean hasContents = false;
        for (int tank = 0; tank < handler.tanks(); tank++) {
            ChemicalStack<?> stack = handler.stored(tank);
            if (!stack.isEmpty()) {
                hasContents = true;
                long capacity = handler.capacity(tank);
                fullness += capacity <= 0 ? 0 : Math.min(1D, stack.getAmount() / (double) capacity);
            }
        }
        return comparatorSignal(fullness / handler.tanks(), hasContents);
    }

    /** Functional Storage's Redstone Upgrade: the tank in its {@code Slot} tag, as their fluid drawer reads it. */
    @Override
    protected int redstoneSignal(ChemicalDrawerTile tile, ItemStack upgrade) {
        int watched = upgrade.getOrCreateTag().getInt("Slot");
        BigChemicalHandler handler = tile.getChemicalHandler();
        if (watched >= handler.tanks()) {
            return 0;
        }
        long capacity = handler.capacity(watched);
        return capacity <= 0 ? 0 : (int) Math.floor(
                Math.min(1D, handler.stored(watched).getAmount() / (double) capacity) * 15);
    }

    /** Planks around Mekanism's pressurized tubes, in Functional Storage's fluid drawer patterns. */
    @Override
    public void registerRecipe(Consumer<FinishedRecipe> consumer) {
        Item tube = ForgeRegistries.ITEMS.getValue(PRESSURIZED_TUBE);
        TitaniumShapedRecipeBuilder builder = switch (type) {
            case X_2 -> TitaniumShapedRecipeBuilder.shapedRecipe(this, 2);
            case X_4 -> TitaniumShapedRecipeBuilder.shapedRecipe(this, 4);
            default -> TitaniumShapedRecipeBuilder.shapedRecipe(this);
        };
        builder.getConditional()
                .addCondition(new ModLoadedCondition(Mods.MEKANISM))
                .addCondition(IDFeatures.enabled(IDFeatures.Feature.CHEMICAL_DRAWERS));
        switch (type) {
            case X_2 -> builder.pattern("PTP").pattern("PPP").pattern("PTP");
            case X_4 -> builder.pattern("TPT").pattern("PPP").pattern("TPT");
            default -> builder.pattern("PPP").pattern("PTP").pattern("PPP");
        }
        builder.define('P', shell()).define('T', tube);
        builder.save(consumer);
    }

    protected Ingredient shell() {
        return Ingredient.of(ItemTags.PLANKS);
    }

    @Override
    protected void appendContents(CompoundTag tile, List<Component> tooltip) {
        CompoundTag tanks = tile.getCompound("chemicalHandler");
        for (int tank = 0; tank < type.getSlots(); tank++) {
            ChemicalStack<?> stack = BigChemicalHandler.read(tanks, String.valueOf(tank));
            if (!stack.isEmpty()) {
                tooltip.add(Component.literal(" - ")
                        .append(Component.literal(ChemicalFormat.format(stack.getAmount())).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(" of ").withStyle(ChatFormatting.WHITE))
                        .append(stack.getTextComponent().copy().withStyle(ChatFormatting.GOLD)));
            }
        }
    }
}
