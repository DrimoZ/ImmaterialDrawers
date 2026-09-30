package dev.drimoz.immaterialdrawers.datagen;

import com.hrznstudio.titanium.block.BasicBlock;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.registry.IDContent;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Consumer;

/** Each block's own {@code registerRecipe}, plus the recipe that frames our framed drawers. */
public class IDRecipeProvider extends RecipeProvider {

    private final List<Block> blocks;

    public IDRecipeProvider(PackOutput output, List<Block> blocks) {
        super(output);
        this.blocks = blocks;
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> output) {
        blocks.stream()
                .filter(block -> block instanceof BasicBlock)
                .forEach(block -> ((BasicBlock) block).registerRecipe(output));

        // A "special" recipe: no ingredients in the JSON, the logic is FramedEnergyDrawerRecipe.
        SpecialRecipeBuilder.special((SimpleCraftingRecipeSerializer<?>) IDContent.FRAMED_RECIPE.get())
                .save(output, ImmaterialDrawers.MOD_ID + ":framed");
    }
}
