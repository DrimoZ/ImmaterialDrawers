package dev.drimoz.immaterialdrawers.client.gui;

import com.buuz135.functionalstorage.FunctionalStorage;
import dev.drimoz.immaterialdrawers.storage.BigEnergyStorage;
import dev.drimoz.immaterialdrawers.util.EnergyFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Supplier;

/**
 * The energy drawer's face in its own screen: the charge fills the window from the bottom, in the
 * colour of the cube's core. Layout, drawing order and number are {@link DrawerFaceGuiAddon}'s.
 *
 * <p>The front texture has a transparent window and goes on top of the fill. An earlier version had
 * an opaque front and painted the fill over it, which is why it looked like a slab of colour instead
 * of a drawer.
 */
public class EnergyDrawerInfoGuiAddon extends DrawerFaceGuiAddon {

    /** The core's colour in the drawer, so the screen and the block agree on what charge looks like. */
    private static final int FILL = 0xFFEE6A2C;

    private final Supplier<BigEnergyStorage> storage;

    public EnergyDrawerInfoGuiAddon(int posX, int posY, ResourceLocation front, Supplier<BigEnergyStorage> storage) {
        super(posX, posY, front, FunctionalStorage.DrawerType.X_1);
        this.storage = storage;
    }

    @Override
    protected void drawContents(GuiGraphics graphics, int slot, Rect2i area) {
        BigEnergyStorage energy = storage.get();
        if (energy.getCapacityLong() > 0 && energy.getStoredLong() > 0) {
            Rect2i fill = bottomPart(area, energy.getStoredLong() / (double) energy.getCapacityLong());
            graphics.fill(fill.getX(), fill.getY(), fill.getX() + fill.getWidth(), fill.getY() + fill.getHeight(), FILL);
        }
    }

    @Override
    protected String amount(int slot) {
        BigEnergyStorage energy = storage.get();
        return energy.getCapacityLong() > 0
                ? EnergyFormat.format(energy.getStoredLong()) + "/" + EnergyFormat.format(energy.getCapacityLong())
                : null;
    }

    @Override
    protected List<Component> tooltip(int slot) {
        BigEnergyStorage energy = storage.get();
        return List.of(
                Component.translatable("gui.immaterialdrawers.energy").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(EnergyFormat.format(energy.getStoredLong()) + " FE")
                                .withStyle(ChatFormatting.WHITE)),
                Component.translatable("gui.immaterialdrawers.capacity").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(EnergyFormat.format(energy.getCapacityLong()) + " FE")
                                .withStyle(ChatFormatting.WHITE)));
    }
}
