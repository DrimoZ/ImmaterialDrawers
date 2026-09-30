package dev.drimoz.immaterialdrawers.datagen;

import com.buuz135.functionalstorage.block.DrawerBlock;
import com.hrznstudio.titanium.block.RotatableBlock;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * Blockstates for our drawers: one multipart per block, the hand-authored model turned to face the
 * player, and the lock as a part of its own when {@code locked=true}.
 *
 * <p>Simpler than on 1.21.1. Functional Storage 1.20.1's drawers only face the four horizontal
 * directions ({@code subfacing}), where 1.21.1 adds floor and ceiling ({@code facing} x
 * {@code subfacing}).
 */
public class IDBlockStateProvider extends BlockStateProvider {

    private final List<Block> blocks;

    public IDBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper, List<Block> blocks) {
        super(output, ImmaterialDrawers.MOD_ID, existingFileHelper);
        this.blocks = blocks;
    }

    @Override
    protected void registerStatesAndModels() {
        blocks.stream().filter(block -> block instanceof RotatableBlock<?>).forEach(this::registerDrawer);
    }

    private void registerDrawer(Block block) {
        ModelFile model = new ModelFile.UncheckedModelFile(modelFor(block));
        ModelFile lock = new ModelFile.UncheckedModelFile(new ResourceLocation(ImmaterialDrawers.MOD_ID, "block/lock"));
        var builder = getMultipartBuilder(block);
        for (Direction facing : RotatableBlock.FACING_HORIZONTAL.getPossibleValues()) {
            int yRotation = (int) facing.getOpposite().toYRot();
            builder.part().modelFile(model).rotationY(yRotation).addModel()
                    .condition(RotatableBlock.FACING_HORIZONTAL, facing).end();
            builder.part().modelFile(lock).rotationY(yRotation).addModel()
                    .condition(RotatableBlock.FACING_HORIZONTAL, facing)
                    .condition(DrawerBlock.LOCKED, true).end();
        }
    }

    static ResourceLocation modelFor(Block block) {
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
        return new ResourceLocation(key.getNamespace(), "block/" + key.getPath());
    }
}
