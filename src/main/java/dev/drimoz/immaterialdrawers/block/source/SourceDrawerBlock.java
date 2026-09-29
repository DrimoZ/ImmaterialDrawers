package dev.drimoz.immaterialdrawers.block.source;

import com.buuz135.functionalstorage.item.FSAttachments;
import com.buuz135.functionalstorage.item.component.EmitRedstoneBehavior;
import com.hrznstudio.titanium.recipe.generator.TitaniumShapedRecipeBuilder;
import com.hrznstudio.titanium.util.TileUtil;
import dev.drimoz.immaterialdrawers.block.ImmaterialDrawerBlock;
import dev.drimoz.immaterialdrawers.block.tile.source.SourceDrawerTile;
import dev.drimoz.immaterialdrawers.compat.Mods;
import dev.drimoz.immaterialdrawers.registry.IDFeatures;
import dev.drimoz.immaterialdrawers.registry.IDSourceContent;
import dev.drimoz.immaterialdrawers.util.EnergyFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;

import java.util.List;

/**
 * The Source Drawer block.
 *
 * <p><b>Ars Nouveau only - see {@code compat.Mods}.</b> Modelled on Functional Storage's
 * {@code FluidDrawerBlock}, Copyright (c) 2021 Buuz135, Rid — MIT, see NOTICE; the shape and the
 * redstone handling follow {@code EnergyDrawerBlock}, which has the same one-content, one-slot shape.
 */
public class SourceDrawerBlock extends ImmaterialDrawerBlock<SourceDrawerTile> {

    /**
     * A Source Jar in the middle, where the fluid drawer has a bucket: the drawer is made from the
     * container of what it holds. By name, because the item lives in Ars's registry classes, not in
     * its API.
     */
    private static final ResourceLocation SOURCE_JAR = ResourceLocation.fromNamespaceAndPath(Mods.ARS_NOUVEAU, "source_jar");

    public SourceDrawerBlock(Properties properties) {
        this(IDSourceContent.SOURCE_DRAWER_NAME, properties);
    }

    protected SourceDrawerBlock(String name, Properties properties) {
        super(name, properties, SourceDrawerTile.class, SourceDrawerTile.TYPE);
    }

    @SuppressWarnings("unchecked")
    @Override
    public BlockEntityType.BlockEntitySupplier<SourceDrawerTile> getTileEntityFactory() {
        return (pos, state) -> new SourceDrawerTile(this,
                (BlockEntityType<SourceDrawerTile>) IDSourceContent.SOURCE_DRAWER.type().get(), pos, state);
    }

    /** Their dispatch sees an item drawer with no items; ours reads the Source - see the energy drawer. */
    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return signalFor(TileUtil.getTileEntity(level, pos, SourceDrawerTile.class).orElse(null));
    }

    /** Functional Storage's Redstone Upgrade reading the Source, exactly as on the energy drawer. */
    @Override
    public int getSignal(BlockState state, BlockGetter blockGetter, BlockPos pos, Direction dir) {
        SourceDrawerTile tile = TileUtil.getTileEntity(blockGetter, pos, SourceDrawerTile.class).orElse(null);
        if (tile != null) {
            for (int slot = 0; slot < tile.getUtilityUpgrades().getSlots(); slot++) {
                if (tile.getUtilityUpgrades().getStackInSlot(slot)
                        .get(FSAttachments.FUNCTIONAL_BEHAVIOR) instanceof EmitRedstoneBehavior) {
                    return signalFor(tile);
                }
            }
        }
        return super.getSignal(state, blockGetter, pos, dir);
    }

    private static int signalFor(SourceDrawerTile tile) {
        if (tile == null) {
            return 0;
        }
        int capacity = tile.getSourceStorage().getSourceCapacity();
        int stored = tile.getSourceStorage().getSource();
        return comparatorSignal(capacity <= 0 ? 0 : stored / (double) capacity, capacity > 0 && stored > 0);
    }

    /** Planks around a Source Jar, the fluid drawer's shape. Conditioned like the chemical drawers'. */
    @Override
    public void registerRecipe(RecipeOutput consumer) {
        TitaniumShapedRecipeBuilder.shapedRecipe(this)
                .pattern("PPP").pattern("PJP").pattern("PPP")
                .define('P', shell())
                .define('J', BuiltInRegistries.ITEM.get(SOURCE_JAR))
                .save(consumer.withConditions(new ModLoadedCondition(Mods.ARS_NOUVEAU),
                        IDFeatures.enabled(IDFeatures.Feature.SOURCE_DRAWER)));
    }

    protected Ingredient shell() {
        return Ingredient.of(ItemTags.PLANKS);
    }

    @Override
    protected void appendContents(CompoundTag tile, Item.TooltipContext context, List<Component> tooltip) {
        CompoundTag source = tile.getCompound("sourceStorage");
        tooltip.add(Component.literal(" - ")
                .append(Component.literal(EnergyFormat.format(source.getInt("Source"))).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" / ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(EnergyFormat.format(source.getInt("Capacity")) + " Source")
                        .withStyle(ChatFormatting.LIGHT_PURPLE)));
    }
}
