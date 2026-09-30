package dev.drimoz.immaterialdrawers.block.energy;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.hrznstudio.titanium.recipe.generator.TitaniumShapedRecipeBuilder;
import com.hrznstudio.titanium.util.TileUtil;
import dev.drimoz.immaterialdrawers.block.ImmaterialDrawerBlock;
import dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile;
import dev.drimoz.immaterialdrawers.registry.IDContent;
import dev.drimoz.immaterialdrawers.registry.IDFeatures;
import dev.drimoz.immaterialdrawers.util.EnergyFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

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

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return signalFor(TileUtil.getTileEntity(level, pos, EnergyDrawerTile.class).orElse(null));
    }

    /**
     * Functional Storage's own Redstone Upgrade, reading a charge instead of a stack count.
     *
     * <p>On 1.20.1 their upgrade is an item compared by identity, not a behaviour: their tile's
     * {@code serverTick} already updates the neighbours for it, and their {@code DrawerBlock.getSignal}
     * reads the item handler, which on ours has no slots. So the upgrade stays theirs and only the
     * number is ours - the comparator's, so two ways of asking a drawer how full it is agree.
     */
    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        EnergyDrawerTile tile = TileUtil.getTileEntity(level, pos, EnergyDrawerTile.class).orElse(null);
        if (tile != null) {
            for (int slot = 0; slot < tile.getUtilityUpgrades().getSlots(); slot++) {
                if (tile.getUtilityUpgrades().getStackInSlot(slot).is(FunctionalStorage.REDSTONE_UPGRADE.get())) {
                    return signalFor(tile);
                }
            }
        }
        return 0;
    }

    /** The long accessors: above 2.1B the clamped view reads full at every fill level. */
    private static int signalFor(EnergyDrawerTile tile) {
        if (tile == null) {
            return 0;
        }
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
