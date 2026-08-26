package dev.drimoz.immaterialdrawers.datagen;

import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import net.minecraft.core.registries.BuiltInRegistries;
import dev.drimoz.immaterialdrawers.registry.IDContent;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.List;

/**
 * Item models: each block's item just wears the block's own model.
 *
 * <p>Functional Storage points its drawer items at {@code minecraft:builtin/entity} instead, which
 * hands rendering to a {@code BlockEntityWithoutLevelRenderer} so the item in your hand shows what
 * the drawer contains. We have no such renderer yet, and an item that renders as its block is a
 * better placeholder than one that renders as nothing — see CLAUDE.md §11, task 6.
 */
public class IDItemModelProvider extends ItemModelProvider {

    private final List<Block> blocks;

    public IDItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper, List<Block> blocks) {
        super(output, ImmaterialDrawers.MOD_ID, existingFileHelper);
        this.blocks = blocks;
    }

    @Override
    protected void registerModels() {
        for (Block block : blocks) {
            String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
            // Unchecked: the block models are hand-authored under src/main/resources and the
            // existing-file helper does not see them from here.
            getBuilder(path).parent(new ModelFile.UncheckedModelFile(IDBlockStateProvider.modelFor(block)));
        }

        // Items that are not blocks get the flat treatment every upgrade in Functional Storage has.
        String charger = BuiltInRegistries.ITEM.getKey(IDContent.WIRELESS_CHARGER.get()).getPath();
        getBuilder(charger)
                .parent(new ModelFile.UncheckedModelFile(ResourceLocation.parse("item/generated")))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(
                        ImmaterialDrawers.MOD_ID, "item/" + charger));
    }
}
