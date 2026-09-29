package dev.drimoz.immaterialdrawers.datagen;

import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Block tags: which tool breaks our blocks.
 *
 * <p>Not optional. The drawers copy a vanilla block's properties, which include
 * {@code requiresCorrectToolForDrops}, and a drawer only drops through {@code getDrops} - which
 * vanilla calls only for the correct tool. Which tool is correct is decided by the
 * {@code mineable/*} tags, so a drawer in none of them drops nothing to anyone: not the block, not
 * its contents, not its upgrades. That shipped in 0.1.0. Functional Storage puts its own drawers in
 * {@code mineable/pickaxe}; so do we, for every block we register, so a drawer added later cannot
 * miss it. {@code aPickaxeIsTheRightToolForEveryDrawer} checks the result.
 */
public class IDBlockTagsProvider extends BlockTagsProvider {

    private final List<Block> blocks;

    public IDBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                               ExistingFileHelper existingFileHelper, List<Block> blocks) {
        super(output, lookupProvider, ImmaterialDrawers.MOD_ID, existingFileHelper);
        this.blocks = blocks;
    }

    /**
     * Every entry optional, and that matters more than it looks. This file is {@code mineable/pickaxe}
     * itself, merged with vanilla's and everyone else's - and a tag with one required entry that does
     * not exist fails to load <em>as a whole</em>. The chemical drawers exist only with Mekanism, so a
     * required entry for them would break the pickaxe on every block in the game, in every pack
     * without it. {@code "required": false} costs nothing when the block is there.
     */
    @Override
    protected void addTags(HolderLookup.Provider provider) {
        var pickaxe = tag(BlockTags.MINEABLE_WITH_PICKAXE);
        blocks.forEach(block -> pickaxe.addOptional(BuiltInRegistries.BLOCK.getKey(block)));
    }
}
