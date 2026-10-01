package dev.drimoz.immaterialdrawers.block;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.Drawer;
import com.buuz135.functionalstorage.block.DrawerBlock;
import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import com.buuz135.functionalstorage.item.LinkingToolItem;
import com.hrznstudio.titanium.block.RotatableBlock;
import com.hrznstudio.titanium.datagenerator.loot.block.BasicBlockLootTables;
import com.hrznstudio.titanium.util.RayTraceUtils;
import com.hrznstudio.titanium.util.TileUtil;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.tile.ImmaterialDrawerTile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * What every drawer block in this mod shares, whatever it holds.
 *
 * <p>Modelled on Functional Storage 1.20.1's {@code block/FluidDrawerBlock}.
 * Copyright (c) 2021 Buuz135, Rid - MIT. See NOTICE.
 *
 * <p><b>More of it than on 1.21.1.</b> There, Functional Storage has a {@code Drawer} base block
 * that carries the interaction, the drops and the linking cleanup, and ours is a thin subclass. On
 * 1.20.1 {@code Drawer} is a one-method interface and every drawer block repeats that logic, so this
 * class does too - taken from their fluid drawer, so ours clicks, breaks and unlinks exactly like the
 * blocks around it.
 *
 * <p>The geometry is theirs: {@link DrawerBlock#CACHED_SHAPES} for the drawer's
 * {@link FunctionalStorage.DrawerType}.
 *
 * @param <T> the tile behind the block
 */
public abstract class ImmaterialDrawerBlock<T extends ImmaterialDrawerTile<T>> extends RotatableBlock<T> implements Drawer {

    /** Where the tile's NBT rides on a dropped drawer - Functional Storage's own key. */
    public static final String TILE_TAG = "Tile";

    protected final FunctionalStorage.DrawerType type;
    private final Class<T> tileClass;

    protected ImmaterialDrawerBlock(String name, Properties properties, Class<T> tileClass,
                                    FunctionalStorage.DrawerType type) {
        super(name, properties, tileClass);
        this.type = type;
        this.tileClass = tileClass;
        setItemGroup(ImmaterialDrawers.TAB);
        registerDefaultState(defaultBlockState()
                .setValue(RotatableBlock.FACING_HORIZONTAL, Direction.NORTH)
                .setValue(DrawerBlock.LOCKED, false));
    }

    public FunctionalStorage.DrawerType getType() {
        return type;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DrawerBlock.LOCKED);
    }

    @NotNull
    @Override
    public RotationType getRotationType() {
        return RotationType.FOUR_WAY;
    }

    @Override
    public List<VoxelShape> getBoundingBoxes(BlockState state, BlockGetter source, BlockPos pos) {
        List<VoxelShape> boxes = new ArrayList<>(
                DrawerBlock.CACHED_SHAPES.get(type).get(state.getValue(RotatableBlock.FACING_HORIZONTAL)));
        boxes.add(Shapes.block());
        return boxes;
    }

    @NotNull
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public boolean hasCustomBoxes(BlockState state, BlockGetter source, BlockPos pos) {
        return true;
    }

    @Override
    public boolean hasIndividualRenderVoxelShape() {
        return true;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult ray) {
        return TileUtil.getTileEntity(level, pos, tileClass)
                .map(tile -> tile.onSlotActivated(player, hand, ray.getDirection(),
                        ray.getLocation().x, ray.getLocation().y, ray.getLocation().z, getHit(state, level, pos, player)))
                .orElse(InteractionResult.PASS);
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        TileUtil.getTileEntity(level, pos, tileClass).ifPresent(tile -> tile.onClicked(player, getHit(state, level, pos, player)));
    }

    /** Which slot of the front the player is looking at, or -1 for the casing. Theirs, verbatim. */
    @Override
    public int getHit(BlockState state, Level level, BlockPos pos, Player player) {
        HitResult result = RayTraceUtils.rayTraceSimple(level, player, 32, 0);
        if (result instanceof BlockHitResult blockHit) {
            VoxelShape hit = RayTraceUtils.rayTraceVoxelShape(blockHit, level, player, 32, 0);
            if (hit != null) {
                if (hit.equals(Shapes.block())) return -1;
                List<VoxelShape> shapes = new ArrayList<>(
                        DrawerBlock.CACHED_SHAPES.get(type).get(state.getValue(RotatableBlock.FACING_HORIZONTAL)));
                for (int i = 0; i < shapes.size(); i++) {
                    if (Shapes.joinIsNotEmpty(shapes.get(i), hit, BooleanOp.AND)) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Nothing through the loot table: {@link #getDrops} builds the stack itself, so the contents
     * and the upgrades travel with the drawer.
     */
    @Override
    public LootTable.Builder getLootTable(@NotNull BasicBlockLootTables blockLootTables) {
        return blockLootTables.droppingNothing();
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        NonNullList<ItemStack> stacks = NonNullList.create();
        ItemStack stack = new ItemStack(this);
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (tileClass.isInstance(blockEntity)) {
            T tile = tileClass.cast(blockEntity);
            if (!tile.isEverythingEmpty()) {
                stack.getOrCreateTag().put(TILE_TAG, blockEntity.saveWithoutMetadata());
            }
            if (tile.isLocked()) {
                stack.getOrCreateTag().putBoolean("Locked", true);
            }
        }
        stacks.add(stack);
        return stacks;
    }

    @Override
    public NonNullList<ItemStack> getDynamicDrops(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        return NonNullList.create();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (stack.hasTag()) {
            if (stack.getTag().contains(TILE_TAG) && tileClass.isInstance(level.getBlockEntity(pos))) {
                T tile = tileClass.cast(level.getBlockEntity(pos));
                tile.load(stack.getTag().getCompound(TILE_TAG));
                tile.markForUpdate();
            }
            if (stack.getTag().contains("Locked")) {
                level.setBlock(pos, state.setValue(DrawerBlock.LOCKED, true), 3);
            }
        }
    }

    /** Takes the drawer out of its controller's network when it is broken, as theirs do. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            TileUtil.getTileEntity(level, pos, tileClass).ifPresent(tile -> {
                if (tile.getControllerPos() != null) {
                    TileUtil.getTileEntity(level, tile.getControllerPos(), StorageControllerTile.class)
                            .ifPresent(controller -> controller.addConnectedDrawers(LinkingToolItem.ActionMode.REMOVE, pos));
                }
            });
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /**
     * Writes the "Contents:" lines for a drawer in hand, from the tile NBT that {@link #getDrops}
     * put on the stack. Nothing to write when the drawer is empty.
     */
    protected abstract void appendContents(CompoundTag tile, List<Component> tooltip);

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (stack.hasTag() && stack.getTag().contains(TILE_TAG)) {
            tooltip.add(Component.translatable("drawer.block.contents").withStyle(ChatFormatting.GRAY));
            appendContents(stack.getTag().getCompound(TILE_TAG), tooltip);
        }
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return true;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** How full this drawer is, on vanilla's 0-15 scale - usually through {@link #comparatorSignal}. */
    protected abstract int signalFor(T tile);

    /**
     * What Functional Storage's Redstone Upgrade emits: the comparator's number, so two ways of
     * asking a drawer how full it is agree. A drawer with several slots overrides this to read the
     * slot in the upgrade's {@code Slot} tag.
     */
    protected int redstoneSignal(T tile, ItemStack upgrade) {
        return signalFor(tile);
    }

    /** Their {@code DrawerBlock} reads the item handler, which on ours has no slots: zero, always. */
    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return TileUtil.getTileEntity(level, pos, tileClass).map(this::signalFor).orElse(0);
    }

    /**
     * Functional Storage's own Redstone Upgrade, reading our contents instead of a stack count. On
     * 1.20.1 their upgrade is an item compared by identity, not a behaviour: their tile's
     * {@code serverTick} already updates the neighbours for it, so only the number is ours.
     */
    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        T tile = TileUtil.getTileEntity(level, pos, tileClass).orElse(null);
        if (tile != null) {
            for (int slot = 0; slot < tile.getUtilityUpgrades().getSlots(); slot++) {
                ItemStack upgrade = tile.getUtilityUpgrades().getStackInSlot(slot);
                if (upgrade.is(FunctionalStorage.REDSTONE_UPGRADE.get())) {
                    return redstoneSignal(tile, upgrade);
                }
            }
        }
        return 0;
    }

    /**
     * Vanilla's container shape, which is also Functional Storage's: anything at all lights the
     * comparator to 1, full reads 15.
     */
    protected static int comparatorSignal(double fullness, boolean hasContents) {
        return hasContents ? Math.min(15, (int) Math.floor(fullness * 14D) + 1) : 0;
    }
}
