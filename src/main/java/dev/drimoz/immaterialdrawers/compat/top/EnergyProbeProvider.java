package dev.drimoz.immaterialdrawers.compat.top;

import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import dev.drimoz.immaterialdrawers.ImmaterialDrawers;
import dev.drimoz.immaterialdrawers.block.tile.energy.EnergyDrawerTile;
import dev.drimoz.immaterialdrawers.storage.ControllerEnergyStorage;
import mcjty.theoneprobe.api.IProbeConfig;
import mcjty.theoneprobe.api.IProbeConfigProvider;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeHitEntityData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.config.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
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
public class EnergyProbeProvider implements IProbeInfoProvider, IProbeConfigProvider {

    /**
     * What {@code IDTopPlugin} sends across InterModComms.
     *
     * <p>Registered twice, as two different things, and the second is not optional. TOP asks
     * <em>every</em> provider for its say — unlike Jade, which keeps only the most specific one — so
     * its own RF readout and ours both appeared, stacked. {@code IProbeConfigProvider} is TOP's
     * answer to exactly that: it lets a mod switch the default RF display off for the blocks it
     * draws itself.
     */
    public static final Function<ITheOneProbe, Void> REGISTER = probe -> {
        EnergyProbeProvider provider = new EnergyProbeProvider();
        probe.registerProvider(provider);
        probe.registerProbeConfigProvider(provider);
        return null;
    };

    /** {@code IProbeConfig}'s "do not show RF at all" mode. */
    private static final int RF_HIDDEN = 0;

    @Override
    public ResourceLocation getID() {
        return new ResourceLocation(ImmaterialDrawers.MOD_ID, "energy");
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
            var totals = ControllerEnergyStorage.of(controller).totals();
            stored = totals.stored();
            capacity = totals.capacity();
        } else {
            return;
        }

        // A controller with no energy drawers on it is not an energy block, and an empty bar on
        // every storage controller in the world would be noise.
        if (capacity <= 0) {
            return;
        }

        // TOP's own bar, exactly as it draws one for any other energy block: its default style, its
        // configured RF colours, its configured number format. The only thing of ours in here is
        // the pair of longs. A bespoke palette would make this block the odd one out on a HUD whose
        // whole job is consistency - and the colours are a player's config setting, not ours to
        // decide.
        probeInfo.progress(stored, capacity, probeInfo.defaultProgressStyle()
                .suffix("RF")
                .filledColor(Config.rfbarFilledColor)
                .alternateFilledColor(Config.rfbarAlternateFilledColor)
                .borderColor(Config.rfbarBorderColor)
                .numberFormat(Config.rfFormat.get()));
    }

    /**
     * Turns TOP's own RF readout off for the blocks we draw ourselves.
     *
     * <p>Without this there are two bars: theirs, read off the int capability and therefore wrong
     * above 2.1B, and ours. TOP collects from every registered provider rather than picking one, so
     * suppression has to be explicit — this is the hook it provides for it.
     */
    @Override
    public void getProbeConfig(IProbeConfig config, Player player, Level level, BlockState state,
                               IProbeHitData data) {
        BlockEntity be = level.getBlockEntity(data.getPos());
        if (be instanceof EnergyDrawerTile || be instanceof StorageControllerTile<?>) {
            config.setRFMode(RF_HIDDEN);
        }
    }

    /** Entities have no energy drawers on them. Required by the interface, nothing to say. */
    @Override
    public void getProbeConfig(IProbeConfig config, Player player, Level level, Entity entity,
                               IProbeHitEntityData data) {
    }
}
