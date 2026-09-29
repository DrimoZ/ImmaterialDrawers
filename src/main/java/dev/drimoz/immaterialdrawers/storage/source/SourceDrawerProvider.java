package dev.drimoz.immaterialdrawers.storage.source;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.api.source.ISpecialSourceProvider;
import dev.drimoz.immaterialdrawers.block.tile.source.SourceDrawerTile;
import net.minecraft.core.BlockPos;

/**
 * A source drawer, as Ars Nouveau's {@code SourceManager} sees it.
 *
 * <p><b>Ars Nouveau only - see {@code compat.Mods}.</b>
 *
 * <p>This is the half of the integration the capability cannot do. Everything in Ars that consumes
 * or generates Source nearby - the enchanting apparatus, imbuement, the sourcelinks - goes through
 * {@code SourceUtil}, which finds Source in exactly two ways: by {@code instanceof} on Ars's own jar
 * class, which we can never pass, and through the providers registered with
 * {@code SourceManager.INSTANCE}, which anyone can join. It is the one extension point on that path,
 * and it is public API.
 *
 * <p>Ars cleans the registry itself: every 60 ticks it drops the providers that say they are no
 * longer {@link #isValid() valid}. So a provider has to answer truthfully for a drawer that was broken
 * or unloaded, and the drawer registers a new one each time it is loaded - see
 * {@code SourceDrawerTile#onLoad}.
 */
public record SourceDrawerProvider(SourceDrawerTile drawer) implements ISpecialSourceProvider {

    @Override
    public ISourceTile getSource() {
        return drawer.getSourceStorage().asTile();
    }

    /**
     * Still the block entity at its position, in a loaded chunk. Checking identity rather than
     * {@code isRemoved()} alone also retires the provider of a drawer that was replaced in place.
     */
    @Override
    public boolean isValid() {
        var level = drawer.getLevel();
        BlockPos pos = drawer.getBlockPos();
        return !drawer.isRemoved() && level != null && level.isLoaded(pos) && level.getBlockEntity(pos) == drawer;
    }

    @Override
    public BlockPos getCurrentPos() {
        return drawer.getBlockPos();
    }
}
