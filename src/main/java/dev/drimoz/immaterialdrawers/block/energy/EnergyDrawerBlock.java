package dev.drimoz.immaterialdrawers.block.energy;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.hrznstudio.titanium.recipe.generator.TitaniumShapedRecipeBuilder;
import dev.drimoz.immaterialdrawers.block.ImmaterialDrawerBlock;
import dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile;
import dev.drimoz.immaterialdrawers.registry.IDContent;
import dev.drimoz.immaterialdrawers.registry.IDFeatures;
import dev.drimoz.immaterialdrawers.util.EnergyFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.List;
import java.util.function.Consumer;

/**
 * The Energy Drawer block.
 *
 * <p>Modelled on Functional Storage 1.20.1's {@code block/FluidDrawerBlock}.
 * Copyright (c) 2021 Buuz135, Rid - MIT. See NOTICE. What every drawer of ours shares is in
 * {@link ImmaterialDrawerBlock}.
 */
public class EnergyDrawerBlock extends ImmaterialDrawerBlock<EnergyDrawerTile> {

    /** One kind of content, one slot layout. See CLAUDE.md §5. */
    public static final FunctionalStorage.DrawerType TYPE = FunctionalStorage.DrawerType.X_1;

    public EnergyDrawerBlock(Properties properties) {
        super(IDContent.ENERGY_DRAWER_NAME, properties, EnergyDrawerTile.class, TYPE);
    }

    @SuppressWarnings("unchecked")
    @Override
    public BlockEntityType.BlockEntitySupplier<EnergyDrawerTile> getTileEntityFactory() {
        return (pos, state) -> new EnergyDrawerTile(this,
                (BlockEntityType<EnergyDrawerTile>) IDContent.ENERGY_DRAWER.getRight().get(), pos, state);
    }

    /** The long accessors: above 2.1B the clamped view reads full at every fill level. */
    @Override
    protected int signalFor(EnergyDrawerTile tile) {
        var storage = tile.getEnergyStorage();
        long capacity = storage.getCapacityLong();
        long stored = storage.getStoredLong();
        return comparatorSignal(capacity <= 0 ? 0 : stored / (double) capacity, capacity > 0 && stored > 0);
    }

    /**
     * Planks around a redstone block, the shape Functional Storage gives its fluid drawer (planks
     * around a bucket): the centre ingredient is what the drawer is for.
     */
    @Override
    public void registerRecipe(Consumer<FinishedRecipe> consumer) {
        TitaniumShapedRecipeBuilder builder = TitaniumShapedRecipeBuilder.shapedRecipe(this);
        builder.getConditional().addCondition(IDFeatures.enabled(IDFeatures.Feature.ENERGY_DRAWER));
        builder.pattern("PPP").pattern("PRP").pattern("PPP")
                .define('P', ItemTags.PLANKS)
                .define('R', Blocks.REDSTONE_BLOCK);
        builder.save(consumer);
    }

    /** The charge, from the field name Titanium's {@code @Save} gave the storage. */
    @Override
    protected void appendContents(CompoundTag tile, List<Component> tooltip) {
        CompoundTag energy = tile.getCompound("energyStorage");
        tooltip.add(Component.literal(" - ")
                .append(Component.literal(EnergyFormat.format(energy.getLong("Energy")))
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" / ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(EnergyFormat.format(energy.getLong("Capacity")) + " FE")
                        .withStyle(ChatFormatting.GOLD)));
    }

    public static class EnergyDrawerItem extends BlockItem {
        public EnergyDrawerItem(EnergyDrawerBlock block, Properties props) {
            super(block, props);
        }
    }
}
