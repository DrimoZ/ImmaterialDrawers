package dev.drimoz.immaterialdrawers.client.gui;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.hrznstudio.titanium.client.screen.addon.BasicScreenAddon;
import com.hrznstudio.titanium.client.screen.asset.IAssetProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.storage.chemical.BigChemicalHandler;
import dev.drimoz.immaterialdrawers.util.ChemicalFormat;
import mekanism.api.chemical.ChemicalStack;
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
 * The drawer's face in its own screen, laid out exactly like the fluid drawer's.
 *
 * <p><b>Mekanism only - see {@code compat.Mods}.</b>
 *
 * <p>Adapted from Functional Storage's {@code client/gui/FluidDrawerInfoGuiAddon}.
 * Copyright (c) 2021 Buuz135, Rid — MIT. See NOTICE. Their slot rectangles, their hover rectangles,
 * their text anchors ({@link FunctionalStorage.DrawerType#getSlotPosition}), their drawing order -
 * contents, then the front over them, the front's windows letting the contents through - so a
 * chemical drawer's screen lines up with a fluid drawer's pixel for pixel, in all three layouts.
 *
 * <p>One addition, from the energy drawer's screen: the graphite tank walls go down first, so an
 * empty slot shows an empty drawer rather than the screen background through its window.
 */
public class ChemicalDrawerInfoGuiAddon extends BasicScreenAddon {

    /** 16px of front, doubled, plus its border - the size their tile is blitted at. */
    private static final int TILE = 48;

    private static final ResourceLocation INNER = new ResourceLocation(
            ImmaterialDrawers.MOD_ID, "textures/block/drawer_inner.png");

    private final ResourceLocation front;
    private final FunctionalStorage.DrawerType type;
    private final Supplier<BigChemicalHandler> handler;

    public ChemicalDrawerInfoGuiAddon(int posX, int posY, ResourceLocation front, FunctionalStorage.DrawerType type,
                                      Supplier<BigChemicalHandler> handler) {
        super(posX, posY);
        this.front = front;
        this.type = type;
        this.handler = handler;
    }

    /** {@code FluidDrawerInfoGuiAddon.getSizeForSlots}: where each slot's contents are drawn. */
    private static Rect2i contentRect(int slot, int slots) {
        if (slots == 2) {
            return slot == 0 ? new Rect2i(0, 30, 48, 13) : new Rect2i(0, 6, 48, 13);
        }
        if (slots == 4) {
            return switch (slot) {
                case 0 -> new Rect2i(30, 30, 16, 16);
                case 1 -> new Rect2i(2, 30, 16, 16);
                case 2 -> new Rect2i(30, 2, 16, 16);
                default -> new Rect2i(2, 2, 16, 16);
            };
        }
        return new Rect2i(9, 9, 30, 30);
    }

    /** {@code FluidDrawerInfoGuiAddon.getSizeForHoverSlots}: where each slot answers the mouse. */
    private static Rect2i hoverRect(int slot, int slots) {
        if (slots == 2) {
            return slot == 0 ? new Rect2i(6, 30, 36, 12) : new Rect2i(6, 6, 36, 12);
        }
        if (slots == 4) {
            return switch (slot) {
                case 0 -> new Rect2i(30, 30, 12, 12);
                case 1 -> new Rect2i(6, 30, 12, 12);
                case 2 -> new Rect2i(30, 6, 12, 12);
                default -> new Rect2i(6, 6, 12, 12);
            };
        }
        return new Rect2i(9, 9, 30, 30);
    }

    @Override
    public int getXSize() {
        return 0;
    }

    @Override
    public int getYSize() {
        return 0;
    }

    /** What a slot shows: its contents, or the chemical a locked slot is pinned to. */
    private static ChemicalStack<?> shown(BigChemicalHandler chemicals, int slot) {
        ChemicalStack<?> stack = chemicals.stored(slot);
        return stack.isEmpty() && chemicals.isDrawerLocked() ? chemicals.getFilter(slot) : stack;
    }

    @Override
    public void drawBackgroundLayer(GuiGraphics graphics, Screen screen, IAssetProvider provider,
                                    int guiX, int guiY, int mouseX, int mouseY, float partialTicks) {
        int x = guiX + getPosX();
        int y = guiY + getPosY();
        int slots = type.getSlots();
        BigChemicalHandler chemicals = handler.get();

        graphics.blit(INNER, x + 2, y + 2, TILE - 4, TILE - 4, 3f, 3f, 10, 10, 16, 16);
        for (int slot = 0; slot < slots; slot++) {
            ChemicalStack<?> stack = shown(chemicals, slot);
            if (!stack.isEmpty()) {
                renderChemical(graphics, x, y, stack, contentRect(slot, slots));
            }
        }

        graphics.blit(front, x, y, 0, 0, TILE, TILE, TILE, TILE);

        for (int slot = 0; slot < slots; slot++) {
            ChemicalStack<?> stack = chemicals.stored(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int textX = x + type.getSlotPosition().apply(slot).getLeft();
            int textY = y + type.getSlotPosition().apply(slot).getRight();
            String amount = ChemicalFormat.format(stack.getAmount()) + "/"
                    + ChemicalFormat.format(chemicals.capacity(slot));
            float scale = 0.5f;
            graphics.pose().translate(0, 0, 200);
            graphics.pose().scale(scale, scale, scale);
            graphics.drawString(Minecraft.getInstance().font, amount,
                    (int) ((textX + 17 - Minecraft.getInstance().font.width(amount) / 2f) * (1 / scale)),
                    (int) ((textY + 12) * (1 / scale)), 0xFFFFFF, true);
            graphics.pose().scale(1 / scale, 1 / scale, 1 / scale);
            graphics.pose().translate(0, 0, -200);
        }
    }

    @Override
    public void drawForegroundLayer(GuiGraphics graphics, Screen screen, IAssetProvider provider,
                                    int guiX, int guiY, int mouseX, int mouseY, float partialTicks) {
        int slots = type.getSlots();
        BigChemicalHandler chemicals = handler.get();
        for (int slot = 0; slot < slots; slot++) {
            Rect2i rect = hoverRect(slot, slots);
            int x = rect.getX() + getPosX() + guiX;
            int y = rect.getY() + getPosY() + guiY;
            if (mouseX <= x || mouseX >= x + rect.getWidth() || mouseY <= y || mouseY >= y + rect.getHeight()) {
                continue;
            }
            int localX = getPosX() + rect.getX();
            int localY = getPosY() + rect.getY();
            graphics.pose().translate(0, 0, 200);
            graphics.fill(localX, localY, localX + rect.getWidth(), localY + rect.getHeight(), -2130706433);
            graphics.pose().translate(0, 0, -200);

            var lines = new ArrayList<Component>();
            ChemicalStack<?> over = shown(chemicals, slot);
            if (over.isEmpty()) {
                lines.add(Component.translatable("gui.immaterialdrawers.chemical").withStyle(ChatFormatting.GOLD)
                        .append(Component.translatable("gui.functionalstorage.empty").withStyle(ChatFormatting.WHITE)));
            } else {
                lines.add(Component.translatable("gui.immaterialdrawers.chemical").withStyle(ChatFormatting.GOLD)
                        .append(over.getTextComponent().copy().withStyle(ChatFormatting.WHITE)));
                String amount = chemicals.stored(slot).getAmount() + " mB / "
                        + chemicals.capacity(slot) + " mB";
                lines.add(Component.translatable("gui.functionalstorage.amount").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(amount).withStyle(ChatFormatting.WHITE)));
            }
            lines.add(Component.translatable("gui.functionalstorage.slot").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(String.valueOf(slot)).withStyle(ChatFormatting.WHITE)));
            graphics.renderTooltip(Minecraft.getInstance().font, lines, Optional.empty(), mouseX - guiX, mouseY - guiY);
        }
    }

    /**
     * The chemical's own texture, tinted with its own colour, tiled over the slot - the way their
     * addon draws a fluid, and the way Mekanism draws a chemical in its own gauges.
     */
    private void renderChemical(GuiGraphics graphics, int x, int y, ChemicalStack<?> stack, Rect2i rect) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(stack.getType().getIcon());
        int tint = stack.getChemicalTint();
        RenderSystem.setShaderColor((tint >> 16 & 0xFF) / 255f, (tint >> 8 & 0xFF) / 255f, (tint & 0xFF) / 255f, 1f);
        RenderSystem.enableBlend();
        for (int dx = 0; dx < rect.getWidth(); dx += 16) {
            for (int dy = 0; dy < rect.getHeight(); dy += 16) {
                graphics.blit(x + rect.getX() + dx, y + rect.getY() + dy, 0,
                        Math.min(16, rect.getWidth() - dx), Math.min(16, rect.getHeight() - dy), sprite);
            }
        }
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }
}
