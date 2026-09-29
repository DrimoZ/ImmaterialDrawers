package dev.drimoz.immaterialdrawers.block.tile.source;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.tile.DrawerProperties;
import com.buuz135.functionalstorage.item.FSAttachments;
import com.buuz135.functionalstorage.item.component.SizeProvider;
import com.hollingsworth.arsnouveau.api.source.SourceManager;
import com.hrznstudio.titanium.annotation.Save;
import com.hrznstudio.titanium.block.BasicTileBlock;
import dev.drimoz.immaterialdrawers.IDConfig;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.tile.ImmaterialDrawerTile;
import dev.drimoz.immaterialdrawers.client.gui.SourceDrawerInfoGuiAddon;
import dev.drimoz.immaterialdrawers.storage.source.BigSourceStorage;
import dev.drimoz.immaterialdrawers.storage.source.SourceDrawerProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Supplier;

/**
 * A drawer that holds Ars Nouveau's Source.
 *
 * <p><b>Ars Nouveau only - see {@code compat.Mods}.</b>
 *
 * <p>The drawer half is {@link ImmaterialDrawerTile}. What is here is how Ars finds it, which takes
 * two separate doors because Ars has two separate ways of looking (see {@code BigSourceStorage}):
 * the {@code ars_nouveau:source} capability, registered on this type in {@code IDSourceContent}, for
 * relays and turrets; and Ars's {@code SourceManager}, joined in {@link #onLoad()}, for everything
 * that pulls Source from nearby - the enchanting apparatus, imbuement - and the sourcelinks that fill
 * it.
 *
 * <p><b>One slot, like the energy drawer.</b> Source has one kind of content; a 2- or 4-slot drawer
 * would have nothing to keep apart.
 *
 * <p><b>The fluid curve.</b> Functional Storage's {@code fluid_storage_modifier} and the 1x1 slot
 * amount, 32, times {@code SOURCE_PER_UNIT}: 32,000 Source unupgraded, 2,097,152,000 with four
 * Netherite upgrades - which fits Ars's {@code int} API, so every slot counts and a component of our
 * own would buy nothing.
 */
public class SourceDrawerTile extends ImmaterialDrawerTile<SourceDrawerTile> {

    public static final FunctionalStorage.DrawerType TYPE = FunctionalStorage.DrawerType.X_1;

    @Save
    public BigSourceStorage sourceStorage;

    public SourceDrawerTile(BasicTileBlock<SourceDrawerTile> base, BlockEntityType<SourceDrawerTile> entityType,
                            BlockPos pos, BlockState state) {
        super(base, entityType, pos, state, new DrawerProperties(TYPE.getSlotAmount(), FSAttachments.FLUID_STORAGE_MODIFIER));
        this.sourceStorage = new BigSourceStorage(capacityFor(getStorageMultiplier())) {
            @Override
            public void onChange() {
                syncObject(sourceStorage);
                updateComparators();
            }

            @Override
            public boolean isDrawerVoid() {
                return isVoid();
            }

            @Override
            public boolean isDrawerCreative() {
                return isCreative();
            }
        };
    }

    /** Source per multiplier, saturating at the int ceiling the Max Storage upgrade reaches. */
    public static int capacityFor(double storageMultiplier) {
        return (int) Math.min(Integer.MAX_VALUE, Math.floor(storageMultiplier * IDConfig.SOURCE_PER_UNIT));
    }

    /**
     * Joins Ars's {@code SourceManager} - server side only: Ars only ever cleans that registry from
     * the server tick, so a client entry would never leave.
     *
     * <p>Once per load, not once per drawer. A drawer in a chunk that unloads and loads again is a new
     * block entity with a new provider; the old one reports itself invalid and Ars drops it within 60
     * ticks.
     */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            SourceManager.INSTANCE.addInterface(level, new SourceDrawerProvider(this));
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void initClient() {
        super.initClient();
        addGuiAddonFactory(() -> new SourceDrawerInfoGuiAddon(64, 16,
                ResourceLocation.fromNamespaceAndPath(ImmaterialDrawers.MOD_ID, "textures/block/source_drawer_front.png"),
                this::getSourceStorage));
    }

    @Override
    public SourceDrawerTile getSelf() {
        return this;
    }

    public BigSourceStorage getSourceStorage() {
        return sourceStorage;
    }

    @Override
    protected Supplier<DataComponentType<SizeProvider>> storageModifier() {
        return FSAttachments.FLUID_STORAGE_MODIFIER;
    }

    @Override
    protected void onStorageMultiplierChanged() {
        this.sourceStorage.setCapacity(capacityFor(getStorageMultiplier()));
        syncObject(this.sourceStorage);
    }

    @Override
    protected boolean canChangeMultiplier(double newSizeMultiplier) {
        return sourceStorage.getStoredRaw() <= capacityFor(newSizeMultiplier);
    }

    @Override
    protected boolean hasContents() {
        return sourceStorage.getStoredRaw() > 0;
    }

    /** One kind of content, so nothing to lock to - the energy drawer's reasoning. */
    @Override
    public void setLocked(boolean locked) {
        super.setLocked(locked);
    }
}
