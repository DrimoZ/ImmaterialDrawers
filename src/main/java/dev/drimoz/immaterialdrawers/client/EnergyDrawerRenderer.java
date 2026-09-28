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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws what an energy drawer holds: an energy cube turning inside the tank, and the charge on the
 * front.
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
 */
public class EnergyDrawerRenderer extends BaseDrawerRenderer<EnergyDrawerTile> {

    /** The scale Functional Storage uses for the amount on a 1x1 fluid drawer. */
    private static final float TEXT_SCALE = 0.007f;

    /** FluidDrawerRenderer's 0.84 and 0.453, measured from the block's bottom edge; ours from its middle. */
    private static final float TEXT_Y = 0.84f - 0.5f;
    private static final float INDICATOR_Y = 0.453f - 0.5f;

    /** The open frame: eight corner caps and twelve edges. Lit like the block it sits in. */
    public static final ModelResourceLocation CUBE_FRAME = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(ImmaterialDrawers.MOD_ID, "block/energy_cube_frame"));

    /** The faceted core the frame holds. Translucent, full bright, and only there when charged. */
    public static final ModelResourceLocation CUBE_CORE = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(ImmaterialDrawers.MOD_ID, "block/energy_cube_core"));

    /**
     * Nine pixels across. The frame turns on its vertical axis, so what has to fit is its diagonal:
     * 9 × √2 ≈ 12.7 px, inside the 13 px between the tank's side walls. Ten would scrape them.
     */
    private static final float CUBE_SCALE = 9f / 16f;

    /**
     * Middle of the tank, in the frame BaseDrawerRenderer leaves us in: its z = 0 is half a pixel
     * into the block, and the tank runs from 1 px to 14.5 px deep.
     */
    private static final float TANK_CENTRE_Z = -(7.75f - 0.5f) / 16f;

    /** Degrees per tick. The frame turns at one pace whatever the charge; it is the core that tells. */
    private static final float FRAME_SPEED = 1.2f;
    private static final float CORE_SPEED_EMPTY = 2f;
    private static final float CORE_SPEED_FULL = 12f;

    /** How far the cube rises and falls, and how fast. */
    private static final float BOB_HEIGHT = 0.25f / 16f;
    private static final float BOB_SPEED = 0.045f;

    /**
     * The core's opacity just above empty. Lower than this and a translucent orange over the dark
     * tank reads as brown, which looks like rust rather than a little charge.
     */
    private static final float CORE_ALPHA_EMPTY = 0.55f;

    private static final Vector3f CORE_AXIS = new Vector3f(1, 1, 1).normalize();

    /** The six faces, then the unculled quads — which is where rotated elements end up. */
    private static final Direction[] SIDES = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN, null
    };

    private final RandomSource random = RandomSource.create();

    @Override
    public void renderItems(EnergyDrawerTile tile, float partialTicks, PoseStack matrixStack,
                            MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        var storage = tile.getEnergyStorage();
        // Long accessors throughout: the capability view saturates at 2.1B, which on a fully
        // upgraded drawer would show a full bar and a frozen number.
        long capacity = storage.getCapacityLong();

        matrixStack.translate(0.5, 0.5, 0.0005f);

        float progress = capacity <= 0 ? 0f : (float) Math.min(1d, storage.getStoredLong() / (double) capacity);

        // The same option that hides the item in an item drawer and the fluid in a fluid drawer.
        if (tile.getDrawerOptions().isActive(ConfigurationToolItem.ConfigurationAction.TOGGLE_RENDER)) {
            renderCube(tile, partialTicks, matrixStack, bufferIn, combinedLightIn, combinedOverlayIn, progress);
        }

        // Where FluidDrawerRenderer puts its indicator and its number, converted from its frame
        // (origin in the corner) to ours (origin in the middle): the front is their front, so the
        // bottom rail of the frame is where there is room to write. An item drawer's placement puts
        // the number a pixel lower, behind the machine casing's bottom rim, where nothing shows.
        matrixStack.pushPose();
        matrixStack.translate(0, INDICATOR_Y, 0);
        DrawerRenderer.renderIndicator(matrixStack, bufferIn, combinedLightIn, combinedOverlayIn,
                progress, tile.getDrawerOptions());
        matrixStack.popPose();

        // Same option a drawer uses to hide its stack counts. An energy drawer has nothing else to
        // show, so honouring it is the difference between "configurable" and "broken".
        if (tile.getDrawerOptions().isActive(ConfigurationToolItem.ConfigurationAction.TOGGLE_NUMBERS)) {
            // Stored and capacity, not just stored. A count on an item drawer is progress on its
            // own, because the slot limit is common knowledge; "30.8M FE" is not, because the
            // capacity moves with whichever upgrades are in the drawer.
            String amount = EnergyFormat.format(storage.getStoredLong()) + "/" + EnergyFormat.format(capacity);
            matrixStack.pushPose();
            matrixStack.translate(0, TEXT_Y, 0);
            DrawerRenderer.renderText(matrixStack, bufferIn, combinedOverlayIn,
                    Component.literal(ChatFormatting.WHITE + amount), Direction.NORTH, TEXT_SCALE);
            matrixStack.popPose();
        }

        // BaseDrawerRenderer pushes; the subclass pops. Their contract, not a choice.
        matrixStack.popPose();
    }

    /**
     * The energy cube in the tank, where a fluid drawer shows its fluid.
     *
     * <p><b>Why a cube and not a level.</b> A fluid has a surface, so a fluid drawer shows how full
     * it is by how high the fluid stands. Energy has neither texture nor surface, and a bar filling a
     * drawer front reads as a machine gauge in a wall of storage. So the drawer holds an object
     * instead — an energy cube in the Mekanism tradition, which every tech player already reads as
     * "stored power" — and the charge is in its core: absent when empty, faint and slow at low
     * charge, solid and fast when full. The exact figure is the number on the front; this is the
     * glance.
     *
     * <p>Two models, not one, because the halves are drawn differently. The frame takes the light of
     * the block in front, like the drawer around it. The core is full bright and translucent: a
     * charged drawer should read in an unlit room.
     *
     * <p>Each drawer turns out of phase with its neighbours, from its position, so a wall of them
     * does not spin in lockstep like one machine.
     *
     * <p>Works on the framed variant unchanged: the tank is not part of the framed design, so the
     * cube sits behind whatever front the player chose.
     */
    private void renderCube(EnergyDrawerTile tile, float partialTicks, PoseStack matrixStack,
                            MultiBufferSource bufferIn, int light, int overlay, float progress) {
        var models = Minecraft.getInstance().getModelManager();
        float time = (tile.getLevel() == null ? 0 : tile.getLevel().getGameTime() % 72000L) + partialTicks;
        float phase = Math.floorMod(tile.getBlockPos().asLong() * 31L, 360L);

        matrixStack.pushPose();
        matrixStack.translate(0, Mth.sin(time * BOB_SPEED + phase) * BOB_HEIGHT, TANK_CENTRE_Z);

        matrixStack.pushPose();
        matrixStack.mulPose(Axis.YP.rotationDegrees(time * FRAME_SPEED + phase));
        centreModel(matrixStack);
        renderModel(models.getModel(CUBE_FRAME), matrixStack, bufferIn.getBuffer(Sheets.cutoutBlockSheet()),
                1f, light, overlay);
        matrixStack.popPose();

        if (progress > 0) {
            float speed = Mth.lerp(progress, CORE_SPEED_EMPTY, CORE_SPEED_FULL);
            matrixStack.pushPose();
            matrixStack.mulPose(new Quaternionf().rotationAxis((time * speed + phase) * Mth.DEG_TO_RAD, CORE_AXIS));
            centreModel(matrixStack);
            renderModel(models.getModel(CUBE_CORE), matrixStack,
                    bufferIn.getBuffer(Sheets.translucentCullBlockSheet()),
                    Mth.lerp(progress, CORE_ALPHA_EMPTY, 1f), LightTexture.FULL_BRIGHT, overlay);
            matrixStack.popPose();
        }

        matrixStack.popPose();
    }

    /** Models are authored in a 0..16 block; this puts their centre on the pivot, at cube size. */
    private static void centreModel(PoseStack matrixStack) {
        matrixStack.scale(CUBE_SCALE, CUBE_SCALE, CUBE_SCALE);
        matrixStack.translate(-0.5, -0.5, -0.5);
    }

    /** Every quad of a baked model, culled faces included — there is no neighbour to cull against. */
    private void renderModel(BakedModel model, PoseStack matrixStack, VertexConsumer buffer,
                             float alpha, int light, int overlay) {
        PoseStack.Pose pose = matrixStack.last();
        for (Direction side : SIDES) {
            random.setSeed(42L);
            for (BakedQuad quad : model.getQuads(null, side, random, ModelData.EMPTY, null)) {
                buffer.putBulkData(pose, quad, 1f, 1f, 1f, alpha, light, overlay, false);
            }
        }
    }
}
