package dev.drimoz.immaterialdrawers.block.chemical;

import com.buuz135.functionalstorage.FunctionalStorage;
import dev.drimoz.immaterialdrawers.block.IDFramedBlock;
import dev.drimoz.immaterialdrawers.block.tile.chemical.ChemicalDrawerTile;
import dev.drimoz.immaterialdrawers.block.tile.chemical.FramedChemicalDrawerTile;
import dev.drimoz.immaterialdrawers.registry.IDChemicalContent;
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

/** A framed chemical drawer. <b>Mekanism only.</b> Design handling in {@link IDFramedBlock}. */
public class FramedChemicalDrawerBlock extends ChemicalDrawerBlock implements IDFramedBlock {

    public FramedChemicalDrawerBlock(FunctionalStorage.DrawerType type, Properties properties) {
        super(IDChemicalContent.nameFor(type, true), type, properties);
    }

    @Override
    protected Ingredient shell() {
        return Ingredient.of(Items.IRON_NUGGET);
    }

    @SuppressWarnings("unchecked")
    @Override
    public BlockEntityType.BlockEntitySupplier<ChemicalDrawerTile> getTileEntityFactory() {
        return (pos, state) -> new FramedChemicalDrawerTile(this,
                (BlockEntityType<ChemicalDrawerTile>) IDChemicalContent.drawer(type, true).getRight().get(), pos, state, type);
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
