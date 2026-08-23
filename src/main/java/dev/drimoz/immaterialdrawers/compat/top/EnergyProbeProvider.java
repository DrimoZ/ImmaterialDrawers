package dev.drimoz.immaterialdrawers.compat.top;

import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile;
import dev.drimoz.immaterialdrawers.storage.ControllerEnergyStorage;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.apiimpl.styles.ProgressStyle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Function;

/**
 * The One Probe's half of CLAUDE.md §11bis, and the twin of {@code compat/jade/IDJadePlugin}.
 *
 * <p>Same problem, same answer, different API: a probe reading
 * {@code IEnergyStorage.getMaxEnergyStored()} sees an int and stops at 2.14G, so the numbers come
 * from {@code getStoredLong()} instead. TOP's {@code progress} takes longs, so the bar it already
 * draws is the right place to put them — no extra line of our own, for the same reason Jade got
 * none.
 *
 * <p>Both the drawer and Functional Storage's controller are handled here: a controller with energy
 * drawers linked to it is an energy block, and its total is a sum over a network. This provider runs
 * server-side inside TOP's own request, so walking that network is safe.
 *
 * <p>Deliberately thin. Everything it knows comes from {@code BigEnergyStorage} and
 * {@code ControllerEnergyStorage}; nothing is recomputed here, which is the rule §11bis sets for
 * every compat layer.
 */
public class EnergyProbeProvider implements IProbeInfoProvider {

    /** What {@code IDTopPlugin} sends across InterModComms. */
    public static final Function<ITheOneProbe, Void> REGISTER = probe -> {
        probe.registerProvider(new EnergyProbeProvider());
        return null;
    };

    private static final int FILLED = 0xFFC4764A;
    private static final int ALTERNATE = 0xFF8E5334;
    private static final int BACKGROUND = 0xFF2A2D30;

    @Override
    public ResourceLocation getID() {
        return ResourceLocation.fromNamespaceAndPath(ImmaterialDrawers.MOD_ID, "energy");
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, Player player, Level level,
                             BlockState state, IProbeHitData data) {
        BlockEntity be = level.getBlockEntity(data.getPos());

        long stored;
        long capacity;
        if (be instanceof EnergyDrawerTile drawer) {
            stored = drawer.getEnergyStorage().getStoredLong();
            capacity = drawer.getEnergyStorage().getCapacityLong();
        } else if (be instanceof StorageControllerTile<?> controller) {
            ControllerEnergyStorage network = new ControllerEnergyStorage(controller);
            stored = network.getStoredLong();
            capacity = network.getCapacityLong();
        } else {
            return;
        }

        // A controller with no energy drawers on it is not an energy block, and an empty bar on
        // every storage controller in the world would be noise.
        if (capacity <= 0) {
            return;
        }

        probeInfo.progress(stored, capacity, new ProgressStyle()
                .suffix(" FE")
                .filledColor(FILLED)
                .alternateFilledColor(ALTERNATE)
                .backgroundColor(BACKGROUND));
    }
}
