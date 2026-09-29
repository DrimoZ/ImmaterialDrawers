package dev.drimoz.immaterialdrawers.datagen;

import com.hrznstudio.titanium.datagenerator.loot.TitaniumLootTableProvider;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;

/**
 * Everything {@code ./gradlew runData} produces.
 *
 * <p>Generated output is committed (see {@code build.gradle}), so a fresh clone builds a working
 * jar without running datagen first.
 *
 * <p><b>What is generated:</b> blockstates, item models, loot tables, recipes and en_us.
 * <b>What is not:</b> the block models and the textures. Those are hand-authored under
 * {@code src/main/resources}, which is also how Functional Storage does it — a drawer model is
 * shaped geometry, and expressing it through a model builder would be writing a worse Blockbench.
 */
public final class IDDataGenerators {

    private IDDataGenerators() {
    }

    public static void gather(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();

        // Everything this mod registered, read back out of the block registry. Listing the blocks
        // by hand here would be a second list to keep in step with IDContent.
        List<Block> blocks = BuiltInRegistries.BLOCK.stream()
                .filter(block -> ImmaterialDrawers.MOD_ID.equals(
                        BuiltInRegistries.BLOCK.getKey(block).getNamespace()))
                .toList();

        generator.addProvider(event.includeClient(),
                new IDBlockStateProvider(generator.getPackOutput(), event.getExistingFileHelper(), blocks));
        generator.addProvider(event.includeClient(),
                new IDItemModelProvider(generator.getPackOutput(), event.getExistingFileHelper(), blocks));
        generator.addProvider(event.includeClient(),
                new IDLangProvider(generator.getPackOutput()));

        // Titanium's provider, driven by BasicBlock#getLootTable. Drawer overrides that to drop
        // nothing, because a drawer does not drop through the loot table at all - Drawer#getDrops
        // builds the stack itself so the contents and upgrades travel with it.
        generator.addProvider(event.includeServer(),
                new TitaniumLootTableProvider(generator, () -> blocks, event.getLookupProvider()));
        generator.addProvider(event.includeServer(),
                new IDRecipeProvider(generator.getPackOutput(), event.getLookupProvider(), blocks));
        // Which tool breaks them - without it, nothing does, and a broken drawer drops nothing.
        generator.addProvider(event.includeServer(),
                new IDBlockTagsProvider(generator.getPackOutput(), event.getLookupProvider(),
                        event.getExistingFileHelper(), blocks));
    }
}
