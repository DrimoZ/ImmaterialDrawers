package dev.drimoz.immaterialdrawers.compat.jade;

import com.buuz135.functionalstorage.block.StorageControllerBlock;
import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.energy.EnergyDrawerBlock;
import dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile;
import dev.drimoz.immaterialdrawers.storage.ControllerEnergyStorage;
import dev.drimoz.immaterialdrawers.util.EnergyFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Shows the real amount of energy on the crosshair, rather than the amount an int can express.
 *
 * <p>Jade's built-in energy line reads {@code IEnergyStorage.getMaxEnergyStored()}, which is an int,
 * so anything past 2,147,483,647 FE reads as exactly 2.14G — two fully upgraded drawers on one
 * controller already exceed it. That is not Jade being wrong: it is the ceiling the standard
 * capability has, and the reason this mod keeps its amounts in a long (see {@code EnergyScaling}).
 *
 * <p>So the numbers travel as longs in Jade's own server-data packet instead of being read off the
 * capability. That is also the only correct way to do it for the controller, whose total is the sum
 * of a network the client does not necessarily have loaded.
 *
 * <p>Registered against Functional Storage's controller block as well as ours — a controller with
 * energy drawers on it is an energy block, whoever wrote it. Their own Jade provider handles items
 * and fluids and knows nothing about us; this adds a line, it does not replace theirs.
 */
@WailaPlugin
public class IDJadePlugin implements IWailaPlugin {

    /**
     * Jade identifies providers by this and lets a player switch them off individually, so it has
     * to be ours and it has to stay stable.
     */
    public static final ResourceLocation ENERGY =
            ResourceLocation.fromNamespaceAndPath(ImmaterialDrawers.MOD_ID, "energy");

    private static final String STORED = "IDEnergyStored";
    private static final String CAPACITY = "IDEnergyCapacity";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(EnergyData.INSTANCE, EnergyDrawerTile.class);
        registration.registerBlockDataProvider(EnergyData.INSTANCE, StorageControllerTile.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(EnergyComponent.INSTANCE, EnergyDrawerBlock.class);
        registration.registerBlockComponent(EnergyComponent.INSTANCE, StorageControllerBlock.class);
    }

    /** Server side: puts the two long values into the packet Jade sends for this block. */
    public enum EnergyData implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            BlockEntity be = accessor.getBlockEntity();
            if (be instanceof EnergyDrawerTile drawer) {
                data.putLong(STORED, drawer.getEnergyStorage().getStoredLong());
                data.putLong(CAPACITY, drawer.getEnergyStorage().getCapacityLong());
            } else if (be instanceof StorageControllerTile<?> controller) {
                ControllerEnergyStorage network = new ControllerEnergyStorage(controller);
                long capacity = network.getCapacityLong();
                // A controller with no energy drawers on it is not an energy block, and "0 FE" on
                // every storage controller in the world would be noise.
                if (capacity > 0) {
                    data.putLong(STORED, network.getStoredLong());
                    data.putLong(CAPACITY, capacity);
                }
            }
        }

        @Override
        public ResourceLocation getUid() {
            return ENERGY;
        }
    }

    /** Client side: one line, formatted the way the drawer formats its own face. */
    public enum EnergyComponent implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains(CAPACITY)) {
                return;
            }
            tooltip.add(Component.literal(
                            EnergyFormat.format(data.getLong(STORED))
                                    + " / " + EnergyFormat.format(data.getLong(CAPACITY)) + " FE")
                    .withStyle(ChatFormatting.GOLD));
        }

        @Override
        public ResourceLocation getUid() {
            return ENERGY;
        }
    }
}
