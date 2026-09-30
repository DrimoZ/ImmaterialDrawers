package dev.drimoz.immaterialdrawers.block.tile.source;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.config.FunctionalStorageConfig;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * A drawer that holds Ars Nouveau's Source. <b>Ars Nouveau only</b> - see {@code compat.Mods}.
 *
 * <p>How Ars finds it on 1.20.1 (Ars 4.12): every Ars consumer that looks around itself for Source -
 * the enchanting apparatus, the imbuement chamber, sourcelinks filling it - goes through
 * {@code SourceManager}, which this tile joins on load. <b>Relays do not</b>: in 4.12 a relay moves
 * Source only between {@code AbstractSourceMachine}s, an Ars block entity class a Functional Storage
 * drawer cannot extend. 1.21.1's relays use a capability instead, which is why they work there.
 *
 * <p>Capacity follows Functional Storage's fluid curve: 32 units, divided by their fluid divisor per
 * upgrade, times {@code SOURCE_PER_UNIT}. Four Netherite upgrades fit under the int ceiling of Ars's
 * API, as on 1.21.1.
 */
public class SourceDrawerTile extends ImmaterialDrawerTile<SourceDrawerTile> {

    public static final FunctionalStorage.DrawerType TYPE = FunctionalStorage.DrawerType.X_1;

    /** 1.20.1's {@code getSlotAmount()} counts items (32 stacks of 64); the base is in units, as for fluids. */
    private static final int BASE_UNITS = TYPE.getSlotAmount() / 64;

    @Save
    public BigSourceStorage sourceStorage;

    public SourceDrawerTile(BasicTileBlock<SourceDrawerTile> base, BlockEntityType<SourceDrawerTile> entityType,
                            BlockPos pos, BlockState state) {
        super(base, entityType, pos, state);
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

    public static int capacityFor(int storageMultiplier) {
        return (int) Math.min(Integer.MAX_VALUE, (long) BASE_UNITS * storageMultiplier * IDConfig.SOURCE_PER_UNIT);
    }

    /** The fluid divisor: Source follows the fluid curve, and fits the int ceiling with it. */
    @Override
    public double getStorageDiv() {
        return FunctionalStorageConfig.FLUID_DIVISOR;
    }

    /**
     * Joins Ars's registry of Source containers. Ars removes providers that report invalid on its own
     * sweep, so a broken drawer leaves by itself.
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
                new ResourceLocation(ImmaterialDrawers.MOD_ID, "textures/block/source_drawer_front.png"),
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
    protected void onStorageMultiplierChanged() {
        this.sourceStorage.setCapacity(capacityFor(getStorageMultiplier()));
        syncObject(this.sourceStorage);
    }

    @Override
    protected boolean canChangeMultiplier(int newStorageMultiplier) {
        return sourceStorage.getStoredRaw() <= capacityFor(newStorageMultiplier);
    }

    @Override
    protected boolean hasContents() {
        return sourceStorage.getStoredRaw() > 0;
    }
}
