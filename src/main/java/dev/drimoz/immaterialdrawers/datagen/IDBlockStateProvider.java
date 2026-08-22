package dev.drimoz.immaterialdrawers.datagen;

import com.buuz135.functionalstorage.block.Drawer;
import com.buuz135.functionalstorage.block.DrawerBlock;
import com.hrznstudio.titanium.block.RotatableBlock;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.List;

/**
 * Blockstates for the drawers.
 *
 * <p>Adapted from Functional Storage's {@code data/FunctionalStorageBlockstateProvider}.
 * Copyright (c) 2021 Buuz135, Rid — MIT. See NOTICE. Theirs is not reusable directly: its
 * constructor hardcodes their mod id as the provider's output namespace, so it would write our
 * blockstates into {@code assets/functionalstorage/}.
 *
 * <p><b>Why multipart and not variants.</b> A drawer's orientation is two properties, not one —
 * {@code facing} (which of the six faces it is attached to) and {@code subfacing} (which way up it
 * sits on that face) — and {@code locked} is a third. Enumerating variants means 6 x 6 x 2 entries
 * that must all exist or the game logs a missing-variant error; multipart lets the lock overlay be
 * a separate part that simply is not applied when the drawer is unlocked.
 *
 * <p>The models themselves are hand-authored under {@code src/main/resources}, the way Functional
 * Storage does it. Only their placement is generated.
 */
public class IDBlockStateProvider extends BlockStateProvider {

    private final List<Block> blocks;

    public IDBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper, List<Block> blocks) {
        super(output, ImmaterialDrawers.MOD_ID, existingFileHelper);
        this.blocks = blocks;
    }

    @Override
    protected void registerStatesAndModels() {
        blocks.stream()
                .filter(block -> block instanceof RotatableBlock<?>)
                .forEach(block -> registerDrawer((RotatableBlock<?>) block));
    }

    /**
     * One part per (facing, subfacing) pair, plus the same again gated on {@code locked}.
     *
     * <p>Drawers are {@code TWENTY_FOUR_WAY}: attached to any of the six faces, and rotated to any
     * of four positions on it. Up and down are the awkward cases — the model has to be tipped 90 or
     * 270 degrees around x first, and then the y rotation runs the opposite way for one of them,
     * which is why they are spelled out separately instead of folded into the general case.
     */
    private void registerDrawer(RotatableBlock<?> block) {
        ModelFile.UncheckedModelFile model = new ModelFile.UncheckedModelFile(modelFor(block));
        ModelFile.UncheckedModelFile lock = new ModelFile.UncheckedModelFile(
                ResourceLocation.fromNamespaceAndPath(ImmaterialDrawers.MOD_ID, "block/lock"));
        var builder = getMultipartBuilder(block);

        for (Direction facing : Direction.values()) {
            if (facing == Direction.DOWN || facing == Direction.UP) {
                int xRotation = facing == Direction.DOWN ? 90 : 270;
                for (Direction subfacing : Drawer.FACING_HORIZONTAL_CUSTOM.getPossibleValues()) {
                    int yRotation = (int) (facing == Direction.DOWN
                            ? subfacing.getOpposite().toYRot()
                            : subfacing.toYRot());
                    builder.part().modelFile(model).rotationX(xRotation).rotationY(yRotation).addModel()
                            .condition(Drawer.FACING_HORIZONTAL_CUSTOM, facing)
                            .condition(RotatableBlock.FACING_ALL, subfacing).end();
                    builder.part().modelFile(lock).rotationX(xRotation).rotationY(yRotation).addModel()
                            .condition(Drawer.FACING_HORIZONTAL_CUSTOM, facing)
                            .condition(RotatableBlock.FACING_ALL, subfacing)
                            .condition(DrawerBlock.LOCKED, true).end();
                }
            } else {
                int yRotation = (int) facing.getOpposite().toYRot();
                builder.part().modelFile(model).rotationY(yRotation).addModel()
                        .condition(Drawer.FACING_HORIZONTAL_CUSTOM, facing)
                        .condition(RotatableBlock.FACING_ALL, Direction.DOWN).end();
                builder.part().modelFile(lock).rotationY(yRotation).addModel()
                        .condition(Drawer.FACING_HORIZONTAL_CUSTOM, facing)
                        .condition(RotatableBlock.FACING_ALL, Direction.DOWN)
                        .condition(DrawerBlock.LOCKED, true).end();
            }
        }
    }

    /** {@code immaterialdrawers:block/<path>}, matching the hand-authored model files. */
    static ResourceLocation modelFor(Block block) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        return ResourceLocation.fromNamespaceAndPath(key.getNamespace(), "block/" + key.getPath());
    }
}
