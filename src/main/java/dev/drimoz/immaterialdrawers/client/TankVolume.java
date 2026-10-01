package dev.drimoz.immaterialdrawers.client;

import com.buuz135.functionalstorage.block.tile.ControllableDrawerTile;
import com.buuz135.functionalstorage.client.DrawerRenderer;
import com.buuz135.functionalstorage.item.ConfigurationToolItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

/**
 * A translucent volume in a drawer's tank - top and front faces, like Functional Storage's fluid -
 * and the slow breathing of its surface. Shared by the tanks of our non-energy drawers.
 *
 * <p>The faces are Functional Storage's {@code FluidDrawerRenderer.renderFluidStack}.
 * Copyright (c) 2021 Buuz135, Rid - MIT. See NOTICE.
 */
public final class TankVolume {

    private static final float BREATH_HEIGHT = 0.25f / 16f;
    private static final float BREATH_SPEED = 0.05f;

    private TankVolume() {
    }

    /** A quarter pixel up and down, out of phase from one drawer to the next. */
    public static float breath(Level level, BlockPos pos, float partialTicks) {
        float time = (level == null ? 0 : level.getGameTime() % 72000L) + partialTicks;
        float phase = Math.floorMod(pos.asLong() * 31L, 360L);
        return Mth.sin(time * BREATH_SPEED + phase) * BREATH_HEIGHT;
    }

    /**
     * The amount and the fill indicator on a tank's front, where {@code FluidDrawerRenderer.renderFluidStack}
     * puts them - its 0.84 / 0.453, its 0.007 text scale, its half-slot shift and squeeze for a 2x2.
     * Both are drawn by Functional Storage's own {@code DrawerRenderer} widgets.
     */
    public static void renderLabel(PoseStack matrixStack, MultiBufferSource bufferIn, int light, int overlay,
                                   String amount, float fill, ControllableDrawerTile.DrawerOptions options, boolean half) {
        if (options.isActive(ConfigurationToolItem.ConfigurationAction.TOGGLE_NUMBERS)) {
            matrixStack.pushPose();
            matrixStack.translate(half ? 0.25 : 0.5, 0.84, 0.97);
            DrawerRenderer.renderText(matrixStack, bufferIn, overlay,
                    Component.literal(ChatFormatting.WHITE + amount), Direction.NORTH, 0.007f);
            matrixStack.popPose();
        }
        matrixStack.pushPose();
        matrixStack.translate(0.5, 0.453, 0.97);
        if (half) {
            matrixStack.scale(0.5f, 0.65f, 0.5f);
            matrixStack.translate(-0.5, -0.18, 0);
        }
        DrawerRenderer.renderIndicator(matrixStack, bufferIn, light, overlay, fill, options);
        matrixStack.popPose();
    }

    public static void render(PoseStack matrixStack, MultiBufferSource bufferIn, TextureAtlasSprite sprite, int rgb,
                              float x1, float y1, float z1, float x2, float y2, float z2,
                              float alpha, int light, int overlay) {
        float red = (rgb >> 16 & 0xFF) / 255f;
        float green = (rgb >> 8 & 0xFF) / 255f;
        float blue = (rgb & 0xFF) / 255f;
        VertexConsumer builder = bufferIn.getBuffer(RenderType.translucent());
        Matrix4f pose = matrixStack.last().pose();

        // 1.20.1's TextureAtlasSprite.getU/getV take 0..16, not 0..1.
        float u1 = sprite.getU(x1 * 16);
        float u2 = sprite.getU(x2 * 16);
        float vTop1 = sprite.getV(z1 * 16);
        float vTop2 = sprite.getV(z2 * 16);
        builder.vertex(pose, x1, y2, z2).color(red, green, blue, alpha).uv(u1, vTop2).overlayCoords(overlay).uv2(light).normal(0f, 1f, 0f).endVertex();
        builder.vertex(pose, x2, y2, z2).color(red, green, blue, alpha).uv(u2, vTop2).overlayCoords(overlay).uv2(light).normal(0f, 1f, 0f).endVertex();
        builder.vertex(pose, x2, y2, z1).color(red, green, blue, alpha).uv(u2, vTop1).overlayCoords(overlay).uv2(light).normal(0f, 1f, 0f).endVertex();
        builder.vertex(pose, x1, y2, z1).color(red, green, blue, alpha).uv(u1, vTop1).overlayCoords(overlay).uv2(light).normal(0f, 1f, 0f).endVertex();

        float vFront1 = sprite.getV(y1 * 16);
        float vFront2 = sprite.getV(Math.min(1f, y2) * 16);
        builder.vertex(pose, x2, y1, z2).color(red, green, blue, alpha).uv(u2, vFront1).overlayCoords(overlay).uv2(light).normal(0f, 0f, 1f).endVertex();
        builder.vertex(pose, x2, y2, z2).color(red, green, blue, alpha).uv(u2, vFront2).overlayCoords(overlay).uv2(light).normal(0f, 0f, 1f).endVertex();
        builder.vertex(pose, x1, y2, z2).color(red, green, blue, alpha).uv(u1, vFront2).overlayCoords(overlay).uv2(light).normal(0f, 0f, 1f).endVertex();
        builder.vertex(pose, x1, y1, z2).color(red, green, blue, alpha).uv(u1, vFront1).overlayCoords(overlay).uv2(light).normal(0f, 0f, 1f).endVertex();
    }
}
