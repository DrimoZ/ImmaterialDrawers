package dev.drimoz.immaterialdrawers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;

/**
 * The visible part of a tank's contents: its top and its front, the two faces the tank walls leave
 * in view. Functional Storage's {@code FluidDrawerRenderer.renderFluidStack} draws exactly these two
 * for a fluid; the chemical and source drawers draw them for what they hold.
 *
 * <p>Copyright (c) 2021 Buuz135, Rid — MIT. See NOTICE.
 *
 * <p>No optional mod in here: the caller hands over a sprite and a colour, whatever they came from.
 */
public final class TankVolume {

    private static final float BREATH_HEIGHT = 0.25f / 16f;
    private static final float BREATH_SPEED = 0.05f;

    private TankVolume() {
    }

    /**
     * How far a breathing surface stands above its level right now, in block units - the slow rise
     * and fall the chemical and source drawers take from the energy drawer's cube. Out of phase from
     * one drawer to the next, from its position, so a wall does not move as one machine.
     */
    public static float breath(Level level, BlockPos pos, float partialTicks) {
        float time = (level == null ? 0 : level.getGameTime() % 72000L) + partialTicks;
        float phase = Math.floorMod(pos.asLong() * 31L, 360L);
        return Mth.sin(time * BREATH_SPEED + phase) * BREATH_HEIGHT;
    }

    /** Top then front of the box, textured with {@code sprite} in block-unit UVs, tinted {@code rgb}. */
    public static void render(PoseStack matrixStack, MultiBufferSource bufferIn, TextureAtlasSprite sprite, int rgb,
                              float x1, float y1, float z1, float x2, float y2, float z2,
                              float alpha, int light, int overlay) {
        float red = (rgb >> 16 & 0xFF) / 255f;
        float green = (rgb >> 8 & 0xFF) / 255f;
        float blue = (rgb & 0xFF) / 255f;

        VertexConsumer builder = bufferIn.getBuffer(RenderType.translucent());
        Matrix4f pose = matrixStack.last().pose();

        float u1 = sprite.getU(x1);
        float u2 = sprite.getU(x2);
        float vTop1 = sprite.getV(z1);
        float vTop2 = sprite.getV(z2);
        builder.addVertex(pose, x1, y2, z2).setColor(red, green, blue, alpha).setUv(u1, vTop2).setOverlay(overlay).setLight(light).setNormal(0f, 1f, 0f);
        builder.addVertex(pose, x2, y2, z2).setColor(red, green, blue, alpha).setUv(u2, vTop2).setOverlay(overlay).setLight(light).setNormal(0f, 1f, 0f);
        builder.addVertex(pose, x2, y2, z1).setColor(red, green, blue, alpha).setUv(u2, vTop1).setOverlay(overlay).setLight(light).setNormal(0f, 1f, 0f);
        builder.addVertex(pose, x1, y2, z1).setColor(red, green, blue, alpha).setUv(u1, vTop1).setOverlay(overlay).setLight(light).setNormal(0f, 1f, 0f);

        float vFront1 = sprite.getV(y1);
        float vFront2 = sprite.getV(Math.min(1f, y2));
        builder.addVertex(pose, x2, y1, z2).setColor(red, green, blue, alpha).setUv(u2, vFront1).setOverlay(overlay).setLight(light).setNormal(0f, 0f, 1f);
        builder.addVertex(pose, x2, y2, z2).setColor(red, green, blue, alpha).setUv(u2, vFront2).setOverlay(overlay).setLight(light).setNormal(0f, 0f, 1f);
        builder.addVertex(pose, x1, y2, z2).setColor(red, green, blue, alpha).setUv(u1, vFront2).setOverlay(overlay).setLight(light).setNormal(0f, 0f, 1f);
        builder.addVertex(pose, x1, y1, z2).setColor(red, green, blue, alpha).setUv(u1, vFront1).setOverlay(overlay).setLight(light).setNormal(0f, 0f, 1f);
    }
}
