package dev.drimoz.immaterialdrawers.datagen;

import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Every drawer of ours in {@code mineable/pickaxe}, or no tool is "correct" and a broken drawer drops
 * nothing (the 0.1.0 bug). Optional entries: one unknown required entry fails the whole tag, and with
 * it the pickaxe on every block in the game.
 */
public class IDBlockTagsProvider extends BlockTagsProvider {

    private final List<Block> blocks;

    public IDBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                               ExistingFileHelper existingFileHelper, List<Block> blocks) {
        super(output, lookupProvider, ImmaterialDrawers.MOD_ID, existingFileHelper);
        this.blocks = blocks;
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        var pickaxe = tag(BlockTags.MINEABLE_WITH_PICKAXE);
        blocks.forEach(block -> pickaxe.addOptional(ForgeRegistries.BLOCKS.getKey(block)));
    }
}
