package dev.drimoz.immaterialdrawers.datagen;

import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.registry.IDContent;
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
        addItem(IDContent.WIRELESS_CHARGER, "Wireless Charger");
        add("augment.immaterialdrawers.wireless_charger.desc", "Charges what nearby players carry, within %s blocks");
        add("augment.immaterialdrawers.wireless_charger.energy_only", "Only does anything in an Energy Drawer");
    }
}
