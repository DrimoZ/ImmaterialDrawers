package dev.drimoz.immaterialdrawers.block;

import com.buuz135.functionalstorage.block.FramedDrawerBlock;
import com.buuz135.functionalstorage.client.model.FramedDrawerModelData;
import dev.drimoz.immaterialdrawers.block.tile.IDFramedTile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Marks a framed drawer block of ours, and holds what each of them does with its design.
 *
 * <p>The 1.20.1 stand-in for Functional Storage 1.21's {@code FramedBlock} marker: the framing recipe
 * accepts any block implementing it. Our framed blocks extend different drawers (energy, Source), so
 * the place / drop / pick-block handling lives here as static helpers they call. The NBT is
 * Functional Storage's own: {@link FramedDrawerBlock#getDrawerModelData} and the {@code Style} key.
 */
public interface IDFramedBlock {

    String STYLE_TAG = "Style";

    /** From the placed stack onto the tile. */
    static void applyStyle(Level level, BlockPos pos, ItemStack stack) {
        FramedDrawerModelData design = FramedDrawerBlock.getDrawerModelData(stack);
        if (design != null && level.getBlockEntity(pos) instanceof IDFramedTile tile) {
            tile.setFramedDrawerModelData(design);
        }
    }

    /** From the tile onto a dropped or picked stack. */
    static void writeStyle(ItemStack stack, @Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof IDFramedTile tile) {
            FramedDrawerModelData design = tile.getFramedDrawerModelData();
            if (design != null && !design.getDesign().isEmpty()) {
                stack.getOrCreateTag().put(STYLE_TAG, design.serializeNBT());
            }
        }
    }

    /** Functional Storage's own line, "use in a crafting table to frame it". */
    static Component frameTooltip() {
        return Component.translatable("frameddrawer.use").withStyle(ChatFormatting.GRAY);
    }
}
