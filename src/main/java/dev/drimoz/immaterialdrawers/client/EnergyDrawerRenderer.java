package dev.drimoz.immaterialdrawers.client;

import com.buuz135.functionalstorage.client.BaseDrawerRenderer;
import com.buuz135.functionalstorage.client.DrawerRenderer;
import com.buuz135.functionalstorage.item.ConfigurationToolItem;
import dev.drimoz.immaterialdrawers.util.EnergyFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

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

    @Override
    public void renderItems(EnergyDrawerTile tile, float partialTicks, PoseStack matrixStack,
                            MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        var storage = tile.getEnergyStorage();
        // Long accessors throughout: the capability view saturates at 2.1B, which on a fully
        // upgraded drawer would show a full bar and a frozen number.
        long capacity = storage.getCapacityLong();

        matrixStack.translate(0.5, 0.5, 0.0005f);

        float progress = capacity <= 0 ? 0f : (float) Math.min(1d, storage.getStoredLong() / (double) capacity);
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
}
