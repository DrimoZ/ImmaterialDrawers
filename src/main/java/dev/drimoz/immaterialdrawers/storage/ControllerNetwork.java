package dev.drimoz.immaterialdrawers.storage;

import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * The drawers of one kind on a Storage Controller's network, read from its public list of linked
 * positions - which is all Functional Storage gives us, see CLAUDE.md §7. Shared by every
 * controller-side aggregate of ours (energy, Source, chemicals).
 */
public final class ControllerNetwork {

    private ControllerNetwork() {
    }

    /**
     * Loaded drawers of {@code type}, in network order - priority, then distance, the order the
     * controller itself keeps. A drawer in an unloaded chunk is skipped rather than loaded.
     */
    public static <T> List<T> drawersOf(StorageControllerTile<?> controller, Class<T> type) {
        Level level = controller.getLevel();
        if (level == null) {
            return List.of();
        }
        List<T> found = new ArrayList<>();
        for (Long packed : controller.getConnectedDrawers().getConnectedDrawers()) {
            BlockPos pos = BlockPos.of(packed);
            if (level.isLoaded(pos)) {
                BlockEntity drawer = level.getBlockEntity(pos);
                if (type.isInstance(drawer)) {
                    found.add(type.cast(drawer));
                }
            }
        }
        return found;
    }
}
