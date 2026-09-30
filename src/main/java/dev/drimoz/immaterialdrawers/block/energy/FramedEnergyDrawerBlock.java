package dev.drimoz.immaterialdrawers.block.energy;

import com.hrznstudio.titanium.recipe.generator.TitaniumShapedRecipeBuilder;
import dev.drimoz.immaterialdrawers.block.IDFramedBlock;
import dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile;
import dev.drimoz.immaterialdrawers.block.tile.energy.FramedEnergyDrawerTile;
import dev.drimoz.immaterialdrawers.registry.IDContent;
import dev.drimoz.immaterialdrawers.registry.IDFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * The framed energy drawer.
 *
 * <p>Modelled on Functional Storage 1.20.1's {@code block/FramedDrawerBlock}.
 * Copyright (c) 2021 Buuz135, Rid - MIT. See NOTICE. On 1.20.1 their framing is written against
 * their own classes, so the design handling is ours - in {@link IDFramedBlock}, shared with every
 * framed drawer of ours.
 */
public class FramedEnergyDrawerBlock extends EnergyDrawerBlock implements IDFramedBlock {

    public FramedEnergyDrawerBlock(Properties properties) {
        super(properties);
    }

    /** Its own block entity type, so its own capability answer and its own renderer registration. */
    @SuppressWarnings("unchecked")
    @Override
    public BlockEntityType.BlockEntitySupplier<EnergyDrawerTile> getTileEntityFactory() {
        return (pos, state) -> new FramedEnergyDrawerTile(this,
                (BlockEntityType<EnergyDrawerTile>) IDContent.FRAMED_ENERGY_DRAWER.getRight().get(), pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        IDFramedBlock.applyStyle(level, pos, stack);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        IDFramedBlock.writeStyle(drops.get(0), builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY));
        return drops;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        ItemStack stack = super.getCloneItemStack(state, target, level, pos, player);
        IDFramedBlock.writeStyle(stack, level.getBlockEntity(pos));
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(IDFramedBlock.frameTooltip());
        super.appendHoverText(stack, level, tooltip, flag);
    }

    /**
     * Iron nuggets around a redstone block, mirroring Functional Storage's framed drawers (iron
     * nuggets around a chest): the frame is the cheap shell you then dress with something else.
     */
    @Override
    public void registerRecipe(Consumer<FinishedRecipe> consumer) {
        TitaniumShapedRecipeBuilder builder = TitaniumShapedRecipeBuilder.shapedRecipe(this);
        builder.getConditional().addCondition(IDFeatures.enabled(IDFeatures.Feature.ENERGY_DRAWER));
        builder.pattern("NNN").pattern("NRN").pattern("NNN")
                .define('N', Items.IRON_NUGGET)
                .define('R', Blocks.REDSTONE_BLOCK);
        builder.save(consumer);
    }
}
