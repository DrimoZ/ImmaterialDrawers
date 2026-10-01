package dev.drimoz.immaterialdrawers.block.energy;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.hrznstudio.titanium.recipe.generator.TitaniumShapedRecipeBuilder;
import dev.drimoz.immaterialdrawers.block.ImmaterialDrawerBlock;
import dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile;
import dev.drimoz.immaterialdrawers.registry.IDContent;
import dev.drimoz.immaterialdrawers.registry.IDFeatures;
import dev.drimoz.immaterialdrawers.util.EnergyFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.List;

/**
 * The Energy Drawer block.
 *
 * <p>Modelled on Functional Storage's {@code block/FluidDrawerBlock}.
 * Copyright (c) 2021 Buuz135, Rid - MIT. See NOTICE. Geometry, tooltip frame and creative tab are
 * shared with every drawer of ours, in {@link ImmaterialDrawerBlock}.
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
                (BlockEntityType<EnergyDrawerTile>) IDContent.ENERGY_DRAWER.type().get(), pos, state);
    }

    /**
     * The long accessors, not the capability ones: above 2.1B the clamped view reads full at every
     * fill level, so a comparator on a big drawer would sit at 15 from the first FE. The Redstone
     * Upgrade reads the same number; its slot setting means nothing where there is one kind of content.
     */
    @Override
    protected int signalFor(EnergyDrawerTile tile) {
        var storage = tile.getEnergyStorage();
        long capacity = storage.getCapacityLong();
        long stored = storage.getStoredLong();
        return comparatorSignal(capacity <= 0 ? 0 : stored / (double) capacity, capacity > 0 && stored > 0);
    }

    /**
     * Planks around a redstone block, the same shape Functional Storage gives its fluid drawer
     * (planks around an empty bucket).
     *
     * <p>The parallel is the point: a player who has made a fluid drawer can guess this one. The
     * centre ingredient is what the drawer is for, and redstone is what vanilla means by power.
     * The copper the block is built from is how it looks, not how it is made.
     */
    @Override
    public void registerRecipe(RecipeOutput consumer) {
        TitaniumShapedRecipeBuilder.shapedRecipe(this)
                .pattern("PPP").pattern("PRP").pattern("PPP")
                .define('P', ItemTags.PLANKS)
                .define('R', Blocks.REDSTONE_BLOCK)
                .save(consumer.withConditions(IDFeatures.enabled(IDFeatures.Feature.ENERGY_DRAWER)));
    }

    /**
     * The charge, as FE stored over FE capacity, read from the field name Titanium's {@code @Save}
     * gave the storage.
     */
    @Override
    protected void appendContents(CompoundTag tile, Item.TooltipContext context, List<Component> tooltip) {
        CompoundTag energy = tile.getCompound("energyStorage");
        tooltip.add(Component.literal(" - ")
                .append(Component.literal(EnergyFormat.format(energy.getLong("Energy")))
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" / ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(EnergyFormat.format(energy.getLong("Capacity")) + " FE")
                        .withStyle(ChatFormatting.GOLD)));
    }

    /**
     * Plain {@link BlockItem} for now.
     *
     * <p>Functional Storage's fluid drawer item carries the drawer's contents in the stack and
     * renders them in the hand. Both need work this spike does not: a stack-level energy handler,
     * and a renderer for a resource with no texture of its own (CLAUDE.md §11, task 6).
     */
    public static class EnergyDrawerItem extends BlockItem {
        public EnergyDrawerItem(EnergyDrawerBlock block, Properties props) {
            super(block, props);
        }
    }
}
