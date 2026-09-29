package dev.drimoz.immaterialdrawers.block;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.Drawer;
import com.buuz135.functionalstorage.block.DrawerBlock;
import com.buuz135.functionalstorage.block.FramedBlock;
import com.buuz135.functionalstorage.item.FSAttachments;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.tile.ImmaterialDrawerTile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * What every drawer block in this mod shares, whatever it holds.
 *
 * <p>Modelled on Functional Storage's {@code block/FluidDrawerBlock}.
 * Copyright (c) 2021 Buuz135, Rid - MIT. See NOTICE.
 *
 * <p>The geometry is Functional Storage's own, taken from the public
 * {@link DrawerBlock#CACHED_SHAPES} and {@link DrawerBlock#getDefaultHitShapes} for the drawer's
 * {@link FunctionalStorage.DrawerType}, so a drawer of ours clicks exactly like the drawers around
 * it in the wall rather than approximately like them - one slot, two or four.
 *
 * @param <T> the tile behind the block
 */
public abstract class ImmaterialDrawerBlock<T extends ImmaterialDrawerTile<T>> extends Drawer<T> {

    protected final FunctionalStorage.DrawerType type;

    protected ImmaterialDrawerBlock(String name, Properties properties, Class<T> tileClass,
                                    FunctionalStorage.DrawerType type) {
        super(name, properties, tileClass);
        this.type = type;
        setItemGroup(ImmaterialDrawers.TAB);
        registerDefaultState(defaultBlockState()
                .setValue(Drawer.FACING_HORIZONTAL_CUSTOM, Direction.NORTH)
                .setValue(DrawerBlock.LOCKED, false));
    }

    public FunctionalStorage.DrawerType getType() {
        return type;
    }

    @Override
    public List<VoxelShape> getBoundingBoxes(BlockState state, BlockGetter source, BlockPos pos) {
        List<VoxelShape> boxes = new ArrayList<>();
        DrawerBlock.CACHED_SHAPES.get(type).get(state.getValue(Drawer.FACING_HORIZONTAL_CUSTOM)).forEach(boxes::add);
        boxes.add(Shapes.block());
        return boxes;
    }

    @Override
    public Collection<VoxelShape> getHitShapes(BlockState state) {
        return DrawerBlock.getDefaultHitShapes(type, state);
    }

    /**
     * Writes the "Contents:" lines for a drawer in hand, from the tile NBT that
     * {@code Drawer.copyTo} put on the stack. Nothing to write when the drawer is empty.
     */
    protected abstract void appendContents(CompoundTag tile, Item.TooltipContext context, List<Component> tooltip);

    /**
     * Says what the drawer in your hand is holding.
     *
     * <p>{@code Drawer.appendHoverText} writes a "Contents:" heading followed by two literal empty
     * lines — placeholders for the two stored item stacks a normal drawer fills in. Our drawers have
     * none, so the inherited tooltip was a heading and two blank lines, which is what a bug looks
     * like.
     *
     * <p>Replaced outright rather than appended to, the same way {@code FluidDrawerBlock} does it:
     * calling {@code super} would print the empty section as well as ours. The upgrade lines below
     * are theirs, kept identical so every drawer reads the same.
     */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        if (stack.has(FSAttachments.TILE)) {
            CompoundTag tile = stack.get(FSAttachments.TILE);

            tooltip.add(Component.translatable("drawer.block.contents").withStyle(ChatFormatting.GRAY));
            appendContents(tile, context, tooltip);

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
     * Vanilla's container shape, which is also Functional Storage's: anything at all lights the
     * comparator to 1, full reads 15.
     */
    protected static int comparatorSignal(double fullness, boolean hasContents) {
        return hasContents ? Math.min(15, (int) Math.floor(fullness * 14D) + 1) : 0;
    }
}
