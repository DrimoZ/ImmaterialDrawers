package dev.drimoz.immaterialdrawers.datagen;

import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.compat.Mods;
import dev.drimoz.immaterialdrawers.registry.IDContent;
import dev.drimoz.immaterialdrawers.registry.IDSourceContent;
import dev.drimoz.immaterialdrawers.registry.IDChemicalContent;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

/** en_us. Keys grow with each backport step; the 1.21.1 branch has the full set. */
public class IDLangProvider extends LanguageProvider {

    public IDLangProvider(PackOutput output) {
        super(output, ImmaterialDrawers.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + ImmaterialDrawers.MOD_ID, "Immaterial Drawers");
        add("gui.immaterialdrawers.energy", "Energy: ");
        add("gui.immaterialdrawers.capacity", "Capacity: ");
        addBlock(IDContent.ENERGY_DRAWER.getLeft(), "Energy Drawer");
        addBlock(IDContent.FRAMED_ENERGY_DRAWER.getLeft(), "Framed Energy Drawer");
        add("gui.immaterialdrawers.source", "Source: ");
        // Datagen runs with Ars, so the keys are always generated; a pack without it never asks for them.
        if (Mods.arsNouveau()) {
            addBlock(IDSourceContent.SOURCE_DRAWER.getLeft(), "Source Drawer");
            addBlock(IDSourceContent.FRAMED_SOURCE_DRAWER.getLeft(), "Framed Source Drawer");
        }
        add("gui.immaterialdrawers.chemical", "Chemical: ");
        // Named like Functional Storage's fluid drawers, "Fluid Drawer (1x2)", so the families read side by side.
        if (Mods.mekanism()) {
            IDChemicalContent.TYPES.forEach(type -> {
                String layout = " (" + type.getDisplayName() + ")";
                addBlock(IDChemicalContent.drawer(type, false).getLeft(), "Chemical Drawer" + layout);
                addBlock(IDChemicalContent.drawer(type, true).getLeft(), "Framed Chemical Drawer" + layout);
            });
        }
        addItem(IDContent.WIRELESS_CHARGER, "Wireless Charger");
        add("augment.immaterialdrawers.wireless_charger.desc", "Charges what nearby players carry, within %s blocks");
        add("augment.immaterialdrawers.wireless_charger.energy_only", "Only does anything in an Energy Drawer");
    }
}
