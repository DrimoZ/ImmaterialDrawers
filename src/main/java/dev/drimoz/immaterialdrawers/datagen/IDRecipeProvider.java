package dev.drimoz.immaterialdrawers.datagen;

import com.hrznstudio.titanium.block.BasicBlock;
import com.hrznstudio.titanium.recipe.generator.TitaniumShapedRecipeBuilder;
import dev.drimoz.immaterialdrawers.registry.IDContent;
import dev.drimoz.immaterialdrawers.registry.IDFeatures;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Recipes, collected from the blocks themselves.
 *
 * <p>Same shape as Functional Storage's {@code FunctionalStorageRecipesProvider}: Titanium's
 * {@code BasicBlock} has a {@code registerRecipe} hook, so a block's recipe lives next to the block
 * rather than in a list somewhere else that has to be kept in step with the registry.
 */
public class IDRecipeProvider extends RecipeProvider {

    private final List<Block> blocks;

    public IDRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, List<Block> blocks) {
        super(output, registries);
        this.blocks = blocks;
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        blocks.stream()
                .filter(block -> block instanceof BasicBlock)
                .forEach(block -> ((BasicBlock) block).registerRecipe(output));

        // The augment is an item, not a block, so it has no registerRecipe hook to be collected
        // from. An ender pearl for the "wireless" half and copper for the "charger" half.
        TitaniumShapedRecipeBuilder.shapedRecipe(IDContent.WIRELESS_CHARGER.get())
                .pattern("CRC").pattern("RER").pattern("CRC")
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('E', Items.ENDER_PEARL)
                .save(output.withConditions(IDFeatures.enabled(IDFeatures.Feature.WIRELESS_CHARGER)));
    }
}
