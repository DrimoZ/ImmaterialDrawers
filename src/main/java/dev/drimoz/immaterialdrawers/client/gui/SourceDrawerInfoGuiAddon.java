package dev.drimoz.immaterialdrawers.client.gui;

import com.buuz135.functionalstorage.FunctionalStorage;
import dev.drimoz.immaterialdrawers.compat.Mods;
import dev.drimoz.immaterialdrawers.storage.source.BigSourceStorage;
import dev.drimoz.immaterialdrawers.util.EnergyFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.List;
import java.util.function.Supplier;

/**
 * The Source drawer's face in its own screen: Ars's Source texture fills the window from the bottom.
 * Layout, drawing order and number are {@link DrawerFaceGuiAddon}'s.
 *
 * <p><b>Ars Nouveau only - see {@code compat.Mods}.</b>
 */
public class SourceDrawerInfoGuiAddon extends DrawerFaceGuiAddon {

    private static final ResourceLocation SOURCE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Mods.ARS_NOUVEAU, "block/mana_still");

    private final Supplier<BigSourceStorage> storage;

    public SourceDrawerInfoGuiAddon(int posX, int posY, ResourceLocation front, Supplier<BigSourceStorage> storage) {
        super(posX, posY, front, FunctionalStorage.DrawerType.X_1);
        this.storage = storage;
    }

    @Override
    protected void drawContents(GuiGraphics graphics, int slot, Rect2i area) {
        BigSourceStorage source = storage.get();
        if (source.getSourceCapacity() > 0 && source.getSource() > 0) {
            blitTiled(graphics, Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(SOURCE_TEXTURE),
                    bottomPart(area, source.getSource() / (double) source.getSourceCapacity()));
        }
    }

    @Override
    protected String amount(int slot) {
        BigSourceStorage source = storage.get();
        return source.getSourceCapacity() > 0
                ? EnergyFormat.format(source.getStoredRaw()) + "/" + EnergyFormat.format(source.getSourceCapacity())
                : null;
    }

    @Override
    protected List<Component> tooltip(int slot) {
        BigSourceStorage source = storage.get();
        return List.of(
                Component.translatable("gui.immaterialdrawers.source").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(EnergyFormat.format(source.getStoredRaw())).withStyle(ChatFormatting.WHITE)),
                Component.translatable("gui.immaterialdrawers.capacity").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(EnergyFormat.format(source.getSourceCapacity())).withStyle(ChatFormatting.WHITE)));
    }
}
