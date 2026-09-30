package dev.drimoz.immaterialdrawers.client.gui;

import com.hrznstudio.titanium.client.screen.addon.BasicScreenAddon;
import com.hrznstudio.titanium.client.screen.asset.IAssetProvider;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.compat.Mods;
import dev.drimoz.immaterialdrawers.storage.source.BigSourceStorage;
import dev.drimoz.immaterialdrawers.util.EnergyFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The drawer's face in its own screen - the energy drawer's layout, which is the 1x1 fluid drawer's,
 * with Ars's Source texture in the window.
 *
 * <p><b>Ars Nouveau only - see {@code compat.Mods}.</b> Adapted from Functional Storage's
 * {@code FluidDrawerInfoGuiAddon}, Copyright (c) 2021 Buuz135, Rid — MIT, see NOTICE. The drawing
 * order is theirs: tank walls, contents, then the front over them.
 */
public class SourceDrawerInfoGuiAddon extends BasicScreenAddon {

    private static final int TILE = 48;
    private static final Rect2i WELL = new Rect2i(9, 9, 30, 30);

    private static final ResourceLocation INNER = new ResourceLocation(
            ImmaterialDrawers.MOD_ID, "textures/block/drawer_inner.png");
    private static final ResourceLocation SOURCE_TEXTURE =
            new ResourceLocation(Mods.ARS_NOUVEAU, "block/mana_still");

    private final ResourceLocation front;
    private final Supplier<BigSourceStorage> storage;

    public SourceDrawerInfoGuiAddon(int posX, int posY, ResourceLocation front, Supplier<BigSourceStorage> storage) {
        super(posX, posY);
        this.front = front;
        this.storage = storage;
    }

    @Override
    public int getXSize() {
        return 0;
    }

    @Override
    public int getYSize() {
        return 0;
    }

    @Override
    public void drawBackgroundLayer(GuiGraphics graphics, Screen screen, IAssetProvider provider,
                                    int guiX, int guiY, int mouseX, int mouseY, float partialTicks) {
        int x = guiX + getPosX();
        int y = guiY + getPosY();
        BigSourceStorage source = storage.get();
        int capacity = source.getSourceCapacity();

        graphics.blit(INNER, x + WELL.getX(), y + WELL.getY(), WELL.getWidth(), WELL.getHeight(),
                3f, 3f, 10, 10, 16, 16);

        if (capacity > 0 && source.getSource() > 0) {
            double ratio = Math.min(1d, source.getSource() / (double) capacity);
            int filled = Math.max(1, (int) Math.round(WELL.getHeight() * ratio));
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(SOURCE_TEXTURE);
            int left = x + WELL.getX();
            int bottom = y + WELL.getY() + WELL.getHeight();
            for (int dx = 0; dx < WELL.getWidth(); dx += 16) {
                for (int dy = 0; dy < filled; dy += 16) {
                    int h = Math.min(16, filled - dy);
                    graphics.blit(left + dx, bottom - dy - h, 0, Math.min(16, WELL.getWidth() - dx), h, sprite);
                }
            }
        }

        graphics.blit(front, x, y, 0, 0, TILE, TILE, TILE, TILE);

        if (capacity > 0) {
            String amount = EnergyFormat.format(source.getStoredRaw()) + "/" + EnergyFormat.format(capacity);
            float scale = 0.5f;
            graphics.pose().translate(0, 0, 200);
            graphics.pose().scale(scale, scale, scale);
            graphics.drawString(Minecraft.getInstance().font, amount,
                    (int) ((x + TILE / 2f) / scale - Minecraft.getInstance().font.width(amount) / 2f),
                    (int) ((y + 28) * (1 / scale)), 0xFFFFFF, true);
            graphics.pose().scale(1 / scale, 1 / scale, 1 / scale);
            graphics.pose().translate(0, 0, -200);
        }
    }

    @Override
    public void drawForegroundLayer(GuiGraphics graphics, Screen screen, IAssetProvider provider,
                                    int guiX, int guiY, int mouseX, int mouseY, float partialTicks) {
        int hoverX = guiX + getPosX() + WELL.getX();
        int hoverY = guiY + getPosY() + WELL.getY();
        if (mouseX <= hoverX || mouseX >= hoverX + WELL.getWidth()
                || mouseY <= hoverY || mouseY >= hoverY + WELL.getHeight()) {
            return;
        }
        int localX = getPosX() + WELL.getX();
        int localY = getPosY() + WELL.getY();
        graphics.pose().translate(0, 0, 200);
        graphics.fill(localX, localY, localX + WELL.getWidth(), localY + WELL.getHeight(), -2130706433);
        graphics.pose().translate(0, 0, -200);

        BigSourceStorage source = storage.get();
        var lines = new ArrayList<Component>();
        lines.add(Component.translatable("gui.immaterialdrawers.source").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(EnergyFormat.format(source.getStoredRaw())).withStyle(ChatFormatting.WHITE)));
        lines.add(Component.translatable("gui.immaterialdrawers.capacity").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(EnergyFormat.format(source.getSourceCapacity())).withStyle(ChatFormatting.WHITE)));
        graphics.renderTooltip(Minecraft.getInstance().font, lines, Optional.empty(), mouseX - guiX, mouseY - guiY);
    }
}
