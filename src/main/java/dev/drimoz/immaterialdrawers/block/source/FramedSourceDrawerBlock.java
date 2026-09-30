package dev.drimoz.immaterialdrawers.block.source;

import dev.drimoz.immaterialdrawers.block.IDFramedBlock;
import dev.drimoz.immaterialdrawers.block.tile.source.FramedSourceDrawerTile;
import dev.drimoz.immaterialdrawers.block.tile.source.SourceDrawerTile;
import dev.drimoz.immaterialdrawers.registry.IDSourceContent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** The framed Source Drawer. <b>Ars Nouveau only.</b> Design handling in {@link IDFramedBlock}. */
public class FramedSourceDrawerBlock extends SourceDrawerBlock implements IDFramedBlock {

    public FramedSourceDrawerBlock(Properties properties) {
        super(IDSourceContent.FRAMED_SOURCE_DRAWER_NAME, properties);
    }

    /** Iron nuggets for the frame, as for every framed drawer. */
    @Override
    protected Ingredient shell() {
        return Ingredient.of(Items.IRON_NUGGET);
    }

    @SuppressWarnings("unchecked")
    @Override
    public BlockEntityType.BlockEntitySupplier<SourceDrawerTile> getTileEntityFactory() {
        return (pos, state) -> new FramedSourceDrawerTile(this,
                (BlockEntityType<SourceDrawerTile>) IDSourceContent.FRAMED_SOURCE_DRAWER.getRight().get(), pos, state);
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
}
