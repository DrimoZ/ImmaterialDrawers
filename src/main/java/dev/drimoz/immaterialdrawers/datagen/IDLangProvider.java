package dev.drimoz.immaterialdrawers.datagen;

import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.compat.Mods;
import dev.drimoz.immaterialdrawers.registry.IDChemicalContent;
import dev.drimoz.immaterialdrawers.registry.IDContent;
import dev.drimoz.immaterialdrawers.registry.IDSourceContent;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * en_us.
 *
 * <p>English only, and that is the whole set for now: translations belong on the wiki's terms, not
 * guessed here. The creative tab key is {@code itemGroup.<mod id>} because that is what Titanium's
 * {@code ModuleController.addCreativeTab} builds from the title it is given.
 */
public class IDLangProvider extends LanguageProvider {

    public IDLangProvider(PackOutput output) {
        super(output, ImmaterialDrawers.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + ImmaterialDrawers.MOD_ID, "Immaterial Drawers");
        // The noun that goes into Functional Storage's own upgrade sentence - see IDTooltips.
        add("storageupgrade.obj.immaterialdrawers.energy_storage", "energy storage");
        add("gui.immaterialdrawers.energy", "Energy: ");
        add("gui.immaterialdrawers.capacity", "Capacity: ");
        addItem(() -> IDContent.WIRELESS_CHARGER.get(), "Wireless Charger");
        add("augment.immaterialdrawers.wireless_charger.desc", "Charges what nearby players carry, within %s blocks");
        add("augment.immaterialdrawers.wireless_charger.energy_only", "Only does anything in an Energy Drawer");
        addBlock(() -> IDContent.ENERGY_DRAWER.getBlock(), "Energy Drawer");
        addBlock(() -> IDContent.FRAMED_ENERGY_DRAWER.getBlock(), "Framed Energy Drawer");

        // Named like Functional Storage's fluid drawers - "Fluid Drawer (1x2)" - so the two families
        // sort and read side by side. The keys exist only if the blocks do, and datagen runs with
        // Mekanism, so they are always generated; a pack without it simply never asks for them.
        add("gui.immaterialdrawers.chemical", "Chemical: ");
        add("gui.immaterialdrawers.source", "Source: ");
        if (Mods.arsNouveau()) {
            addBlock(() -> IDSourceContent.SOURCE_DRAWER.getBlock(), "Source Drawer");
            addBlock(() -> IDSourceContent.FRAMED_SOURCE_DRAWER.getBlock(), "Framed Source Drawer");
        }
        if (Mods.mekanism()) {
            IDChemicalContent.TYPES.forEach(type -> {
                String layout = " (" + type.getDisplayName() + ")";
                addBlock(() -> IDChemicalContent.drawer(type, false).getBlock(), "Chemical Drawer" + layout);
                addBlock(() -> IDChemicalContent.drawer(type, true).getBlock(), "Framed Chemical Drawer" + layout);
            });
        }
    }
}
