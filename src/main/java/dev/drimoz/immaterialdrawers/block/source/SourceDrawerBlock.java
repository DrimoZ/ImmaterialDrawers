package dev.drimoz.immaterialdrawers.block.source;

import com.hrznstudio.titanium.recipe.generator.TitaniumShapedRecipeBuilder;
import dev.drimoz.immaterialdrawers.block.ImmaterialDrawerBlock;
import dev.drimoz.immaterialdrawers.block.tile.source.SourceDrawerTile;
import dev.drimoz.immaterialdrawers.compat.Mods;
import dev.drimoz.immaterialdrawers.registry.IDFeatures;
import dev.drimoz.immaterialdrawers.registry.IDSourceContent;
import dev.drimoz.immaterialdrawers.util.EnergyFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.crafting.conditions.ModLoadedCondition;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.function.Consumer;

/**
 * The Source Drawer block. <b>Ars Nouveau only</b> - see {@code compat.Mods}.
 *
 * <p>Modelled on Functional Storage 1.20.1's {@code block/FluidDrawerBlock}.
 * Copyright (c) 2021 Buuz135, Rid - MIT. See NOTICE.
 */
public class SourceDrawerBlock extends ImmaterialDrawerBlock<SourceDrawerTile> {

    private static final ResourceLocation SOURCE_JAR = new ResourceLocation(Mods.ARS_NOUVEAU, "source_jar");

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
                (BlockEntityType<SourceDrawerTile>) IDSourceContent.SOURCE_DRAWER.getRight().get(), pos, state);
    }

    @Override
    protected int signalFor(SourceDrawerTile tile) {
        int capacity = tile.getSourceStorage().getSourceCapacity();
        int stored = tile.getSourceStorage().getSource();
        return comparatorSignal(capacity <= 0 ? 0 : stored / (double) capacity, capacity > 0 && stored > 0);
    }

    /** Planks around a Source Jar, the way Functional Storage's fluid drawer wraps a bucket. */
    @Override
    public void registerRecipe(Consumer<FinishedRecipe> consumer) {
        TitaniumShapedRecipeBuilder builder = TitaniumShapedRecipeBuilder.shapedRecipe(this);
        builder.getConditional()
                .addCondition(new ModLoadedCondition(Mods.ARS_NOUVEAU))
                .addCondition(IDFeatures.enabled(IDFeatures.Feature.SOURCE_DRAWER));
        builder.pattern("PPP").pattern("PJP").pattern("PPP")
                .define('P', shell())
                .define('J', ForgeRegistries.ITEMS.getValue(SOURCE_JAR));
        builder.save(consumer);
    }

    protected Ingredient shell() {
        return Ingredient.of(ItemTags.PLANKS);
    }

    @Override
    protected void appendContents(CompoundTag tile, List<Component> tooltip) {
        CompoundTag source = tile.getCompound("sourceStorage");
        tooltip.add(Component.literal(" - ")
                .append(Component.literal(EnergyFormat.format(source.getInt("Source"))).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" / ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(EnergyFormat.format(source.getInt("Capacity")) + " Source")
                        .withStyle(ChatFormatting.LIGHT_PURPLE)));
    }
}
