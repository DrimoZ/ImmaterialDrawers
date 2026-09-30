package dev.drimoz.immaterialdrawers.datagen;

import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.registry.IDContent;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/** Each drawer item is its block model. */
public class IDItemModelProvider extends ItemModelProvider {

    private final List<Block> blocks;

    public IDItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper, List<Block> blocks) {
        super(output, ImmaterialDrawers.MOD_ID, existingFileHelper);
        this.blocks = blocks;
    }

    @Override
    protected void registerModels() {
        for (Block block : blocks) {
            // Unchecked: the block models are hand-authored and the existing-file helper does not see them.
            getBuilder(ForgeRegistries.BLOCKS.getKey(block).getPath())
                    .parent(new ModelFile.UncheckedModelFile(IDBlockStateProvider.modelFor(block)));
        }
        // Flat, like every upgrade in Functional Storage.
        basicItem(IDContent.WIRELESS_CHARGER.get());
    }
}
