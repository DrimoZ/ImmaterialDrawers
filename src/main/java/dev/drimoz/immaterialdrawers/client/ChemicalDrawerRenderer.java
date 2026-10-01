package dev.drimoz.immaterialdrawers.client;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.tile.ControllableDrawerTile;
import com.buuz135.functionalstorage.client.BaseDrawerRenderer;
import com.buuz135.functionalstorage.item.ConfigurationToolItem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.drimoz.immaterialdrawers.block.tile.chemical.ChemicalDrawerTile;
import dev.drimoz.immaterialdrawers.storage.chemical.BigChemicalHandler;
import dev.drimoz.immaterialdrawers.util.ChemicalFormat;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * Draws what a chemical drawer holds: each tank's chemical at its level behind the window, and the
 * amount on the front.
 *
 * <p><b>Mekanism only - see {@code compat.Mods}.</b>
 *
 * <p><b>The fluid drawer's layout, exactly.</b> Functional Storage's {@code FluidDrawerRenderer}
 * places the level, the number and the indicator for 1, 2 and 4 slots; every coordinate below is
 * theirs. Theirs does its own orientation maths into a frame whose front sits at z = 1 - 1/32; ours
 * extends {@code BaseDrawerRenderer}, like the energy drawer, whose front sits at z = 0. One
 * translation, {@link #FLUID_FRAME_Z}, maps one onto the other, and their numbers then apply
 * unchanged. Copyright (c) 2021 Buuz135, Rid — MIT. See NOTICE.
 *
 * <p><b>What is ours: the gas is alive.</b> A fluid drawer shows a still liquid. A chemical drawer
 * mostly holds gases, and a gas behind glass in a wall of storage should read as a gas - so it takes
 * two things from the energy drawer's cube. It is drawn at full brightness and translucent, like the
 * cube's core, so a filled drawer reads in an unlit room. And its surface breathes: a slow rise and
 * fall of a quarter pixel, out of phase from one drawer to the next, so a wall of them does not move
 * as one machine. The heavy chemicals - slurries, pigments, infuse types - are drawn like a fluid,
 * opaque and lit by the block, because they are not gases and should not look like them.
 */
public class ChemicalDrawerRenderer extends BaseDrawerRenderer<ChemicalDrawerTile> {

    /** Where {@code FluidDrawerRenderer}'s frame puts the front: 1 - 0.5/16. */
    private static final float FLUID_FRAME_Z = 1f - 0.5f / 16f;

    /** A gas is seen through, but not so much that a pale one vanishes against the tank walls. */
    private static final float GAS_ALPHA = 0.8f;

    /** Their alpha for the assignment of an empty, locked tank: a ghost of what goes there. */
    private static final float GHOST_ALPHA = 0.3f;

    @Override
    public void renderItems(ChemicalDrawerTile tile, float partialTicks, PoseStack matrixStack,
                            MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        matrixStack.translate(0, 0, -FLUID_FRAME_Z);
        float breath = TankVolume.breath(tile.getLevel(), tile.getBlockPos(), partialTicks);

        FunctionalStorage.DrawerType type = tile.getDrawerType();
        for (int slot = 0; slot < type.getSlots(); slot++) {
            matrixStack.pushPose();
            // FluidDrawerRenderer.render2Slot / render4Slot: which quarter or half each slot is in.
            if (type == FunctionalStorage.DrawerType.X_2 && slot == 1) {
                matrixStack.translate(0, 0.5, 0);
            } else if (type == FunctionalStorage.DrawerType.X_4) {
                matrixStack.translate(slot == 0 || slot == 2 ? 0.5 : 0, slot >= 2 ? 0.5 : 0, 0);
            }
            renderSlot(tile, slot, type, breath, matrixStack, bufferIn, combinedLightIn, combinedOverlayIn);
            matrixStack.popPose();
        }

        // BaseDrawerRenderer pushes; the subclass pops. Their contract, not a choice.
        matrixStack.popPose();
    }

    private void renderSlot(ChemicalDrawerTile tile, int slot, FunctionalStorage.DrawerType type, float breath,
                            PoseStack matrixStack, MultiBufferSource bufferIn, int light, int overlay) {
        BigChemicalHandler handler = tile.getChemicalHandler();
        ChemicalStack stack = handler.getChemicalInTank(slot);
        long amount = stack.getAmount();
        if (stack.isEmpty()) {
            if (!tile.isLocked() || handler.getFilter(slot).isEmpty()) {
                return;
            }
            stack = handler.getFilter(slot);
            amount = 0;
        }
        long capacity = handler.getChemicalTankCapacity(slot);
        float fill = capacity <= 0 ? 0 : (float) Math.min(1d, amount / (double) capacity);

        // FluidDrawerRenderer's bounds: a 1x1 tank is 12.5 px tall; a half or a quarter is 5.5.
        boolean small = type != FunctionalStorage.DrawerType.X_1;
        boolean half = type == FunctionalStorage.DrawerType.X_4;
        float x2 = half ? 8 / 16f : 15 / 16f;
        float height = small ? 5.5f / 16f : 12.5f / 16f;
        float y2 = 1.25f / 16f + fill * height;
        if (fill > 0 && fill < 1) {
            y2 += breath;
        }

        ControllableDrawerTile.DrawerOptions options = tile.getDrawerOptions();
        if (options.isActive(ConfigurationToolItem.ConfigurationAction.TOGGLE_RENDER)) {
            boolean gas = stack.getChemical().isGaseous();
            float alpha = amount == 0 ? GHOST_ALPHA : gas ? GAS_ALPHA : 1f;
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(stack.getChemical().getIcon());
            TankVolume.render(matrixStack, bufferIn, sprite, stack.getChemicalTint(), 1 / 16f, 1.25f / 16f, 1 / 16f, x2, y2, 15 / 16f,
                    alpha, gas ? LightTexture.FULL_BRIGHT : light, overlay);
        }

        TankVolume.renderLabel(matrixStack, bufferIn, light, overlay, ChemicalFormat.format(amount), fill, options, half);
    }

}
