package dev.drimoz.immaterialdrawers.block.energy;

import com.buuz135.functionalstorage.block.FramedBlock;
import dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile;
import dev.drimoz.immaterialdrawers.block.tile.energy.FramedEnergyDrawerTile;
import dev.drimoz.immaterialdrawers.registry.IDContent;
import com.hrznstudio.titanium.recipe.generator.TitaniumShapedRecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The framed energy drawer.
 *
 * <p>Modelled on Functional Storage's {@code block/FramedFluidDrawerBlock}, which is 57 lines of
 * which 40 are recipes. Copyright (c) 2021 Buuz135, Rid — MIT. See NOTICE.
 *
 * <p>{@code FramedBlock} is an empty marker interface, and that is the whole trick: every place
 * Functional Storage does something framed-specific — the crafting recipe that applies a style, the
 * tooltip, pick-block, copying the style onto the dropped item — tests for that interface rather
 * than for its own classes. This is the one extension point in the mod that generalises for free,
 * and it is worth noticing that it does so by accident of being written with {@code instanceof} on
 * an interface instead of on a class.
 *
 * <p>No recipe here yet: recipes come from datagen, which is task 6.
 */
public class FramedEnergyDrawerBlock extends EnergyDrawerBlock implements FramedBlock {

    public FramedEnergyDrawerBlock(Properties properties) {
        super(properties);
    }

    /**
     * Iron nuggets around a redstone block, mirroring Functional Storage's framed fluid drawer
     * (iron nuggets around a bucket). Nuggets rather than planks throughout their framed range,
     * because the frame is meant to be the cheap shell you then dress with something else.
     */
    @Override
    public void registerRecipe(RecipeOutput consumer) {
        TitaniumShapedRecipeBuilder.shapedRecipe(this)
                .pattern("NNN").pattern("NRN").pattern("NNN")
                .define('N', Items.IRON_NUGGET)
                .define('R', Blocks.REDSTONE_BLOCK)
                .save(consumer);
    }

    /**
     * Its own block entity type, not the unframed one's.
     *
     * <p>Which means its own capability registration too — a {@code BlockEntityType} is what
     * providers are registered against, so a framed drawer whose type nobody registered has no
     * energy capability at all and silently refuses every cable. See
     * {@code ImmaterialDrawers#registerCapabilities}, and the test that would catch it.
     */
    @SuppressWarnings("unchecked")
    @Override
    public BlockEntityType.BlockEntitySupplier<EnergyDrawerTile> getTileEntityFactory() {
        return (pos, state) -> new FramedEnergyDrawerTile(this,
                (BlockEntityType<EnergyDrawerTile>) IDContent.FRAMED_ENERGY_DRAWER.type().get(), pos, state);
    }
}
