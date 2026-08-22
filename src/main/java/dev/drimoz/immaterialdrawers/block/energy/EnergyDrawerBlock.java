package dev.drimoz.immaterialdrawers.block.energy;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.Drawer;
import com.buuz135.functionalstorage.block.DrawerBlock;
import com.buuz135.functionalstorage.block.FramedBlock;
import com.buuz135.functionalstorage.item.FSAttachments;
import com.buuz135.functionalstorage.util.NumberUtils;
import com.hrznstudio.titanium.util.TileUtil;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile;
import dev.drimoz.immaterialdrawers.registry.IDContent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.hrznstudio.titanium.recipe.generator.TitaniumShapedRecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.tags.ItemTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The Energy Drawer block.
 *
 * <p>Modelled on Functional Storage's {@code block/FluidDrawerBlock}.
 * Copyright (c) 2021 Buuz135, Rid - MIT. See NOTICE.
 *
 * <p>The geometry is Functional Storage's own, taken from the public
 * {@link DrawerBlock#CACHED_SHAPES} and {@link DrawerBlock#getDefaultHitShapes}, so an energy
 * drawer clicks exactly like the drawers around it in the wall rather than approximately like them.
 */
public class EnergyDrawerBlock extends Drawer<EnergyDrawerTile> {

    /** One kind of content, one slot layout. See CLAUDE.md §5. */
    public static final FunctionalStorage.DrawerType TYPE = FunctionalStorage.DrawerType.X_1;

    public EnergyDrawerBlock(Properties properties) {
        super(IDContent.ENERGY_DRAWER_NAME, properties, EnergyDrawerTile.class);
        setItemGroup(ImmaterialDrawers.TAB);
        registerDefaultState(defaultBlockState()
                .setValue(Drawer.FACING_HORIZONTAL_CUSTOM, Direction.NORTH)
                .setValue(DrawerBlock.LOCKED, false));
    }

    @SuppressWarnings("unchecked")
    @Override
    public BlockEntityType.BlockEntitySupplier<EnergyDrawerTile> getTileEntityFactory() {
        return (pos, state) -> new EnergyDrawerTile(this,
                (BlockEntityType<EnergyDrawerTile>) IDContent.ENERGY_DRAWER.type().get(), pos, state);
    }

    @Override
    public List<VoxelShape> getBoundingBoxes(BlockState state, BlockGetter source, BlockPos pos) {
        List<VoxelShape> boxes = new ArrayList<>();
        DrawerBlock.CACHED_SHAPES.get(TYPE).get(state.getValue(Drawer.FACING_HORIZONTAL_CUSTOM)).forEach(boxes::add);
        boxes.add(Shapes.block());
        return boxes;
    }

    @Override
    public Collection<VoxelShape> getHitShapes(BlockState state) {
        return DrawerBlock.getDefaultHitShapes(TYPE, state);
    }

    /**
     * Redefined because Functional Storage's dispatch cannot know about us.
     *
     * <p>{@code Drawer.getAnalogOutputSignal} branches on {@code FluidDrawerTile} then on
     * {@code ItemControllableDrawerTile}. We are the second, so without this override every energy
     * drawer would report the fill level of its deliberately empty item handler: zero, always,
     * whatever it is holding. See CLAUDE.md §11.
     */
    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        EnergyDrawerTile tile = TileUtil.getTileEntity(level, pos, EnergyDrawerTile.class).orElse(null);
        if (tile == null) {
            return 0;
        }
        var storage = tile.getEnergyStorage();
        if (storage.getMaxEnergyStored() <= 0 || storage.getEnergyStored() <= 0) {
            return 0;
        }
        // Same shape as vanilla's container signal: anything at all lights the comparator to 1,
        // full reads 15.
        return 1 + (int) ((storage.getEnergyStored() / (double) storage.getMaxEnergyStored()) * 14);
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
                .save(consumer);
    }

    /**
     * Says how much FE the drawer in your hand is holding.
     *
     * <p>{@code Drawer.appendHoverText} writes a "Contents:" heading followed by two literal empty
     * lines — placeholders for the two stored item stacks a normal drawer fills in. An energy
     * drawer has none, so the inherited tooltip was a heading and two blank lines, which is what a
     * bug looks like.
     *
     * <p>Replaced outright rather than appended to, the same way {@code FluidDrawerBlock} does it:
     * calling {@code super} would print the empty section as well as ours. The upgrade lines below
     * are theirs, kept identical so both drawers read the same.
     *
     * <p>The numbers come out of the tile NBT that {@code Drawer.copyTo} puts on the dropped stack,
     * under the field name Titanium's {@code @Save} gave it.
     */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        if (stack.has(FSAttachments.TILE)) {
            CompoundTag tile = stack.get(FSAttachments.TILE);
            CompoundTag energy = tile.getCompound("energyStorage");

            tooltip.add(Component.translatable("drawer.block.contents").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal(" - ")
                    .append(Component.literal(NumberUtils.getFormatedBigNumber(energy.getInt("Energy")))
                            .withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" / ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(NumberUtils.getFormatedBigNumber(energy.getInt("Capacity")) + " FE")
                            .withStyle(ChatFormatting.GOLD)));

            tooltip.add(Component.translatable("drawer.block.upgrades").withStyle(ChatFormatting.GRAY));
            boolean anyUpgrade = false;
            if (tile.getBoolean("isCreative")) {
                tooltip.add(Component.literal("- ").withStyle(ChatFormatting.GRAY)
                        .append(Component.translatable("drawer.block.upgrades.is_creative")
                                .withStyle(ChatFormatting.LIGHT_PURPLE)));
                anyUpgrade = true;
            }
            if (tile.getBoolean("isVoid")) {
                tooltip.add(Component.literal("- ").withStyle(ChatFormatting.GRAY)
                        .append(Component.translatable("drawer.block.upgrades.is_void")
                                .withStyle(ChatFormatting.BLUE)));
                anyUpgrade = true;
            }
            if (!anyUpgrade) {
                tooltip.add(Component.literal("- ").withStyle(ChatFormatting.GRAY)
                        .append(Component.translatable("drawer.block.upgrades.none")
                                .withStyle(ChatFormatting.GRAY)));
            }
        }

        if (this instanceof FramedBlock) {
            tooltip.add(Component.translatable("frameddrawer.use").withStyle(ChatFormatting.GRAY));
        }
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
