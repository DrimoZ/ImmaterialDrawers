package dev.drimoz.immaterialdrawers.storage;

import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The drawers of one kind on a Storage Controller's network, read from its public list of linked
 * positions - which is all Functional Storage gives us, see CLAUDE.md §7. Shared by every
 * controller-side aggregate of ours.
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
        return controller.getConnectedDrawers().getConnectedDrawers().stream()
                .map(BlockPos::of).filter(level::isLoaded).map(level::getBlockEntity)
                .filter(type::isInstance).map(type::cast).toList();
    }
}
