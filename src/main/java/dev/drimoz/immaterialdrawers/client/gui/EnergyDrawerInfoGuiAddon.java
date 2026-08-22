package dev.drimoz.immaterialdrawers.client.gui;

import dev.drimoz.immaterialdrawers.util.EnergyFormat;
import com.hrznstudio.titanium.client.screen.addon.BasicScreenAddon;
import com.hrznstudio.titanium.client.screen.asset.IAssetProvider;
import dev.drimoz.immaterialdrawers.storage.BigEnergyStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The drawer's face, in the drawer's screen, with the charge drawn on it.
 *
 * <p>Modelled on Functional Storage's {@code client/gui/FluidDrawerInfoGuiAddon}.
 * Copyright (c) 2021 Buuz135, Rid — MIT. See NOTICE.
 *
 * <p>Their fluid drawer shows the drawer front as a 48x48 tile with the contents behind it and the
 * amount written across it, and hovering names what is inside. This does the same with a fill
 * instead of a fluid texture, because that is the layout a player already knows from every other
 * drawer they own.
 *
 * <p>An earlier version used Titanium's {@code EnergyBarScreenAddon}, which works and is one line.
 * It also looks like a machine, not like a drawer, and a wall of drawers should not have one block
 * whose screen came from somewhere else.
 */
public class EnergyDrawerInfoGuiAddon extends BasicScreenAddon {

    /** Matches the 48x48 the fluid drawer uses: one 16px tile of front, doubled, plus its border. */
    private static final int SIZE = 48;

    /** Inset of the drawn fill from the edge of the tile, in screen pixels. */
    private static final int INSET = 9;

    private final ResourceLocation front;
    private final Supplier<BigEnergyStorage> storage;

    public EnergyDrawerInfoGuiAddon(int posX, int posY, ResourceLocation front, Supplier<BigEnergyStorage> storage) {
        super(posX, posY);
        this.front = front;
        this.storage = storage;
    }

    @Override
    public int getXSize() {
        return SIZE;
    }

    @Override
    public int getYSize() {
        return SIZE;
    }

    @Override
    public void drawBackgroundLayer(GuiGraphics graphics, Screen screen, IAssetProvider provider,
                                    int guiX, int guiY, int mouseX, int mouseY, float partialTicks) {
        int x = guiX + getPosX();
        int y = guiY + getPosY();
        var energy = storage.get();
        long capacity = energy.getCapacityLong();

        graphics.blit(front, x, y, 0, 0, SIZE, SIZE, SIZE, SIZE);

        // On top of the front, not behind it. The fluid drawer draws its contents behind because its
        // front texture has a hole cut in the middle; ours is a flat placeholder with no alpha, so a
        // fill drawn underneath is a fill nobody ever sees. Painted into the recessed panel, so it
        // still reads as being inside the drawer rather than across its face.
        if (capacity > 0 && energy.getStoredLong() > 0) {
            int inner = SIZE - INSET * 2;
            int filled = Math.max(1, (int) Math.round(inner * Math.min(1d, energy.getStoredLong() / (double) capacity)));
            graphics.fill(x + INSET, y + INSET + (inner - filled), x + INSET + inner, y + INSET + inner, 0xFFC4764A);
        }

        if (capacity > 0) {
            String amount = EnergyFormat.format(energy.getStoredLong()) + "/" + EnergyFormat.format(capacity);
            // Half scale, like the fluid drawer's amount, so a nine-digit number still fits.
            float scale = 0.5f;
            graphics.pose().translate(0, 0, 200);
            graphics.pose().scale(scale, scale, scale);
            graphics.drawString(Minecraft.getInstance().font, amount,
                    (int) ((x + SIZE / 2f - Minecraft.getInstance().font.width(amount) / 4f) * (1 / scale)),
                    (int) ((y + SIZE - 10) * (1 / scale)), 0xFFFFFF, true);
            graphics.pose().scale(1 / scale, 1 / scale, 1 / scale);
            graphics.pose().translate(0, 0, -200);
        }
    }

    @Override
    public void drawForegroundLayer(GuiGraphics graphics, Screen screen, IAssetProvider provider,
                                    int guiX, int guiY, int mouseX, int mouseY, float partialTicks) {
        int x = guiX + getPosX();
        int y = guiY + getPosY();
        if (mouseX < x || mouseX > x + SIZE || mouseY < y || mouseY > y + SIZE) {
            return;
        }

        var energy = storage.get();
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
