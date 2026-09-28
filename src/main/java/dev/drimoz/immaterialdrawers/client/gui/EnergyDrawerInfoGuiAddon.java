package dev.drimoz.immaterialdrawers.client.gui;

import com.hrznstudio.titanium.client.screen.addon.BasicScreenAddon;
import com.hrznstudio.titanium.client.screen.asset.IAssetProvider;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.storage.BigEnergyStorage;
import dev.drimoz.immaterialdrawers.util.EnergyFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The drawer's face in its own screen, laid out exactly like the fluid drawer's.
 *
 * <p>Adapted from Functional Storage's {@code client/gui/FluidDrawerInfoGuiAddon}.
 * Copyright (c) 2021 Buuz135, Rid — MIT. See NOTICE.
 *
 * <p><b>The order matters, and it is theirs.</b> The contents go down first, into a 30x30 well inset
 * in the tile; the drawer front is blitted over them; the front texture has a transparent window, so
 * what is behind shows through it. An earlier version had an opaque front and painted the fill on
 * top of it, which is why it looked like a slab of colour instead of a drawer.
 *
 * <p>Sizes, text position and hover rectangle are theirs too, so an energy drawer's screen and a
 * fluid drawer's screen line up pixel for pixel.
 */
public class EnergyDrawerInfoGuiAddon extends BasicScreenAddon {

    /** 16px of front, doubled, plus its border — the size their tile is blitted at. */
    private static final int TILE = 48;

    /** Their single-slot content well: inset 9, 30 across. */
    private static final Rect2i WELL = new Rect2i(9, 9, 30, 30);

    /** The core's colour in the drawer, so the screen and the block agree on what charge looks like. */
    private static final int FILL = 0xFFEE6A2C;

    private static final ResourceLocation INNER = ResourceLocation.fromNamespaceAndPath(
            ImmaterialDrawers.MOD_ID, "textures/block/energy_drawer_inner.png");

    private final ResourceLocation front;
    private final Supplier<BigEnergyStorage> storage;

    public EnergyDrawerInfoGuiAddon(int posX, int posY, ResourceLocation front, Supplier<BigEnergyStorage> storage) {
        super(posX, posY);
        this.front = front;
        this.storage = storage;
    }

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
        BigEnergyStorage energy = storage.get();
        long capacity = energy.getCapacityLong();

        // The tank walls first, so an empty drawer shows an empty drawer rather than the screen
        // background through its window. The window is texels 3..13 of the 16px texture.
        graphics.blit(INNER, x + WELL.getX(), y + WELL.getY(), WELL.getWidth(), WELL.getHeight(),
                3f, 3f, 10, 10, 16, 16);

        // Behind the front, filling from the bottom.
        if (capacity > 0 && energy.getStoredLong() > 0) {
            double ratio = Math.min(1d, energy.getStoredLong() / (double) capacity);
            int filled = Math.max(1, (int) Math.round(WELL.getHeight() * ratio));
            int wellX = x + WELL.getX();
            int wellBottom = y + WELL.getY() + WELL.getHeight();
            graphics.fill(wellX, wellBottom - filled, wellX + WELL.getWidth(), wellBottom, FILL);
        }

        graphics.blit(front, x, y, 0, 0, TILE, TILE, TILE, TILE);

        if (capacity > 0) {
            String amount = EnergyFormat.format(energy.getStoredLong())
                    + "/" + EnergyFormat.format(capacity);
            // Half scale and their offsets, so the number sits where a fluid drawer's does.
            float scale = 0.5f;
            graphics.pose().translate(0, 0, 200);
            graphics.pose().scale(scale, scale, scale);
            graphics.drawString(Minecraft.getInstance().font, amount,
                    (int) ((x + 17 - Minecraft.getInstance().font.width(amount) / 2f) * (1 / scale)),
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

        // Their highlight, their colour: a drawer lights up the slot you are pointing at.
        int localX = getPosX() + WELL.getX();
        int localY = getPosY() + WELL.getY();
        graphics.pose().translate(0, 0, 200);
        graphics.fill(localX, localY, localX + WELL.getWidth(), localY + WELL.getHeight(), -2130706433);
        graphics.pose().translate(0, 0, -200);

        BigEnergyStorage energy = storage.get();
        var lines = new ArrayList<Component>();
        lines.add(Component.translatable("gui.immaterialdrawers.energy").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(EnergyFormat.format(energy.getStoredLong()) + " FE")
                        .withStyle(ChatFormatting.WHITE)));
        lines.add(Component.translatable("gui.immaterialdrawers.capacity").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(EnergyFormat.format(energy.getCapacityLong()) + " FE")
                        .withStyle(ChatFormatting.WHITE)));
        graphics.renderTooltip(Minecraft.getInstance().font, lines, Optional.empty(), mouseX - guiX, mouseY - guiY);
    }
}
