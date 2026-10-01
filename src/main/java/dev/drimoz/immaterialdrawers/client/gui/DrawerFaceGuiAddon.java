package dev.drimoz.immaterialdrawers.client.gui;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.client.gui.FluidDrawerInfoGuiAddon;
import com.hrznstudio.titanium.client.screen.addon.BasicScreenAddon;
import com.hrznstudio.titanium.client.screen.asset.IAssetProvider;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * A drawer's face in its own screen, laid out exactly like Functional Storage's fluid drawer - for
 * every drawer of ours, whatever it holds. Subclasses only say what a slot contains, what its
 * number reads and what its tooltip says.
 *
 * <p>Adapted from Functional Storage's {@code client/gui/FluidDrawerInfoGuiAddon}.
 * Copyright (c) 2021 Buuz135, Rid — MIT. See NOTICE.
 *
 * <p><b>Read from them, not copied:</b> the slot and hover rectangles
 * ({@link FluidDrawerInfoGuiAddon#getSizeForSlots}, {@link FluidDrawerInfoGuiAddon#getSizeForHoverSlots})
 * and the text anchors ({@link FunctionalStorage.DrawerType#getSlotPosition}). If they move their
 * layout, ours follows.
 *
 * <p><b>The order is theirs:</b> contents first, then the front over them, its windows letting the
 * contents through. One addition: the graphite tank walls go down before anything, so an empty slot
 * shows an empty drawer rather than the screen background through its window.
 *
 * <p><b>The number cannot be read from them</b> - they draw it inline, no method to call. Their
 * {@code +12} below the slot anchor is kept. Across, theirs is {@code +17 - width / 2} with the width
 * counted at full scale for half-scale text, which pushes every number left by a quarter of its width;
 * ours centres on the slot, except on the 2x2, where their placement already lands inside its quarters.
 */
public abstract class DrawerFaceGuiAddon extends BasicScreenAddon {

    /** 16px of front, doubled, plus its border - the size their tile is blitted at. */
    private static final int TILE = 48;

    private static final float TEXT_SCALE = 0.5f;

    private static final ResourceLocation INNER = ResourceLocation.fromNamespaceAndPath(
            ImmaterialDrawers.MOD_ID, "textures/block/drawer_inner.png");

    private final ResourceLocation front;
    private final FunctionalStorage.DrawerType type;

    protected DrawerFaceGuiAddon(int posX, int posY, ResourceLocation front, FunctionalStorage.DrawerType type) {
        super(posX, posY);
        this.front = front;
        this.type = type;
    }

    /** Draws what the slot holds into {@code area}, in screen coordinates, behind the front. */
    protected abstract void drawContents(GuiGraphics graphics, int slot, Rect2i area);

    /** The "stored/capacity" line on the slot, or {@code null} to write nothing. */
    @Nullable
    protected abstract String amount(int slot);

    /** The tooltip of the slot under the mouse. */
    protected abstract List<Component> tooltip(int slot);

    /** Zero, like theirs: the addon draws where it was placed and claims no layout space. */
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
        int slots = type.getSlots();

        // The window is texels 3..13 of the 16px texture; the front hides everything but its windows.
        graphics.blit(INNER, x + 2, y + 2, TILE - 4, TILE - 4, 3f, 3f, 10, 10, 16, 16);
        for (int slot = 0; slot < slots; slot++) {
            Rect2i area = FluidDrawerInfoGuiAddon.getSizeForSlots(slot, slots);
            drawContents(graphics, slot, new Rect2i(x + area.getX(), y + area.getY(), area.getWidth(), area.getHeight()));
        }

        graphics.blit(front, x, y, 0, 0, TILE, TILE, TILE, TILE);

        Font font = Minecraft.getInstance().font;
        for (int slot = 0; slot < slots; slot++) {
            String amount = amount(slot);
            if (amount == null) {
                continue;
            }
            int anchorX = x + type.getSlotPosition().apply(slot).getLeft();
            int anchorY = y + type.getSlotPosition().apply(slot).getRight();
            Rect2i area = FluidDrawerInfoGuiAddon.getSizeForSlots(slot, slots);
            int textX = slots == 4
                    ? (int) ((anchorX + 17 - font.width(amount) / 2f) / TEXT_SCALE)
                    : (int) ((x + area.getX() + area.getWidth() / 2f) / TEXT_SCALE - font.width(amount) / 2f);
            graphics.pose().translate(0, 0, 200);
            graphics.pose().scale(TEXT_SCALE, TEXT_SCALE, TEXT_SCALE);
            graphics.drawString(font, amount, textX, (int) ((anchorY + 12) / TEXT_SCALE), 0xFFFFFF, true);
            graphics.pose().scale(1 / TEXT_SCALE, 1 / TEXT_SCALE, 1 / TEXT_SCALE);
            graphics.pose().translate(0, 0, -200);
        }
    }

    @Override
    public void drawForegroundLayer(GuiGraphics graphics, Screen screen, IAssetProvider provider,
                                    int guiX, int guiY, int mouseX, int mouseY, float partialTicks) {
        int slots = type.getSlots();
        for (int slot = 0; slot < slots; slot++) {
            Rect2i rect = FluidDrawerInfoGuiAddon.getSizeForHoverSlots(slot, slots);
            int localX = getPosX() + rect.getX();
            int localY = getPosY() + rect.getY();
            if (mouseX <= guiX + localX || mouseX >= guiX + localX + rect.getWidth()
                    || mouseY <= guiY + localY || mouseY >= guiY + localY + rect.getHeight()) {
                continue;
            }
            // Their highlight, their colour: a drawer lights up the slot you are pointing at.
            graphics.pose().translate(0, 0, 200);
            graphics.fill(localX, localY, localX + rect.getWidth(), localY + rect.getHeight(), -2130706433);
            graphics.pose().translate(0, 0, -200);
            graphics.renderTooltip(Minecraft.getInstance().font, tooltip(slot), Optional.empty(),
                    mouseX - guiX, mouseY - guiY);
        }
    }

    /** {@code sprite} tiled over {@code area}, the way their addon draws a fluid. */
    protected static void blitTiled(GuiGraphics graphics, TextureAtlasSprite sprite, Rect2i area) {
        for (int dx = 0; dx < area.getWidth(); dx += 16) {
            for (int dy = 0; dy < area.getHeight(); dy += 16) {
                graphics.blit(area.getX() + dx, area.getY() + dy, 0,
                        Math.min(16, area.getWidth() - dx), Math.min(16, area.getHeight() - dy), sprite);
            }
        }
    }

    /** The bottom {@code ratio} of {@code area}, at least a pixel when anything is there. */
    protected static Rect2i bottomPart(Rect2i area, double ratio) {
        int filled = Math.max(1, (int) Math.round(area.getHeight() * Math.min(1d, ratio)));
        return new Rect2i(area.getX(), area.getY() + area.getHeight() - filled, area.getWidth(), filled);
    }
}
