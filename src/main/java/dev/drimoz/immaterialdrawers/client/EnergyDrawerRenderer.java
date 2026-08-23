package dev.drimoz.immaterialdrawers.client;

import com.buuz135.functionalstorage.client.BaseDrawerRenderer;
import com.buuz135.functionalstorage.client.DrawerRenderer;
import com.buuz135.functionalstorage.item.ConfigurationToolItem;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.util.EnergyFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Draws the charge on the face of the drawer, the way a drawer draws what it holds.
 *
 * <p>Extends Functional Storage's {@code BaseDrawerRenderer}, which is where all the orientation
 * maths lives — a drawer can be on any of six faces in any of four rotations, and getting that
 * wrong is a whole afternoon. Copyright (c) 2021 Buuz135, Rid — MIT. See NOTICE.
 *
 * <p><b>Their widgets, not lookalikes.</b> {@code DrawerRenderer.renderText} and
 * {@code renderIndicator} are public and static, so the number on an energy drawer is drawn by the
 * same code, at the same scale, in the same place as the count on the drawer next to it, and the
 * fill indicator is the drawer indicator — including being opt-in through the Configuration Tool,
 * like every other drawer. A wall of drawers should not have one block that looks like it came from
 * somewhere else.
 *
 * <p>This is also the only display a <em>framed</em> drawer can have: its front is whatever texture
 * the player framed it with, so anything showing the charge has to be drawn on top of it rather
 * than baked into it.
 */
public class EnergyDrawerRenderer extends BaseDrawerRenderer<EnergyDrawerTile> {

    /** Matches the scale Functional Storage uses for the count on a 1x1 drawer. */
    private static final float TEXT_SCALE = 0.015f;

    private static final ResourceLocation GAUGE = ResourceLocation.fromNamespaceAndPath(
            ImmaterialDrawers.MOD_ID, "textures/block/energy_gauge.png");

    // The recessed panel in the drawer front, in the centred frame BaseDrawerRenderer leaves us in:
    // texture pixel p maps to p/16 - 0.5. The panel is pixels 3..12, and the gauge sits one pixel
    // inside its trim so the bezel still reads as a bezel.
    private static final float PANEL_LEFT = 4f / 16f - 0.5f;
    private static final float PANEL_RIGHT = 12f / 16f - 0.5f;
    private static final float PANEL_BOTTOM = 4f / 16f - 0.5f;
    private static final float PANEL_HEIGHT = 8f / 16f;

    /** Just in front of the face, and behind the number that goes on top of it. */
    private static final float GAUGE_Z = 0.0002f;

    @Override
    public void renderItems(EnergyDrawerTile tile, float partialTicks, PoseStack matrixStack,
                            MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        var storage = tile.getEnergyStorage();
        // Long accessors throughout: the capability view saturates at 2.1B, which on a fully
        // upgraded drawer would show a full bar and a frozen number.
        long capacity = storage.getCapacityLong();

        matrixStack.translate(0.5, 0.5, 0.0005f);

        float progress = capacity <= 0 ? 0f : (float) Math.min(1d, storage.getStoredLong() / (double) capacity);

        // The gauge itself, before the number goes on top of it.
        if (progress > 0 && tile.getDrawerOptions().isActive(ConfigurationToolItem.ConfigurationAction.TOGGLE_RENDER)) {
            renderGauge(matrixStack, bufferIn, combinedOverlayIn, progress);
        }

        DrawerRenderer.renderIndicator(matrixStack, bufferIn, combinedLightIn, combinedOverlayIn,
                progress, tile.getDrawerOptions());

        // Same option a drawer uses to hide its stack counts. An energy drawer has nothing else to
        // show, so honouring it is the difference between "configurable" and "broken".
        if (tile.getDrawerOptions().isActive(ConfigurationToolItem.ConfigurationAction.TOGGLE_NUMBERS)) {
            // The two half-turns cancel. Kept as a pair because that is what renderStack does
            // around the item it draws between them, and the next person to add something in the
            // middle should find the frame they expect.
            matrixStack.mulPose(Axis.YP.rotationDegrees(180));
            matrixStack.mulPose(Axis.YP.rotationDegrees(180));
            matrixStack.scale(0.665f, 0.665f, 0.665f);

            // Stored and capacity, not just stored. A count on an item drawer is progress on its
            // own, because the slot limit is common knowledge; "30.8M FE" is not, because the
            // capacity moves with whichever upgrades are in the drawer.
            String amount = EnergyFormat.format(storage.getStoredLong()) + "/" + EnergyFormat.format(capacity);
            DrawerRenderer.renderText(matrixStack, bufferIn, combinedOverlayIn,
                    Component.literal(ChatFormatting.WHITE + amount), Direction.NORTH, TEXT_SCALE);
        }

        // BaseDrawerRenderer pushes; the subclass pops. Their contract, not a choice.
        matrixStack.popPose();
    }

