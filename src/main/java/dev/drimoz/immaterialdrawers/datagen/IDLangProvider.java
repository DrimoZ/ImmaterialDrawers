package dev.drimoz.immaterialdrawers.datagen;

import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.registry.IDContent;
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
        addBlock(() -> IDContent.ENERGY_DRAWER.getBlock(), "Energy Drawer");
        addBlock(() -> IDContent.FRAMED_ENERGY_DRAWER.getBlock(), "Framed Energy Drawer");
    }
}
