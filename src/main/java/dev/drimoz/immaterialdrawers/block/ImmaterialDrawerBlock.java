package dev.drimoz.immaterialdrawers.block;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.Drawer;
import com.buuz135.functionalstorage.block.DrawerBlock;
import com.buuz135.functionalstorage.block.FramedBlock;
import com.buuz135.functionalstorage.item.FSAttachments;
import com.buuz135.functionalstorage.item.component.EmitRedstoneBehavior;
import com.hrznstudio.titanium.util.TileUtil;
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
import net.minecraft.world.level.Level;
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

    /** How full this drawer is, on vanilla's 0-15 scale - usually through {@link #comparatorSignal}. */
    protected abstract int signalFor(T tile);

    /**
     * What Functional Storage's Redstone Upgrade emits: the comparator's number, so two ways of
     * asking a drawer how full it is do not disagree. A drawer with several slots overrides this to
     * read the slot the upgrade was configured for ({@code FSAttachments.SLOT}).
     */
    protected int redstoneSignal(T tile, ItemStack upgrade) {
        return signalFor(tile);
    }

    /**
     * Redefined because Functional Storage's dispatch cannot know about us.
     *
     * <p>{@code Drawer.getAnalogOutputSignal} branches on {@code FluidDrawerTile} then on
     * {@code ItemControllableDrawerTile}. We are the second, so without this override every drawer of
     * ours would report the fill level of its deliberately empty item handler: zero, always, whatever
     * it is holding. See CLAUDE.md §11.
     */
    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return TileUtil.getTileEntity(level, pos, getTileClass()).map(this::signalFor).orElse(0);
    }

    /**
     * Functional Storage's own Redstone Upgrade, reading our contents instead of a stack count.
     *
     * <p>No augment of ours for this. {@code EmitRedstoneBehavior} already ticks the neighbours and
     * already reports {@code canConnectRedstone} for us - we are an {@code ItemControllableDrawerTile},
     * which is what it checks. The one half that cannot work is the signal itself: it reads
     * {@code getStorage()}, our handler has no slots, and its item branch yields -1. Their
     * {@code Drawer.getSignal} reads -1 as "this upgrade has nothing to say" and returns 0, so the
     * upgrade slots in, connects, and sits dead. So the upgrade stays theirs and only the number is
     * ours.
     */
    @Override
    public int getSignal(BlockState state, BlockGetter blockGetter, BlockPos pos, Direction dir) {
        T tile = TileUtil.getTileEntity(blockGetter, pos, getTileClass()).orElse(null);
        if (tile != null) {
            for (int slot = 0; slot < tile.getUtilityUpgrades().getSlots(); slot++) {
                ItemStack upgrade = tile.getUtilityUpgrades().getStackInSlot(slot);
                if (upgrade.get(FSAttachments.FUNCTIONAL_BEHAVIOR) instanceof EmitRedstoneBehavior) {
                    return redstoneSignal(tile, upgrade);
                }
            }
        }
        // Anything else in the utility slots is theirs to answer, including our own augments.
        return super.getSignal(state, blockGetter, pos, dir);
    }

    /**
     * Vanilla's container shape, which is also Functional Storage's: anything at all lights the
     * comparator to 1, full reads 15.
     */
    protected static int comparatorSignal(double fullness, boolean hasContents) {
        return hasContents ? Math.min(15, (int) Math.floor(fullness * 14D) + 1) : 0;
    }
}
