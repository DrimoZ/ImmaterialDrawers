package dev.drimoz.immaterialdrawers.datagen;

import com.hrznstudio.titanium.datagenerator.loot.TitaniumLootTableProvider;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import net.minecraft.data.DataGenerator;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.util.NonNullLazy;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * Datagen: blockstates, item models, lang, loot tables, recipes, tags. {@code ./gradlew runData},
 * output committed under {@code src/generated/resources}. Block models and textures are hand-authored.
 */
public final class IDDataGenerators {

    private IDDataGenerators() {
    }

    public static void gather(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();

        // Everything this mod registered, read back out of the registry rather than listed twice.
        List<Block> blocks = ForgeRegistries.BLOCKS.getValues().stream()
                .filter(block -> ImmaterialDrawers.MOD_ID.equals(ForgeRegistries.BLOCKS.getKey(block).getNamespace()))
                .toList();

        generator.addProvider(event.includeClient(),
                new IDBlockStateProvider(generator.getPackOutput(), event.getExistingFileHelper(), blocks));
        generator.addProvider(event.includeClient(),
                new IDItemModelProvider(generator.getPackOutput(), event.getExistingFileHelper(), blocks));
        generator.addProvider(event.includeClient(), new IDLangProvider(generator.getPackOutput()));

        // Titanium's provider reads BasicBlock#getLootTable, which our drawers answer with "drop
        // nothing": getDrops builds the stack itself so the contents travel with it.
        generator.addProvider(event.includeServer(), new TitaniumLootTableProvider(generator, NonNullLazy.of(() -> blocks)));
        generator.addProvider(event.includeServer(), new IDRecipeProvider(generator.getPackOutput(), blocks));
        generator.addProvider(event.includeServer(),
                new IDBlockTagsProvider(generator.getPackOutput(), event.getLookupProvider(),
                        event.getExistingFileHelper(), blocks));
    }
}