    /**
     * Lights the drawer's recessed panel from the bottom, in proportion to the charge.
     *
     * <p><b>Why a panel and not a bar.</b> Every energy block in every tech mod wears a vertical
     * gauge down one side, and one of those in a wall of drawers reads as the wrong block. This is
     * the same idea Functional Storage uses for a fluid drawer — the content shows through the
     * window in the front — with a glow instead of a fluid texture, because energy has no texture to
     * borrow.
     *
     * <p>Drawn at full brightness rather than at the block's light level: a charged drawer should be
     * readable in an unlit room, which is the whole point of a gauge. Not an emissive render type,
     * just {@link LightTexture#FULL_BRIGHT} on the vertices — same result, and it composes with the
     * translucency the panel needs.
     *
     * <p>The V coordinates track the fill instead of stretching to it, so the gradient and its
     * scanlines stay put as the level rises rather than sliding around.
     *
     * <p>Works on the framed variant for free: this is drawn over whatever the front happens to be,
     * so a drawer framed with oak still shows its charge.
     */
    private static void renderGauge(PoseStack matrixStack, MultiBufferSource bufferIn,
                                    int combinedOverlayIn, float progress) {
        VertexConsumer builder = bufferIn.getBuffer(RenderType.entityTranslucent(GAUGE));
        Matrix4f pose = matrixStack.last().pose();

        float bottom = PANEL_BOTTOM;
        float top = PANEL_BOTTOM + PANEL_HEIGHT * progress;
        // Sample the lower slice of the texture, matching how full the panel is.
        float v1 = 1f - progress;

        // Winding and normal copied from DrawerRenderer.renderIndicator: in this frame +Z faces the
        // viewer, and a quad wound the other way is simply not there.
        builder.addVertex(pose, PANEL_RIGHT, bottom, GAUGE_Z).setColor(255, 255, 255, 255)
                .setUv(1f, 1f).setOverlay(combinedOverlayIn).setLight(LightTexture.FULL_BRIGHT).setNormal(0f, 0f, 1f);
        builder.addVertex(pose, PANEL_RIGHT, top, GAUGE_Z).setColor(255, 255, 255, 255)
                .setUv(1f, v1).setOverlay(combinedOverlayIn).setLight(LightTexture.FULL_BRIGHT).setNormal(0f, 0f, 1f);
        builder.addVertex(pose, PANEL_LEFT, top, GAUGE_Z).setColor(255, 255, 255, 255)
                .setUv(0f, v1).setOverlay(combinedOverlayIn).setLight(LightTexture.FULL_BRIGHT).setNormal(0f, 0f, 1f);
        builder.addVertex(pose, PANEL_LEFT, bottom, GAUGE_Z).setColor(255, 255, 255, 255)
                .setUv(0f, 1f).setOverlay(combinedOverlayIn).setLight(LightTexture.FULL_BRIGHT).setNormal(0f, 0f, 1f);
    }
}
