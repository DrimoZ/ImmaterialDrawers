package dev.drimoz.immaterialdrawers.client;

import com.buuz135.functionalstorage.client.DrawerRenderer;
import com.buuz135.functionalstorage.client.FunctionalStorageClientConfig;
import com.buuz135.functionalstorage.item.ConfigurationToolItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile;
import dev.drimoz.immaterialdrawers.util.EnergyFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws what an energy drawer holds: an energy cube turning inside the tank, and the charge on the
 * front. The art and its reasons are the 1.21.1 renderer's (CLAUDE.md §11); what changes here is
 * the frame it draws in.
 *
 * <p>Functional Storage 1.20.1 has no {@code BaseDrawerRenderer}: each renderer turns itself to face
 * the drawer's front. The orientation block below is their {@code FluidDrawerRenderer}'s, and it
 * leaves us in block coordinates with the front at z = 1. Copyright (c) 2021 Buuz135, Rid - MIT.
 * See NOTICE. The number, the indicator and the upgrade icons are drawn by their own static helpers,
 * at their fluid drawer's positions, so a wall of drawers reads as one family.
 */
public class EnergyDrawerRenderer implements BlockEntityRenderer<EnergyDrawerTile> {

    /** The scale and positions Functional Storage uses for the amount on a 1x1 fluid drawer. */
    private static final float TEXT_SCALE = 0.007f;
    private static final float TEXT_Y = 0.84f;
    private static final float INDICATOR_Y = 0.453f;
    private static final float FRONT_Z = 0.97f;

    /** The open frame: eight corner caps and twelve edges. Lit like the block it sits in. */
    public static final ResourceLocation CUBE_FRAME = new ResourceLocation(ImmaterialDrawers.MOD_ID, "block/energy_cube_frame");

    /** The faceted core the frame holds. Translucent, full bright, and only there when charged. */
    public static final ResourceLocation CUBE_CORE = new ResourceLocation(ImmaterialDrawers.MOD_ID, "block/energy_cube_core");

    /** Nine pixels across: the frame turns on Y, so its diagonal (12.7 px) must fit the 13 px tank. */
    private static final float CUBE_SCALE = 9f / 16f;

    /** Middle of the tank: it runs from 1 px to 14.5 px behind a front recessed half a pixel. */
    private static final float TANK_CENTRE_Z = 1f - 7.75f / 16f;

    private static final float FRAME_SPEED = 1.2f;
    private static final float CORE_SPEED_EMPTY = 2f;
    private static final float CORE_SPEED_FULL = 12f;
    private static final float BOB_HEIGHT = 0.25f / 16f;
    private static final float BOB_SPEED = 0.045f;
    /** Below this a translucent orange over the dark tank reads as rust, not as a little charge. */
    private static final float CORE_ALPHA_EMPTY = 0.55f;
    private static final Vector3f CORE_AXIS = new Vector3f(1, 1, 1).normalize();

    /** The six faces, then the unculled quads - which is where rotated elements end up. */
    private static final Direction[] SIDES = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN, null
    };

    private final RandomSource random = RandomSource.create();

    @Override
    public void render(EnergyDrawerTile tile, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn,
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

        var storage = tile.getEnergyStorage();
        // Long accessors: the capability view saturates at 2.1B.
        long capacity = storage.getCapacityLong();
        float progress = capacity <= 0 ? 0f : (float) Math.min(1d, storage.getStoredLong() / (double) capacity);

        if (tile.getDrawerOptions().isActive(ConfigurationToolItem.ConfigurationAction.TOGGLE_RENDER)) {
            renderCube(tile, partialTicks, matrixStack, bufferIn, light, combinedOverlayIn, progress);
        }

        matrixStack.pushPose();
        matrixStack.translate(0.5, INDICATOR_Y, FRONT_Z);
        DrawerRenderer.renderIndicator(matrixStack, bufferIn, light, combinedOverlayIn, progress, tile.getDrawerOptions());
        matrixStack.popPose();

        if (tile.getDrawerOptions().isActive(ConfigurationToolItem.ConfigurationAction.TOGGLE_NUMBERS)) {
            // Stored and capacity: the capacity moves with the upgrades, so the stored amount alone
            // says nothing about how full the drawer is.
            String amount = EnergyFormat.format(storage.getStoredLong()) + "/" + EnergyFormat.format(capacity);
            matrixStack.pushPose();
            matrixStack.translate(0.5, TEXT_Y, FRONT_Z);
            DrawerRenderer.renderText(matrixStack, bufferIn, combinedOverlayIn,
                    Component.literal(ChatFormatting.WHITE + amount), Direction.NORTH, TEXT_SCALE);
            matrixStack.popPose();
        }

        matrixStack.pushPose();
        matrixStack.translate(0, 0, 0.9688);
        DrawerRenderer.renderUpgrades(matrixStack, bufferIn, light, combinedOverlayIn, tile);
        matrixStack.popPose();

        matrixStack.popPose();
    }

    /**
     * The energy cube in the tank. The frame takes the block's light, the core is full bright and
     * translucent, and each drawer turns out of phase with its neighbours.
     */
    private void renderCube(EnergyDrawerTile tile, float partialTicks, PoseStack matrixStack,
                            MultiBufferSource bufferIn, int light, int overlay, float progress) {
        var models = Minecraft.getInstance().getModelManager();
        float time = (tile.getLevel() == null ? 0 : tile.getLevel().getGameTime() % 72000L) + partialTicks;
        float phase = Math.floorMod(tile.getBlockPos().asLong() * 31L, 360L);

        matrixStack.pushPose();
        matrixStack.translate(0.5, 0.5 + Mth.sin(time * BOB_SPEED + phase) * BOB_HEIGHT, TANK_CENTRE_Z);

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

    /** Every quad of a baked model, culled faces included - there is no neighbour to cull against. */
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
