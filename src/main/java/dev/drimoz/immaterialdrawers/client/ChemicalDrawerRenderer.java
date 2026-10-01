package dev.drimoz.immaterialdrawers.client;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.tile.ControllableDrawerTile;
import com.buuz135.functionalstorage.client.DrawerRenderer;
import com.buuz135.functionalstorage.client.FunctionalStorageClientConfig;
import com.buuz135.functionalstorage.item.ConfigurationToolItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.drimoz.immaterialdrawers.block.tile.chemical.ChemicalDrawerTile;
import dev.drimoz.immaterialdrawers.storage.chemical.BigChemicalHandler;
import dev.drimoz.immaterialdrawers.util.ChemicalFormat;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.ChemicalType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * Draws a chemical drawer's tanks the way Functional Storage draws a fluid drawer's: the chemical's own
 * texture and tint, its level, the number and the indicator. <b>Mekanism only.</b> What comes from the
 * energy drawer: gases glow (full bright, translucent) and their surface breathes.
 *
 * <p>Orientation, bounds and positions are Functional Storage 1.20.1's {@code FluidDrawerRenderer}'s.
 * Copyright (c) 2021 Buuz135, Rid - MIT. See NOTICE.
 */
public class ChemicalDrawerRenderer implements BlockEntityRenderer<ChemicalDrawerTile> {

    private static final float GAS_ALPHA = 0.8f;
    private static final float GHOST_ALPHA = 0.3f;

    @Override
    public void render(ChemicalDrawerTile tile, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn,
                       int combinedLightIn, int combinedOverlayIn) {
        if (Minecraft.getInstance().player != null
                && !tile.getBlockPos().closerThan(Minecraft.getInstance().player.getOnPos(),
                FunctionalStorageClientConfig.DRAWER_RENDER_RANGE)) {
            return;
        }
        matrixStack.pushPose();
        Direction facing = tile.getFacingDirection();
        matrixStack.mulPose(Axis.YP.rotationDegrees(-180));
        if (facing == Direction.NORTH) {
            matrixStack.translate(-1, 0, -1);
        }
        if (facing == Direction.EAST) {
            matrixStack.translate(0, 0, -1);
            matrixStack.mulPose(Axis.YP.rotationDegrees(-90));
        }
        if (facing == Direction.SOUTH) {
            matrixStack.mulPose(Axis.YP.rotationDegrees(-180));
        }
        if (facing == Direction.WEST) {
            matrixStack.translate(-1, 0, 0);
            matrixStack.mulPose(Axis.YP.rotationDegrees(90));
        }
        int light = LevelRenderer.getLightColor(tile.getLevel(), tile.getBlockPos().relative(facing));

        float breath = TankVolume.breath(tile.getLevel(), tile.getBlockPos(), partialTicks);
        FunctionalStorage.DrawerType type = tile.getDrawerType();
        for (int slot = 0; slot < type.getSlots(); slot++) {
            matrixStack.pushPose();
            // FluidDrawerRenderer.render2Slot / render4Slot: which half or quarter each slot is in.
            if (type == FunctionalStorage.DrawerType.X_2 && slot == 1) {
                matrixStack.translate(0, 0.5, 0);
            } else if (type == FunctionalStorage.DrawerType.X_4) {
                matrixStack.translate(slot == 0 || slot == 2 ? 0.5 : 0, slot >= 2 ? 0.5 : 0, 0);
            }
            renderSlot(tile, slot, type, breath, matrixStack, bufferIn, light, combinedOverlayIn);
            matrixStack.popPose();
        }

        matrixStack.pushPose();
        matrixStack.translate(0, 0, 0.9688);
        DrawerRenderer.renderUpgrades(matrixStack, bufferIn, light, combinedOverlayIn, tile);
        matrixStack.popPose();

        matrixStack.popPose();
    }

    private void renderSlot(ChemicalDrawerTile tile, int slot, FunctionalStorage.DrawerType type, float breath,
                            PoseStack matrixStack, MultiBufferSource bufferIn, int light, int overlay) {
        BigChemicalHandler handler = tile.getChemicalHandler();
        ChemicalStack<?> stack = handler.stored(slot);
        long amount = stack.getAmount();
        if (stack.isEmpty()) {
            if (!tile.isLocked() || handler.getFilter(slot).isEmpty()) {
                return;
            }
            stack = handler.getFilter(slot);
            amount = 0;
        }
        long capacity = handler.capacity(slot);
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
            // Gaseous, in Mekanism 10.4, is exactly "a gas": the three other types are heavy.
            boolean gas = ChemicalType.getTypeFor(stack) == ChemicalType.GAS;
            float alpha = amount == 0 ? GHOST_ALPHA : gas ? GAS_ALPHA : 1f;
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(stack.getType().getIcon());
            TankVolume.render(matrixStack, bufferIn, sprite, stack.getChemicalTint(),
                    1 / 16f, 1.25f / 16f, 1 / 16f, x2, y2, 15 / 16f,
                    alpha, gas ? LightTexture.FULL_BRIGHT : light, overlay);
        }

        TankVolume.renderLabel(matrixStack, bufferIn, light, overlay, ChemicalFormat.format(amount), fill, options, half);
    }
}
