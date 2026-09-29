package dev.drimoz.immaterialdrawers.client;

import com.buuz135.functionalstorage.client.BaseDrawerRenderer;
import com.buuz135.functionalstorage.client.DrawerRenderer;
import com.buuz135.functionalstorage.item.ConfigurationToolItem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.drimoz.immaterialdrawers.block.tile.source.SourceDrawerTile;
import dev.drimoz.immaterialdrawers.compat.Mods;
import dev.drimoz.immaterialdrawers.util.EnergyFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * Draws the Source a drawer holds: Ars Nouveau's own Source, at its level behind the window.
 *
 * <p><b>Ars Nouveau only - see {@code compat.Mods}.</b>
 *
 * <p>The fluid drawer's 1x1 layout, in {@code FluidDrawerRenderer}'s coordinates, the way
 * {@code ChemicalDrawerRenderer} does it. Copyright (c) 2021 Buuz135, Rid — MIT. See NOTICE.
 *
 * <p><b>Ars's texture, not ours.</b> {@code ars_nouveau:block/mana_still} is what fills a Source Jar,
 * animated, and a player who has filled a jar recognises it. Untinted, because the texture already is
 * the colour. Full bright and breathing, like the gases in the chemical drawer and the core of the
 * energy cube: Source glows in every block Ars puts it in.
 */
public class SourceDrawerRenderer extends BaseDrawerRenderer<SourceDrawerTile> {

    private static final ResourceLocation SOURCE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Mods.ARS_NOUVEAU, "block/mana_still");

    /** See {@code ChemicalDrawerRenderer.FLUID_FRAME_Z}. */
    private static final float FLUID_FRAME_Z = 1f - 0.5f / 16f;

    private static final float TEXT_SCALE = 0.007f;

    private static final float SOURCE_ALPHA = 0.9f;

    @Override
    public void renderItems(SourceDrawerTile tile, float partialTicks, PoseStack matrixStack,
                            MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        matrixStack.translate(0, 0, -FLUID_FRAME_Z);

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

        if (options.isActive(ConfigurationToolItem.ConfigurationAction.TOGGLE_NUMBERS)) {
            matrixStack.pushPose();
            matrixStack.translate(0.5, 0.84, 0.97);
            DrawerRenderer.renderText(matrixStack, bufferIn, combinedOverlayIn,
                    Component.literal(ChatFormatting.WHITE + EnergyFormat.format(storage.getStoredRaw())),
                    Direction.NORTH, TEXT_SCALE);
            matrixStack.popPose();
        }
        matrixStack.pushPose();
        matrixStack.translate(0.5, 0.453, 0.97);
        DrawerRenderer.renderIndicator(matrixStack, bufferIn, combinedLightIn, combinedOverlayIn, fill, options);
        matrixStack.popPose();

        // BaseDrawerRenderer pushes; the subclass pops.
        matrixStack.popPose();
    }
}
