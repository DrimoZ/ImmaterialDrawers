package dev.drimoz.immaterialdrawers.compat.jade;

import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile;
import dev.drimoz.immaterialdrawers.storage.ControllerEnergyStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.Accessor;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.EnergyView;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.List;

/**
 * Puts the real amount of energy into the energy bar Jade already draws.
 *
 * <p>Jade's built-in energy bar reads {@code IEnergyStorage.getMaxEnergyStored()}, an int, so
 * anything past 2,147,483,647 FE reads as exactly 2.14G — two fully upgraded drawers on one
 * controller already exceed it. That is not Jade being wrong; it is the ceiling the standard
 * capability has, and the reason this mod keeps its amounts in a long (see {@code EnergyScaling}).
 *
 * <p><b>Its bar, not another line.</b> An earlier version registered a component provider and wrote
 * its own text, which gave two readouts of the same thing in two different styles. Jade's
 * {@code registerEnergyStorage} hook is the right one: it feeds the same widget, and
 * {@link EnergyView#of(long, long)} already takes longs — Jade was never the part that could not
 * count past an int.
 *
 * <p><b>Why this replaces their reading rather than adding to it.</b> Jade resolves one energy
 * provider per block, most specific first ({@code getServerExtensionData} returns a single entry out
 * of a hierarchy lookup). Registering against our tile classes therefore wins over the universal
 * capability-reading one, and the player sees one bar.
 *
 * <p>Registered for Functional Storage's controller as well as our drawers: a controller with energy
 * drawers linked to it is an energy block, whoever wrote it, and its total is the sum of a network
 * the client does not necessarily have loaded — so it has to be computed server side, which is
 * exactly what this hook is for.
 */
@WailaPlugin
public class IDJadePlugin implements IWailaPlugin {

    /** Jade keys providers by this, and lets a player switch them off individually. Keep it stable. */
    public static final ResourceLocation ENERGY =
            new ResourceLocation(ImmaterialDrawers.MOD_ID, "energy");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEnergyStorage(EnergyExtension.INSTANCE, EnergyDrawerTile.class);
        registration.registerEnergyStorage(EnergyExtension.INSTANCE, StorageControllerTile.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEnergyStorageClient(EnergyExtension.INSTANCE);
    }

    /**
     * Both halves of the same provider, the way Jade's own {@code EnergyStorageProvider.Extension}
     * does it — the client looks its provider up by the UID the server stamped on the data, so the
     * two sides have to be one object with one {@link #getUid()}.
     */
    public enum EnergyExtension
            implements IServerExtensionProvider<BlockEntity, CompoundTag>, IClientExtensionProvider<CompoundTag, EnergyView> {
        INSTANCE;

        /** Shown after the numbers in the bar. */
        private static final String UNIT = "FE";

        @Override
        public List<ViewGroup<CompoundTag>> getGroups(ServerPlayer player, ServerLevel level, BlockEntity be,
                                                      boolean showDetails) {
            // Jade 11 (1.20.1) hands the block entity directly; Jade 15 (1.21.1) hands an Accessor.

            long stored;
            long capacity;
            if (be instanceof EnergyDrawerTile drawer) {
                stored = drawer.getEnergyStorage().getStoredLong();
                capacity = drawer.getEnergyStorage().getCapacityLong();
            } else if (be instanceof StorageControllerTile<?> controller) {
                var totals = ControllerEnergyStorage.of(controller).totals();
                stored = totals.stored();
                capacity = totals.capacity();
            } else {
                return List.of();
            }

            // A controller with no energy drawers on it is not an energy block, and an empty bar on
            // every storage controller in the world would be noise.
            if (capacity <= 0) {
                return List.of();
            }
            return List.of(new ViewGroup<>(List.of(EnergyView.of(stored, capacity))));
        }

        @Override
        public List<ClientViewGroup<EnergyView>> getClientGroups(Accessor<?> accessor,
                                                                List<ViewGroup<CompoundTag>> groups) {
            return ClientViewGroup.map(groups, tag -> EnergyView.read(tag, UNIT), null);
        }

        @Override
        public ResourceLocation getUid() {
            return ENERGY;
        }
    }
}
