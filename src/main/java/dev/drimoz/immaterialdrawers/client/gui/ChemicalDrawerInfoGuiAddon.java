package dev.drimoz.immaterialdrawers.client.gui;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.drimoz.immaterialdrawers.storage.chemical.BigChemicalHandler;
import dev.drimoz.immaterialdrawers.util.ChemicalFormat;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The chemical drawer's face in its own screen, in all three layouts: each slot shows its chemical,
 * or the chemical a locked slot is pinned to. Layout, drawing order and number are
 * {@link DrawerFaceGuiAddon}'s; the tooltip is the fluid drawer's, line for line.
 *
 * <p><b>Mekanism only - see {@code compat.Mods}.</b>
 */
public class ChemicalDrawerInfoGuiAddon extends DrawerFaceGuiAddon {

    private final Supplier<BigChemicalHandler> handler;

    public ChemicalDrawerInfoGuiAddon(int posX, int posY, ResourceLocation front, FunctionalStorage.DrawerType type,
                                      Supplier<BigChemicalHandler> handler) {
        super(posX, posY, front, type);
        this.handler = handler;
    }

    /** What a slot shows: its contents, or the chemical a locked slot is pinned to. */
    private ChemicalStack shown(int slot) {
        BigChemicalHandler chemicals = handler.get();
        ChemicalStack stack = chemicals.getChemicalInTank(slot);
        return stack.isEmpty() && chemicals.isDrawerLocked() ? chemicals.getFilter(slot) : stack;
    }

    /**
     * The chemical's own texture, tinted with its own colour, over the whole slot - the way their
     * addon draws a fluid, and the way Mekanism draws a chemical in its own gauges.
     */
    @Override
    protected void drawContents(GuiGraphics graphics, int slot, Rect2i area) {
        ChemicalStack stack = shown(slot);
        if (stack.isEmpty()) {
            return;
        }
        int tint = stack.getChemicalTint();
        RenderSystem.setShaderColor((tint >> 16 & 0xFF) / 255f, (tint >> 8 & 0xFF) / 255f, (tint & 0xFF) / 255f, 1f);
        RenderSystem.enableBlend();
        blitTiled(graphics, Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(stack.getChemical().getIcon()), area);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    @Override
    protected String amount(int slot) {
        ChemicalStack stack = handler.get().getChemicalInTank(slot);
        return stack.isEmpty() ? null : ChemicalFormat.format(stack.getAmount()) + "/"
                + ChemicalFormat.format(handler.get().getChemicalTankCapacity(slot));
    }

    @Override
    protected List<Component> tooltip(int slot) {
        BigChemicalHandler chemicals = handler.get();
        var lines = new ArrayList<Component>();
        ChemicalStack over = shown(slot);
        if (over.isEmpty()) {
            lines.add(Component.translatable("gui.immaterialdrawers.chemical").withStyle(ChatFormatting.GOLD)
                    .append(Component.translatable("gui.functionalstorage.empty").withStyle(ChatFormatting.WHITE)));
        } else {
            lines.add(Component.translatable("gui.immaterialdrawers.chemical").withStyle(ChatFormatting.GOLD)
                    .append(over.getTextComponent().copy().withStyle(ChatFormatting.WHITE)));
            String amount = chemicals.getChemicalInTank(slot).getAmount() + " mB / "
                    + chemicals.getChemicalTankCapacity(slot) + " mB";
            lines.add(Component.translatable("gui.functionalstorage.amount").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(amount).withStyle(ChatFormatting.WHITE)));
        }
        lines.add(Component.translatable("gui.functionalstorage.slot").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(String.valueOf(slot)).withStyle(ChatFormatting.WHITE)));
        return lines;
    }
}
