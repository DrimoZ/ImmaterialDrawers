package dev.drimoz.immaterialdrawers.client;

import com.buuz135.functionalstorage.client.DrawerRenderer;
import com.buuz135.functionalstorage.client.FunctionalStorageClientConfig;
import com.buuz135.functionalstorage.item.ConfigurationToolItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.drimoz.immaterialdrawers.block.tile.source.SourceDrawerTile;
import dev.drimoz.immaterialdrawers.compat.Mods;
import dev.drimoz.immaterialdrawers.util.EnergyFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * Draws a Source Drawer's tank: Ars's own Source texture - the one its Source Jars show - full bright
 * and breathing, where a fluid drawer shows its fluid. <b>Ars Nouveau only.</b>
 *
 * <p>Orientation and positions are Functional Storage 1.20.1's {@code FluidDrawerRenderer}'s:
 * Copyright (c) 2021 Buuz135, Rid - MIT. See NOTICE.
 */
public class SourceDrawerRenderer implements BlockEntityRenderer<SourceDrawerTile> {

    private static final ResourceLocation SOURCE_TEXTURE = new ResourceLocation(Mods.ARS_NOUVEAU, "block/mana_still");
    private static final float SOURCE_ALPHA = 0.9f;

    @Override
    public void render(SourceDrawerTile tile, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn,
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

        var storage = tile.getSourceStorage();
        int capacity = storage.getSourceCapacity();
        int stored = storage.getSource();
        float fill = capacity <= 0 ? 0 : Math.min(1f, stored / (float) capacity);
        var options = tile.getDrawerOptions();

        if (stored > 0 && options.isActive(ConfigurationToolItem.ConfigurationAction.TOGGLE_RENDER)) {
            float y2 = 1.25f / 16f + fill * (12.5f / 16f);
            if (fill < 1) {
                y2 += TankVolume.breath(tile.getLevel(), tile.getBlockPos(), partialTicks);
            }
            var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(SOURCE_TEXTURE);
            TankVolume.render(matrixStack, bufferIn, sprite, 0xFFFFFF,
                    1 / 16f, 1.25f / 16f, 1 / 16f, 15 / 16f, y2, 15 / 16f,
                    SOURCE_ALPHA, LightTexture.FULL_BRIGHT, combinedOverlayIn);
        }

        TankVolume.renderLabel(matrixStack, bufferIn, light, combinedOverlayIn,
                EnergyFormat.format(storage.getStoredRaw()), fill, options, false);

        matrixStack.pushPose();
        matrixStack.translate(0, 0, 0.9688);
        DrawerRenderer.renderUpgrades(matrixStack, bufferIn, light, combinedOverlayIn, tile);
        matrixStack.popPose();

        matrixStack.popPose();
    }
}
